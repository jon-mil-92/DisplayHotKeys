/*
 * Gets current display settings for a given display.
 *
 * License:
 *
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

#include <jni.h>
#include "DisplayConfig.h"
#include "com_dhk_io_GetDisplay.h"

using namespace std;

/*
 * Outputs an array of supported display modes for the given display
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 * @param displayId
 *            - The ID of the display to get the display modes for
 *
 * @return The array of supported display modes for the given display
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_GetDisplay_enumDisplayModes(JNIEnv *env, jobject obj,
        jstring displayId) {
    // We'll collect mode fields into a small POD so we can enumerate using either ANSI or Unicode DEVMODE
    struct ModeInfo {
        int width;
        int height;
        int bitsPerPel;
        int frequency;
    };

    DISPLAY_DEVICE displayDevice;

    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    jboolean isCopy;
    const char *displayIdChars = (env)->GetStringUTFChars(displayId, &isCopy);
    string displayIdString = displayIdChars;

    /*
     * Try to map the incoming display ID (which comes from QueryDisplayConfig/DisplayConfigGetDeviceInfo)
     * to a source/GDI device name and enumerate modes from that. This avoids mixing ID formats between
     * QueryDisplayConfig and EnumDisplayDevices which can fail for virtual displays.
     */
    DisplayConfig displayConfig = getDisplayConfig();
    int qIndex = getQueryDisplayConfigDisplayIdIndex(displayIdString);

    UINT32 displayModeIndex = 0;
    vector<ModeInfo> displayModesVector;

    bool enumerated = false;

    if (qIndex >= 0 && (UINT32) qIndex < displayConfig.numPathInfoArrayElements) {
        // Build a source name structure to get the GDI device name (viewGdiDeviceName)
        DISPLAYCONFIG_SOURCE_DEVICE_NAME sourceName = { };
        sourceName.header.adapterId = displayConfig.pathInfoArray[qIndex].sourceInfo.adapterId;
        sourceName.header.id = displayConfig.pathInfoArray[qIndex].sourceInfo.id;
        sourceName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME;
        sourceName.header.size = sizeof(sourceName);

        LONG displayConfigGetDeviceInfoResult = DisplayConfigGetDeviceInfo(&sourceName.header);

        if (displayConfigGetDeviceInfoResult == ERROR_SUCCESS) {
            // Use the wide-character GDI name directly with EnumDisplaySettingsW
            DEVMODEW displayModeW;
            SecureZeroMemory(&displayModeW, sizeof(DEVMODEW));
            displayModeW.dmSize = sizeof(displayModeW);

            while (EnumDisplaySettingsW(sourceName.viewGdiDeviceName, displayModeIndex, &displayModeW)) {
                ModeInfo modeInfo;
                modeInfo.width = displayModeW.dmPelsWidth;
                modeInfo.height = displayModeW.dmPelsHeight;
                modeInfo.bitsPerPel = displayModeW.dmBitsPerPel;
                modeInfo.frequency = displayModeW.dmDisplayFrequency;
                displayModesVector.push_back(modeInfo);
                displayModeIndex++;
            }

            enumerated = true;
        }
    }

    // If we couldn't enumerate via QueryDisplayConfig mapping, fall back to EnumDisplayDevices logic
    if (!enumerated) {
        int displayIndex = getEnumDisplayDevicesDisplayIdIndex(displayIdString);
        BOOL enumDisplayDevicesResult = EnumDisplayDevices(NULL, displayIndex, &displayDevice, 0);

        if (!enumDisplayDevicesResult) {
            env->ReleaseStringUTFChars(displayId, displayIdChars);
            return NULL;
        }

        // Only attempt to enumerate display modes if the device is attached to the desktop
        if (displayDevice.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            DEVMODEA displayModeA;
            SecureZeroMemory(&displayModeA, sizeof(DEVMODEA));
            displayModeA.dmSize = sizeof(displayModeA);

            while (EnumDisplaySettingsA(displayDevice.DeviceName, displayModeIndex, &displayModeA)) {
                ModeInfo modeInfo;
                modeInfo.width = displayModeA.dmPelsWidth;
                modeInfo.height = displayModeA.dmPelsHeight;
                modeInfo.bitsPerPel = displayModeA.dmBitsPerPel;
                modeInfo.frequency = displayModeA.dmDisplayFrequency;
                displayModesVector.push_back(modeInfo);
                displayModeIndex++;
            }
        }
    }

    jclass displayModeClass = env->FindClass("java/awt/DisplayMode");

    if (displayModeClass == NULL) {
        env->ReleaseStringUTFChars(displayId, displayIdChars);
        return NULL;
    }

    jmethodID displayModeConstructor = env->GetMethodID(displayModeClass, "<init>", "(IIII)V");

    if (displayModeConstructor == NULL) {
        env->DeleteLocalRef(displayModeClass);
        env->ReleaseStringUTFChars(displayId, displayIdChars);
        return NULL;
    }

    jobjectArray displayModeArray = env->NewObjectArray(displayModesVector.size(), displayModeClass, NULL);

    if (displayModeArray == NULL) {
        env->DeleteLocalRef(displayModeClass);
        env->ReleaseStringUTFChars(displayId, displayIdChars);
        return NULL;
    }

    displayModeIndex = 0;

    for (const ModeInfo &modeInfo : displayModesVector) {
        jobject displayModeObject = env->NewObject(displayModeClass, displayModeConstructor, modeInfo.width,
                modeInfo.height, modeInfo.bitsPerPel, modeInfo.frequency);

        if (displayModeObject == NULL) {
            for (int j = 0; j < displayModeIndex; j++) {
                jobject displayModeToDelete = (jobject) env->GetObjectArrayElement(displayModeArray, j);
                env->DeleteLocalRef(displayModeToDelete);
            }

            env->DeleteLocalRef(displayModeArray);
            env->DeleteLocalRef(displayModeClass);
            env->ReleaseStringUTFChars(displayId, displayIdChars);

            return NULL;
        }

        env->SetObjectArrayElement(displayModeArray, displayModeIndex, displayModeObject);
        env->DeleteLocalRef(displayModeObject);

        displayModeIndex++;
    }

    env->DeleteLocalRef(displayModeClass);
    env->ReleaseStringUTFChars(displayId, displayIdChars);

    return displayModeArray;
}

