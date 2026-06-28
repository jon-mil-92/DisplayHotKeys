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
#include "DisplayConfig.h"

#include <algorithm>
#include <cctype>
#include <string>
#include <vector>
#include <windows.h>

using namespace std;

/**
 * Queries the display configuration to initialize a structure that holds the persisted paths and modes as defined in
 * the Windows display persistence database. This uses QDC_DATABASE_CURRENT to retrieve the full configuration,
 * including displays that may not currently be active.
 *
 * @return A DisplayConfig structure containing the display paths and modes for the current configuration
 */
DisplayConfig getDisplayConfig() {
    DisplayConfig displayConfig = {};
    displayConfig.numPathInfoArrayElements = 0;
    displayConfig.numModeInfoArrayElements = 0;
    displayConfig.pathInfoArray = nullptr;
    displayConfig.modeInfoArray = nullptr;

    UINT32 numPath = 0;
    UINT32 numMode = 0;
    DISPLAYCONFIG_TOPOLOGY_ID topology = {};

    LONG sizeResult = GetDisplayConfigBufferSizes(QDC_DATABASE_CURRENT, &numPath, &numMode);

    if (sizeResult != ERROR_SUCCESS || numPath == 0 || numMode == 0) {
        return displayConfig;
    }

    DISPLAYCONFIG_PATH_INFO *pathArray = new DISPLAYCONFIG_PATH_INFO[numPath];
    DISPLAYCONFIG_MODE_INFO *modeArray = new DISPLAYCONFIG_MODE_INFO[numMode];

    SecureZeroMemory(pathArray, sizeof(DISPLAYCONFIG_PATH_INFO) * numPath);
    SecureZeroMemory(modeArray, sizeof(DISPLAYCONFIG_MODE_INFO) * numMode);

    LONG queryResult = QueryDisplayConfig(QDC_DATABASE_CURRENT, &numPath, pathArray, &numMode, modeArray, &topology);

    if (queryResult != ERROR_SUCCESS) {
        delete[] pathArray;
        delete[] modeArray;
        return displayConfig;
    }

    displayConfig.numPathInfoArrayElements = numPath;
    displayConfig.numModeInfoArrayElements = numMode;
    displayConfig.pathInfoArray = pathArray;
    displayConfig.modeInfoArray = modeArray;

    return displayConfig;
}

/**
 * Internal helper that queries display paths using the specified flags and applies visibility rules to determine which
 * displays are currently visible. Over-allocation is used to avoid ERROR_INVALID_PARAMETER caused by driver
 * under-reporting buffer sizes. Returned IDs are converted into stable IDs.
 *
 * Visibility rules:
 *  - A path must be active (DISPLAYCONFIG_PATH_ACTIVE)
 *  - The path's sourceInfo.modeInfoIdx must be valid and reference a DISPLAYCONFIG_MODE_INFO entry
 *  - The referenced DISPLAYCONFIG_MODE_INFO must be of type DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE
 *  - The source mode must have non-zero width and height (width != 0 && height != 0)
 *  - The source position must not be an extreme offscreen sentinel (we treat large negative positions as offscreen)
 *
 * If any of the above fail, the path is considered not visible for the purposes of getVisibleDisplayIds().
 *
 * @param flags
 *        - The QueryDisplayConfig flags (e.g., QDC_ONLY_ACTIVE_PATHS or QDC_DATABASE_CURRENT)
 * @param includeGeometry
 *        - When true, each entry is the stable ID plus its resolution, position, rotation, and DPI scale index; when
 *          false, each entry is just the stable ID
 *
 * @return A vector of stable display IDs (optionally with geometry appended) for the visible displays
 */
