/*
 * Original Author: Jonathan Miller
 * Version: 1.0.0.0
 * 
 * Description: Immediately apply display settings for a given display.
 * 
 * License: The MIT License - https://mit-license.org/
 * Copyright (c) 2024 Jonathan Miller
 */

#include <jni.h>

#include "DisplayConfig.h"
#include "com_dhk_io_SetDisplay.h"

using namespace std;

// Forward declarations of functions.
void setDisplayMode(UINT32 displayIndex, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate);
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode);
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage);

/*
 * This function updates the display mode, display scaling mode, and DPI scale percentage for the given display.
 *
 * @param env 				 - The structure containing methods to use to to access Java elements.
 * @param obj 				 - The reference to the Java native object instance.
 * @param displayId 		 - The ID of the display to apply the display settings for.
 * @param resWidth     		 - The new horizontal resolution for the given display.
 * @param resHeight    		 - The new vertical resolution for the given display.
 * @param bitDepth     		 - The new bit depth for the given display.
 * @param refreshRate  		 - The new refresh rate for the given display.
 * @param scalingMode  		 - The new scaling mode for the given display.
 * @param dpiScalePercentage - The new DPI scale percentage for the given display.
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setDisplay(JNIEnv *env, jobject obj, jstring displayId, jint resWidth,
        jint resHeight, jint bitDepth, jint refreshRate, jint scalingMode, jint dpiScalePercentage) {
    // Convert the display ID jstring to a string.
    jboolean isCopy;
    const char *displayIdChars = (env)->GetStringUTFChars(displayId, &isCopy);
    string displayIdString = displayIdChars;

    // Get the display index in the EnumDisplayDevices display ID vector for the give display.
    int enumDisplayDevicesDisplayIdIndex = getEnumDisplayDevicesDisplayIdIndex(displayIdString);

    // Get the display index in the QueryDisplayConfig display ID vector for the give display.
    int queryDisplayConfigDisplayIdIndex = getQueryDisplayConfigDisplayIdIndex(displayIdString);

    // Set the display mode for the given display.
    setDisplayMode(enumDisplayDevicesDisplayIdIndex, resWidth, resHeight, bitDepth, refreshRate);

    // Set the display scaling mode for the given display.
    setDisplayScalingMode(queryDisplayConfigDisplayIdIndex, scalingMode);

    // Set the DPI scale percentage for the given display.
    setDpiScalePercentage(queryDisplayConfigDisplayIdIndex, dpiScalePercentage);
}

/*
 * This function sets the display mode for the given display.
 *
 * @param displayIndex - The index of the display to set the display mode for.
 * @param resWidth     - The new horizontal resolution to apply for the given display.
 * @param resHeight    - The new vertical resolution to apply for the given display.
 * @param bitDepth     - The new bit depth to apply for the given display.
 * @param refreshRate  - The new refresh rate to apply for the given display.
 */
void setDisplayMode(UINT32 displayIndex, UINT32 resWidth, UINT32 resHeight, UINT32 bitDepth, UINT32 refreshRate) {
    // Declare a DEVMODE structure to hold the display mode.
    DEVMODE devMode;

    // Initialize the DEVMODE structure.
    SecureZeroMemory(&devMode, sizeof(DEVMODE));
    devMode.dmSize = sizeof(devMode);

    // Declare a DISPLAY_DEVICE structure to hold the device name for the given display index.
    DISPLAY_DEVICE displayDevice;

    // Initialize the DISPLAY_DEVICE structure.
    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    // Update the devmode members with the given values.
    devMode.dmPelsWidth = resWidth;
    devMode.dmPelsHeight = resHeight;
    devMode.dmBitsPerPel = bitDepth;
    devMode.dmDisplayFrequency = refreshRate;
    devMode.dmDriverExtra = 0;
    devMode.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;

    // Get the display device info for the given display index.
    EnumDisplayDevices(NULL, displayIndex, &displayDevice, 0);

    // Apply the display mode for the given display.
    LONG changeDisplaySettingsResult = ChangeDisplaySettingsEx(displayDevice.DeviceName, &devMode, NULL,
    CDS_UPDATEREGISTRY, NULL);

    // Check if the display mode could be set, and output an error message if it failed.
    if (changeDisplaySettingsResult != DISP_CHANGE_SUCCESSFUL) {
        cerr << "Failed to set the display mode! Error Code: " << changeDisplaySettingsResult << endl;
    }
}

