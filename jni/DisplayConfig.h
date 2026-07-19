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

/**
 * Supported DPI scale percentages, in ascending order, that Windows can apply.
 */
static const vector<int32_t> DPI_SCALE_PERCENTAGES = {100, 125, 150, 175, 200, 225, 250, 300, 350, 400, 450, 500};

/**
 * Number of entries in DPI_SCALE_PERCENTAGES.
 */
static const int32_t NUM_OF_DPI_SCALE_PERCENTAGES = 12;

/**
 * DisplayConfigGetDeviceInfo type value for reading the relative DPI scale indices. Undocumented but identical and
 * stable on Windows 10 and 11; callers must check ERROR_SUCCESS and degrade gracefully.
 */
static const int32_t DISPLAYCONFIG_DEVICE_INFO_HEADER_GET_DPI_TYPE = -3;

/**
 * DisplayConfigSetDeviceInfo type value for setting the relative DPI scale index. Undocumented but identical and
 * stable on Windows 10 and 11; callers must check the result and degrade gracefully.
 */
static const int32_t DISPLAYCONFIG_DEVICE_INFO_HEADER_SET_DPI_TYPE = -4;

/**
 * Minimum effective (logical) long-edge resolution kept usable when capping the max DPI scale, so a higher scale is
 * offered only while the raw long edge divided by that scale stays at or above this floor.
 */
static const int32_t MIN_EFFECTIVE_LONG_EDGE = 800;

/**
 * Minimum effective (logical) short-edge resolution kept usable when capping the max DPI scale, applied together with
 * MIN_EFFECTIVE_LONG_EDGE so the offered scale set is orientation-independent.
 */
static const int32_t MIN_EFFECTIVE_SHORT_EDGE = 600;

/**
 * Relative DPI scale indices (minimum, current, maximum) from the recommended scale.
 */
struct DISPLAYCONFIG_GET_DPI_SCALE_INDICES {
    /**
     * Standard device-info header identifying the source adapter and ID.
     */
    DISPLAYCONFIG_DEVICE_INFO_HEADER header;

    /**
     * Minimum DPI scale index relative to the recommended scale.
     */
    int32_t relativeMinimumDpiScaleIndex;

    /**
     * Current DPI scale index relative to the recommended scale.
     */
    int32_t relativeCurrentDpiScaleIndex;

    /**
     * Maximum DPI scale index relative to the recommended scale.
     */
    int32_t relativeMaximumDpiScaleIndex;
};

/**
 * Relative index of the DPI scale percentage to set.
 */
struct DISPLAYCONFIG_SET_DPI_SCALE_INDEX {
    /**
     * Standard device-info header identifying the source adapter and ID.
     */
    DISPLAYCONFIG_DEVICE_INFO_HEADER header;

    /**
     * DPI scale index to apply, relative to the recommended scale.
     */
    int32_t relativeDpiScaleIndex;
};

/**
 * Owns the path/mode arrays returned by QueryDisplayConfig and frees them on destruction.
 */
struct DisplayConfig {
    /**
     * Number of elements in pathInfoArray.
     */
    UINT32 numPathInfoArrayElements;

    /**
     * Number of elements in modeInfoArray.
     */
    UINT32 numModeInfoArrayElements;

    /**
     * Owned array of display paths from QueryDisplayConfig.
     */
    DISPLAYCONFIG_PATH_INFO *pathInfoArray;

    /**
     * Owned array of display modes from QueryDisplayConfig.
     */
    DISPLAYCONFIG_MODE_INFO *modeInfoArray;

    ~DisplayConfig() {
        delete[] pathInfoArray;
        delete[] modeInfoArray;
    }
};

/**
 * A single visible display collected during enumeration, carrying its GDI device number so the list can be ordered to
 * match how Windows Display Settings numbers displays before the descriptor is emitted.
 */
struct VisibleDisplay {
    /**
     * The GDI device number (the trailing integer of \\.\DISPLAYn). Windows Display Settings orders and numbers
     * displays by this value across all adapters, so it is the correct ordering key even when a virtual-display
     * adapter hands out target ids that do not follow the on-screen order.
     */
    UINT32 gdiNumber;

    /**
     * The stable display ID, or the geometry signature when geometry is included.
     */
    string descriptor;

    /**
     * The display's rotation, emitted in lockstep with the descriptor when the caller requests rotations.
     */
    int rotation;
};

/**
 * A single supported display mode collected during enumeration.
 */
struct ModeInfo {
    /**
     * Horizontal resolution in pixels.
     */
    int width;

    /**
     * Vertical resolution in pixels.
     */
    int height;

    /**
     * Color depth in bits per pixel.
     */
    int bitsPerPel;

    /**
     * Refresh rate in hertz.
     */
    int frequency;
};

/**
 * Queries the full persisted display configuration (QDC_DATABASE_CURRENT), including inactive displays.
 *
 * @return The display paths and modes for the current configuration
 */
DisplayConfig getDisplayConfig();

/**
 * Gets stable display IDs for all desktop-attached devices via EnumDisplayDevices.
 *
 * @return Stable display IDs for devices attached to the desktop
 */
vector<string> getEnumDisplayDevicesDisplayIds();

