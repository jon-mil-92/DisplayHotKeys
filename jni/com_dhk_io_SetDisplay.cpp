/*
 * Immediately applies display settings for a given display.
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
#include "com_dhk_io_SetDisplay.h"

using namespace std;

void setDisplayMode(UINT32 displayIndex, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate);
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode);
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage);
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation);

/*
 * Updates the display mode, scaling mode, and DPI scale percentage for the given
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
    int queryDisplayConfigDisplayIdIndex = getQueryDisplayConfigDisplayIdIndex(displayIdString);

    setDisplayMode(queryDisplayConfigDisplayIdIndex, resWidth, resHeight, bitDepth, refreshRate);
    setDisplayScalingMode(queryDisplayConfigDisplayIdIndex, scalingMode);
    setDpiScalePercentage(queryDisplayConfigDisplayIdIndex, dpiScalePercentage);

    env->ReleaseStringUTFChars(displayId, displayIdChars);
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
    int queryDisplayConfigDisplayIdIndex = getQueryDisplayConfigDisplayIdIndex(displayIdString);

    setDisplayOrientation(queryDisplayConfigDisplayIdIndex, orientation);

    env->ReleaseStringUTFChars(displayId, displayIdChars);
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
    /*
     * Map the displayIndex (QueryDisplayConfig index) to the source/GDI device name and use the Unicode
     * ChangeDisplaySettingsExW API with a DEVMODEW structure. This is required for virtual displays
     * where EnumDisplayDevices indices/strings may not match.
     */
    DisplayConfig displayConfig = getDisplayConfig();

    if ((UINT32) displayIndex >= displayConfig.numPathInfoArrayElements) {
        cerr << "Invalid display index for setDisplayMode: " << displayIndex << endl;
        return;
    }

    DISPLAYCONFIG_SOURCE_DEVICE_NAME sourceName = { };
    sourceName.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    sourceName.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;
    sourceName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME;
    sourceName.header.size = sizeof(sourceName);

    LONG displayConfigGetDeviceInfoResult = DisplayConfigGetDeviceInfo(&sourceName.header);

    if (displayConfigGetDeviceInfoResult != ERROR_SUCCESS) {
        cerr << "Failed to get source device name for setDisplayMode! Error Code: " << displayConfigGetDeviceInfoResult
                << endl;
        return;
    }

    DEVMODEW devModeW;
    SecureZeroMemory(&devModeW, sizeof(DEVMODEW));
    devModeW.dmSize = sizeof(devModeW);
    devModeW.dmPelsWidth = resWidth;
    devModeW.dmPelsHeight = resHeight;
    devModeW.dmBitsPerPel = bitDepth;
    devModeW.dmDisplayFrequency = refreshRate;
    devModeW.dmDriverExtra = 0;

    // Try to set all fields first; if the mode is not supported, progressively relax fields and retry
    devModeW.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;

    // First validate the requested mode using CDS_TEST to get a specific error code
    LONG testResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_TEST, NULL);

    if (testResult == DISP_CHANGE_SUCCESSFUL) {
        LONG changeDisplaySettingsResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL,
        CDS_UPDATEREGISTRY, NULL);

        if (changeDisplaySettingsResult != DISP_CHANGE_SUCCESSFUL) {
            cerr << "Failed to set the display mode! Error Code: " << changeDisplaySettingsResult << endl;
        }
    } else {
        bool applied = false;

        // Try without refresh rate
        devModeW.dmFields &= ~DM_DISPLAYFREQUENCY;
        devModeW.dmDisplayFrequency = 0;
        testResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_TEST, NULL);

        if (testResult == DISP_CHANGE_SUCCESSFUL) {
            LONG changeDisplaySettingsResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL,
            CDS_UPDATEREGISTRY, NULL);

            if (changeDisplaySettingsResult == DISP_CHANGE_SUCCESSFUL) {
                applied = true;
            }
        } else {
            cerr << "Failed to set the display mode without refresh rate! Error Code: " << testResult << endl;
        }

        // If still not applied, try without bit depth as well
        if (!applied) {
            devModeW.dmFields &= ~DM_BITSPERPEL;
            devModeW.dmBitsPerPel = 0;
            testResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_TEST, NULL);

            if (testResult == DISP_CHANGE_SUCCESSFUL) {
                LONG changeDisplaySettingsResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW,
                NULL,
                CDS_UPDATEREGISTRY, NULL);
            } else {
                cerr << "Failed to set the display mode without refresh rate and bit depth! Error Code: " << testResult
                        << endl;
            }
        }
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
