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
#include "com_dhk_io_SetDisplay.h"
#include "DisplayConfig.h"
#include <jni.h>

using namespace std;

/*
 * Forward declarations for internal helpers.
 */
void setDisplayMode(UINT32 displayIndex, UINT32 width, UINT32 height, UINT32 bitDepth, UINT32 refreshRate);
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode);
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage);
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation);

/**
 * Resolves a stable display ID to a valid QueryDisplayConfig index. Returns -1 if the ID is not currently present or
 * the index is invalid.
 *
 * @param stableId
 *        - The stable display ID to resolve
 *
 * @return A valid display index, or -1 if the display does not exist
 */
static int resolveDisplayIndex(const string &stableId) {
    int index = getQueryDisplayConfigDisplayIdIndex(stableId);

    if (index < 0) {
        return -1;
    }

    DisplayConfig config = getDisplayConfig();

    if ((UINT32) index >= config.numPathInfoArrayElements) {
        return -1;
    }

    return index;
}

/**
 * Applies display settings (resolution, bit depth, refresh rate, scaling mode, and DPI scale percentage) to the given
 * display.
 *
 * @param env
 *        - The JNI environment pointer
 * @param obj
 *        - The Java SetDisplay instance
 * @param displayId
 *        - The stable display ID of the display to modify
 * @param resWidth
 *        - The horizontal resolution to apply
 * @param resHeight
 *        - The vertical resolution to apply
 * @param bitDepth
 *        - The bit depth to apply
 * @param refreshRate
 *        - The refresh rate to apply
 * @param scalingMode
 *        - The scaling mode to apply (0 = aspect ratio, 1 = stretched, 2 = centered)
 * @param dpiScalePercentage
 *        - The DPI scale percentage to apply (e.g., 100, 125, 150)
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setDisplay(JNIEnv *env, jobject obj, jstring displayId, jint resWidth,
                                                             jint resHeight, jint bitDepth, jint refreshRate,
                                                             jint scalingMode, jint dpiScalePercentage) {
    jboolean isCopy;
    const char *displayIdChars = env->GetStringUTFChars(displayId, &isCopy);
    string stableId = displayIdChars;

    int displayIndex = resolveDisplayIndex(stableId);

    if (displayIndex < 0) {
        env->ReleaseStringUTFChars(displayId, displayIdChars);

        // Display disappeared or invalid
        return;
    }

    setDisplayMode(displayIndex, resWidth, resHeight, bitDepth, refreshRate);
    setDisplayScalingMode(displayIndex, scalingMode);
    setDpiScalePercentage(displayIndex, dpiScalePercentage);

    env->ReleaseStringUTFChars(displayId, displayIdChars);
}

/**
 * Applies a new orientation to the given display.
 *
 * @param env
 *        - The JNI environment pointer
 * @param obj
 *        - The Java SetDisplay instance
 * @param displayId
 *        - The stable display ID of the display to modify
 * @param orientation
 *        - The orientation to apply
 *          (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 */
JNIEXPORT void JNICALL Java_com_dhk_io_SetDisplay_setOrientation(JNIEnv *env, jobject obj, jstring displayId,
                                                                 jint orientation) {
    jboolean isCopy;
    const char *displayIdChars = env->GetStringUTFChars(displayId, &isCopy);
    string stableId = displayIdChars;

    int displayIndex = resolveDisplayIndex(stableId);

    if (displayIndex < 0) {
        env->ReleaseStringUTFChars(displayId, displayIdChars);
        return; // Display disappeared or invalid
    }

    setDisplayOrientation(displayIndex, orientation);

    env->ReleaseStringUTFChars(displayId, displayIdChars);
}

/**
 * Sets the display mode (resolution, bit depth, refresh rate) for the given display. Uses ChangeDisplaySettingsExW with
 * the GDI device name obtained from QueryDisplayConfig.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param resWidth
 *        - The horizontal resolution to apply
 * @param resHeight
 *        - The vertical resolution to apply
 * @param bitDepth
 *        - The bit depth to apply
 * @param refreshRate
 *        - The refresh rate to apply
 */
void setDisplayMode(UINT32 displayIndex, UINT32 resWidth, UINT32 resHeight, UINT32 bitDepth, UINT32 refreshRate) {
    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    DISPLAYCONFIG_SOURCE_DEVICE_NAME sourceName = {};
    sourceName.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    sourceName.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;
    sourceName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME;
    sourceName.header.size = sizeof(sourceName);

    if (DisplayConfigGetDeviceInfo(&sourceName.header) != ERROR_SUCCESS) {
        return;
    }

    DEVMODEW devModeW;
    SecureZeroMemory(&devModeW, sizeof(DEVMODEW));
    devModeW.dmSize = sizeof(devModeW);
    devModeW.dmPelsWidth = resWidth;
    devModeW.dmPelsHeight = resHeight;
    devModeW.dmBitsPerPel = bitDepth;
    devModeW.dmDisplayFrequency = refreshRate;
    devModeW.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;

    LONG testResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_TEST, NULL);

    if (testResult == DISP_CHANGE_SUCCESSFUL) {
        ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_UPDATEREGISTRY, NULL);
        return;
    }

    // Retry without refresh rate
    devModeW.dmFields &= ~DM_DISPLAYFREQUENCY;
    devModeW.dmDisplayFrequency = 0;

    testResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_TEST, NULL);

    if (testResult == DISP_CHANGE_SUCCESSFUL) {
        ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_UPDATEREGISTRY, NULL);
        return;
    }

    // Retry without bit depth
    devModeW.dmFields &= ~DM_BITSPERPEL;
    devModeW.dmBitsPerPel = 0;

    testResult = ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_TEST, NULL);

    if (testResult == DISP_CHANGE_SUCCESSFUL) {
        ChangeDisplaySettingsExW(sourceName.viewGdiDeviceName, &devModeW, NULL, CDS_UPDATEREGISTRY, NULL);
    }
}

