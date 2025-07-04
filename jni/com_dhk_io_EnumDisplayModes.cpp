/*
 * Original Author: Jonathan Miller
 * Version: 1.4.0.0
 *
 * Description: Enumerate the display modes in descending order for a given display.
 *
 * License: The MIT License - https://mit-license.org/
 * Copyright (c) 2024 Jonathan Miller
 */

#include <jni.h>
#include <ranges>

#include "DisplayConfig.h"
#include "com_dhk_io_EnumDisplayModes.h"

using namespace std;

/*
 * This function outputs an array of supported display modes for the given display.
 *
 * @param env 		   - The structure containing methods to use to to access Java elements.
 * @param obj		   - The reference to the Java native object instance.
 * @param displayId    - The ID of the display to get the display modes for.
 *
 * @return The array of supported display modes for the given display.
 */
JNIEXPORT jobjectArray JNICALL Java_com_dhk_io_EnumDisplayModes_enumDisplayModes(JNIEnv *env, jobject obj,
        jstring displayId) {
    // Declare a DEVMODE struct to hold a display mode.
    DEVMODE displayMode;

    // Initialize the DEVMODE struct.
    SecureZeroMemory(&displayMode, sizeof(DEVMODE));
    displayMode.dmSize = sizeof(displayMode);

    // Declare a DISPLAY_DEVICE struct to hold the device name for the given display index.
    DISPLAY_DEVICE displayDevice;

    // Initialize the DISPLAY_DEVICE struct.
    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    // Convert the display ID jstring to a string.
    jboolean isCopy;
    const char *displayIdChars = (env)->GetStringUTFChars(displayId, &isCopy);
    string displayIdString = displayIdChars;

    // Get the display index for the give display ID.
    int displayIndex = getEnumDisplayDevicesDisplayIdIndex(displayIdString);

    // Get the display device info for the given display index.
    EnumDisplayDevices(NULL, displayIndex, &displayDevice, 0);

    // Initialize a boolean that will hold the result of the display mode enumeration function.
    bool enumDisplaySettingsResult = true;

    // Initialize an integer to hold the current display mode index.
    UINT32 displayModeIndex = 0;

    // Declare a vector to hold the display modes from the enumeration.
    vector<DEVMODE> displayModesVector;

    // While a display mode could be obtained...
    while (enumDisplaySettingsResult == true) {
        // Only use the display devices that are attached to the desktop.
        if (displayDevice.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            // Retrieve the display mode at the current display mode index for the given display.
            enumDisplaySettingsResult = EnumDisplaySettings(displayDevice.DeviceName, displayModeIndex, &displayMode);

            // Add the retrieved display mode to the vector of display modes.
            displayModesVector.push_back(displayMode);

            // Move to the next display mode.
            displayModeIndex++;
        }
    }

    // Find the java.awt.DisplayMode class.
    jclass displayModeClass = env->FindClass("java/awt/DisplayMode");

    // Return null if the DisplayMode class could not be found.
    if (displayModeClass == NULL) {
        return NULL;
    }

    // Get the constructor of DisplayMode class.
    jmethodID displayModeConstructor = env->GetMethodID(displayModeClass, "<init>", "(IIII)V");

    // Return null if the DisplayMode constructor could not be found.
    if (displayModeConstructor == NULL) {
        return NULL;
    }

    // Create a jobjectArray to hold all of the JNI DisplayMode objects.
    jobjectArray displayModeArray = env->NewObjectArray(displayModesVector.size(), displayModeClass, NULL);

    // Return null if the array of DisplayMode objects could not be created.
    if (displayModeArray == NULL) {
        return NULL;
    }

    // Reset the display mode index for usage with the JNI DisplayMode array.
    displayModeIndex = 0;

    // Traverse the display modes vector in reverse order to get display modes in descending order.
    for (const auto &displayMode : displayModesVector | std::views::reverse) {
        // Create the JNI DisplayMode object.
        jobject displayModeObject = env->NewObject(displayModeClass, displayModeConstructor, displayMode.dmPelsWidth,
                displayMode.dmPelsHeight, displayMode.dmBitsPerPel, displayMode.dmDisplayFrequency);

        // Return null if the JNI DisplayMode object could not be created.
        if (displayModeObject == NULL) {
            // Clean up the previously created JNI DisplayMode objects.
            for (int j = 0; j < displayModeIndex; j++) {
                jobject displayModeToDelete = (jobject) env->GetObjectArrayElement(displayModeArray, j);
                env->DeleteLocalRef(displayModeToDelete);
            }

            // Clean up the JNI DisplayMode array.
            env->DeleteLocalRef(displayModeArray);

            return NULL;
        }

        // Add the current JNI DisplayMode to the JNI DisplayMode array, and clean up memory.
        env->SetObjectArrayElement(displayModeArray, displayModeIndex, displayModeObject);
        env->DeleteLocalRef(displayModeObject);

        // Increment the index for the JNI DisplayMode array.
        displayModeIndex++;
    }

    return displayModeArray;
}
