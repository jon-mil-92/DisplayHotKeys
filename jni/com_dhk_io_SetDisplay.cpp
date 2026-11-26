/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
#include "com_dhk_io_SetDisplay.h"
#include "ArrangeDisplay.h"
#include "DisplayConfig.h"
#include <jni.h>

using namespace std;

static vector<DisplaySettingsRequest> readDisplaySettingsRequests(JNIEnv *env, jobjectArray displayIds,
                                                                  jintArray displaySettings);
static int resolveDisplayIndex(const string &stableId);
static bool applyMergedDisplaySettings(const vector<DisplaySettingsRequest> &requests);
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation);
static DISPLAYCONFIG_ROTATION toRotationValue(UINT32 orientation);
bool setDisplayMode(const string &stableId, UINT32 displayIndex, UINT32 width, UINT32 height, UINT32 refreshNumerator,
                    UINT32 refreshDenominator);
static bool applyLargestSelectableMode(const WCHAR *gdiDeviceName, UINT32 excludeWidth, UINT32 excludeHeight);
static bool ccdApplySourceMode(const string &stableId, UINT32 width, UINT32 height, UINT32 refreshNumerator,
                               UINT32 refreshDenominator);
static void waitForCcdSourceModeResolution(UINT32 displayIndex, UINT32 width, UINT32 height);
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode);
static DISPLAYCONFIG_SCALING toScalingValue(UINT32 scalingMode);
static LONG applyDisplayConfig(const DisplayConfig &config);
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage);

/**
 * Flags for applying a supplied CCD configuration and persisting it to the Windows display database.
 */
static const UINT32 SDC_SUPPLIED_APPLY_FLAGS = SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE;

/**
 * Index of the horizontal resolution in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_WIDTH = com_dhk_io_SetDisplay_DISPLAY_SETTING_WIDTH;

/**
 * Index of the vertical resolution in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_HEIGHT = com_dhk_io_SetDisplay_DISPLAY_SETTING_HEIGHT;

/**
 * Index of the refresh rate's numerator in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_REFRESH_NUMERATOR = com_dhk_io_SetDisplay_DISPLAY_SETTING_REFRESH_NUMERATOR;

/**
 * Index of the refresh rate's denominator in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_REFRESH_DENOMINATOR = com_dhk_io_SetDisplay_DISPLAY_SETTING_REFRESH_DENOMINATOR;

/**
 * Index of the DPI scale percentage in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_DPI_SCALE_PERCENTAGE = com_dhk_io_SetDisplay_DISPLAY_SETTING_DPI_SCALE_PERCENTAGE;

/**
 * Index of the scaling mode in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_SCALING_MODE = com_dhk_io_SetDisplay_DISPLAY_SETTING_SCALING_MODE;

/**
 * Index of the orientation in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_ORIENTATION = com_dhk_io_SetDisplay_DISPLAY_SETTING_ORIENTATION;

/**
 * Number of values in each display's settings record.
 */
static constexpr int DISPLAY_SETTING_COUNT = com_dhk_io_SetDisplay_DISPLAY_SETTING_COUNT;

/**
 * Applies display settings (orientation, resolution, refresh rate, scaling mode, and DPI scale percentage) to one or
 * more displays. Their modes are applied in one reconfiguration when Windows accepts it, and one display at a time
 * otherwise.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param displayIds
 *            - The stable display IDs of the displays to modify
 * @param displaySettings
 *            - The settings of each display, DISPLAY_SETTING_COUNT values per display in displayIds order
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setDisplays(JNIEnv *env, jobject obj, jobjectArray displayIds,
                                                              jintArray displaySettings) {
    (void) obj;
    vector<DisplaySettingsRequest> requests = readDisplaySettingsRequests(env, displayIds, displaySettings);

    bool settingsMerged = applyMergedDisplaySettings(requests);

    // Without a merged apply, every display is rotated first, so each mode is applied to its new orientation
    if (!settingsMerged) {
        for (const DisplaySettingsRequest &request : requests) {
            setDisplayOrientation(request.displayIndex, request.orientation);
        }
    }

    for (const DisplaySettingsRequest &request : requests) {
        // Without a merged apply, each display's mode is applied on its own
        bool modeApplied =
            settingsMerged || setDisplayMode(request.stableId, request.displayIndex, request.width, request.height,
                                             request.refreshRate.Numerator, request.refreshRate.Denominator);

        // Settle, so the scaling re-apply does not revert the resolution with a stale source mode
        if (modeApplied) {
            waitForCcdSourceModeResolution(request.displayIndex, request.width, request.height);
        }

        /*
         * The scaling mode is applied before the DPI scale percentage, because the scaling re-apply submits a full
         * display configuration and the DPI scale must be set last against the settled resolution
         */
        setDisplayScalingMode(request.displayIndex, request.scalingMode);

        setDpiScalePercentage(request.displayIndex, request.dpiScalePercentage);
    }
}

