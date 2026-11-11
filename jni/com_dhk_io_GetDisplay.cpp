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
#include "com_dhk_io_GetDisplay.h"
#include "ActiveDisplaySettings.h"
#include "ArrangeDisplay.h"
#include "DisplayConfig.h"

#include <cstdint>
#include <dxgi.h>
#include <jni.h>
#include <map>
#include <unordered_set>
#include <utility>
#include <vector>
#include <windows.h>

using namespace std;

static vector<ModeInfo> enumDisplayOutputModes(const wstring &gdiDeviceName);
static void collectOutputModes(IDXGIOutput *output, vector<ModeInfo> &modes);
static void addCustomResolutionRefreshRates(vector<ModeInfo> &modes, map<long long, pair<int, int>> &canonicalRates);
static long long rateKey(int refreshNumerator, int refreshDenominator);
static vector<string> readUtf8Strings(JNIEnv *env, jobjectArray strings);
static const DISPLAYCONFIG_SOURCE_MODE *visibleSourceMode(const DISPLAYCONFIG_PATH_INFO &path,
                                                          const vector<DISPLAYCONFIG_MODE_INFO> &modes);
static ActiveDisplaySettings readActiveDisplaySettings(const DISPLAYCONFIG_PATH_INFO &path,
                                                       const DISPLAYCONFIG_SOURCE_MODE &sourceMode,
                                                       const vector<DISPLAYCONFIG_MODE_INFO> &modes,
                                                       const DisplayConfig &databaseConfig);
static jint currentDpiScalePercentage(const DISPLAYCONFIG_PATH_SOURCE_INFO &sourceInfo);
static DISPLAYCONFIG_SCALING persistedScaling(const DisplayConfig &databaseConfig,
                                              const DISPLAYCONFIG_PATH_TARGET_INFO &targetInfo);
static jint scalingModeIndex(DISPLAYCONFIG_SCALING scaling);
static int orientationIndex(DISPLAYCONFIG_ROTATION rotation);

/**
 * Number of integer fields packed per mode in the flat enumDisplayModes result: width, height, refresh-rate numerator,
 * and refresh-rate denominator.
 */
static const jsize FIELDS_PER_MODE = 4;

/**
 * Number of integer fields packed per display in the flat queryActiveDisplaySettings result: width, height,
 * refresh-rate numerator and denominator, DPI scale percentage, scaling mode, orientation, and desktop x and y.
 */
static const jsize FIELDS_PER_ACTIVE_DISPLAY = 9;

/**
 * Desktop coordinate at or below which a source counts as parked offscreen rather than visible.
 */
static constexpr LONG OFFSCREEN_POSITION = -30000;

/**
 * Enumerates the supported display modes for the given display, reading each mode's exact rational refresh rate from
 * the matching DXGI output so no truncated integer rate is ever used, then augmenting GPU-scaled custom resolutions
 * with the panel rates DXGI omits. The returned modes are de-duplicated in first-seen order.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param displayId
 *            - The stable display ID to enumerate modes for
 *
 * @return A flat int array of {width, height, refreshNumerator, refreshDenominator} per mode, an empty array if none,
 *         or null on unrecoverable native failure
 */