/**
 * Sets the scaling mode for the given display.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param scalingMode
 *        - The scaling mode to apply
 *          (0 = aspect ratio, 1 = stretched, 2 = centered)
 */
void setDisplayScalingMode(UINT32 displayIndex, UINT32 scalingMode) {
    DISPLAYCONFIG_SCALING scaling;

    switch (scalingMode) {
    case 0:
        // Preserve aspect ratio
        scaling = DISPLAYCONFIG_SCALING_ASPECTRATIOCENTEREDMAX;
        break;
    case 1:
        // Stretch to fill panel
        scaling = DISPLAYCONFIG_SCALING_STRETCHED;
        break;
    case 2:
        // Centered in panel
        scaling = DISPLAYCONFIG_SCALING_CENTERED;
        break;
    default:
        // Preserve aspect ratio by default
        scaling = DISPLAYCONFIG_SCALING_ASPECTRATIOCENTEREDMAX;
        break;
    }

    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    displayConfig.pathInfoArray[displayIndex].targetInfo.scaling = scaling;

    SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
                     displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray,
                     SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);
}

/**
 * Sets the DPI scale percentage for the given display.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param dpiScalePercentage
 *        - The DPI scale percentage to apply (e.g., 100, 125, 150)
 */
void setDpiScalePercentage(UINT32 displayIndex, int32_t dpiScalePercentage) {
    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    DISPLAYCONFIG_GET_DPI_SCALE_INDICES getIndices = {};
    getIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
    getIndices.header.size = sizeof(getIndices);
    getIndices.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    getIndices.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;

    DisplayConfigGetDeviceInfo(&getIndices.header);

    int32_t defaultIndex = abs(getIndices.relativeMinimumDpiScaleIndex);
    int32_t dpiIndex = 0;

    for (int32_t i = 0; i < NUM_OF_DPI_SCALE_PERCENTAGES; i++) {
        if (dpiScalePercentage == DPI_SCALE_PERCENTAGES.at(i)) {
            dpiIndex = i;
        }
    }

    int32_t relativeIndex = dpiIndex - defaultIndex;

    DISPLAYCONFIG_SET_DPI_SCALE_INDEX setIndex = {};
    setIndex.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE;
    setIndex.header.size = sizeof(setIndex);
    setIndex.header.adapterId = displayConfig.pathInfoArray[displayIndex].sourceInfo.adapterId;
    setIndex.header.id = displayConfig.pathInfoArray[displayIndex].sourceInfo.id;
    setIndex.relativeDpiScaleIndex = relativeIndex;

    DisplayConfigSetDeviceInfo(&setIndex.header);
}

/**
 * Sets the orientation for the given display.
 *
 * @param displayIndex
 *        - The QueryDisplayConfig index of the display to modify
 * @param orientation
 *        - The orientation to apply
 *          (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait)
 */
void setDisplayOrientation(UINT32 displayIndex, UINT32 orientation) {
    DISPLAYCONFIG_ROTATION rotation;

    switch (orientation) {
    case 0:
        // Landscape mode
        rotation = DISPLAYCONFIG_ROTATION_IDENTITY;
        break;
    case 1:
        // Portrait mode
        rotation = DISPLAYCONFIG_ROTATION_ROTATE90;
        break;
    case 2:
        // Inverted landscape mode
        rotation = DISPLAYCONFIG_ROTATION_ROTATE180;
        break;
    case 3:
        // Inverted portrait mode
        rotation = DISPLAYCONFIG_ROTATION_ROTATE270;
        break;
    default:
        // Landscape mode
        rotation = DISPLAYCONFIG_ROTATION_IDENTITY;
        break;
    }

    DisplayConfig displayConfig = getDisplayConfig();

    if (displayIndex >= displayConfig.numPathInfoArrayElements) {
        return;
    }

    displayConfig.pathInfoArray[displayIndex].targetInfo.rotation = rotation;

    SetDisplayConfig(displayConfig.numPathInfoArrayElements, displayConfig.pathInfoArray,
                     displayConfig.numModeInfoArrayElements, displayConfig.modeInfoArray,
                     SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE);
}