/**
 * Reflows the multi-display arrangement against the snapshot from GetDisplay's captureDisplayArrangement, forwarding to
 * ArrangeDisplay so every display keeps its relative position and alignment after a batch of display changes.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param snapshot
 *            - The arrangement snapshot as "id|x|y|width|height" strings, one per display
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_preserveDisplayArrangement(JNIEnv *env, jobject obj,
                                                                             jobjectArray snapshot) {
    (void) obj;
    preserveDisplayArrangement(env, snapshot);
}

/**
 * Reads the requested settings of each display from the display IDs and their flat settings records, skipping a display
 * that is no longer in the persisted configuration.
 *
 * @param env
 *            - The JNI environment pointer
 * @param displayIds
 *            - The stable display IDs of the displays to modify
 * @param displaySettings
 *            - The settings of each display, DISPLAY_SETTING_COUNT values per display in displayIds order
 *
 * @return The requested settings of each display that can be modified, in displayIds order
 */
static vector<DisplaySettingsRequest> readDisplaySettingsRequests(JNIEnv *env, jobjectArray displayIds,
                                                                  jintArray displaySettings) {
    vector<DisplaySettingsRequest> requests;

    if (displayIds == nullptr || displaySettings == nullptr) {
        return requests;
    }

    jsize displayCount = env->GetArrayLength(displayIds);

    if (env->GetArrayLength(displaySettings) != displayCount * DISPLAY_SETTING_COUNT) {
        return requests;
    }

    vector<jint> settings((size_t) displayCount * DISPLAY_SETTING_COUNT);
    env->GetIntArrayRegion(displaySettings, 0, (jsize) settings.size(), settings.data());

    for (jsize requestIndex = 0; requestIndex < displayCount; requestIndex++) {
        jstring displayId = (jstring) env->GetObjectArrayElement(displayIds, requestIndex);

        if (displayId == nullptr) {
            continue;
        }

        const char *displayIdChars = env->GetStringUTFChars(displayId, nullptr);

        if (displayIdChars == nullptr) {
            env->DeleteLocalRef(displayId);
            continue;
        }

        string stableId = displayIdChars;
        env->ReleaseStringUTFChars(displayId, displayIdChars);
        env->DeleteLocalRef(displayId);

        int displayIndex = resolveDisplayIndex(stableId);

        // A display that disappeared since the caller checked is skipped, never resolved to another display
        if (displayIndex < 0) {
            continue;
        }

        const jint *record = settings.data() + (size_t) requestIndex * DISPLAY_SETTING_COUNT;

        DisplaySettingsRequest request = {};
        request.stableId = stableId;
        request.displayIndex = (UINT32) displayIndex;
        request.width = (UINT32) record[DISPLAY_SETTING_WIDTH];
        request.height = (UINT32) record[DISPLAY_SETTING_HEIGHT];
        request.refreshRate.Numerator = (UINT32) record[DISPLAY_SETTING_REFRESH_NUMERATOR];
        request.refreshRate.Denominator = (UINT32) record[DISPLAY_SETTING_REFRESH_DENOMINATOR];
        request.dpiScalePercentage = (int32_t) record[DISPLAY_SETTING_DPI_SCALE_PERCENTAGE];
        request.scalingMode = (UINT32) record[DISPLAY_SETTING_SCALING_MODE];
        request.orientation = (UINT32) record[DISPLAY_SETTING_ORIENTATION];

        requests.push_back(request);
    }

    return requests;
}

/**
 * Resolves a stable display ID to the index of its path in the persisted configuration (QDC_DATABASE_CURRENT), which
 * the orientation, settle wait, scaling, and DPI steps index into.
 *
 * @param stableId
 *            - The stable display ID to resolve
 *
 * @return The path index, or -1 if the display is not in the persisted configuration
 */
