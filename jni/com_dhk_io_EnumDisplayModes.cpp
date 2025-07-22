/*
 * Enumerates the display modes in descending order for a given display.
 *
 * Author: Jonathan Miller
 * License: The MIT License - https://mit-license.org/
 *
 * Copyright © 2025 Jonathan Miller
 */

#include <jni.h>
#include "DisplayConfig.h"
#include "com_dhk_io_EnumDisplayModes.h"

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
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_EnumDisplayModes_enumDisplayModes(JNIEnv *env, jobject obj,
        jstring displayId) {
    DEVMODE displayMode;

    SecureZeroMemory(&displayMode, sizeof(DEVMODE));
    displayMode.dmSize = sizeof(displayMode);

    DISPLAY_DEVICE displayDevice;

    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    jboolean isCopy;
    const char *displayIdChars = (env)->GetStringUTFChars(displayId, &isCopy);
    string displayIdString = displayIdChars;
    int displayIndex = getEnumDisplayDevicesDisplayIdIndex(displayIdString);

    EnumDisplayDevices(NULL, displayIndex, &displayDevice, 0);

    bool enumDisplaySettingsResult = true;
    UINT32 displayModeIndex = 0;
    vector<DEVMODE> displayModesVector;

    while (enumDisplaySettingsResult == true) {
        if (displayDevice.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            enumDisplaySettingsResult = EnumDisplaySettings(displayDevice.DeviceName, displayModeIndex, &displayMode);
            displayModesVector.push_back(displayMode);

            displayModeIndex++;
        }
    }

    jclass displayModeClass = env->FindClass("java/awt/DisplayMode");

    if (displayModeClass == NULL) {
        return NULL;
    }

    jmethodID displayModeConstructor = env->GetMethodID(displayModeClass, "<init>", "(IIII)V");

    if (displayModeConstructor == NULL) {
        return NULL;
    }

    jobjectArray displayModeArray = env->NewObjectArray(displayModesVector.size(), displayModeClass, NULL);

    if (displayModeArray == NULL) {
        return NULL;
    }

    displayModeIndex = 0;

    for (const DEVMODE &displayMode : displayModesVector) {
        jobject displayModeObject = env->NewObject(displayModeClass, displayModeConstructor, displayMode.dmPelsWidth,
                displayMode.dmPelsHeight, displayMode.dmBitsPerPel, displayMode.dmDisplayFrequency);

        if (displayModeObject == NULL) {
            for (int j = 0; j < displayModeIndex; j++) {
                jobject displayModeToDelete = (jobject) env->GetObjectArrayElement(displayModeArray, j);
                env->DeleteLocalRef(displayModeToDelete);
            }

            env->DeleteLocalRef(displayModeArray);

            return NULL;
        }

        env->SetObjectArrayElement(displayModeArray, displayModeIndex, displayModeObject);
        env->DeleteLocalRef(displayModeObject);

        displayModeIndex++;
    }

    return displayModeArray;
}
