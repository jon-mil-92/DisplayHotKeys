/*
 * Defines various aspects of the current display configuration.
 *
 * Author: Jonathan Miller
 * License: The MIT License - https://mit-license.org/
 *
 * Copyright © 2025 Jonathan Miller
 */

#include "DisplayConfig.h"

/*
 * Queries the display configuration to initialize a structure that holds the active paths as defined in
 * the persistence database for the currently connected displays.
 *
 * @return A structure that holds the display configuration paths for the currently connected displays
 */
DisplayConfig getDisplayConfig() {
    DisplayConfig displayConfig = { };
    UINT32 numPathInfoArrayElements = 0;
    UINT32 numModeInfoArrayElements = 0;
    DISPLAYCONFIG_TOPOLOGY_ID *currentTopology = new DISPLAYCONFIG_TOPOLOGY_ID;

    LONG getDisplayConfigBufferSizesResult = GetDisplayConfigBufferSizes(QDC_DATABASE_CURRENT,
            &numPathInfoArrayElements, &numModeInfoArrayElements);

    if (getDisplayConfigBufferSizesResult != ERROR_SUCCESS) {
        cerr << "Failed to get the display config buffer sizes! Error Code: " << getDisplayConfigBufferSizesResult
                << endl;
    }

    DISPLAYCONFIG_PATH_INFO *pathInfoArray = new DISPLAYCONFIG_PATH_INFO[numPathInfoArrayElements];
    DISPLAYCONFIG_MODE_INFO *modeInfoArray = new DISPLAYCONFIG_MODE_INFO[numModeInfoArrayElements];

    LONG queryDisplayConfigResult = QueryDisplayConfig(QDC_DATABASE_CURRENT, &numPathInfoArrayElements, pathInfoArray,
            &numModeInfoArrayElements, modeInfoArray, currentTopology);

    if (queryDisplayConfigResult != ERROR_SUCCESS) {
        cerr << "Failed to query the display configuration! Error Code: " << queryDisplayConfigResult << endl;
    }

    displayConfig.numPathInfoArrayElements = numPathInfoArrayElements;
    displayConfig.numModeInfoArrayElements = numModeInfoArrayElements;
    displayConfig.pathInfoArray = pathInfoArray;
    displayConfig.modeInfoArray = modeInfoArray;

    delete currentTopology;

    return displayConfig;
}

/*
 * Converts a wide string to a string.
 *
 * @param wideString
 *            - The wide string to convert
 *
 * @return The string version of the wide string
 */
string wStrToStr(const wstring &wideString) {
    using convert_typeX = codecvt_utf8<wchar_t>;
    wstring_convert<convert_typeX, wchar_t> strConverter;

    return strConverter.to_bytes(wideString);
}

/*
 * Gets a vector of display IDs by utilizing the EnumDisplayDevices API.
 *
 * @return The vector of display IDs
 */
vector<string> getEnumDisplayDevicesDisplayIds() {
    DISPLAY_DEVICE displayDevice;

    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    bool enumDisplayDevicesResult = true;
    UINT32 displayDeviceIndex = 0;
    vector<string> displayIdsVector;
    string displayId;

    while (enumDisplayDevicesResult == true) {
        enumDisplayDevicesResult = EnumDisplayDevices(NULL, displayDeviceIndex, &displayDevice, 0);

        if (displayDevice.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            LPSTR monitorName = new CHAR[32];

            lstrcpy(monitorName, displayDevice.DeviceName);
            EnumDisplayDevices(monitorName, 0, &displayDevice, EDD_GET_DEVICE_INTERFACE_NAME);

            displayId = displayDevice.DeviceID;
            displayIdsVector.push_back(displayId);

            delete[] monitorName;
        }

        displayDeviceIndex++;
    }

    return displayIdsVector;
}

/*
 * Gets a vector of display IDs by utilizing the QueryDisplayConfig and DisplayConfigGetDeviceInfo APIs.
 *
 * @return The vector of display IDs
 */
vector<string> getQueryDisplayConfigDisplayIds() {
    DisplayConfig displayConfig = getDisplayConfig();
    vector<string> displayIdsVector;
    wstringstream displayIdStream;

    for (int i = 0; i < displayConfig.numPathInfoArrayElements; i++) {
        DISPLAYCONFIG_TARGET_DEVICE_NAME targetName = { };
        targetName.header.adapterId = displayConfig.pathInfoArray[i].targetInfo.adapterId;
        targetName.header.id = displayConfig.pathInfoArray[i].targetInfo.id;
        targetName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME;
        targetName.header.size = sizeof(targetName);

        DisplayConfigGetDeviceInfo(&targetName.header);

        displayIdStream << targetName.monitorDevicePath;
        displayIdsVector.push_back(wStrToStr(displayIdStream.str()));
        displayIdStream.str(wstring());
        displayIdStream.clear();
    }

    return displayIdsVector;
}

/*
 * Gets the index in the EnumDisplayDevices display ID vector for the given display ID.
 *
 * @param displayId
 *            - The ID of the display to get the index for
 *
 * @return The index in the EnumDisplayDevices display ID vector for the given display ID
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId) {
    vector<string> displayIdsVector = getEnumDisplayDevicesDisplayIds();
    int displayIdIndex = 0;

    for (int i = 0; i < displayIdsVector.size(); i++) {
        if (displayIdsVector.at(i).compare(displayId) == 0) {
            displayIdIndex = i;
        }
    }

    return displayIdIndex;
}

/*
 * Gets the index in the QueryDisplayConfig display ID vector for the given display ID.
 *
 * @param displayId
 *            - The ID of the display to get the index for
 *
 * @return The index in the QueryDisplayConfig display ID vector for the given display ID
 */
int getQueryDisplayConfigDisplayIdIndex(string displayId) {
    vector<string> displayIdsVector = getQueryDisplayConfigDisplayIds();
    int displayIdIndex = 0;

    for (int i = 0; i < displayIdsVector.size(); i++) {
        if (displayIdsVector.at(i).compare(displayId) == 0) {
            displayIdIndex = i;
        }
    }

    return displayIdIndex;
}