/**
 * Gets stable display IDs for the persisted configuration via QueryDisplayConfig.
 *
 * @return Stable display IDs for the current persisted display configuration
 */
vector<string> getQueryDisplayConfigDisplayIds();

/**
 * Gets stable IDs for currently visible displays (active path, non-zero source resolution, on-screen position),
 * ordered by ascending GDI device number, matching the order Windows Display Settings numbers displays in.
 *
 * @return Stable display IDs for the currently visible displays
 */
vector<string> getVisibleDisplayIds();

/**
 * Gets each visible display's orientation, aligned index-for-index with getVisibleDisplayIds so callers can map a
 * visible display to its rotation without an index-space mismatch.
 *
 * @return The rotation of each visible display, in getVisibleDisplayIds order
 */
vector<int> getVisibleDisplayOrientations();

/**
 * Gets the Windows Display Settings number of each given visible display. The number is the display's rank by ascending
 * target id among the displays Windows still knows (active or retained after a disconnect). Taking the visible IDs as
 * input avoids re-querying them and keeps the result aligned with the caller's list.
 *
 * @param visibleIds
 *            - The visible display IDs to number, typically from getVisibleDisplayIds
 *
 * @return The Windows display number of each given visible display, index-for-index with visibleIds
 */
vector<int> getVisibleDisplayNumbers(const vector<string> &visibleIds);

/**
 * Gets a per-display signature (stable ID plus source resolution, position, rotation, DPI scale index, and Windows
 * display number) for each visible display, so resolution/DPI/orientation/identifier changes are detectable even when
 * the visible set is unchanged.
 *
 * @return Signatures for the currently visible displays
 */
vector<string> getVisibleDisplaySignatures();

/**
 * Gets the index of the given stable ID in the EnumDisplayDevices ID list.
 *
 * @param displayId
 *            - The stable display ID to locate
 *
 * @return The index, or 0 if not found
 */
int getEnumDisplayDevicesDisplayIdIndex(const string &displayId);

/**
 * Gets the index of the given stable ID in the QueryDisplayConfig ID list.
 *
 * @param displayId
 *            - The stable display ID to locate
 *
 * @return The index, or 0 if not found
 */
int getQueryDisplayConfigDisplayIdIndex(const string &displayId);

/**
 * Builds a compact, stable display ID from a display's device path and its friendly name. Strips the constant
 * device-interface boilerplate, the volatile connection UID, and the trailing GUID so the ID survives instance
 * changes, then appends the sanitized friendly name so displays sharing a device-path prefix stay distinct.
 *
 * @param monitorDevicePath
 *            - The raw device path from DisplayConfigGetDeviceInfo
 * @param friendlyName
 *            - The display/client name from DISPLAYCONFIG_TARGET_DEVICE_NAME, or empty when unavailable
 *
 * @return A normalized, stable display ID
 */
string buildStableDisplayId(const wstring &monitorDevicePath, const wstring &friendlyName = L"");

/**
 * Converts a UTF-16 wide string to UTF-8.
 *
 * @param wideString
 *            - The wide string to convert
 *
 * @return The UTF-8 string, or empty on failure
 */
string wStrToStr(const wstring &wideString);

/**
 * Resolves a path target's device path to a stable display ID, centralizing the
 * DisplayConfigGetDeviceInfo(GET_TARGET_NAME) then buildStableDisplayId sequence.
 *
 * @param targetInfo
 *            - The path target to resolve
 *
 * @return The stable display ID, or empty if it could not be resolved
 */
string stableIdForTarget(const DISPLAYCONFIG_PATH_TARGET_INFO &targetInfo);

/**
 * Resolves a path source to its GDI device name (e.g. \\.\DISPLAY1) for the legacy EnumDisplaySettings/
 * ChangeDisplaySettings APIs, centralizing the DisplayConfigGetDeviceInfo(GET_SOURCE_NAME) sequence.
 *
 * @param sourceInfo
 *            - The path source to resolve
 *
 * @return The GDI device name, or empty if it could not be resolved
 */
wstring sourceGdiDeviceName(const DISPLAYCONFIG_PATH_SOURCE_INFO &sourceInfo);

/**
 * Shared CCD (Connecting and Configuring Displays) helpers that query the active configuration and submit a supplied
 * source-mode config to SetDisplayConfig, decoupling desktop resolution from refresh rate via GPU scaling the way
 * Windows Advanced Display Settings does. GetDisplay uses them to validate modes and SetDisplay to apply them.
 */

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
bool queryActiveCcdConfig(vector<DISPLAYCONFIG_PATH_INFO> &paths, vector<DISPLAYCONFIG_MODE_INFO> &modes);

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
int findActivePathForDisplay(const vector<DISPLAYCONFIG_PATH_INFO> &paths, const string &stableId);

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
vector<DISPLAYCONFIG_RATIONAL> toRefreshRationalCandidates(int rate);

/**
 * Sets the source resolution and target refresh rate on the chosen path of copies of the base arrays, then submits them
 * to SetDisplayConfig with the given flags. Taking the arrays by value keeps the caller's base config reusable.
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
                         UINT32 width, UINT32 height, DISPLAYCONFIG_RATIONAL refreshRate, UINT32 flags);