static int resolveDisplayIndex(const string &stableId) {
    return findPathForDisplay(getDisplayConfig(), stableId);
}

/**
 * Applies every display's orientation, mode, and scaling in one strict reconfiguration, sparing the separate rotation,
 * a reconfiguration per display, and the scaling re-apply. Every other display keeps its persisted scaling, and
 * displays that cannot be merged are left to the per-display apply.
 *
 * @param requests
 *            - The requested settings of each display
 *
 * @return Whether every display's orientation and mode were applied
 */
static bool applyMergedDisplaySettings(const vector<DisplaySettingsRequest> &requests) {
    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> modes;

    if (requests.empty() || !queryActiveCcdConfig(paths, modes)) {
        return false;
    }

    // Every display keeps its persisted scaling, since the active paths report the scaling Windows is effectively doing
    applyPersistedScaling(paths, getDisplayConfig());

    vector<UINT32> requestedSourceModeIndices;

    for (const DisplaySettingsRequest &request : requests) {
        int pathIndex = findActivePathForDisplay(paths, request.stableId);

        if (pathIndex < 0) {
            return false;
        }

        UINT32 sourceModeIndex = paths[pathIndex].sourceInfo.modeInfoIdx;

        // Displays sharing a source would all take the last resolution, so they are applied one display at a time
        for (UINT32 requestedSourceModeIndex : requestedSourceModeIndices) {
            if (requestedSourceModeIndex == sourceModeIndex) {
                return false;
            }
        }

        requestedSourceModeIndices.push_back(sourceModeIndex);

        // Rotate the path before its resolution is set, since the source mode takes the resolution in its rotation
        paths[pathIndex].targetInfo.rotation = toRotationValue(request.orientation);

        if (!setCcdSourceMode(paths, modes, pathIndex, request.width, request.height, request.refreshRate)) {
            return false;
        }

        paths[pathIndex].targetInfo.scaling = toScalingValue(request.scalingMode);
    }

    // Only a strict apply merges, so Windows never adjusts more of the configuration than the per-display apply allows
    return SetDisplayConfig((UINT32) paths.size(), paths.data(), (UINT32) modes.size(), modes.data(),
                            SDC_SUPPLIED_APPLY_FLAGS) == ERROR_SUCCESS;
}

/**
 * Sets the orientation for the given display.
 *
 * @param displayIndex
 *            - The QueryDisplayConfig index of the display to modify
 * @param orientation
 *            - The orientation (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 */
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation) {
    DISPLAYCONFIG_ROTATION rotation = toRotationValue(orientation);
    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    // Skip the reconfiguration when the display is already in the requested orientation
    if (displayConfig.pathInfoArray[displayIndex].targetInfo.rotation == rotation) {
        return;
    }

    displayConfig.pathInfoArray[displayIndex].targetInfo.rotation = rotation;

    applyDisplayConfig(displayConfig);
}

/**
 * Maps an app orientation to its DISPLAYCONFIG_ROTATION value.
 *
 * @param orientation
 *            - The orientation to map (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 *
 * @return The DISPLAYCONFIG_ROTATION value, defaulting to landscape for unknown orientations
 */
static DISPLAYCONFIG_ROTATION toRotationValue(UINT32 orientation) {
    switch (orientation) {
    case 0:
        // Landscape mode
        return DISPLAYCONFIG_ROTATION_IDENTITY;
    case 1:
        // Portrait mode
        return DISPLAYCONFIG_ROTATION_ROTATE90;
    case 2:
        // Inverted landscape mode
        return DISPLAYCONFIG_ROTATION_ROTATE180;
    case 3:
        // Inverted portrait mode
        return DISPLAYCONFIG_ROTATION_ROTATE270;
    default:
        // Landscape mode
        return DISPLAYCONFIG_ROTATION_IDENTITY;
    }
}

