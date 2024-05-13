/*
 * Original Author: Jonathan Miller
 * Version: 1.0.0.0
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
DisplayConfig getDisplayConfig() {
    // Initialize a structure to hold the active paths as defined in the persistence database for the currently
    // connected displays.
    DisplayConfig displayConfig = { };

    // Initialize variables to hold the path info array and mode info array buffer sizes.
    UINT32 numPathInfoArrayElements = 0;
    UINT32 numModeInfoArrayElements = 0;

    // Point to the current display topology after querying the display configuration.
    DISPLAYCONFIG_TOPOLOGY_ID *currentTopology = new DISPLAYCONFIG_TOPOLOGY_ID;

    // Retrieve the size of the buffers that are required to call the QueryDisplayConfig function.
    LONG getDisplayConfigBufferSizesResult = GetDisplayConfigBufferSizes(QDC_DATABASE_CURRENT,
            &numPathInfoArrayElements, &numModeInfoArrayElements);

    // Check if the display config buffer sizes were successfully queried, and output an error message if it failed.
    if (getDisplayConfigBufferSizesResult != ERROR_SUCCESS) {
        cerr << "Failed to get the display config buffer sizes! Error Code: " << getDisplayConfigBufferSizesResult
                << endl;
    }

    // Instantiate the path info array and mode info array with the correct buffers to hold the active paths as defined
    // in the persistence database for the currently connected displays.
    DISPLAYCONFIG_PATH_INFO *pathInfoArray = new DISPLAYCONFIG_PATH_INFO[numPathInfoArrayElements];
    DISPLAYCONFIG_MODE_INFO *modeInfoArray = new DISPLAYCONFIG_MODE_INFO[numModeInfoArrayElements];

    // Query the active display configuration.
    LONG queryDisplayConfigResult = QueryDisplayConfig(QDC_DATABASE_CURRENT, &numPathInfoArrayElements, pathInfoArray,
            &numModeInfoArrayElements, modeInfoArray, currentTopology);

    // Check if the active display configuration was successfully queried, and output an error message if it failed.
    if (queryDisplayConfigResult != ERROR_SUCCESS) {
        cerr << "Failed to query the display configuration! Error Code: " << queryDisplayConfigResult << endl;
    }

    // Initialize the display configuration structure with the queried display configuration results.
    displayConfig.numPathInfoArrayElements = numPathInfoArrayElements;
    displayConfig.numModeInfoArrayElements = numModeInfoArrayElements;
    displayConfig.pathInfoArray = pathInfoArray;
    displayConfig.modeInfoArray = modeInfoArray;

    // Clean up allocated memory.
    delete currentTopology;

    return displayConfig;
}

/*
 * This function converts a wide string to a string.
 *
 * @param wideString - The wide string to convert.
 * @return The string version of the wide string.
 */
string wStrToStr(const wstring &wideString) {
    // Define the encoding type for the string.
    using convert_typeX = codecvt_utf8<wchar_t>;

    // Define the string converter.
    wstring_convert<convert_typeX, wchar_t> strConverter;

    return strConverter.to_bytes(wideString);
}

/*
 * This function gets a vector of display IDs by utilizing the EnumDisplayDevices API.
 *
 * @return The vector of display IDs.
 */
vector<string> getEnumDisplayDevicesDisplayIds() {
    // Declare a DISPLAY_DEVICE struct to hold the display ID for the current display device index.
    DISPLAY_DEVICE displayDevice;

    // Initialize the DISPLAY_DEVICE struct.
    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    // Initialize a boolean that will hold the result of the EnumDisplayDevices enumeration.
    bool enumDisplayDevicesResult = true;

    // Initialize an unsigned integer to hold the current display device index.
    UINT32 displayDeviceIndex = 0;

    // Declare a vector to hold all of the display IDs from the EnumDisplayDevices enumeration.
    vector<string> displayIdsVector;

    // Declare a string to hold the current monitor's ID.
    string displayId;

    // While a display device could be obtained...
    while (enumDisplayDevicesResult == true) {
        // Retrieve the display device at the current display device index.
        enumDisplayDevicesResult = EnumDisplayDevices(NULL, displayDeviceIndex, &displayDevice, 0);

        // Only use the display devices that are attached to the desktop.
        if (displayDevice.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            // Create a variable to hold the monitor name attached to the current display device.
            LPSTR monitorName = new CHAR[32];

            // Get the monitor name from the display device, and then use that name to get the display ID.
            lstrcpy(monitorName, displayDevice.DeviceName);
            EnumDisplayDevices(monitorName, 0, &displayDevice, EDD_GET_DEVICE_INTERFACE_NAME);

            // Get the current monitor's ID string.
            displayId = displayDevice.DeviceID;

            // Add the retrieved display ID to the vector of display IDs.
            displayIdsVector.push_back(displayId);

            // Clean up allocated memory.
            delete[] monitorName;
        }

        // Move to the next display device.
        displayDeviceIndex++;
    }

    return displayIdsVector;
}