JNIEXPORT jintArray JNICALL Java_com_dhk_io_GetDisplay_enumDisplayModes(JNIEnv *env, jobject obj, jstring displayId) {
    (void) obj;

    if (displayId == nullptr) {
        return nullptr;
    }

    jboolean isCopy = JNI_FALSE;
    const char *displayIdChars = env->GetStringUTFChars(displayId, &isCopy);

    if (displayIdChars == nullptr) {
        return nullptr;
    }

    string stableDisplayId = displayIdChars;
    env->ReleaseStringUTFChars(displayId, displayIdChars);

    // Resolve the display's GDI device name (\\.\DISPLAYn) so the matching DXGI output can be found by its DeviceName
    wstring gdiDeviceName;
    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> ccdModes;

    if (queryActiveCcdConfig(paths, ccdModes)) {
        int activePathIndex = findActivePathForDisplay(paths, stableDisplayId);

        if (activePathIndex >= 0) {
            gdiDeviceName = sourceGdiDeviceName(paths[activePathIndex].sourceInfo);
        }
    }

    // Fall back to the persisted DB config so a display not on an active path is still found
    if (gdiDeviceName.empty()) {
        DisplayConfig displayConfig = getDisplayConfig();

        for (UINT32 i = 0; i < displayConfig.numPathInfoArrayElements; i++) {
            if (stableIdForTarget(displayConfig.pathInfoArray[i].targetInfo) == stableDisplayId) {
                gdiDeviceName = sourceGdiDeviceName(displayConfig.pathInfoArray[i].sourceInfo);
                break;
            }
        }
    }

    // Last resort: locate the display by its EnumDisplayDevices index to read its GDI device name
    if (gdiDeviceName.empty()) {
        DISPLAY_DEVICEW displayDeviceW;
        SecureZeroMemory(&displayDeviceW, sizeof(DISPLAY_DEVICEW));
        displayDeviceW.cb = sizeof(displayDeviceW);

        int enumDisplayIndex = getEnumDisplayDevicesDisplayIdIndex(stableDisplayId);

        if (EnumDisplayDevicesW(NULL, enumDisplayIndex, &displayDeviceW, 0) &&
            (displayDeviceW.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP)) {
            gdiDeviceName = displayDeviceW.DeviceName;
        }
    }

    vector<ModeInfo> modeList;

    if (!gdiDeviceName.empty()) {
        modeList = enumDisplayOutputModes(gdiDeviceName);
    }

    // Fill GPU-scaled custom resolutions with the panel rates DXGI omits, capturing the canonical rational per rate
    map<long long, pair<int, int>> canonicalRates;
    addCustomResolutionRefreshRates(modeList, canonicalRates);

    // De-duplicate in first-seen order, since a resolution/rate can repeat across the output's scaling variants
    unordered_set<uint64_t> seenModes;
    seenModes.reserve(modeList.size());
    vector<ModeInfo> uniqueModes;
    uniqueModes.reserve(modeList.size());

    for (const ModeInfo &mode : modeList) {
        long long modeRateKey = rateKey(mode.refreshNumerator, mode.refreshDenominator);

        // Pack width, height, and the rounded rate key so representation-variant rationals of one rate collapse
        uint64_t modeKey = (static_cast<uint64_t>(static_cast<uint32_t>(mode.width)) << 48) |
                           (static_cast<uint64_t>(static_cast<uint32_t>(mode.height)) << 32) |
                           static_cast<uint64_t>(modeRateKey);

        if (!seenModes.insert(modeKey).second) {
            continue;
        }

        ModeInfo uniqueMode = mode;

        // Rewrite the rate to its canonical rational so one rate reads identically across every resolution
        auto canonicalRate = canonicalRates.find(modeRateKey);

        if (canonicalRate != canonicalRates.end()) {
            uniqueMode.refreshNumerator = canonicalRate->second.first;
            uniqueMode.refreshDenominator = canonicalRate->second.second;
        }

        uniqueModes.push_back(uniqueMode);
    }

    modeList = std::move(uniqueModes);

    /*
     * Marshal the modes as one flat int array of {width, height, refreshNumerator, refreshDenominator} records,
     * keeping the JNI boundary primitive; the calling object instance rebuilds the display-mode objects from these
     * fields, so the exact rational refresh rate survives without a custom cross-boundary type
     */
    jsize modeCount = static_cast<jsize>(modeList.size());
    jintArray resultArray = env->NewIntArray(modeCount * FIELDS_PER_MODE);

    if (resultArray == nullptr) {
        return nullptr;
    }

    vector<jint> flat;
    flat.reserve(static_cast<size_t>(modeCount) * FIELDS_PER_MODE);

    for (const ModeInfo &modeInfo : modeList) {
        flat.push_back(modeInfo.width);
        flat.push_back(modeInfo.height);
        flat.push_back(modeInfo.refreshNumerator);
        flat.push_back(modeInfo.refreshDenominator);
    }

    env->SetIntArrayRegion(resultArray, 0, modeCount * FIELDS_PER_MODE, flat.data());

    return resultArray;
}

