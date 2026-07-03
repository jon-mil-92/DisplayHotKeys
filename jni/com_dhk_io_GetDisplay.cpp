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
#include "DisplayConfig.h"

#include <jni.h>
#include <map>
#include <set>
#include <tuple>
#include <utility>
#include <vector>
#include <windows.h>

using namespace std;

/**
 * A single supported display mode collected during enumeration.
 */
struct ModeInfo {
    /**
     * Horizontal resolution in pixels.
     */
    int width;

    /**
     * Vertical resolution in pixels.
     */
    int height;

    /**
     * Color depth in bits per pixel.
     */
    int bitsPerPel;

    /**
     * Refresh rate in hertz.
     */
    int frequency;
};

/**
 * Adds the refresh rates a GPU-scaled custom resolution supports but the legacy mode table omits, identifying such a
 * resolution by its small distinct-rate count. To keep the expensive SDC_VALIDATE probes to a minimum it finds each
 * resolution's drivable ceiling by validating only the panel rates above its existing maximum, highest first, stopping
 * at the first that validates; every panel rate at or below that ceiling is then added without a probe, since a fixed
 * resolution's bandwidth scales with refresh rate. The active config is queried once and reused, the validating
 * rational form is cached, and ordinary multi-rate resolutions are skipped.
 *
 * @param displayId
 *            - The stable display ID being enumerated
 * @param modes
 *            - The mode list to augment in place
 */
static void addCustomResolutionRefreshRates(const string &displayId, vector<ModeInfo> &modes) {
    const size_t MAX_RATES_FOR_CUSTOM_RESOLUTION = 2;

    // Query the active CCD configuration once and locate the display's path once; every probe below reuses them
    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> ccdModes;

    if (!queryActiveCcdConfig(paths, ccdModes)) {
        return;
    }

    int pathIndex = findActivePathForDisplay(paths, displayId);

    if (pathIndex < 0) {
        return;
    }

    // Distinct rates per resolution, since the driver lists each mode many times across bit depths and flags
    set<int> panelRates;
    map<pair<int, int>, set<int>> ratesByResolution;

    for (const ModeInfo &mode : modes) {
        panelRates.insert(mode.frequency);
        ratesByResolution[make_pair(mode.width, mode.height)].insert(mode.frequency);
    }

    vector<int> candidateRates(panelRates.begin(), panelRates.end());

    // The rational form (integer or NTSC fractional) that first validated for a rate, reused as the first try elsewhere
    map<int, DISPLAYCONFIG_RATIONAL> workingRational;

    /*
     * Validates a single rate at a resolution by trying the form that already worked for this rate first and then the
     * remaining candidate forms. SDC_VALIDATE does not change the display, and the rate is drivable if any form works
     */
    auto validatesRate = [&](int width, int height, int rate) {
        vector<DISPLAYCONFIG_RATIONAL> forms;
        map<int, DISPLAYCONFIG_RATIONAL>::iterator cached = workingRational.find(rate);

        if (cached != workingRational.end()) {
            forms.push_back(cached->second);
        }

        for (const DISPLAYCONFIG_RATIONAL &rational : toRefreshRationalCandidates(rate)) {
            bool alreadyQueued = cached != workingRational.end() && rational.Numerator == cached->second.Numerator &&
                                 rational.Denominator == cached->second.Denominator;

            if (!alreadyQueued) {
                forms.push_back(rational);
            }
        }

        for (const DISPLAYCONFIG_RATIONAL &rational : forms) {
            if (submitCcdSourceMode(paths, ccdModes, pathIndex, (UINT32) width, (UINT32) height, rational,
                                    SDC_VALIDATE | SDC_USE_SUPPLIED_DISPLAY_CONFIG) == ERROR_SUCCESS) {
                workingRational[rate] = rational;
                return true;
            }
        }

        return false;
    };

    for (const pair<const pair<int, int>, set<int>> &entry : ratesByResolution) {
        if (entry.second.size() > MAX_RATES_FOR_CUSTOM_RESOLUTION) {
            continue;
        }

        int width = entry.first.first;
        int height = entry.first.second;
        const set<int> &existingRates = entry.second;
        int maxExistingRate = *existingRates.rbegin();

        /*
         * Raise the drivable ceiling by validating panel rates above the existing max, highest first, stopping at the
         * first that validates. The existing max is already drivable, so it stays the ceiling if nothing higher works
         */
        int ceilingRate = maxExistingRate;

        for (vector<int>::const_reverse_iterator it = candidateRates.rbegin(); it != candidateRates.rend(); ++it) {
            if (*it <= maxExistingRate) {
                break;
            }

            if (validatesRate(width, height, *it)) {
                ceilingRate = *it;
                break;
            }
        }

        /*
         * Every panel rate at or below the ceiling is drivable (lower rate = lower pixel clock), so add the ones the
         * legacy table omitted without probing
         */
        for (int rate : candidateRates) {
            if (rate <= ceilingRate && existingRates.find(rate) == existingRates.end()) {
                modes.push_back({width, height, 32, rate});
            }
        }
    }
}