/**
 * Sets the display mode (resolution and exact refresh rate) for the given display through the CCD API the way Advanced
 * Display Settings does, so an existing mode is selected and Windows never creates a new one. If the first apply fails,
 * it steps through the largest selectable mode (letting driver-virtualized DSR/VSR resolutions be selected from a small
 * current mode, e.g. 5760x3240 from 800x600) then retries, restoring the original mode if that retry also fails.
 *
 * @param stableId
 *            - The stable display ID of the display to modify
 * @param displayIndex
 *            - The QueryDisplayConfig index of the display to modify
 * @param resWidth
 *            - The horizontal resolution to apply
 * @param resHeight
 *            - The vertical resolution to apply
 * @param refreshNumerator
 *            - The numerator of the exact refresh rate to apply (Hz = numerator / denominator)
 * @param refreshDenominator
 *            - The denominator of the exact refresh rate to apply (Hz = numerator / denominator)
 *
 * @return Whether the resolution was applied
 */
bool setDisplayMode(const string &stableId, UINT32 displayIndex, UINT32 resWidth, UINT32 resHeight,
                    UINT32 refreshNumerator, UINT32 refreshDenominator) {
    if (stableId.empty()) {
        return false;
    }

    // Apply through the CCD API with the exact rational so an existing mode is selected and Windows never creates one
    if (ccdApplySourceMode(stableId, resWidth, resHeight, refreshNumerator, refreshDenominator)) {
        return true;
    }

    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return false;
    }

    wstring gdiDeviceName = sourceGdiDeviceName(displayConfig.pathInfoArray[displayIndex].sourceInfo);

    if (gdiDeviceName.empty()) {
        return false;
    }

    const WCHAR *gdi = gdiDeviceName.c_str();

    // Capture the current (still original) mode before stepping so it can be restored if the retry also fails
    DEVMODEW originalMode = {};
    originalMode.dmSize = sizeof(originalMode);
    bool haveOriginal = EnumDisplaySettingsW(gdi, ENUM_CURRENT_SETTINGS, &originalMode);

    // A driver-virtualized (DSR/VSR) target is only offered from a larger current mode, so step up first then retry
    if (applyLargestSelectableMode(gdi, resWidth, resHeight)) {
        // Let the driver re-enumerate the resolutions available from the intermediate mode before retrying
        Sleep(100);

        if (ccdApplySourceMode(stableId, resWidth, resHeight, refreshNumerator, refreshDenominator)) {
            return true;
        }

        // The target still would not apply, so restore the original mode rather than leaving the intermediate one
        if (haveOriginal) {
            originalMode.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;
            ChangeDisplaySettingsExW(gdi, &originalMode, NULL, CDS_UPDATEREGISTRY, NULL);
        }
    }

    return false;
}

/**
 * Applies the largest currently selectable display mode, excluding the given resolution. Driver-virtualized (DSR/VSR)
 * resolutions are only offered when the current mode is large enough, so stepping through the largest real mode makes a
 * high virtualized resolution selectable from a small current mode.
 *
 * @param gdiDeviceName
 *            - The GDI device name of the display to modify
 * @param excludeWidth
 *            - A horizontal resolution to skip (the eventual target)
 * @param excludeHeight
 *            - A vertical resolution to skip (the eventual target)
 *
 * @return Whether an intermediate mode was applied
 */
static bool applyLargestSelectableMode(const WCHAR *gdiDeviceName, UINT32 excludeWidth, UINT32 excludeHeight) {
    DWORD bestWidth = 0;
    DWORD bestHeight = 0;
    DWORD bestPixels = 0;

    for (DWORD i = 0;; i++) {
        DEVMODEW mode = {};
        mode.dmSize = sizeof(mode);

        if (!EnumDisplaySettingsW(gdiDeviceName, i, &mode)) {
            break;
        }

        if (mode.dmPelsWidth == excludeWidth && mode.dmPelsHeight == excludeHeight) {
            continue;
        }

        DWORD pixels = (DWORD) mode.dmPelsWidth * (DWORD) mode.dmPelsHeight;

        if (pixels <= bestPixels) {
            continue;
        }

        DEVMODEW candidate = {};
        candidate.dmSize = sizeof(candidate);
        candidate.dmPelsWidth = mode.dmPelsWidth;
        candidate.dmPelsHeight = mode.dmPelsHeight;
        candidate.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT;

        // Keep the largest mode that the display will actually accept right now as the stepping stone
        if (ChangeDisplaySettingsExW(gdiDeviceName, &candidate, NULL, CDS_TEST, NULL) == DISP_CHANGE_SUCCESSFUL) {
            bestWidth = mode.dmPelsWidth;
            bestHeight = mode.dmPelsHeight;
            bestPixels = pixels;
        }
    }

    if (bestPixels == 0) {
        return false;
    }

    DEVMODEW best = {};
    best.dmSize = sizeof(best);
    best.dmPelsWidth = bestWidth;
    best.dmPelsHeight = bestHeight;
    best.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT;

    return ChangeDisplaySettingsExW(gdiDeviceName, &best, NULL, CDS_UPDATEREGISTRY, NULL) == DISP_CHANGE_SUCCESSFUL;
}