/**
 * Gets the stabilized IDs for visible displays.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 *
 * @return A String[] of stable display IDs for visible displays
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_GetDisplay_enumVisibleDisplayIds(JNIEnv *env, jobject obj) {
    (void) obj;
    vector<string> visibleIds = getVisibleDisplayIds();
    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray displayIds = env->NewObjectArray(visibleIds.size(), strClass, NULL);

    for (int i = 0; i < (int) visibleIds.size(); i++) {
        jstring visibleId = env->NewStringUTF(visibleIds[i].c_str());
        env->SetObjectArrayElement(displayIds, i, visibleId);
        env->DeleteLocalRef(visibleId);
    }

    return displayIds;
}

/**
 * Computes the DPI scale percentages Windows supports for the given resolution, capping the maximum so the effective
 * resolution stays usable. Derives the set directly so a not-yet-applied resolution can report its scales without a
 * mode change, since Windows reports the live range only for the applied resolution.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param width
 *            - The horizontal resolution to compute supported DPI scale percentages for
 * @param height
 *            - The vertical resolution to compute supported DPI scale percentages for
 *
 * @return An int[] of supported DPI scale percentages (always at least {100}), or null on native failure
 */
JNIEXPORT jintArray JNICALL Java_com_dhk_io_GetDisplay_getSupportedDpiScalePercentages(JNIEnv *env, jobject obj,
                                                                                       jint width, jint height) {
    (void) obj;

    // Compare against the long and short edges so the supported set does not depend on orientation
    int32_t longEdge = (width >= height) ? width : height;
    int32_t shortEdge = (width >= height) ? height : width;

    vector<int32_t> supported;
    supported.reserve(NUM_OF_DPI_SCALE_PERCENTAGES);

    for (int32_t i = 0; i < NUM_OF_DPI_SCALE_PERCENTAGES; i++) {
        int32_t percentage = DPI_SCALE_PERCENTAGES.at(i);
        int32_t effectiveLong = longEdge * 100 / percentage;
        int32_t effectiveShort = shortEdge * 100 / percentage;

        // Index 0 (100%) is always supported and percentages ascend, so stop at the first one that does not fit
        if (i == 0 || (effectiveLong >= MIN_EFFECTIVE_LONG_EDGE && effectiveShort >= MIN_EFFECTIVE_SHORT_EDGE)) {
            supported.push_back(percentage);
        } else {
            break;
        }
    }

    jsize count = static_cast<jsize>(supported.size());
    jintArray supportedArray = env->NewIntArray(count);

    if (supportedArray == nullptr) {
        return nullptr;
    }

    env->SetIntArrayRegion(supportedArray, 0, count, reinterpret_cast<const jint *>(supported.data()));

    return supportedArray;
}

/**
 * Gets the orientation of each visible display, aligned index-for-index with getVisibleDisplayIds so a display's
 * rotation matches its ID at the same index.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 *
 * @return An int[] of orientation values (1 = Landscape, 2 = Portrait, etc.), in getVisibleDisplayIds order
 */
JNIEXPORT jintArray JNICALL Java_com_dhk_io_GetDisplay_queryVisibleDisplayOrientations(JNIEnv *env, jobject obj) {
    (void) obj;
    vector<int> rotations = getVisibleDisplayOrientations();
    jsize count = (jsize) rotations.size();
    jintArray orientations = env->NewIntArray(count);

    if (orientations == nullptr) {
        return nullptr;
    }

    if (count > 0) {
        env->SetIntArrayRegion(orientations, 0, count, reinterpret_cast<const jint *>(rotations.data()));
    }

    return orientations;
}

/**
 * Gets the Windows Display Settings number of each given visible display, index-for-index with the provided IDs. Taking
 * the IDs the caller already holds avoids re-querying them, and the numbers carry the gaps Windows leaves for
 * disconnected displays.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param visibleIds
 *            - The visible display IDs to number, typically from getVisibleDisplayIds
 *
 * @return An int[] of Windows display numbers, index-for-index with visibleIds
 */
