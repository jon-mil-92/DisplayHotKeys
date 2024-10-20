/*
 * Original Author: Jonathan Miller
 * Version: 1.1.4.0
 *
 * Description: Define various aspects of the current display configuration.
 *
 * License: The MIT License - https://mit-license.org/
 * Copyright (c) 2024 Jonathan Miller
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

// Define a vector of allowed DPI scale percentage values.
static const vector<int32_t> DPI_SCALE_PERCENTAGES = { 100, 125, 150, 175, 200, 225, 250, 300, 350 };

// Define an integer for the number of DPI scale percentage values in Windows 10 and Windows 11.
static const int32_t NUM_OF_DPI_SCALE_PERCENTAGES = 9;

// Define integers for the header types that get and set the DPI scale percentages.
static const int32_t DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE = -3;
static const int32_t DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE = -4;

// Define a structure to hold the relative DPI scale percentage indices from the recommended DPI scale percentage.
struct DISPLAYCONFIG_GET_DPI_SCALE_INDICES {
    // The header that will define the type of display config device query as well as the source's id and adapter id.
    DISPLAYCONFIG_DEVICE_INFO_HEADER header;

    // The number of steps down to the minimum DPI scale percentage from the recommended DPI scale percentage.
    int32_t relativeMinimumDpiScaleIndex;

    // The number of steps away from the recommended DPI scale percentage for the current DPI scale percentage.
    int32_t relativeCurrentDpiScaleIndex;

    // The number of steps up to the maximum DPI scale percentage from the recommended DPI scale percentage.
    int32_t relativeMaximumDpiScaleIndex;
};

// Define a structure to hold the relative index for the DPI scale percentage to set.
struct DISPLAYCONFIG_SET_DPI_SCALE_INDEX {
    // The header that will define the type of display config device query as well as the source's id and adapter id.
    DISPLAYCONFIG_DEVICE_INFO_HEADER header;

    // The number of steps away from the recommended DPI scale percentage to set for the new DPI scale percentage.
    int32_t relativeDpiScaleIndex;
};

// Define a structure to hold the display configuration information for the connected displays.
struct DisplayConfig {
    // Variables to hold the path info array and mode info array buffer sizes.
    UINT32 numPathInfoArrayElements;
    UINT32 numModeInfoArrayElements;

    // Variables to hold the active paths as defined in the persistence database for the currently connected displays.
    DISPLAYCONFIG_PATH_INFO *pathInfoArray;
    DISPLAYCONFIG_MODE_INFO *modeInfoArray;

    // Destructor for the DisplayConfig structure.
    ~DisplayConfig() {
        // Clean up allocated memory.
        delete[] pathInfoArray;
        delete[] modeInfoArray;
    }
};

/*
 * This function queries the display configuration to initialize a structure that holds the active paths as defined in
 * the persistence database for the currently connected displays.
 *
 * @return A structure that holds the display configuration paths for the currently connected displays.
 */
DisplayConfig getDisplayConfig();

/*
 * This function converts a wide string to a string.
 *
 * @param wideString - The wide string to convert.
 * @return The string version of the wide string.
 */
string wStrToStr(const wstring &wideString);

/*
 * This function gets a vector of display IDs by utilizing the EnumDisplayDevices API.
 *
 * @return The vector of display IDs.
 */
vector<string> getEnumDisplayDevicesDisplayIds();

/*
 * This function gets a vector of display IDs by utilizing the QueryDisplayConfig and DisplayConfigGetDeviceInfo APIs.
 *
 * @return The vector of display IDs.
 */
vector<string> getQueryDisplayConfigDisplayIds();

/*
 * This function gets the index in the EnumDisplayDevices display ID vector for the given display ID.
 *
 * @param displayId - The ID of the display to get the index for.
 * @return The index in the EnumDisplayDevices display ID vector for the given display ID.
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId);

/*
 * This function gets the index in the QueryDisplayConfig display ID vector for the given display ID.
 *
 * @param displayId - The ID of the display to get the index for.
 * @return The index in the QueryDisplayConfig display ID vector for the given display ID.
 */
int getQueryDisplayConfigDisplayIdIndex(string displayId);