/*
 * This function gets a vector of display IDs by utilizing the QueryDisplayConfig and DisplayConfigGetDeviceInfo APIs.
 *
 * @return The vector of display IDs.
 */
vector<string> getQueryDisplayConfigDisplayIds() {
    // Initialize a structure to hold the active paths as defined in the persistence database for the currently
    // connected displays.
    DisplayConfig displayConfig = getDisplayConfig();

    // Declare a vector to hold all of the display IDs from the QueryDisplayConfig enumeration.
    vector<string> displayIdsVector;

    // Declare a wide string stream to hold the current monitor's ID.
    wstringstream displayIdStream;

    // For each active path...
    for (int i = 0; i < displayConfig.numPathInfoArrayElements; i++) {
        // Find the target (monitor) device path.
        DISPLAYCONFIG_TARGET_DEVICE_NAME targetName = { };
        targetName.header.adapterId = displayConfig.pathInfoArray[i].targetInfo.adapterId;
        targetName.header.id = displayConfig.pathInfoArray[i].targetInfo.id;
        targetName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME;
        targetName.header.size = sizeof(targetName);
        DisplayConfigGetDeviceInfo(&targetName.header);

        // Send the display path to the display ID wide string stream.
        displayIdStream << targetName.monitorDevicePath;

        // Push the string representation of the display ID wide string stream to the vector.
        displayIdsVector.push_back(wStrToStr(displayIdStream.str()));

        // Clear the display ID wide string stream.
        displayIdStream.str(wstring());
        displayIdStream.clear();
    }

    return displayIdsVector;
}

/*
 * This function gets the index in the EnumDisplayDevices display ID vector for the given display ID.
 *
 * @param displayId - The ID of the display to get the index for.
 * @return The index in the EnumDisplayDevices display ID vector for the given display ID.
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId) {
    // Initialize a vector to hold all of the display IDs from the EnumDisplayDevices enumeration.
    vector<string> displayIdsVector = getEnumDisplayDevicesDisplayIds();

    // Initialize a variable to hold the index in the EnumDisplayDevices display ID vector for the given display ID.
    int displayIdIndex = 0;

    // For each display ID in the EnumDisplayDevices display ID vector...
    for (int i = 0; i < displayIdsVector.size(); i++) {
        // If the given display ID was found in the vector...
        if (displayIdsVector.at(i).compare(displayId) == 0) {
            // Store the index for the found display ID.
            displayIdIndex = i;
        }
    }

    return displayIdIndex;
}

/*
 * This function gets the index in the QueryDisplayConfig display ID vector for the given display ID.
 *
 * @param displayId - The ID of the display to get the index for.
 * @return The index in the QueryDisplayConfig display ID vector for the given display ID.
 */
int getQueryDisplayConfigDisplayIdIndex(string displayId) {
    // Initialize a vector to hold all of the display IDs from the QueryDisplayConfig enumeration.
    vector<string> displayIdsVector = getQueryDisplayConfigDisplayIds();

    // Initialize a variable to hold the index in the QueryDisplayConfig display ID vector for the given display ID.
    int displayIdIndex = 0;

    // For each display ID in the QueryDisplayConfig display ID vector...
    for (int i = 0; i < displayIdsVector.size(); i++) {
        // If the given display ID was found in the vector...
        if (displayIdsVector.at(i).compare(displayId) == 0) {
            // Store the index for the found display ID.
            displayIdIndex = i;
        }
    }

    return displayIdIndex;
}