JNIEXPORT jintArray JNICALL Java_com_dhk_io_GetDisplay_enumVisibleDisplayNumbers(JNIEnv *env, jobject obj,
                                                                                 jobjectArray visibleIds) {
    (void) obj;
    vector<string> ids = readUtf8Strings(env, visibleIds);
    vector<int> displayNumbers = getVisibleDisplayNumbers(ids);
    jsize count = (jsize) displayNumbers.size();
    jintArray numbers = env->NewIntArray(count);

    if (numbers == nullptr) {
        return nullptr;
    }

    if (count > 0) {
        env->SetIntArrayRegion(numbers, 0, count, reinterpret_cast<const jint *>(displayNumbers.data()));
    }

    return numbers;
}

/**
 * Captures the current multi-display arrangement, forwarding to ArrangeDisplay so the caller can hand it back to
 * SetDisplay's preserveDisplayArrangement after a batch of display changes.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 *
 * @return A String[] with one encoded rectangle per active display
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_GetDisplay_captureDisplayArrangement(JNIEnv *env, jobject obj) {
    (void) obj;
    return captureDisplayArrangement(env);
}

/**
 * Reads the settings currently in effect on each given display from the active configuration in a single pass. Each
 * record holds the display's rotation-adjusted resolution, exact refresh rate, DPI scale percentage, scaling mode,
 * orientation, and desktop position, and a display without a visible active path gets a record of zeros.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param displayIds
 *            - The stable IDs of the displays to read the active settings of
 *
 * @return An int[] of one active display settings record per given display, in the given order, or null on failure
 */
JNIEXPORT jintArray JNICALL Java_com_dhk_io_GetDisplay_queryActiveDisplaySettings(JNIEnv *env, jobject obj,
                                                                                  jobjectArray displayIds) {
    (void) obj;
    vector<string> ids = readUtf8Strings(env, displayIds);

    // A display without a visible active path keeps the all-zero settings each entry starts with
    vector<ActiveDisplaySettings> activeSettings(ids.size());
    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> modes;

    if (!ids.empty() && queryActiveCcdConfig(paths, modes)) {
        // The persisted configuration holds the scaling mode that was set, which the active paths do not report
        DisplayConfig databaseConfig = getDisplayConfig();

        for (const DISPLAYCONFIG_PATH_INFO &path : paths) {
            const DISPLAYCONFIG_SOURCE_MODE *sourceMode = visibleSourceMode(path, modes);

            if (sourceMode == nullptr) {
                continue;
            }

            // Resolve each visible path's ID once and match it to the requested IDs, avoiding a lookup per ID
            string stableId = stableIdForTarget(path.targetInfo);

            if (stableId.empty()) {
                continue;
            }

            for (size_t idIndex = 0; idIndex < ids.size(); idIndex++) {
                if (ids[idIndex] == stableId) {
                    activeSettings[idIndex] = readActiveDisplaySettings(path, *sourceMode, modes, databaseConfig);
                }
            }
        }
    }

    /*
     * Marshal the settings as one flat int array of records so the JNI boundary stays primitive, and the calling object
     * instance rebuilds the settings objects from these fields in this order
     */
    vector<jint> records;
    records.reserve(activeSettings.size() * FIELDS_PER_ACTIVE_DISPLAY);

    for (const ActiveDisplaySettings &settings : activeSettings) {
        records.push_back(settings.width);
        records.push_back(settings.height);
        records.push_back(settings.refreshNumerator);
        records.push_back(settings.refreshDenominator);
        records.push_back(settings.dpiScalePercentage);
        records.push_back(settings.scalingMode);
        records.push_back(settings.orientation);
        records.push_back(settings.desktopX);
        records.push_back(settings.desktopY);
    }

    jsize count = (jsize) records.size();
    jintArray settingsRecords = env->NewIntArray(count);

    if (settingsRecords == nullptr) {
        return nullptr;
    }

    if (count > 0) {
        env->SetIntArrayRegion(settingsRecords, 0, count, records.data());
    }

    return settingsRecords;
}

