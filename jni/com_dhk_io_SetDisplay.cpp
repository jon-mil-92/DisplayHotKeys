/*
 * Immediately applies display settings for a given display.
 *
 * Author: Jonathan Miller
 * License: The MIT License - https://mit-license.org/
 *
 * Copyright © 2025 Jonathan Miller
 */

#include <jni.h>
#include "DisplayConfig.h"
#include "com_dhk_io_SetDisplay.h"

using namespace std;

void setDisplayMode(UINT32 displayIndex, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate);
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode);
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage);
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation);

/*
 * Updates the display mode, scaling mode, orientation, and DPI scale percentage for the given
 * display.
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 * @param displayId
 *            - The ID of the display to apply the display settings for
 * @param resWidth
 *            - The new horizontal resolution for the given display
 * @param resHeight
 *            - The new vertical resolution for the given display
 * @param bitDepth
 *            - The new bit depth for the given display
 * @param refreshRate
 *            - The new refresh rate for the given display
 * @param scalingMode
 *            - The new scaling mode for the given display
 * @param dpiScalePercentage
 *            - The new DPI scale percentage for the given display
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setDisplay(JNIEnv *env, jobject obj, jstring displayId, jint resWidth,
        jint resHeight, jint bitDepth, jint refreshRate, jint scalingMode, jint dpiScalePercentage) {
    jboolean isCopy;
    const char *displayIdChars = (env)->GetStringUTFChars(displayId, &isCopy);
    string displayIdString = displayIdChars;
    int enumDisplayDevicesDisplayIdIndex = getEnumDisplayDevicesDisplayIdIndex(displayIdString);
    int queryDisplayConfigDisplayIdIndex = getQueryDisplayConfigDisplayIdIndex(displayIdString);

    setDisplayMode(enumDisplayDevicesDisplayIdIndex, resWidth, resHeight, bitDepth, refreshRate);
    setDisplayScalingMode(queryDisplayConfigDisplayIdIndex, scalingMode);
    setDpiScalePercentage(queryDisplayConfigDisplayIdIndex, dpiScalePercentage);
}

/*
 * Updates the orientation for the given display.
 *
 * @param env
 *            - The structure containing methods to use to to access Java elements
 * @param obj
 *            - The reference to the Java native object instance
 * @param displayId
 *            - The ID of the display to apply the display settings for
 * @param orientation
 *            - The new orientation for the given display
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setOrientation(JNIEnv *env, jobject obj, jstring displayId,
        jint orientation) {
    jboolean isCopy;
    const char *displayIdChars = (env)->GetStringUTFChars(displayId, &isCopy);
    string displayIdString = displayIdChars;
    int enumDisplayDevicesDisplayIdIndex = getEnumDisplayDevicesDisplayIdIndex(displayIdString);
    int queryDisplayConfigDisplayIdIndex = getQueryDisplayConfigDisplayIdIndex(displayIdString);

    setDisplayOrientation(queryDisplayConfigDisplayIdIndex, orientation);
}

/*
 * Sets the display mode for the given display.
 *
 * @param displayIndex
 *            - The index of the display to set the display mode for
 * @param resWidth
 *            - The new horizontal resolution to apply for the given display
 * @param resHeight
 *            - The new vertical resolution to apply for the given display
 * @param bitDepth
 *            - The new bit depth to apply for the given display
 * @param refreshRate
 *            - The new refresh rate to apply for the given display
 */
void setDisplayMode(UINT32 displayIndex, UINT32 resWidth, UINT32 resHeight, UINT32 bitDepth, UINT32 refreshRate) {
    DEVMODE devMode;

    SecureZeroMemory(&devMode, sizeof(DEVMODE));
    devMode.dmSize = sizeof(devMode);

    DISPLAY_DEVICE displayDevice;

    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    devMode.dmPelsWidth = resWidth;
    devMode.dmPelsHeight = resHeight;
    devMode.dmBitsPerPel = bitDepth;
    devMode.dmDisplayFrequency = refreshRate;
    devMode.dmDriverExtra = 0;
    devMode.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;

    EnumDisplayDevices(NULL, displayIndex, &displayDevice, 0);

    LONG changeDisplaySettingsResult = ChangeDisplaySettingsEx(displayDevice.DeviceName, &devMode, NULL,
    CDS_UPDATEREGISTRY, NULL);

    if (changeDisplaySettingsResult != DISP_CHANGE_SUCCESSFUL) {
        cerr << "Failed to set the display mode! Error Code: " << changeDisplaySettingsResult << endl;
    }
}

