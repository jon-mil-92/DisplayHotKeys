/*
 * Defines various aspects of the current display configuration.
 *
 * Author: Jonathan Miller
 * License: The MIT License - https://mit-license.org/
 *
 * Copyright © 2025 Jonathan Miller
 */

#pragma once

#include <Windows.h>
#include <iostream>
#include <cstdint>
#include <vector>
#include <string>
#include <sstream>
#include <locale>
#include <codecvt>

using namespace std;

static const vector<int32_t> DPI_SCALE_PERCENTAGES = { 100, 125, 150, 175, 200, 225, 250, 300, 350 };
static const int32_t NUM_OF_DPI_SCALE_PERCENTAGES = 9;
static const int32_t DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE = -3;
static const int32_t DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE = -4;

/*
 * Holds the relative DPI scale percentage indices from the recommended DPI scale percentage.
 */
struct DISPLAYCONFIG_GET_DPI_SCALE_INDICES {
    DISPLAYCONFIG_DEVICE_INFO_HEADER header;
    int32_t relativeMinimumDpiScaleIndex;
    int32_t relativeCurrentDpiScaleIndex;
    int32_t relativeMaximumDpiScaleIndex;
};

/*
 * Holds the relative index for the DPI scale percentage to set.
 */
struct DISPLAYCONFIG_SET_DPI_SCALE_INDEX {
    DISPLAYCONFIG_DEVICE_INFO_HEADER header;
    int32_t relativeDpiScaleIndex;
};

/*
 * Holds the display configuration information for the connected displays.
 */
struct DisplayConfig {
    UINT32 numPathInfoArrayElements;
    UINT32 numModeInfoArrayElements;
    DISPLAYCONFIG_PATH_INFO *pathInfoArray;
    DISPLAYCONFIG_MODE_INFO *modeInfoArray;

    ~DisplayConfig() {
        delete[] pathInfoArray;
        delete[] modeInfoArray;
    }
};

/*
 * Queries the display configuration to initialize a structure that holds the active paths as defined in
 * the persistence database for the currently connected displays.
 *
 * @return A structure that holds the display configuration paths for the currently connected displays
 */
DisplayConfig getDisplayConfig();

/*
 * Converts a wide string to a string.
 *
 * @param wideString
 *            - The wide string to convert
 *
 * @return The string version of the wide string
 */
string wStrToStr(const wstring &wideString);

/*
 * Gets a vector of display IDs by utilizing the EnumDisplayDevices API.
 *
 * @return The vector of display IDs
 */
vector<string> getEnumDisplayDevicesDisplayIds();

/*
 * Gets a vector of display IDs by utilizing the QueryDisplayConfig and DisplayConfigGetDeviceInfo APIs.
 *
 * @return The vector of display IDs
 */
vector<string> getQueryDisplayConfigDisplayIds();

/*
 * Gets the index in the EnumDisplayDevices display ID vector for the given display ID.
 *
 * @param displayId
 *            - The ID of the display to get the index for
 *
 * @return The index in the EnumDisplayDevices display ID vector for the given display ID
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId);

/*
 * Gets the index in the QueryDisplayConfig display ID vector for the given display ID.
 *
 * @param displayId
 *            - The ID of the display to get the index for
 *
 * @return The index in the QueryDisplayConfig display ID vector for the given display ID
 */
int getQueryDisplayConfigDisplayIdIndex(string displayId);