/**
 * Enumerates the supported modes of the display driven by the given GDI device name by matching it to a DXGI output
 * and reading that output's mode list. DXGI supplies the exact rational refresh rate of every mode, so no truncated
 * integer rate is involved.
 *
 * @param gdiDeviceName
 *            - The GDI device name (\\.\DISPLAYn) of the display to enumerate
 *
 * @return The supported modes, or an empty list if no matching output or its modes could be read
 */
static vector<ModeInfo> enumDisplayOutputModes(const wstring &gdiDeviceName) {
    vector<ModeInfo> modes;

    IDXGIFactory1 *factory = nullptr;

    if (FAILED(CreateDXGIFactory1(__uuidof(IDXGIFactory1), reinterpret_cast<void **>(&factory))) ||
        factory == nullptr) {
        return modes;
    }

    IDXGIAdapter1 *adapter = nullptr;

    for (UINT adapterIndex = 0; factory->EnumAdapters1(adapterIndex, &adapter) != DXGI_ERROR_NOT_FOUND;
         adapterIndex++) {
        IDXGIOutput *output = nullptr;

        for (UINT outputIndex = 0; adapter->EnumOutputs(outputIndex, &output) != DXGI_ERROR_NOT_FOUND; outputIndex++) {
            DXGI_OUTPUT_DESC desc = {};

            // Match the output to the requested display by its GDI device name (\\.\DISPLAYn)
            if (SUCCEEDED(output->GetDesc(&desc)) && gdiDeviceName == desc.DeviceName) {
                collectOutputModes(output, modes);
                output->Release();
                adapter->Release();
                factory->Release();
                return modes;
            }

            output->Release();
        }

        adapter->Release();
    }

    factory->Release();

    return modes;
}

/**
 * Reads every supported mode of the given DXGI output for the 32-bit desktop format, appending each progressive,
 * non-zero mode as {width, height, exact refresh numerator, exact refresh denominator}.
 *
 * @param output
 *            - The DXGI output to read modes from
 * @param modes
 *            - The mode list to append to
 */
static void collectOutputModes(IDXGIOutput *output, vector<ModeInfo> &modes) {
    // The 32-bit BGRA desktop format, with the scaling flag so GPU-scaled custom resolutions are included
    const DXGI_FORMAT format = DXGI_FORMAT_B8G8R8A8_UNORM;
    const UINT flags = DXGI_ENUM_MODES_SCALING;

    UINT numModes = 0;

    if (FAILED(output->GetDisplayModeList(format, flags, &numModes, nullptr)) || numModes == 0) {
        return;
    }

    vector<DXGI_MODE_DESC> dxgiModes(numModes);

    if (FAILED(output->GetDisplayModeList(format, flags, &numModes, dxgiModes.data()))) {
        return;
    }

    for (UINT i = 0; i < numModes; i++) {
        const DXGI_MODE_DESC &mode = dxgiModes[i];

        // Skip zero-size and unspecified-rate modes so only real desktop modes are reported
        if (mode.Width == 0 || mode.Height == 0 || mode.RefreshRate.Denominator == 0) {
            continue;
        }

        // Skip interlaced modes so only progressive desktop modes are reported
        if (mode.ScanlineOrdering == DXGI_MODE_SCANLINE_ORDER_UPPER_FIELD_FIRST ||
            mode.ScanlineOrdering == DXGI_MODE_SCANLINE_ORDER_LOWER_FIELD_FIRST) {
            continue;
        }

        modes.push_back({static_cast<int>(mode.Width), static_cast<int>(mode.Height),
                         static_cast<int>(mode.RefreshRate.Numerator), static_cast<int>(mode.RefreshRate.Denominator)});
    }
}