/**
 * Applies the given desktop (source) resolution and exact target refresh rate through the CCD API (SetDisplayConfig),
 * the way Windows Advanced Display Settings does. This can apply resolution/refresh-rate combinations the legacy API
 * rejects with DISP_CHANGE_BADMODE (e.g. a GPU-scaled custom resolution at an alternate rate). The active config is
 * queried once, then the exact rational is applied strictly and finally with SDC_ALLOW_CHANGES so Windows may adjust
 * the rest of the configuration around the requested mode. Only the exact rational is submitted, so an existing mode is
 * selected and no new mode is created.
 *
 * @param stableId
 *            - The stable display ID of the display to modify
 * @param width
 *            - The horizontal (source) resolution to apply
 * @param height
 *            - The vertical (source) resolution to apply
 * @param refreshNumerator
 *            - The numerator of the exact refresh rate to apply (Hz = numerator / denominator)
 * @param refreshDenominator
 *            - The denominator of the exact refresh rate to apply (Hz = numerator / denominator)
 *
 * @return Whether the mode was applied
 */
static bool ccdApplySourceMode(const string &stableId, UINT32 width, UINT32 height, UINT32 refreshNumerator,
                               UINT32 refreshDenominator) {
    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> modes;

    if (!queryActiveCcdConfig(paths, modes)) {
        return false;
    }

    int pathIndex = findActivePathForDisplay(paths, stableId);

    if (pathIndex < 0) {
        return false;
    }

    DISPLAYCONFIG_RATIONAL rational = {};
    rational.Numerator = refreshNumerator;
    rational.Denominator = refreshDenominator;

    // Strict apply first, so an existing mode is selected without disturbing the rest of the configuration
    if (submitCcdSourceMode(paths, modes, pathIndex, width, height, rational, SDC_SUPPLIED_APPLY_FLAGS) ==
        ERROR_SUCCESS) {
        return true;
    }

    // Then retry letting Windows adjust the remainder of the configuration around the requested mode
    return submitCcdSourceMode(paths, modes, pathIndex, width, height, rational,
                               SDC_SUPPLIED_APPLY_FLAGS | SDC_ALLOW_CHANGES) == ERROR_SUCCESS;
}

/**
 * Waits until the CCD database reports the given source-mode resolution for the display. setDisplayMode applies the
 * resolution via ChangeDisplaySettingsExW, but the full-path SetDisplayConfig re-apply in setDisplayScalingMode reads
 * the CCD database, which can lag behind a large change; re-applying a stale config would revert the resolution, so
 * this lets the database catch up first, giving up after a bounded wait.
 *
 * @param displayIndex
 *            - The QueryDisplayConfig index of the display to check
 * @param width
 *            - The horizontal source-mode resolution to wait for
 * @param height
 *            - The vertical source-mode resolution to wait for
 */
static void waitForCcdSourceModeResolution(UINT32 displayIndex, UINT32 width, UINT32 height) {
    const int MAX_ATTEMPTS = 20;
    const DWORD RETRY_DELAY_MS = 50;

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
        DisplayConfig config = getDisplayConfig();
        bool matched = false;

        if (displayIndex < config.numPathInfoArrayElements) {
            UINT32 modeIdx = config.pathInfoArray[displayIndex].sourceInfo.modeInfoIdx;

            if (modeIdx != DISPLAYCONFIG_PATH_MODE_IDX_INVALID && modeIdx < config.numModeInfoArrayElements) {
                const DISPLAYCONFIG_MODE_INFO &mode = config.modeInfoArray[modeIdx];

                /*
                 * The source mode is always the unrotated native size, so swap it by the target rotation before
                 * comparing; otherwise a rotated display never matches
                 */
                DISPLAYCONFIG_ROTATION rotation = config.pathInfoArray[displayIndex].targetInfo.rotation;
                bool rotated =
                    rotation == DISPLAYCONFIG_ROTATION_ROTATE90 || rotation == DISPLAYCONFIG_ROTATION_ROTATE270;
                UINT32 footprintWidth = rotated ? mode.sourceMode.height : mode.sourceMode.width;
                UINT32 footprintHeight = rotated ? mode.sourceMode.width : mode.sourceMode.height;

                // The legacy mode change has propagated once the source-mode footprint reports the requested resolution
                matched = mode.infoType == DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE && footprintWidth == width &&
                          footprintHeight == height;
            }
        }

        // The DisplayConfig destructor frees the queried arrays when the local leaves scope each iteration
        if (matched) {
            return;
        }

        Sleep(RETRY_DELAY_MS);
    }
}

