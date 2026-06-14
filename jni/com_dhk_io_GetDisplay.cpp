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