/*
 * Gets the current number of connected displays.
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 *
 * @return The current number of connected displays
 */
JNIEXPORT jint JNICALL Java_com_dhk_io_GetDisplay_queryNumOfConnectedDisplays(JNIEnv *env, jobject obj) {
    /*
     * Initialize a structure to hold the active paths as defined in the persistence database for the currently
     * connected displays.
     */
    DisplayConfig displayConfig = getDisplayConfig();

    return displayConfig.numPathInfoArrayElements;
}

/*
 * Gets the IDs for the connected displays.
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 *
 * @return An array of IDs for the connected displays
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_GetDisplay_enumDisplayIds(JNIEnv *env, jobject obj) {
    vector<string> displayIdsVector = getQueryDisplayConfigDisplayIds();
    jclass stringClass = env->FindClass("java/lang/String");

    if (stringClass == NULL) {
        return NULL;
    }

    jobjectArray displayIdsArray = env->NewObjectArray(displayIdsVector.size(), stringClass, NULL);

    if (displayIdsArray == NULL) {
        env->DeleteLocalRef(stringClass);
        return NULL;
    }

    for (int displayIndex = 0; displayIndex < displayIdsVector.size(); displayIndex++) {
        jstring displayId = env->NewStringUTF(displayIdsVector.at(displayIndex).c_str());

        if (displayId == NULL) {
            for (int j = 0; j < displayIndex; j++) {
                jstring displayIdToDelete = (jstring) env->GetObjectArrayElement(displayIdsArray, j);
                env->DeleteLocalRef(displayIdToDelete);
            }

            env->DeleteLocalRef(displayIdsArray);
            env->DeleteLocalRef(stringClass);

            return NULL;
        }

        env->SetObjectArrayElement(displayIdsArray, displayIndex, displayId);
        env->DeleteLocalRef(displayId);
    }

    env->DeleteLocalRef(stringClass);

    return displayIdsArray;
}

/*
 * Gets the orientation for the given display.
 *
 * @param displayIndex
 *            - The index of the display to get the orientation for
 *
 * @return 1 for Landscape, 2 for Portrait, 3 for Inverted Landscape, 4 for Inverted Portrait
 */
JNIEXPORT jint JNICALL Java_com_dhk_io_GetDisplay_queryDisplayOrientation(JNIEnv *env, jobject obj, jint displayIndex) {
    DisplayConfig displayConfig = getDisplayConfig();
    DISPLAYCONFIG_ROTATION orientation = displayConfig.pathInfoArray[displayIndex].targetInfo.rotation;

    return orientation;
}