/**
 * Fills GPU-scaled custom resolutions with the panel rates their DXGI mode list omits and reports the canonical
 * rational for each rate. Every panel rate at or below a resolution's own maximum is added, and the richest
 * resolution's rate set is handed back so callers collapse DXGI's representation-variant rationals to one.
 *
 * @param modes
 *            - The mode list to augment in place
 * @param canonicalRates
 *            - Populated with the canonical rational (value) for each rounded rate key (key)
 */
static void addCustomResolutionRefreshRates(vector<ModeInfo> &modes, map<long long, pair<int, int>> &canonicalRates) {
    /*
     * Group every mode by resolution in one pass, mapping each resolution's rateKey to a single exact rational
     * (first seen wins, so the representation-variant rationals DXGI reports for one rate collapse to one entry). The
     * raw mode count per resolution then selects the canonical rate set below
     */
    map<pair<int, int>, int> modeCountByResolution;
    map<pair<int, int>, map<long long, pair<int, int>>> ratesByResolution;

    for (const ModeInfo &mode : modes) {
        pair<int, int> resolution = {mode.width, mode.height};
        modeCountByResolution[resolution]++;
        ratesByResolution[resolution].emplace(rateKey(mode.refreshNumerator, mode.refreshDenominator),
                                              make_pair(mode.refreshNumerator, mode.refreshDenominator));
    }

    /*
     * The resolution with the most modes carries the fullest rate set and is the canonical source: drawing every fill
     * rate from one resolution yields one clean representation per rate and avoids the cross-resolution duplicates
     */
    pair<int, int> richest = {0, 0};
    int richestCount = -1;

    for (const auto &[resolution, count] : modeCountByResolution) {
        if (count > richestCount) {
            richestCount = count;
            richest = resolution;
        }
    }

    if (richestCount < 0) {
        return;
    }

    // The richest resolution's rate set is the canonical source: one clean rational per rate for callers to match
    canonicalRates = ratesByResolution.at(richest);

    for (const auto &[resolution, rates] : ratesByResolution) {
        long long ceilingKey = rates.rbegin()->first;

        // Add every canonical panel rate at or below this resolution's own ceiling that it is missing
        for (const auto &[key, rate] : canonicalRates) {
            if (key <= ceilingKey && !rates.contains(key)) {
                modes.push_back({resolution.first, resolution.second, rate.first, rate.second});
            }
        }
    }
}

/**
 * Rounds a rational refresh rate to hundredths of a hertz, giving a single key for rationals that render as the same
 * rate. This collapses the timing-variant representations DXGI reports for one rate while keeping genuinely distinct
 * rates apart (for example 23.98 stays separate from 24.00).
 *
 * @param refreshNumerator
 *            - The numerator of the refresh rate
 * @param refreshDenominator
 *            - The denominator of the refresh rate
 *
 * @return The refresh rate rounded to hundredths of a hertz
 */
static long long rateKey(int refreshNumerator, int refreshDenominator) {
    if (refreshDenominator == 0) {
        return 0;
    }

    return ((long long) refreshNumerator * 100 + refreshDenominator / 2) / refreshDenominator;
}

/**
 * Copies an array of strings from the virtual machine into UTF-8 strings, keeping a null element as an empty string so
 * every copied string stays at the same index as its source.
 *
 * @param env
 *            - The JNI environment pointer
 * @param strings
 *            - The strings to copy, which may be null
 *
 * @return The copied strings, or an empty list when there is nothing to copy
 */
static vector<string> readUtf8Strings(JNIEnv *env, jobjectArray strings) {
    vector<string> copiedStrings;
    jsize stringCount = strings != nullptr ? env->GetArrayLength(strings) : 0;
    copiedStrings.reserve((size_t) stringCount);

    for (jsize i = 0; i < stringCount; i++) {
        jstring element = (jstring) env->GetObjectArrayElement(strings, i);

        if (element == nullptr) {
            copiedStrings.push_back("");
            continue;
        }

        const char *chars = env->GetStringUTFChars(element, nullptr);
        copiedStrings.push_back(chars != nullptr ? chars : "");

        if (chars != nullptr) {
            env->ReleaseStringUTFChars(element, chars);
        }

        env->DeleteLocalRef(element);
    }

    return copiedStrings;
}