/**
 * Sets the scaling mode for the given display.
 *
 * @param displayIndex
 *            - The QueryDisplayConfig index of the display to modify
 * @param scalingMode
 *            - The scaling mode to apply (0 = aspect ratio, 1 = stretched, 2 = centered)
 */
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode) {
    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    DISPLAYCONFIG_SCALING scaling = toScalingValue(scalingMode);

    // Skip the reconfiguration when the display already uses the requested scaling mode
    if (displayConfig.pathInfoArray[displayIndex].targetInfo.scaling == scaling) {
        return;
    }

    displayConfig.pathInfoArray[displayIndex].targetInfo.scaling = scaling;

    applyDisplayConfig(displayConfig);
}

/**
 * Maps an app scaling mode to its DISPLAYCONFIG_SCALING value.
 *
 * @param scalingMode
 *            - The scaling mode to map (0 = aspect ratio, 1 = stretched, 2 = centered)
 *
 * @return The DISPLAYCONFIG_SCALING value, defaulting to aspect ratio for unknown modes
 */
static DISPLAYCONFIG_SCALING toScalingValue(UINT32 scalingMode) {
    switch (scalingMode) {
    case 1:
        // Stretch to fill panel
        return DISPLAYCONFIG_SCALING_STRETCHED;
    case 2:
        // Centered in panel
        return DISPLAYCONFIG_SCALING_CENTERED;
    default:
        // Preserve aspect ratio
        return DISPLAYCONFIG_SCALING_ASPECTRATIOCENTEREDMAX;
    }
}

/**
 * Applies a supplied display configuration through SetDisplayConfig using the standard apply-and-persist flags.
 *
 * @param config
 *            - The display configuration to apply
 *
 * @return The SetDisplayConfig result code
 */
static LONG applyDisplayConfig(const DisplayConfig &config) {
    return SetDisplayConfig(config.numPathInfoArrayElements, config.pathInfoArray, config.numModeInfoArrayElements,
                            config.modeInfoArray, SDC_SUPPLIED_APPLY_FLAGS);
}

