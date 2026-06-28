/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
#include "com_dhk_io_SetDisplay.h"
#include "DisplayConfig.h"
#include <jni.h>

using namespace std;

/*
 * Forward declarations for internal helpers.
 */
bool setDisplayMode(UINT32 displayIndex, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate);
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode);
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage);
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation);
static void waitForCcdSourceModeResolution(UINT32 displayIndex, UINT32 width, UINT32 height);
static DISPLAYCONFIG_SCALING toScalingValue(UINT32 scalingMode);
static bool tryApplyMode(const WCHAR *gdiDeviceName, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate);
static bool applyLargestSelectableMode(const WCHAR *gdiDeviceName, UINT32 excludeWidth, UINT32 excludeHeight);

/**
 * Resolves a stable display ID to a valid QueryDisplayConfig index. Returns -1 if the ID is not currently present or
 * the index is invalid.
 *
 * @param stableId
 *        - The stable display ID to resolve
 *
 * @return A valid display index, or -1 if the display does not exist
 */
static int resolveDisplayIndex(const string &stableId) {
    int index = getQueryDisplayConfigDisplayIdIndex(stableId);

    if (index < 0) {
        return -1;
    }

    DisplayConfig config = getDisplayConfig();

    if ((UINT32) index >= config.numPathInfoArrayElements) {
        return -1;
    }

    return index;
}

/**
 * Applies display settings (resolution, bit depth, refresh rate, scaling mode, and DPI scale percentage) to the given
 * display.
 *
 * @param env
 *        - The JNI environment pointer
 * @param obj
 *        - The Java SetDisplay instance
 * @param displayId
 *        - The stable display ID of the display to modify
 * @param resWidth
 *        - The horizontal resolution to apply
 * @param resHeight
 *        - The vertical resolution to apply
 * @param bitDepth
 *        - The bit depth to apply
 * @param refreshRate
 *        - The refresh rate to apply
 * @param scalingMode
 *        - The scaling mode to apply (0 = aspect ratio, 1 = stretched, 2 = centered)
 * @param dpiScalePercentage
 *        - The DPI scale percentage to apply (e.g., 100, 125, 150)
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setDisplay(JNIEnv *env, jobject obj, jstring displayId, jint resWidth,
                                                             jint resHeight, jint bitDepth, jint refreshRate,
                                                             jint scalingMode, jint dpiScalePercentage) {
    jboolean isCopy;
    const char *displayIdChars = env->GetStringUTFChars(displayId, &isCopy);
    string stableId = displayIdChars;

    int displayIndex = resolveDisplayIndex(stableId);

    if (displayIndex < 0) {
        env->ReleaseStringUTFChars(displayId, displayIdChars);

        // Display disappeared or invalid
        return;
    }

    // Apply the resolution, then settle so the scaling re-apply does not revert it with a stale source mode
    if (setDisplayMode(displayIndex, resWidth, resHeight, bitDepth, refreshRate)) {
        waitForCcdSourceModeResolution(displayIndex, resWidth, resHeight);
    }

    setDisplayScalingMode(displayIndex, scalingMode);
    setDpiScalePercentage(displayIndex, dpiScalePercentage);

    env->ReleaseStringUTFChars(displayId, displayIdChars);
}

/**
 * Applies a new orientation to the given display.
 *
 * @param env
 *        - The JNI environment pointer
 * @param obj
 *        - The Java SetDisplay instance
 * @param displayId
 *        - The stable display ID of the display to modify
 * @param orientation
 *        - The orientation to apply
 *          (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setOrientation(JNIEnv *env, jobject obj, jstring displayId,
                                                                 jint orientation) {
    jboolean isCopy;
    const char *displayIdChars = env->GetStringUTFChars(displayId, &isCopy);
    string stableId = displayIdChars;

    int displayIndex = resolveDisplayIndex(stableId);

    if (displayIndex < 0) {
        env->ReleaseStringUTFChars(displayId, displayIdChars);
        return; // Display disappeared or invalid
    }

    setDisplayOrientation(displayIndex, orientation);

    env->ReleaseStringUTFChars(displayId, displayIdChars);
}

/**
 * Waits until the CCD display database reports the given source-mode resolution for the display. setDisplayMode
 * applies the resolution through ChangeDisplaySettingsExW, but the full-path SetDisplayConfig re-apply in
 * setDisplayScalingMode reads the CCD database, which can lag behind a large resolution change. Re-applying a
 * stale config would revert the resolution, so this lets the database catch up first, giving up after a bounded wait.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to check
 * @param width
 *        - The horizontal source-mode resolution to wait for
 * @param height
 *        - The vertical source-mode resolution to wait for
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

                // The legacy mode change has propagated once the CCD source mode reports the requested resolution
                matched = mode.infoType == DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE && mode.sourceMode.width == width &&
                          mode.sourceMode.height == height;
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
 * Applies the given resolution, bit depth, and refresh rate through ChangeDisplaySettingsExW, retrying without the
 * refresh rate and then without the bit depth so a mode is still applied when an exact match is unavailable.
 *
 * @param gdiDeviceName
 *        - The GDI device name of the display to modify
 * @param width
 *        - The horizontal resolution to apply
 * @param height
 *        - The vertical resolution to apply
 * @param bitDepth
 *        - The bit depth to apply
 * @param refreshRate
 *        - The refresh rate to apply
 *
 * @return Whether the mode was applied
 */