/*
 * Sets the scaling mode for the given display.
 *
 * @param displayIndex
 *            - The index of the display to set the scaling mode for
 * @param scalingMode
 *            - The new scaling mode to apply for the given display
 */
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode) {
    DISPLAYCONFIG_SCALING displayConfigScalingMode;

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

    DisplayConfig displayConfig = getDisplayConfig();
    displayConfig.pathInfoArray[displayIndex].targetInfo.scaling = displayConfigScalingMode;

    LONG setDisplayConfigResult = SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
            displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray,
            SDC_APPLY |
            SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);

    if (setDisplayConfigResult != ERROR_SUCCESS) {
        cerr << "Failed to set the scaling mode! Error Code: " << setDisplayConfigResult << endl;
    }
}

/*
 * Sets the DPI scale percentage for the given display.
 *
 * @param displayIndex
 *            - The index of the display to set the DPI scale percentage for
 * @param dpiScalePercentage
 *            - The new DPI scale percentage to apply for the given display
 */
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage) {
    DisplayConfig displayConfig = getDisplayConfig();
    DISPLAYCONFIG_GET_DPI_SCALE_INDICES getDpiScaleIndices = { };

    getDpiScaleIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
    getDpiScaleIndices.header.size = sizeof(getDpiScaleIndices);
    getDpiScaleIndices.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    getDpiScaleIndices.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;

    DisplayConfigGetDeviceInfo(&getDpiScaleIndices.header);

    int32_t defaultDpiScaleIndex = abs(getDpiScaleIndices.relativeMinimumDpiScaleIndex);
    int32_t dpiScaleIndex = 0;

    for (int32_t i = 0; i < NUM_OF_DPI_SCALE_PERCENTAGES; i++) {
        if (dpiScalePercentage == DPI_SCALE_PERCENTAGES.at(i)) {
            dpiScaleIndex = i;
        }
    }

    int32_t relativeDpiScaleIndex = 0;
    relativeDpiScaleIndex = dpiScaleIndex - defaultDpiScaleIndex;
    DISPLAYCONFIG_SET_DPI_SCALE_INDEX setDpiScaleIndex = { };

    setDpiScaleIndex.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE;
    setDpiScaleIndex.header.size = sizeof(setDpiScaleIndex);
    setDpiScaleIndex.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    setDpiScaleIndex.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;
    setDpiScaleIndex.relativeDpiScaleIndex = relativeDpiScaleIndex;

    bool setDpiScaleResult = DisplayConfigSetDeviceInfo(&setDpiScaleIndex.header);

    if (setDpiScaleResult != ERROR_SUCCESS) {
        cerr << "Failed to set the DPI scale percentage! Error Code : " << setDpiScaleResult << endl;
    }
}

/*
 * Sets the orientation for the given display.
 *
 * @param displayIndex
 *            - The index of the display to set the scaling mode for
 * @param orientation
 *            - The new orientation to apply for the given display
 */
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation) {
    DISPLAYCONFIG_ROTATION displayConfigRotation;

    switch (orientation) {
    case 0:
        displayConfigRotation = DISPLAYCONFIG_ROTATION_IDENTITY; // Landscape mode.
        break;
    case 1:
        displayConfigRotation = DISPLAYCONFIG_ROTATION_ROTATE90; // Portrait mode.
        break;
    case 2:
        displayConfigRotation = DISPLAYCONFIG_ROTATION_ROTATE180; // Inverted landscape mode.
        break;
    case 3:
        displayConfigRotation = DISPLAYCONFIG_ROTATION_ROTATE270; // Inverted portrait mode.
        break;
    default:
        displayConfigRotation = DISPLAYCONFIG_ROTATION_IDENTITY; // Landscape mode.
        break;
    }

    DisplayConfig displayConfig = getDisplayConfig();
    displayConfig.pathInfoArray[displayIndex].targetInfo.rotation = displayConfigRotation;

    LONG setDisplayConfigResult = SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
            displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray,
            SDC_APPLY |
            SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);

    if (setDisplayConfigResult != ERROR_SUCCESS) {
        cerr << "Failed to set the display orientation! Error Code: " << setDisplayConfigResult << endl;
    }
}
