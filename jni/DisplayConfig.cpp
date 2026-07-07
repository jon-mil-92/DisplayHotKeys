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
 * Queries the full persisted display configuration (QDC_DATABASE_CURRENT), including inactive displays, into an owning
 * DisplayConfig.
 *
 * @return The display paths and modes for the current configuration, or an empty configuration on failure
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
 * Gets stable IDs for desktop-attached devices via EnumDisplayDevices.
 *
 * @return Stable display IDs for devices attached to the desktop
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
 * Gets stable IDs for the persisted configuration, in QueryDisplayConfig path order.
 *
 * @return Stable display IDs for the current persisted display configuration
 */
vector<string> getQueryDisplayConfigDisplayIds() {
    DisplayConfig config = getDisplayConfig();
    vector<string> displayIds;

    for (UINT32 i = 0; i < config.numPathInfoArrayElements; i++) {
        string displayId = stableIdForTarget(config.pathInfoArray[i].targetInfo);

        if (displayId.empty()) {
            continue;
        }

        displayIds.push_back(displayId);
    }

    return displayIds;
}

/**
 * Queries paths with the given flags and returns the stable ID of each visible display, where a display is visible when
 * it has an active path, a valid source mode of type SOURCE, non-zero size, and an on-screen position (large negative
 * positions are treated as offscreen). Buffers are over-allocated to avoid ERROR_INVALID_PARAMETER from drivers that
 * under-report sizes.
 *
 * @param flags
 *            - The QueryDisplayConfig flags selecting which paths to enumerate
 * @param includeGeometry
 *            - Whether each entry also carries resolution, position, rotation, and DPI scale index
 * @param rotationsOut
 *            - When non-null, receives each visible display's rotation in lockstep with the returned IDs
 *
 * @return The stable IDs of the visible displays, or their signatures when includeGeometry is set
 */
static vector<string> queryVisibleDisplaysWithFlags(UINT32 flags, bool includeGeometry,
                                                    vector<int> *rotationsOut = nullptr) {
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

        string entry = stableIdForTarget(path.targetInfo);

        if (entry.empty()) {
            continue;
        }

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

        // Emit the rotation in lockstep so callers can align orientation to the visible ID at the same index
        if (rotationsOut != nullptr) {
            rotationsOut->push_back((int) path.targetInfo.rotation);
        }
    }

    delete[] pathArray;
    delete[] modeArray;

    return visible;
}

/**
 * Gets stable IDs for the currently visible displays via QDC_ONLY_ACTIVE_PATHS, falling back to QDC_DATABASE_CURRENT.
 *
 * @return Stable display IDs for the currently visible displays
 */
vector<string> getVisibleDisplayIds() {
    vector<string> displayIds = queryVisibleDisplaysWithFlags(QDC_ONLY_ACTIVE_PATHS, false);

    if (!displayIds.empty()) {
        return displayIds;
    }

    return queryVisibleDisplaysWithFlags(QDC_DATABASE_CURRENT, false);
}

/**
 * Gets each visible display's orientation, in the same order and with the same visibility filtering as
 * getVisibleDisplayIds, so the returned rotation at a given index belongs to that same visible display.
 *
 * @return The rotation of each visible display, aligned index-for-index with getVisibleDisplayIds
 */
vector<int> getVisibleDisplayOrientations() {
    vector<int> rotations;
    queryVisibleDisplaysWithFlags(QDC_ONLY_ACTIVE_PATHS, false, &rotations);

    if (!rotations.empty()) {
        return rotations;
    }

    rotations.clear();
    queryVisibleDisplaysWithFlags(QDC_DATABASE_CURRENT, false, &rotations);

    return rotations;
}

/**
 * Gets per-display geometry signatures for the visible displays, using the same active-then-database fallback as
 * getVisibleDisplayIds.
 *
 * @return Signatures for the currently visible displays
 */
vector<string> getVisibleDisplaySignatures() {
    vector<string> signatures = queryVisibleDisplaysWithFlags(QDC_ONLY_ACTIVE_PATHS, true);

    if (!signatures.empty()) {
        return signatures;
    }

    return queryVisibleDisplaysWithFlags(QDC_DATABASE_CURRENT, true);
}