/**
 * Gets the source mode of a path when the path drives a visible display, applying the same filter as
 * getVisibleDisplayIds (active, a valid source mode, a non-zero size, and an on-screen position).
 *
 * @param path
 *            - The path to check
 * @param modes
 *            - The mode array the path indexes into
 *
 * @return The path's source mode, or null when the path does not drive a visible display
 */
static const DISPLAYCONFIG_SOURCE_MODE *visibleSourceMode(const DISPLAYCONFIG_PATH_INFO &path,
                                                          const vector<DISPLAYCONFIG_MODE_INFO> &modes) {
    UINT32 sourceModeIndex = path.sourceInfo.modeInfoIdx;

    if ((path.flags & DISPLAYCONFIG_PATH_ACTIVE) == 0 || sourceModeIndex == DISPLAYCONFIG_PATH_MODE_IDX_INVALID ||
        sourceModeIndex >= modes.size() || modes[sourceModeIndex].infoType != DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE) {
        return nullptr;
    }

    const DISPLAYCONFIG_SOURCE_MODE &sourceMode = modes[sourceModeIndex].sourceMode;

    // Treat a zero-size or offscreen source as not visible
    if (sourceMode.width == 0 || sourceMode.height == 0 || sourceMode.position.x <= OFFSCREEN_POSITION ||
        sourceMode.position.y <= OFFSCREEN_POSITION) {
        return nullptr;
    }

    return &sourceMode;
}

/**
 * Reads the settings in effect on a visible display from its path and source mode. The refresh rate is the exact
 * rational the path carries, so it is never derived from a truncated integer.
 *
 * @param path
 *            - The active path driving the display
 * @param sourceMode
 *            - The path's source mode
 * @param modes
 *            - The mode array the path indexes into
 * @param databaseConfig
 *            - The persisted display configuration, which holds the scaling mode that was set
 *
 * @return The settings in effect on the display
 */
static ActiveDisplaySettings readActiveDisplaySettings(const DISPLAYCONFIG_PATH_INFO &path,
                                                       const DISPLAYCONFIG_SOURCE_MODE &sourceMode,
                                                       const vector<DISPLAYCONFIG_MODE_INFO> &modes,
                                                       const DisplayConfig &databaseConfig) {
    ActiveDisplaySettings settings = {};
    DISPLAYCONFIG_ROTATION rotation = path.targetInfo.rotation;

    // The source mode is always the unrotated native size, so swap it by the target rotation to get the footprint
    bool rotated = rotation == DISPLAYCONFIG_ROTATION_ROTATE90 || rotation == DISPLAYCONFIG_ROTATION_ROTATE270;

    settings.width = (int) (rotated ? sourceMode.height : sourceMode.width);
    settings.height = (int) (rotated ? sourceMode.width : sourceMode.height);

    DISPLAYCONFIG_RATIONAL refreshRate = pathRefreshRate(path, modes.data(), (UINT32) modes.size());

    settings.refreshNumerator = (int) refreshRate.Numerator;
    settings.refreshDenominator = (int) refreshRate.Denominator;
    settings.dpiScalePercentage = currentDpiScalePercentage(path.sourceInfo);
    settings.scalingMode = scalingModeIndex(persistedScaling(databaseConfig, path.targetInfo));
    settings.orientation = orientationIndex(rotation);
    settings.desktopX = (int) sourceMode.position.x;
    settings.desktopY = (int) sourceMode.position.y;

    return settings;
}

/**
 * Gets the DPI scale percentage currently applied to the given source, converting its relative DPI scale index to an
 * absolute index into DPI_SCALE_PERCENTAGES.
 *
 * @param sourceInfo
 *            - The path source to read the DPI scale of
 *
 * @return The applied DPI scale percentage, or 0 when it could not be read
 */