static vector<string> queryVisibleDisplaysWithFlags(UINT32 flags, bool includeGeometry) {
    vector<string> visible;

    UINT32 numPath = 0;
    UINT32 numMode = 0;
    DISPLAYCONFIG_TOPOLOGY_ID topology = {};

    LONG sizeResult = GetDisplayConfigBufferSizes(flags, &numPath, &numMode);

    if (sizeResult != ERROR_SUCCESS || numPath == 0 || numMode == 0) {
        return visible;
    }

    UINT32 allocPath = numPath + 4;
    UINT32 allocMode = numMode + 8;

    DISPLAYCONFIG_PATH_INFO *pathArray = new DISPLAYCONFIG_PATH_INFO[allocPath];
    DISPLAYCONFIG_MODE_INFO *modeArray = new DISPLAYCONFIG_MODE_INFO[allocMode];

    SecureZeroMemory(pathArray, sizeof(DISPLAYCONFIG_PATH_INFO) * allocPath);
    SecureZeroMemory(modeArray, sizeof(DISPLAYCONFIG_MODE_INFO) * allocMode);

    UINT32 queryPath = allocPath;
    UINT32 queryMode = allocMode;

    LONG queryResult = QueryDisplayConfig(flags, &queryPath, pathArray, &queryMode, modeArray, &topology);

    if (queryResult != ERROR_SUCCESS) {
        delete[] pathArray;
        delete[] modeArray;
        return visible;
    }

    for (UINT32 i = 0; i < queryPath; i++) {
        const auto &path = pathArray[i];

        if ((path.flags & DISPLAYCONFIG_PATH_ACTIVE) == 0) {
            continue;
        }

        // Validate mode index
        if (path.sourceInfo.modeInfoIdx == DISPLAYCONFIG_PATH_MODE_IDX_INVALID ||
            path.sourceInfo.modeInfoIdx >= queryMode) {
            continue;
        }

        const auto &mode = modeArray[path.sourceInfo.modeInfoIdx];

        if (mode.infoType != DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE) {
            continue;
        }

        const auto &src = mode.sourceMode;

        // Treat zero-size or offscreen as not visible
        bool zeroSize = (src.width == 0 || src.height == 0);
        bool offscreen = (src.position.x <= -30000 || src.position.y <= -30000);

        if (zeroSize || offscreen) {
            continue;
        }

        DISPLAYCONFIG_TARGET_DEVICE_NAME target = {};
        target.header.adapterId = path.targetInfo.adapterId;
        target.header.id = path.targetInfo.id;
        target.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME;
        target.header.size = sizeof(target);

        if (DisplayConfigGetDeviceInfo(&target.header) != ERROR_SUCCESS) {
            continue;
        }

        string entry = buildStableDisplayId(target.monitorDevicePath);

        if (includeGeometry) {
            // Resolution, desktop position, and rotation so a mode or orientation change is reflected in the signature
            entry += "|" + to_string(src.width) + "x" + to_string(src.height) + "|" + to_string(src.position.x) + "," +
                     to_string(src.position.y) + "|r" + to_string((int) path.targetInfo.rotation);

            // Relative current DPI scale index so a DPI scale change is reflected in the signature
            DISPLAYCONFIG_GET_DPI_SCALE_INDICES dpiIndices = {};
            dpiIndices.header.type = (DISPLAYCONFIG_DEVICE_INFO_TYPE) DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE;
            dpiIndices.header.size = sizeof(dpiIndices);
            dpiIndices.header.adapterId = path.sourceInfo.adapterId;
            dpiIndices.header.id = path.sourceInfo.id;

            if (DisplayConfigGetDeviceInfo(&dpiIndices.header) == ERROR_SUCCESS) {
                entry += "|d" + to_string(dpiIndices.relativeCurrentDpiScaleIndex);
            }
        }

        visible.push_back(entry);
    }

    delete[] pathArray;
    delete[] modeArray;

    return visible;
}

/**
 * Gets a vector of display IDs for displays that are currently visible. A display is considered visible if it has an
 * active display path and its source mode reports a non-zero resolution and a valid desktop position. If
 * QDC_ONLY_ACTIVE_PATHS fails due to driver behavior, the function falls back to QDC_DATABASE_CURRENT while applying
 * the same visibility rules. Returned IDs are converted into stable IDs.
 *
 * @return A vector of stable display IDs for the currently visible displays
 */
vector<string> getVisibleDisplayIds() {
    vector<string> displayIds = queryVisibleDisplaysWithFlags(QDC_ONLY_ACTIVE_PATHS, false);

    if (!displayIds.empty()) {
        return displayIds;
    }

    return queryVisibleDisplaysWithFlags(QDC_DATABASE_CURRENT, false);
}

/**
 * Gets a per-display signature for each currently visible display. Each signature is the display's stable ID plus its
 * source resolution, desktop position, rotation, and relative DPI scale index, so resolution, DPI, and orientation
 * changes are detectable even when the set of visible displays is unchanged. Falls back to QDC_DATABASE_CURRENT when
 * QDC_ONLY_ACTIVE_PATHS yields nothing, matching getVisibleDisplayIds().
 *
 * @return A vector of signatures (stable ID plus geometry) for the currently visible displays
 */
vector<string> getVisibleDisplaySignatures() {
    vector<string> signatures = queryVisibleDisplaysWithFlags(QDC_ONLY_ACTIVE_PATHS, true);

    if (!signatures.empty()) {
        return signatures;
    }

    return queryVisibleDisplaysWithFlags(QDC_DATABASE_CURRENT, true);
}

/**
 * Gets a vector of display IDs by utilizing QueryDisplayConfig and DisplayConfigGetDeviceInfo. This reflects the
 * persisted display configuration in the Windows display database. The returned IDs are converted into stable IDs.
 *
 * @return A vector of stable display IDs for the current persisted display configuration
 */