/**
 * Enumerates the supported display modes for the given display. Modes are read with EnumDisplaySettingsExW via the GDI
 * device name from QueryDisplayConfig, falling back to an EnumDisplayDevices index. Default (EDID-pruned) enumeration
 * mirrors Windows Advanced Display Settings; interlaced and zero-size modes are skipped. GPU-scaled custom resolutions
 * are then expanded with their other supported refresh rates via the CCD API. The returned set is de-duplicated in
 * first-seen order so the caller needs no distinct pass. Wide-character (Unicode) APIs are used throughout to avoid
 * ANSI/Unicode mismatches.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The Java GetDisplay instance
 * @param displayId
 *            - The stable display ID to enumerate modes for
 *
 * @return A DisplayMode[] of supported modes, an empty array if none, or null on unrecoverable native failure
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_GetDisplay_enumDisplayModes(JNIEnv *env, jobject obj,
                                                                           jstring displayId) {
    if (displayId == nullptr) {
        return nullptr;
    }

    // Acquire UTF chars for the incoming jstring and ensure we release them on all paths
    jboolean isCopy = JNI_FALSE;
    const char *displayIdChars = env->GetStringUTFChars(displayId, &isCopy);

    if (displayIdChars == nullptr) {
        return nullptr;
    }

    string stableDisplayId = displayIdChars;

    vector<ModeInfo> modeList;

    // Skip zero-size and interlaced modes so the reported set mirrors Advanced Display Settings (progressive only)
    auto collectMode = [&modeList](const DEVMODEW &devModeW) {
        if (devModeW.dmPelsWidth == 0 || devModeW.dmPelsHeight == 0) {
            return;
        }

        if ((devModeW.dmFields & DM_DISPLAYFLAGS) && (devModeW.dmDisplayFlags & DM_INTERLACED)) {
            return;
        }

        modeList.push_back({static_cast<int>(devModeW.dmPelsWidth), static_cast<int>(devModeW.dmPelsHeight),
                            static_cast<int>(devModeW.dmBitsPerPel), static_cast<int>(devModeW.dmDisplayFrequency)});
    };

    // Default (EDID-pruned) enumeration so the reported set matches Advanced Display Settings
    auto enumerateModes = [&collectMode](LPCWSTR gdiDeviceName) {
        DEVMODEW devModeW;
        SecureZeroMemory(&devModeW, sizeof(DEVMODEW));
        devModeW.dmSize = sizeof(devModeW);

        for (UINT32 i = 0; EnumDisplaySettingsExW(gdiDeviceName, i, &devModeW, 0); i++) {
            collectMode(devModeW);
        }
    };

    /*
     * Match the display's active path in the current configuration and enumerate its modes by its GDI device name,
     * matching here rather than via getQueryDisplayConfigDisplayIdIndex to avoid a second full QueryDisplayConfig
     */
    DisplayConfig displayConfig = getDisplayConfig();
    wstring gdiDeviceName;

    for (UINT32 i = 0; i < displayConfig.numPathInfoArrayElements; i++) {
        if (stableIdForTarget(displayConfig.pathInfoArray[i].targetInfo) == stableDisplayId) {
            gdiDeviceName = sourceGdiDeviceName(displayConfig.pathInfoArray[i].sourceInfo);
            break;
        }
    }

    if (!gdiDeviceName.empty()) {
        enumerateModes(gdiDeviceName.c_str());
    } else {
        // Otherwise locate the display by its EnumDisplayDevices index and enumerate by its device name
        DISPLAY_DEVICEW displayDeviceW;
        SecureZeroMemory(&displayDeviceW, sizeof(DISPLAY_DEVICEW));
        displayDeviceW.cb = sizeof(displayDeviceW);

        int enumDisplayIndex = getEnumDisplayDevicesDisplayIdIndex(stableDisplayId);

        if (EnumDisplayDevicesW(NULL, enumDisplayIndex, &displayDeviceW, 0)) {
            if (displayDeviceW.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
                enumerateModes(displayDeviceW.DeviceName);
            }
        } else {
            // Fallback enumeration failed; release and return null
            env->ReleaseStringUTFChars(displayId, displayIdChars);
            return nullptr;
        }
    }

    env->ReleaseStringUTFChars(displayId, displayIdChars);

    // Expand GPU-scaled custom resolutions with the extra refresh rates the legacy mode table omits
    addCustomResolutionRefreshRates(stableDisplayId, modeList);

    /*
     * De-duplicate in place, preserving first-seen order, since EnumDisplaySettingsExW repeats each mode across bit
     * depths and flags and the custom-resolution expansion may re-add a rate; the caller then needs no distinct pass
     */
    set<tuple<int, int, int, int>> seenModes;
    vector<ModeInfo> uniqueModes;

    for (const ModeInfo &mode : modeList) {
        if (seenModes.insert(make_tuple(mode.width, mode.height, mode.bitsPerPel, mode.frequency)).second) {
            uniqueModes.push_back(mode);
        }
    }

    modeList = std::move(uniqueModes);

    // Prepare Java DisplayMode class and constructor
    jclass displayModeClass = env->FindClass("java/awt/DisplayMode");

    if (displayModeClass == nullptr) {
        return nullptr;
    }

    jmethodID displayModeCtor = env->GetMethodID(displayModeClass, "<init>", "(IIII)V");

    if (displayModeCtor == nullptr) {
        env->DeleteLocalRef(displayModeClass);
        return nullptr;
    }

    // Create the Java array (may be empty if no modes found)
    jsize modeCount = static_cast<jsize>(modeList.size());
    jobjectArray displayModeArray = env->NewObjectArray(modeCount, displayModeClass, nullptr);

    if (displayModeArray == nullptr) {
        env->DeleteLocalRef(displayModeClass);
        return nullptr;
    }

    // Populate the Java array
    for (jsize i = 0; i < modeCount; ++i) {
        const ModeInfo &modeInfo = modeList[static_cast<size_t>(i)];
        jobject displayModeObj = env->NewObject(displayModeClass, displayModeCtor, modeInfo.width, modeInfo.height,
                                                modeInfo.bitsPerPel, modeInfo.frequency);

        if (displayModeObj == nullptr) {
            // Clean up created local refs and return null
            for (jsize j = 0; j < i; ++j) {
                jobject tmp = (jobject) env->GetObjectArrayElement(displayModeArray, j);
                env->DeleteLocalRef(tmp);
            }

            env->DeleteLocalRef(displayModeArray);
            env->DeleteLocalRef(displayModeClass);

            return nullptr;
        }

        env->SetObjectArrayElement(displayModeArray, i, displayModeObj);
        env->DeleteLocalRef(displayModeObj);
    }

    env->DeleteLocalRef(displayModeClass);

    return displayModeArray;
}

/**
 * Gets the stabilized IDs for visible displays.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The Java GetDisplay instance
 *
 * @return A Java String[] of stable display IDs for visible displays
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
 * Computes the DPI scale percentages Windows supports for the given resolution. Windows caps the maximum scale so the
 * effective (logical) resolution stays usable, so fewer scales are offered as resolution drops and only 100% remains at
 * very low resolutions. The set always starts at 100% and includes each higher percentage while its effective
 * resolution stays at or above the usable floor on both edges. Since Windows reports the live range only for the
 * applied resolution, this computation lets a not-yet-applied resolution report its scales without a mode change.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The Java GetDisplay instance
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
 * Gets the orientation for the given display.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The Java GetDisplay instance
 * @param index
 *            - The index of the display to query
 *
 * @return The orientation value (1 = Landscape, 2 = Portrait, etc.)
 */
JNIEXPORT jint JNICALL Java_com_dhk_io_GetDisplay_queryDisplayOrientation(JNIEnv *env, jobject obj, jint index) {
    (void) env;
    (void) obj;
    DisplayConfig config = getDisplayConfig();

    return (jint) config.pathInfoArray[index].targetInfo.rotation;
}