static jint currentDpiScalePercentage(const DISPLAYCONFIG_PATH_SOURCE_INFO &sourceInfo) {
    DISPLAYCONFIG_GET_DPI_SCALE_INDICES dpiIndices = {};
    dpiIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
    dpiIndices.header.size = sizeof(dpiIndices);
    dpiIndices.header.adapterId = sourceInfo.adapterId;
    dpiIndices.header.id = sourceInfo.id;

    if (DisplayConfigGetDeviceInfo(&dpiIndices.header) != ERROR_SUCCESS) {
        return 0;
    }

    /*
     * DPI indices are relative to the display's recommended scale, whose absolute index is the magnitude of the
     * relative minimum (absolute index 0 is the global minimum)
     */
    int32_t recommendedAbsoluteIndex = abs(dpiIndices.relativeMinimumDpiScaleIndex);
    int32_t currentAbsoluteIndex = recommendedAbsoluteIndex + dpiIndices.relativeCurrentDpiScaleIndex;

    if (currentAbsoluteIndex < 0) {
        currentAbsoluteIndex = 0;
    }

    if (currentAbsoluteIndex >= NUM_OF_DPI_SCALE_PERCENTAGES) {
        currentAbsoluteIndex = NUM_OF_DPI_SCALE_PERCENTAGES - 1;
    }

    return DPI_SCALE_PERCENTAGES.at(currentAbsoluteIndex);
}

/**
 * Gets the scaling mode persisted for the display driven by the given target. The active paths report the scaling
 * Windows is effectively doing (identity even while a lower resolution is centered), so the set mode is read instead.
 *
 * @param databaseConfig
 *            - The persisted display configuration to search
 * @param targetInfo
 *            - The active path target of the display
 *
 * @return The persisted scaling mode, or the active path's scaling when the display has none persisted
 */
static DISPLAYCONFIG_SCALING persistedScaling(const DisplayConfig &databaseConfig,
                                              const DISPLAYCONFIG_PATH_TARGET_INFO &targetInfo) {
    for (UINT32 i = 0; i < databaseConfig.numPathInfoArrayElements; i++) {
        const DISPLAYCONFIG_PATH_TARGET_INFO &databaseTarget = databaseConfig.pathInfoArray[i].targetInfo;

        // A target is identified by its adapter and its ID on that adapter
        if (databaseTarget.id == targetInfo.id && databaseTarget.adapterId.LowPart == targetInfo.adapterId.LowPart &&
            databaseTarget.adapterId.HighPart == targetInfo.adapterId.HighPart) {
            return databaseTarget.scaling;
        }
    }

    return targetInfo.scaling;
}

/**
 * Maps a DISPLAYCONFIG_SCALING value to its scaling mode index, the inverse of the mapping used when applying one.
 *
 * @param scaling
 *            - The DISPLAYCONFIG_SCALING value to map
 *
 * @return The scaling mode index (0 = aspect ratio, 1 = stretched, 2 = centered)
 */
static jint scalingModeIndex(DISPLAYCONFIG_SCALING scaling) {
    switch (scaling) {
    case DISPLAYCONFIG_SCALING_STRETCHED:
        // Stretch to fill panel
        return 1;
    case DISPLAYCONFIG_SCALING_CENTERED:
        // Centered in panel
        return 2;
    default:
        // Preserve aspect ratio, which also covers the identity, preferred, and custom values a driver may report
        return 0;
    }
}

/**
 * Maps a DISPLAYCONFIG_ROTATION value to its orientation index, the inverse of the mapping used when applying one.
 *
 * @param rotation
 *            - The DISPLAYCONFIG_ROTATION value to map
 *
 * @return The orientation index (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 */
static int orientationIndex(DISPLAYCONFIG_ROTATION rotation) {
    switch (rotation) {
    case DISPLAYCONFIG_ROTATION_ROTATE90:
        // Portrait mode
        return 1;
    case DISPLAYCONFIG_ROTATION_ROTATE180:
        // Inverted landscape mode
        return 2;
    case DISPLAYCONFIG_ROTATION_ROTATE270:
        // Inverted portrait mode
        return 3;
    default:
        // Landscape mode
        return 0;
    }
}