/*
 * This function sets the display scaling mode for the given display.
 *
 * @param displayIndex - The index of the display to set the scaling mode for.
 * @param scalingMode  - The new display scaling mode to apply for the given display.
 */
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode) {
    // Variable to hold the new display scaling mode.
    DISPLAYCONFIG_SCALING displayConfigScalingMode;

    // Update the display config scaling mode corresponding to the given scaling mode value.
    switch (scalingMode) {
    case 0:
        displayConfigScalingMode = DISPLAYCONFIG_SCALING_ASPECTRATIOCENTEREDMAX; // Preserve aspect ratio.
        break;
    case 1:
        displayConfigScalingMode = DISPLAYCONFIG_SCALING_STRETCHED; // Stretch to fill panel.
        break;
    case 2:
        displayConfigScalingMode = DISPLAYCONFIG_SCALING_CENTERED; // Centered in panel.
        break;
    default:
        displayConfigScalingMode = DISPLAYCONFIG_SCALING_ASPECTRATIOCENTEREDMAX; // Preserve aspect ratio by default.
        break;
    }

    // Initialize a structure to hold the active paths as defined in the persistence database for the currently
    // connected displays.
    DisplayConfig displayConfig = getDisplayConfig();

    // Set the new display scaling mode for the given display.
    displayConfig.pathInfoArray[displayIndex].targetInfo.scaling = displayConfigScalingMode;

    // Apply the new display scaling mode.
    LONG setDisplayConfigResult = SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
            displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray, SDC_APPLY |
            SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);

    // Check if the display configuration could be set, and output an error message if it failed.
    if (setDisplayConfigResult != ERROR_SUCCESS) {
        cerr << "Failed to set the display configuration! Error Code: " << setDisplayConfigResult << endl;
    }
}

/*
 * This function sets the DPI scale percentage for the given display.
 *
 * @param displayIndex       - The index of the display to set the DPI scale percentage for.
 * @param dpiScalePercentage - The new DPI scale percentage to apply for the given display.
 */
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage) {
    // Initialize a structure to hold the active paths as defined in the persistence database for the currently
    // connected displays.
    DisplayConfig displayConfig = getDisplayConfig();

    // Declare a structure to get the relative DPI scale percentage indices for the given display.
    DISPLAYCONFIG_GET_DPI_SCALE_INDICES getDpiScaleIndices = { };

    // Initialize the structure to get the relative DPI scale percentage indices for the given display.
    getDpiScaleIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
    getDpiScaleIndices.header.size = sizeof(getDpiScaleIndices);
    getDpiScaleIndices.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    getDpiScaleIndices.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;

    // Get the device information to obtain the the relative DPI scale percentage indices for the given display.
    DisplayConfigGetDeviceInfo(&getDpiScaleIndices.header);

    // Get the index of the default DPI scale percentage.
    int32_t defaultDpiScaleIndex = abs(getDpiScaleIndices.relativeMinimumDpiScaleIndex);

    // Define an integer to hold the index of the DPI scale percentage to set.
    int32_t dpiScaleIndex = 0;

    // For each possible defined DPI scale percentage...
    for (int32_t i = 0; i < NUM_OF_DPI_SCALE_PERCENTAGES; i++) {
        // Store the index of the found DPI scale percentage.
        if (dpiScalePercentage == DPI_SCALE_PERCENTAGES.at(i)) {
            dpiScaleIndex = i;
        }
    }

    // Define an integer to hold the relative DPI scale percentage index for the given DPI scale percentage compared to
    // the default index.
    int32_t relativeDpiScaleIndex = 0;

    // Calculate the relative DPI scale percentage index.
    relativeDpiScaleIndex = dpiScaleIndex - defaultDpiScaleIndex;

    // Declare a structure to hold the DPI scale percentage index to set for the given display.
    DISPLAYCONFIG_SET_DPI_SCALE_INDEX setDpiScaleIndex = { };

    // Initialize the structure to set the DPI scale percentage index to set for the given display.
    setDpiScaleIndex.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE;
    setDpiScaleIndex.header.size = sizeof(setDpiScaleIndex);
    setDpiScaleIndex.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    setDpiScaleIndex.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;
    setDpiScaleIndex.relativeDpiScaleIndex = relativeDpiScaleIndex;

    // Set the given DPI scale percentage value using the relative DPI scale percentage index.
    bool setDpiScaleResult = DisplayConfigSetDeviceInfo(&setDpiScaleIndex.header);

    // Check if the DPI scale percentage could be set, and output an error message if it failed.
    if (setDpiScaleResult != ERROR_SUCCESS) {
        cerr << "Failed to set the DPI scale percentage! Error Code : " << setDpiScaleResult << endl;
    }
}