static bool tryApplyMode(const WCHAR *gdiDeviceName, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate) {
    DEVMODEW devModeW;
    SecureZeroMemory(&devModeW, sizeof(DEVMODEW));
    devModeW.dmSize = sizeof(devModeW);
    devModeW.dmPelsWidth = width;
    devModeW.dmPelsHeight = height;
    devModeW.dmBitsPerPel = bitDepth;
    devModeW.dmDisplayFrequency = refreshRate;
    devModeW.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;

    if (ChangeDisplaySettingsExW(gdiDeviceName, &devModeW, NULL, CDS_TEST, NULL) == DISP_CHANGE_SUCCESSFUL) {
        ChangeDisplaySettingsExW(gdiDeviceName, &devModeW, NULL, CDS_UPDATEREGISTRY, NULL);
        return true;
    }

    // Retry without refresh rate
    devModeW.dmFields &= ~DM_DISPLAYFREQUENCY;
    devModeW.dmDisplayFrequency = 0;

    if (ChangeDisplaySettingsExW(gdiDeviceName, &devModeW, NULL, CDS_TEST, NULL) == DISP_CHANGE_SUCCESSFUL) {
        ChangeDisplaySettingsExW(gdiDeviceName, &devModeW, NULL, CDS_UPDATEREGISTRY, NULL);
        return true;
    }

    // Retry without bit depth
    devModeW.dmFields &= ~DM_BITSPERPEL;
    devModeW.dmBitsPerPel = 0;

    if (ChangeDisplaySettingsExW(gdiDeviceName, &devModeW, NULL, CDS_TEST, NULL) == DISP_CHANGE_SUCCESSFUL) {
        ChangeDisplaySettingsExW(gdiDeviceName, &devModeW, NULL, CDS_UPDATEREGISTRY, NULL);
        return true;
    }

    return false;
}

/**
 * Applies the largest currently selectable display mode, excluding the given resolution. Driver-virtualized
 * (DSR/VSR) resolutions are only offered when the current mode is large enough, so stepping through the largest
 * real mode makes a high virtualized resolution selectable from a small current mode.
 *
 * @param gdiDeviceName
 *        - The GDI device name of the display to modify
 * @param excludeWidth
 *        - A horizontal resolution to skip (the eventual target)
 * @param excludeHeight
 *        - A vertical resolution to skip (the eventual target)
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
 * Sets the display mode (resolution, bit depth, refresh rate) for the given display. Uses ChangeDisplaySettingsExW
 * with the GDI device name obtained from QueryDisplayConfig. When the target cannot be applied directly, it steps
 * through the largest selectable mode first, which lets driver-virtualized (DSR/VSR) resolutions be selected from a
 * small current mode (for example, applying 5760x3240 from 800x600).
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param resWidth
 *        - The horizontal resolution to apply
 * @param resHeight
 *        - The vertical resolution to apply
 * @param bitDepth
 *        - The bit depth to apply
 * @param refreshRate
 *        - The refresh rate to apply
 *
 * @return Whether the resolution was applied
 */
