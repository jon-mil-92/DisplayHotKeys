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
#include <windows.h>

using namespace std;

/**
 * Enumerates supported display modes for the given display. DisplayConfigGetDeviceInfo is used to obtain the GDI device
 * name, and modes are enumerated with EnumDisplaySettingsW. If that fails, fall back to an EnumDisplayDevices index and
 * enumerate modes with EnumDisplaySettingsW. Wide-character Windows APIs (Unicode) are used throughout to avoid
 * ANSI/Unicode mismatches.
 *
 * @param env
 *        - The JNI environment pointer
 * @param obj
 *        - The Java GetDisplay instance
 * @param displayId
 *        - The stable display ID to enumerate modes for
 *
 * @return A DisplayMode[] containing the supported display modes, an empty array if
 *         no modes are reported, or null on unrecoverable native failure
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_GetDisplay_enumDisplayModes(JNIEnv *env, jobject obj,
                                                                           jstring displayId) {
    struct ModeInfo {
        int width;
        int height;
        int bitsPerPel;
        int frequency;
    };

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
    UINT32 modeEnumIndex = 0;
    bool enumeratedFromQueryConfig = false;

    // Primary path: QueryDisplayConfig -> DisplayConfigGetDeviceInfo -> EnumDisplaySettingsW
    DisplayConfig displayConfig = getDisplayConfig();
    int queryConfigIndex = getQueryDisplayConfigDisplayIdIndex(stableDisplayId);

    if (queryConfigIndex >= 0 && (UINT32) queryConfigIndex < displayConfig.numPathInfoArrayElements) {
        DISPLAYCONFIG_SOURCE_DEVICE_NAME sourceName = {};
        sourceName.header.adapterId = displayConfig.pathInfoArray[queryConfigIndex].sourceInfo.adapterId;
        sourceName.header.id = displayConfig.pathInfoArray[queryConfigIndex].sourceInfo.id;
        sourceName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME;
        sourceName.header.size = sizeof(sourceName);

        if (DisplayConfigGetDeviceInfo(&sourceName.header) == ERROR_SUCCESS) {
            DEVMODEW devModeW;
            SecureZeroMemory(&devModeW, sizeof(DEVMODEW));
            devModeW.dmSize = sizeof(devModeW);

            modeEnumIndex = 0;

            while (EnumDisplaySettingsW(sourceName.viewGdiDeviceName, modeEnumIndex, &devModeW)) {
                modeList.push_back({static_cast<int>(devModeW.dmPelsWidth), static_cast<int>(devModeW.dmPelsHeight),
                                    static_cast<int>(devModeW.dmBitsPerPel),
                                    static_cast<int>(devModeW.dmDisplayFrequency)});
                modeEnumIndex++;
            }

            enumeratedFromQueryConfig = true;
        }
    }

    // Fallback path: EnumDisplayDevicesW -> EnumDisplaySettingsW
    if (!enumeratedFromQueryConfig) {
        DISPLAY_DEVICEW displayDeviceW;
        SecureZeroMemory(&displayDeviceW, sizeof(DISPLAY_DEVICEW));
        displayDeviceW.cb = sizeof(displayDeviceW);

        int enumDisplayIndex = getEnumDisplayDevicesDisplayIdIndex(stableDisplayId);

        if (EnumDisplayDevicesW(NULL, enumDisplayIndex, &displayDeviceW, 0)) {
            if (displayDeviceW.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
                DEVMODEW devModeW;
                SecureZeroMemory(&devModeW, sizeof(DEVMODEW));
                devModeW.dmSize = sizeof(devModeW);

                modeEnumIndex = 0;

                while (EnumDisplaySettingsW(displayDeviceW.DeviceName, modeEnumIndex, &devModeW)) {
                    modeList.push_back({static_cast<int>(devModeW.dmPelsWidth), static_cast<int>(devModeW.dmPelsHeight),
                                        static_cast<int>(devModeW.dmBitsPerPel),
                                        static_cast<int>(devModeW.dmDisplayFrequency)});
                    modeEnumIndex++;
                }
            }
        } else {
            // Fallback enumeration failed; release and return null
            env->ReleaseStringUTFChars(displayId, displayIdChars);
            return nullptr;
        }
    }

    env->ReleaseStringUTFChars(displayId, displayIdChars);

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
 *        - The JNI environment pointer
 * @param obj
 *        - The Java GetDisplay instance
 *
 * @return A Java String[] containing stable display IDs for visible displays
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
 * Computes the DPI scale percentages Windows supports for the given resolution. Windows caps the maximum DPI scale so
 * the effective (logical) resolution stays usable, which is why fewer scales are offered as the resolution drops and
 * only 100% remains at very low resolutions. The supported set always starts at 100% and includes each higher
 * percentage while the effective resolution it produces stays at or above the usable floor on both edges. Because
 * Windows reports the live range only for the currently applied resolution, this resolution-derived computation lets a
 * not-yet-applied resolution (such as one selected in the UI) report its supported scales without changing the mode.
 *
 * @param env
 *        - The JNI environment pointer
 * @param obj
 *        - The Java GetDisplay instance
 * @param width
 *        - The horizontal resolution to compute supported DPI scale percentages for
 * @param height
 *        - The vertical resolution to compute supported DPI scale percentages for
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
 *        - The JNI environment pointer
 * @param obj
 *        - The Java GetDisplay instance
 * @param index
 *        - The index of the display to query
 *
 * @return The orientation value (1 = Landscape, 2 = Portrait, etc.)
 */
JNIEXPORT jint JNICALL Java_com_dhk_io_GetDisplay_queryDisplayOrientation(JNIEnv *env, jobject obj, jint index) {
    (void) env;
    (void) obj;
    DisplayConfig config = getDisplayConfig();

    return (jint) config.pathInfoArray[index].targetInfo.rotation;
}
