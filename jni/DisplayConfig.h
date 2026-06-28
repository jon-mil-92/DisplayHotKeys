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
#pragma once

#include <Windows.h>
#include <cstdint>
#include <string>
#include <vector>

using namespace std;

/*
 * Supported DPI scale percentages and related constants.
 */
static const vector<int32_t> DPI_SCALE_PERCENTAGES = {100, 125, 150, 175, 200, 225, 250, 300, 350};
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
 * Holds the display configuration information for the connected displays as returned by QueryDisplayConfig.
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

/**
 * Queries the display configuration to initialize a structure that holds the persisted paths and modes as defined in
 * the Windows display persistence database.
 *
 * @return A DisplayConfig structure containing the display paths and modes for the current configuration
 */
DisplayConfig getDisplayConfig();

/**
 * Gets a vector of display IDs by utilizing the EnumDisplayDevices API. Only devices attached to the desktop are
 * included. The returned IDs are converted into stable IDs.
 *
 * @return A vector of stable display IDs for devices attached to the desktop
 */
vector<string> getEnumDisplayDevicesDisplayIds();

/**
 * Gets a vector of display IDs by utilizing QueryDisplayConfig and DisplayConfigGetDeviceInfo. This reflects the
 * persisted display configuration in the Windows display database. The returned IDs are converted into stable IDs.
 *
 * @return A vector of stable display IDs for the current persisted display configuration
 */
vector<string> getQueryDisplayConfigDisplayIds();

/**
 * Gets a vector of display IDs for displays that are currently visible. A display is considered visible if it has an
 * active display path and its source mode reports a non-zero resolution and a valid desktop position. Returned IDs are
 * converted into stable IDs.
 *
 * @return A vector of stable display IDs for the currently visible displays
 */
vector<string> getVisibleDisplayIds();

/**
 * Gets a per-display signature for each currently visible display. Each signature is the display's stable ID followed
 * by its source resolution, desktop position, rotation, and relative DPI scale index, so that resolution, DPI, and
 * orientation changes are detectable even when the set of visible displays is unchanged.
 *
 * @return A vector of signatures (stable ID plus geometry) for the currently visible displays
 */
vector<string> getVisibleDisplaySignatures();

/**
 * Gets the index in the EnumDisplayDevices display ID vector for the given display ID.
 *
 * @param displayId
 *        - The stable ID of the display to get the index for
 *
 * @return The index in the EnumDisplayDevices display ID vector for the given display ID, or 0 if not found
 */
int getEnumDisplayDevicesDisplayIdIndex(string displayId);

/**
 * Gets the index in the QueryDisplayConfig display ID vector for the given display ID.
 *
 * @param displayId
 *        - The stable ID of the display to get the index for
 *
 * @return The index in the QueryDisplayConfig display ID vector for the given display ID, or 0 if not found
 */
int getQueryDisplayConfigDisplayIdIndex(string displayId);

/**
 * Builds a stable display ID from a monitor device path by removing the volatile UID segment and optional trailing
 * GUID. This produces a consistent identifier across device instance changes (e.g., virtual display re-creations).
 *
 * @param monitorDevicePath
 *        - The raw monitor device path as returned by DisplayConfigGetDeviceInfo
 *
 * @return A normalized, stable display ID string
 */
string buildStableDisplayId(const wstring &monitorDevicePath);

/**
 * Converts a wide string (UTF-16) to a UTF-8 encoded string.
 *
 * @param wideString
 *        - The wide string to convert
 *
 * @return A UTF-8 encoded string, or an empty string if conversion fails
 */
string wStrToStr(const wstring &wideString);