/**
 * Gets the index of the given stable ID in the EnumDisplayDevices ID list.
 *
 * @param displayId
 *            - The stable display ID to locate
 *
 * @return The index, or 0 if not found
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId) {
    vector<string> displayIds = getEnumDisplayDevicesDisplayIds();

    for (int i = 0; i < (int) displayIds.size(); i++) {
        const string &coreId = displayIds[i];

        // EnumDisplayDevices IDs carry no friendly-name suffix, so match the core that precedes it
        if (displayId.rfind(coreId, 0) == 0 && (displayId.size() == coreId.size() || displayId[coreId.size()] == '#')) {
            return i;
        }
    }

    return 0;
}

/**
 * Gets the index of the given stable ID in the QueryDisplayConfig ID list.
 *
 * @param displayId
 *            - The stable display ID to locate
 *
 * @return The index, or 0 if not found
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
 * Builds a compact, stable display ID from a monitor device path and its friendly name. Strips the constant
 * device-interface boilerplate, the volatile connection UID, and the trailing GUID so the ID survives instance
 * changes, then appends the sanitized friendly name so displays sharing a device-path prefix stay distinct.
 *
 * @param monitorDevicePath
 *            - The raw monitor device path from DisplayConfigGetDeviceInfo
 * @param friendlyName
 *            - The monitor/client name from DISPLAYCONFIG_TARGET_DEVICE_NAME, or empty when unavailable
 *
 * @return A normalized, stable display ID
 */
string buildStableDisplayId(const wstring &monitorDevicePath, const wstring &friendlyName) {
    string path = wStrToStr(monitorDevicePath);

    // Drop the constant \\?\DISPLAY# boilerplate so the ID keeps only distinguishing parts and stays readable
    if (path.rfind("\\\\?\\", 0) == 0) {
        size_t branchEnd = path.find('#');

        if (branchEnd != string::npos) {
            path.erase(0, branchEnd + 1);
        }
    }

    // Strip trailing GUID (#{...})
    size_t guidPos = path.find("#{");

    if (guidPos != string::npos) {
        path.erase(guidPos);
    }

    // Remove the volatile &UID#### segment so a display re-created with a new connection UID keeps the same ID
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

    // Append the sanitized friendly name ([a-z0-9]) so clients sharing one device-path prefix stay distinct
    string nameToken;

    for (wchar_t wc : friendlyName) {
        if (wc < 128 && isalnum((unsigned char) wc)) {
            nameToken.push_back((char) tolower((unsigned char) wc));
        }
    }

    if (!nameToken.empty()) {
        path += "#" + nameToken;
    }

    return path;
}

/**
 * Converts a UTF-16 wide string to UTF-8.
 *
 * @param wideString
 *            - The wide string to convert
 *
 * @return The UTF-8 string, or empty on failure
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

/**
 * Resolves a path target's monitor device path to a stable display ID, centralizing the
 * DisplayConfigGetDeviceInfo(GET_TARGET_NAME) then buildStableDisplayId sequence.
 *
 * @param targetInfo
 *            - The path target to resolve
 *
 * @return The stable display ID, or empty if it could not be resolved
 */
string stableIdForTarget(const DISPLAYCONFIG_PATH_TARGET_INFO &targetInfo) {
    DISPLAYCONFIG_TARGET_DEVICE_NAME target = {};
    target.header.adapterId = targetInfo.adapterId;
    target.header.id = targetInfo.id;
    target.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME;
    target.header.size = sizeof(target);

    if (DisplayConfigGetDeviceInfo(&target.header) != ERROR_SUCCESS) {
        return string();
    }

    return buildStableDisplayId(target.monitorDevicePath, target.monitorFriendlyDeviceName);
}

/**
 * Resolves a path source to its GDI device name (e.g. \\.\DISPLAY1) for the legacy EnumDisplaySettings/
 * ChangeDisplaySettings APIs, centralizing the DisplayConfigGetDeviceInfo(GET_SOURCE_NAME) sequence.
 *
 * @param sourceInfo
 *            - The path source to resolve
 *
 * @return The GDI device name, or empty if it could not be resolved
 */
wstring sourceGdiDeviceName(const DISPLAYCONFIG_PATH_SOURCE_INFO &sourceInfo) {
    DISPLAYCONFIG_SOURCE_DEVICE_NAME sourceName = {};
    sourceName.header.adapterId = sourceInfo.adapterId;
    sourceName.header.id = sourceInfo.id;
    sourceName.header.type = DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME;
    sourceName.header.size = sizeof(sourceName);

    if (DisplayConfigGetDeviceInfo(&sourceName.header) != ERROR_SUCCESS) {
        return wstring();
    }

    return wstring(sourceName.viewGdiDeviceName);
}

/**
 * Queries the active display configuration (QDC_ONLY_ACTIVE_PATHS) into the given vectors.
 *
 * @param paths
 *            - Receives the active path array
 * @param modes
 *            - Receives the active mode array
 *
 * @return Whether the query succeeded
 */