vector<string> getQueryDisplayConfigDisplayIds() {
    DisplayConfig config = getDisplayConfig();
    vector<string> displayIds;

    for (UINT32 i = 0; i < config.numPathInfoArrayElements; i++) {
        DISPLAYCONFIG_TARGET_DEVICE_NAME target = {};
        target.header.adapterId = config.pathInfoArray[i].targetInfo.adapterId;
        target.header.id = config.pathInfoArray[i].targetInfo.id;
        target.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME;
        target.header.size = sizeof(target);

        if (DisplayConfigGetDeviceInfo(&target.header) != ERROR_SUCCESS) {
            continue;
        }

        displayIds.push_back(buildStableDisplayId(target.monitorDevicePath));
    }

    return displayIds;
}

/**
 * Gets a vector of display IDs by utilizing the EnumDisplayDevices API. Only devices attached to the desktop are
 * included. The returned IDs are converted into stable IDs.
 *
 * @return A vector of stable display IDs for devices attached to the desktop
 */
vector<string> getEnumDisplayDevicesDisplayIds() {
    DISPLAY_DEVICE displayDevice;
    SecureZeroMemory(&displayDevice, sizeof(DISPLAY_DEVICE));
    displayDevice.cb = sizeof(displayDevice);

    vector<string> displayIds;
    UINT32 index = 0;

    while (EnumDisplayDevices(NULL, index, &displayDevice, 0)) {
        if (displayDevice.StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            WCHAR nameBuf[32];
            lstrcpyW(nameBuf, displayDevice.DeviceName);

            EnumDisplayDevices(nameBuf, 0, &displayDevice, EDD_GET_DEVICE_INTERFACE_NAME);

            displayIds.push_back(buildStableDisplayId(displayDevice.DeviceID));
        }

        index++;
    }

    return displayIds;
}

/**
 * Gets the index in the EnumDisplayDevices display ID vector for the given display ID.
 *
 * @param displayId
 *        - The stable ID of the display to get the index for
 *
 * @return The index in the EnumDisplayDevices display ID vector for the given display ID, or 0 if not found
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId) {
    vector<string> displayIds = getEnumDisplayDevicesDisplayIds();

    for (int i = 0; i < (int) displayIds.size(); i++) {
        if (displayIds[i] == displayId) {
            return i;
        }
    }

    return 0;
}

/**
 * Gets the index in the QueryDisplayConfig display ID vector for the given display ID.
 *
 * @param displayId
 *        - The stable ID of the display to get the index for
 *
 * @return The index in the QueryDisplayConfig display ID vector for the given display ID, or 0 if not found
 */
int getQueryDisplayConfigDisplayIdIndex(string displayId) {
    vector<string> displayIds = getQueryDisplayConfigDisplayIds();

    for (int i = 0; i < (int) displayIds.size(); i++) {
        if (displayIds[i] == displayId) {
            return i;
        }
    }

    return 0;
}

/**
 * Builds a stable display ID from a monitor device path by removing the volatile UID segment and optional trailing
 * GUID. This produces a consistent identifier across device instance changes (e.g., virtual display re-creations).
 *
 * @param monitorDevicePath
 *        - The raw monitor device path as returned by DisplayConfigGetDeviceInfo
 *
 * @return A normalized, stable display ID string
 */
string buildStableDisplayId(const wstring &monitorDevicePath) {
    string path = wStrToStr(monitorDevicePath);

    // Strip trailing GUID (#{...})
    size_t guidPos = path.find("#{");

    if (guidPos != string::npos) {
        path.erase(guidPos);
    }

    // Remove &UID#### segment
    size_t uidPos = path.find("&UID");

    if (uidPos != string::npos) {
        size_t endPos = uidPos + 4;

        while (endPos < path.size() && isdigit(static_cast<unsigned char>(path[endPos]))) {
            endPos++;
        }

        path.erase(uidPos, endPos - uidPos);
    }

    // Trim whitespace
    auto trim = [](string &s) {
        while (!s.empty() && isspace(static_cast<unsigned char>(s.front()))) {
            s.erase(s.begin());
        }

        while (!s.empty() && isspace(static_cast<unsigned char>(s.back()))) {
            s.pop_back();
        }
    };

    trim(path);

    // Normalize case
    transform(path.begin(), path.end(), path.begin(), [](unsigned char c) { return static_cast<char>(tolower(c)); });

    return path;
}

/**
 * Converts a wide string (UTF-16) to a UTF-8 encoded string.
 *
 * @param wideString
 *        - The wide string to convert
 *
 * @return A UTF-8 encoded string, or an empty string if conversion fails
 */
string wStrToStr(const wstring &wideString) {
    if (wideString.empty()) {
        return string();
    }

    int sizeNeeded = WideCharToMultiByte(CP_UTF8, 0, wideString.c_str(), -1, NULL, 0, NULL, NULL);

    if (sizeNeeded <= 0) {
        return string();
    }

    string result(sizeNeeded - 1, '\0');
    WideCharToMultiByte(CP_UTF8, 0, wideString.c_str(), -1, &result[0], sizeNeeded, NULL, NULL);

    return result;
}