bool setDisplayMode(UINT32 displayIndex, UINT32 resWidth, UINT32 resHeight, UINT32 bitDepth, UINT32 refreshRate) {
    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return false;
    }

    DISPLAYCONFIG_SOURCE_DEVICE_NAME sourceName = {};
    sourceName.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    sourceName.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;
    sourceName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME;
    sourceName.header.size = sizeof(sourceName);

    if (DisplayConfigGetDeviceInfo(&sourceName.header) != ERROR_SUCCESS) {
        return false;
    }

    if (tryApplyMode(sourceName.viewGdiDeviceName, resWidth, resHeight, bitDepth, refreshRate)) {
        return true;
    }

    // Capture the current (still original) mode before stepping so it can be restored if the retry also fails
    DEVMODEW originalMode = {};
    originalMode.dmSize = sizeof(originalMode);
    bool haveOriginal = EnumDisplaySettingsW(sourceName.viewGdiDeviceName, ENUM_CURRENT_SETTINGS, &originalMode);

    // A driver-virtualized (DSR/VSR) target is only offered from a larger current mode, so step up first then retry
    if (applyLargestSelectableMode(sourceName.viewGdiDeviceName, resWidth, resHeight)) {
        // Let the driver re-enumerate the resolutions available from the intermediate mode before retrying
        Sleep(100);

        if (tryApplyMode(sourceName.viewGdiDeviceName, resWidth, resHeight, bitDepth, refreshRate)) {
            return true;
        }

        // The target still would not apply, so restore the original mode rather than leaving the intermediate one
        if (haveOriginal) {
            originalMode.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;
            ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &originalMode, NULL, CDS_UPDATEREGISTRY, NULL);
        }
    }

    return false;
}

/**
 * Maps an app scaling mode to its DISPLAYCONFIG_SCALING value.
 *
 * @param scalingMode
 *        - The scaling mode to map (0 = aspect ratio, 1 = stretched, 2 = centered)
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
 * Sets the scaling mode for the given display.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param scalingMode
 *        - The scaling mode to apply
 *          (0 = aspect ratio, 1 = stretched, 2 = centered)
 */
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode) {
    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    displayConfig.pathInfoArray[displayIndex].targetInfo.scaling = toScalingValue(scalingMode);

    SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
                     displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray,
                     SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);
}

/**
 * Sets the DPI scale percentage for the given display.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param dpiScalePercentage
 *        - The DPI scale percentage to apply (e.g., 100, 125, 150)
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
     * Applying a DPI scale immediately after a resolution change is racy: the recommended DPI baseline is
     * resolution-dependent and Windows may not have finished recomputing it, the reported maximum can be transiently
     * conservative on the first apply of a new resolution, and the query/set calls can transiently fail under
     * back-to-back reconfigurations. Each attempt re-queries the baseline from a fresh configuration and verifies the
     * applied scale, retrying briefly until the target is in effect
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
         * DPI indices are reported relative to the display's recommended scale. The minimum supported scale is the
         * first entry in the global list (absolute index 0), so the recommended scale's absolute index is the
         * magnitude of the relative minimum. Work in absolute indices so the math is independent of which scale is
         * currently recommended
         */
        int32_t recommendedAbsoluteIndex = abs(getIndices.relativeMinimumDpiScaleIndex);
        int32_t maxAbsoluteIndex = recommendedAbsoluteIndex + getIndices.relativeMaximumDpiScaleIndex;
        int32_t currentAbsoluteIndex = recommendedAbsoluteIndex + getIndices.relativeCurrentDpiScaleIndex;

        /*
         * On the first apply of a new resolution the reported maximum can be too low to include the target. Commit a
         * valid in-range scale once so Windows finalizes the new resolution's DPI baseline, then re-query on the next
         * attempt instead of clamping the target to a lower scale. On the final attempt fall through and apply the
         * best available scale rather than nothing
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

/**
 * Sets the orientation for the given display.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param orientation
 *        - The orientation to apply
 *          (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 */
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation) {
    DISPLAYCONFIG_ROTATION rotation;

    switch (orientation) {
    case 0:
        // Landscape mode
        rotation = DISPLAYCONFIG_ROTATION_IDENTITY;
        break;
    case 1:
        // Portrait mode
        rotation = DISPLAYCONFIG_ROTATION_ROTATE90;
        break;
    case 2:
        // Inverted landscape mode
        rotation = DISPLAYCONFIG_ROTATION_ROTATE180;
        break;
    case 3:
        // Inverted portrait mode
        rotation = DISPLAYCONFIG_ROTATION_ROTATE270;
        break;
    default:
        // Landscape mode
        rotation = DISPLAYCONFIG_ROTATION_IDENTITY;
        break;
    }

    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    displayConfig.pathInfoArray[displayIndex].targetInfo.rotation = rotation;

    SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
                     displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray,
                     SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);
}