/**
 * Sets the DPI scale percentage for the given display.
 *
 * @param displayIndex
 *            - The QueryDisplayConfig index of the display to modify
 * @param dpiScalePercentage
 *            - The DPI scale percentage to apply (e.g. 100, 125, 150)
 */
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage) {
    // Map the requested percentage to its absolute index in the supported list
    int32_t targetAbsoluteIndex = -1;

    for (int32_t i = 0; i < NUM_OF_DPI_SCALE_PERCENTAGES; i++) {
        if (dpiScalePercentage == DPI_SCALE_PERCENTAGES.at(i)) {
            targetAbsoluteIndex = i;
            break;
        }
    }

    // Ignore unsupported percentages rather than applying a wrong value
    if (targetAbsoluteIndex < 0) {
        return;
    }

    /*
     * Applying a DPI scale right after a resolution change is racy, since Windows may still be recomputing the
     * resolution-dependent baseline and can transiently report a low maximum or fail the query/set. Each attempt
     * therefore re-queries the baseline from a fresh configuration and verifies the applied scale, retrying briefly
     */
    const int MAX_ATTEMPTS = 8;
    const DWORD RETRY_DELAY_MS = 40;
    bool primed = false;

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
        DisplayConfig displayConfig = getDisplayConfig();

        if (displayIndex >= displayConfig.numPathInfoArrayElements) {
            return;
        }

        LUID adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
        UINT32 sourceId = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;

        DISPLAYCONFIG_GET_DPI_SCALE_INDICES getIndices = {};
        getIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
        getIndices.header.size = sizeof(getIndices);
        getIndices.header.adapterId = adapterId;
        getIndices.header.id = sourceId;

        // Acting on a zeroed baseline would apply a wrong value, so wait briefly and retry on failure
        if (DisplayConfigGetDeviceInfo(&getIndices.header) != ERROR_SUCCESS) {
            Sleep(RETRY_DELAY_MS);
            continue;
        }

        /*
         * DPI indices are relative to the display's recommended scale, whose absolute index is the magnitude of the
         * relative minimum (absolute index 0 is the global minimum). Work in absolute indices so the math does not
         * depend on which scale is currently recommended
         */
        int32_t recommendedAbsoluteIndex = abs(getIndices.relativeMinimumDpiScaleIndex);
        int32_t maxAbsoluteIndex = recommendedAbsoluteIndex + getIndices.relativeMaximumDpiScaleIndex;
        int32_t currentAbsoluteIndex = recommendedAbsoluteIndex + getIndices.relativeCurrentDpiScaleIndex;

        /*
         * On the first apply of a new resolution the reported maximum can be too low to include the target, so commit a
         * valid in-range scale once to make Windows finalize the baseline, then re-query on the next attempt. On the
         * final attempt fall through and apply the best available scale rather than nothing
         */
        if (targetAbsoluteIndex > maxAbsoluteIndex && attempt < MAX_ATTEMPTS - 1) {
            if (!primed) {
                int32_t primeAbsoluteIndex = (currentAbsoluteIndex == maxAbsoluteIndex) ? 0 : maxAbsoluteIndex;

                DISPLAYCONFIG_SET_DPI_SCALE_INDEX primeIndex = {};
                primeIndex.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE;
                primeIndex.header.size = sizeof(primeIndex);
                primeIndex.header.adapterId = adapterId;
                primeIndex.header.id = sourceId;
                primeIndex.relativeDpiScaleIndex = primeAbsoluteIndex - recommendedAbsoluteIndex;

                DisplayConfigSetDeviceInfo(&primeIndex.header);
                primed = true;
            }

            Sleep(RETRY_DELAY_MS);
            continue;
        }

        // Clamp to the range this display actually supports, otherwise the set is silently rejected
        int32_t desiredAbsoluteIndex =
            (targetAbsoluteIndex > maxAbsoluteIndex) ? maxAbsoluteIndex : targetAbsoluteIndex;

        // Already at the desired scale; nothing to do
        if (currentAbsoluteIndex == desiredAbsoluteIndex) {
            return;
        }

        DISPLAYCONFIG_SET_DPI_SCALE_INDEX setIndex = {};
        setIndex.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE;
        setIndex.header.size = sizeof(setIndex);
        setIndex.header.adapterId = adapterId;
        setIndex.header.id = sourceId;
        setIndex.relativeDpiScaleIndex = desiredAbsoluteIndex - recommendedAbsoluteIndex;

        // On failure, wait briefly and retry against a freshly queried configuration
        if (DisplayConfigSetDeviceInfo(&setIndex.header) != ERROR_SUCCESS) {
            Sleep(RETRY_DELAY_MS);
            continue;
        }

        /*
         * Verify against the absolute scale actually in effect. If the baseline was still stale when this attempt
         * computed the relative index, the applied absolute scale will not match, so re-query (which also lets the
         * reconfiguration settle) and try again
         */
        DISPLAYCONFIG_GET_DPI_SCALE_INDICES verifyIndices = {};
        verifyIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
        verifyIndices.header.size = sizeof(verifyIndices);
        verifyIndices.header.adapterId = adapterId;
        verifyIndices.header.id = sourceId;

        if (DisplayConfigGetDeviceInfo(&verifyIndices.header) == ERROR_SUCCESS) {
            int32_t verifyRecommended = abs(verifyIndices.relativeMinimumDpiScaleIndex);
            int32_t verifyCurrentAbsolute = verifyRecommended + verifyIndices.relativeCurrentDpiScaleIndex;

            if (verifyCurrentAbsolute == desiredAbsoluteIndex) {
                return;
            }
        }

        // Not yet applied; let the reconfiguration settle and try again
        Sleep(RETRY_DELAY_MS);
    }
}