bool queryActiveCcdConfig(vector<DISPLAYCONFIG_PATH_INFO> &paths, vector<DISPLAYCONFIG_MODE_INFO> &modes) {
    UINT32 numPath = 0;
    UINT32 numMode = 0;

    if (GetDisplayConfigBufferSizes(QDC_ONLY_ACTIVE_PATHS, &numPath, &numMode) != ERROR_SUCCESS) {
        return false;
    }

    paths.assign(numPath, DISPLAYCONFIG_PATH_INFO{});
    modes.assign(numMode, DISPLAYCONFIG_MODE_INFO{});

    if (QueryDisplayConfig(QDC_ONLY_ACTIVE_PATHS, &numPath, paths.data(), &numMode, modes.data(), nullptr) !=
        ERROR_SUCCESS) {
        return false;
    }

    paths.resize(numPath);
    modes.resize(numMode);

    return true;
}

/**
 * Finds the active path index that drives the display with the given stable ID.
 *
 * @param paths
 *            - The active path array to search
 * @param stableId
 *            - The stable display ID to match
 *
 * @return The matching path index, or -1 if not found
 */
int findActivePathForDisplay(const vector<DISPLAYCONFIG_PATH_INFO> &paths, const string &stableId) {
    for (size_t i = 0; i < paths.size(); i++) {
        const DISPLAYCONFIG_PATH_INFO &path = paths[i];

        if ((path.flags & DISPLAYCONFIG_PATH_ACTIVE) == 0) {
            continue;
        }

        if (stableIdForTarget(path.targetInfo) == stableId) {
            return (int) i;
        }
    }

    return -1;
}

/**
 * Maps a truncated integer refresh rate to the rational encodings the CCD API may expect. The exact integer form
 * (rate / 1) is tried first, then the two forms of a fractional NTSC rate ((rate + 1) * 1000 / 1001 and its rounded
 * decimal). Derived from the value itself with no per-rate table, and the caller uses whichever encoding validates.
 *
 * @param rate
 *            - The integer refresh rate to map
 *
 * @return The ordered candidate rationals to try
 */
vector<DISPLAYCONFIG_RATIONAL> toRefreshRationalCandidates(int rate) {
    auto rational = [](UINT32 numerator, UINT32 denominator) {
        DISPLAYCONFIG_RATIONAL value = {};
        value.Numerator = numerator;
        value.Denominator = denominator;
        return value;
    };

    /*
     * Offer the exact integer form (60, 120, 144, ...) first, then the two rational encodings drivers use for a
     * fractional NTSC rate (n000/1001 and its rounded decimal, where n is rate + 1). Deriving both from the rate itself
     * covers every fractional rate without a per-rate table, and the caller uses whichever one validates
     */
    UINT32 nominal = (UINT32) rate + 1;

    return {rational((UINT32) rate, 1), rational(nominal * 1000, 1001), rational((nominal * 100000 + 500) / 1001, 100)};
}

/**
 * Sets the source resolution and refresh rate on the chosen path of copies of the base arrays, then submits them to
 * SetDisplayConfig with the given flags. Taking the arrays by value keeps the caller's base config reusable.
 *
 * @param paths
 *            - A copy of the active path array (modified locally)
 * @param modes
 *            - A copy of the active mode array (modified locally)
 * @param pathIndex
 *            - Index of the path to reconfigure
 * @param width
 *            - Source resolution width to set
 * @param height
 *            - Source resolution height to set
 * @param refreshRate
 *            - Target refresh rate (rational) to set
 * @param flags
 *            - SetDisplayConfig flags (e.g. SDC_VALIDATE or SDC_APPLY variants)
 *
 * @return The SetDisplayConfig result code
 */
LONG submitCcdSourceMode(vector<DISPLAYCONFIG_PATH_INFO> paths, vector<DISPLAYCONFIG_MODE_INFO> modes, int pathIndex,
                         UINT32 width, UINT32 height, DISPLAYCONFIG_RATIONAL refreshRate, UINT32 flags) {
    DISPLAYCONFIG_PATH_INFO &path = paths[pathIndex];
    UINT32 sourceModeIdx = path.sourceInfo.modeInfoIdx;

    if (sourceModeIdx == DISPLAYCONFIG_PATH_MODE_IDX_INVALID || sourceModeIdx >= modes.size() ||
        modes[sourceModeIdx].infoType != DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE) {
        return ERROR_INVALID_PARAMETER;
    }

    // Set the desktop (source) resolution
    modes[sourceModeIdx].sourceMode.width = width;
    modes[sourceModeIdx].sourceMode.height = height;

    // Request the refresh rate and clear the target mode index so Windows recomputes the target timing
    path.targetInfo.refreshRate = refreshRate;
    path.targetInfo.scanLineOrdering = DISPLAYCONFIG_SCANLINE_ORDERING_PROGRESSIVE;
    path.targetInfo.modeInfoIdx = DISPLAYCONFIG_PATH_MODE_IDX_INVALID;

    return SetDisplayConfig((UINT32) paths.size(), paths.data(), (UINT32) modes.size(), modes.data(), flags);
}
