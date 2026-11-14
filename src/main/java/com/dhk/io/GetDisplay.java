/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
package com.dhk.io;

/**
 * Utilizes the GetDisplay JNI library to retrieve current display settings, including supported display modes,
 * connected display IDs, visible display IDs, and display orientations.
 *
 * @author Jonathan R. Miller
 */
public class GetDisplay {

    /**
     * Default constructor for the {@link GetDisplay} class.
     */
    public GetDisplay() {
    }

    static {
        try {
            System.loadLibrary("GetDisplay");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Defines a JNI function to enumerate the supported display modes for the given display.
     *
     * @param displayId
     *            - The ID of the display to get the array of supported display modes for
     *
     * @return A flat int array of {width, height, refreshNumerator, refreshDenominator} per supported mode
     */
    private native int[] enumDisplayModes(String displayId);

    /**
     * Defines a JNI function to get the current orientation of each visible display, aligned index-for-index with
     * getVisibleDisplayIds.
     *
     * @return The current orientation of each visible display, in getVisibleDisplayIds order
     */
    private native int[] queryVisibleDisplayOrientations();

    /**
     * Defines a JNI function to enumerate the display IDs for the displays that are currently visible.
     *
     * @return The array of display IDs for the currently visible displays
     */
    private native String[] enumVisibleDisplayIds();

    /**
     * Defines a JNI function to get the Windows Display Settings number of each given visible display, index-for-index
     * with the provided IDs. Taking the IDs the caller already holds avoids re-querying them.
     *
     * @param visibleIds
     *            - The visible display IDs to number, typically from getVisibleDisplayIds
     *
     * @return The Windows display number of each given visible display, index-for-index with visibleIds
     */
    private native int[] enumVisibleDisplayNumbers(String[] visibleIds);

    /**
     * Defines a JNI function to compute the supported DPI scale percentages for the given resolution.
     *
     * @param width
     *            - The horizontal resolution to get the supported DPI scale percentages for
     * @param height
     *            - The vertical resolution to get the supported DPI scale percentages for
     *
     * @return The array of supported DPI scale percentages for the given resolution
     */
    private native int[] getSupportedDpiScalePercentages(int width, int height);

    /**
     * Defines a JNI function to capture the current multi-display arrangement as encoded strings.
     *
     * @return The captured arrangement, one encoded rectangle per active display
     */
    private native String[] captureDisplayArrangement();

    /**
     * Defines a JNI function to read the settings currently in effect on each given display, as a flat int array of
     * {width, height, refreshNumerator, refreshDenominator, dpiScalePercentage, scalingMode, orientation, desktopX,
     * desktopY} records.
     *
     * @param displayIds
     *            - The IDs of the displays to read the active settings of
     *
     * @return A flat int array of one active display settings record per given display, in the given order
     */
    private native int[] queryActiveDisplaySettings(String[] displayIds);

    /**
     * Gets the supported display modes for the given display as a flat int array of {width, height, refreshNumerator,
     * refreshDenominator} records. The caller rebuilds the display-mode objects from these fields so the JNI boundary
     * stays primitive and the exact rational refresh rate is preserved.
     *
     * @param displayId
     *            - The ID of the display to get the supported display mode records for
     *
     * @return A flat int array of {width, height, refreshNumerator, refreshDenominator} per supported mode
     */
    public int[] getDisplayModeRecords(String displayId) {
        return enumDisplayModes(displayId);
    }

    /**
     * Gets the current orientation of each visible display, aligned index-for-index with getVisibleDisplayIds.
     *
     * @return The current orientation of each visible display, in getVisibleDisplayIds order
     */
    public int[] getDisplayOrientations() {
        return queryVisibleDisplayOrientations();
    }

    /**
     * Gets the display IDs for the displays that are currently visible.
     *
     * @return The array of display IDs for the currently visible displays
     */
    public String[] getVisibleDisplayIds() {
        return enumVisibleDisplayIds();
    }

    /**
     * Gets the Windows Display Settings number of each given visible display, index-for-index with the provided IDs.
     *
     * @param visibleIds
     *            - The visible display IDs to number, typically from getVisibleDisplayIds
     *
     * @return The Windows display number of each given visible display, index-for-index with visibleIds
     */
    public int[] getVisibleDisplayNumbers(String[] visibleIds) {
        return enumVisibleDisplayNumbers(visibleIds);
    }

    /**
     * Gets the supported DPI scale percentages for the given resolution. The supported set matches the percentages
     * Windows would offer for that resolution.
     *
     * @param width
     *            - The horizontal resolution to get the supported DPI scale percentages for
     * @param height
     *            - The vertical resolution to get the supported DPI scale percentages for
     *
     * @return The array of supported DPI scale percentages for the given resolution
     */
    public Integer[] getDpiScalePercentages(int width, int height) {
        int[] supportedDpiScalePercentages = getSupportedDpiScalePercentages(width, height);
        Integer[] dpiScalePercentages = new Integer[supportedDpiScalePercentages.length];

        for (int i = 0; i < supportedDpiScalePercentages.length; i++) {
            dpiScalePercentages[i] = supportedDpiScalePercentages[i];
        }

        return dpiScalePercentages;
    }

    /**
     * Captures the current multi-display arrangement so a following batch of applied display settings can be reflowed
     * against it. The result is opaque and is passed back to SetDisplay to preserve the arrangement.
     *
     * @return The captured arrangement, one encoded rectangle per active display
     */
    public String[] captureArrangement() {
        return captureDisplayArrangement();
    }

    /**
     * Gets the settings currently in effect on each given display as a flat int array of {width, height,
     * refreshNumerator, refreshDenominator, dpiScalePercentage, scalingMode, orientation, desktopX, desktopY} records.
     * A display without a visible active path gets a record of zeros, and the caller rebuilds the settings objects.
     *
     * @param displayIds
     *            - The IDs of the displays to read the active settings of
     *
     * @return A flat int array of one active display settings record per given display, in the given order
     */
    public int[] getActiveDisplaySettingsRecords(String[] displayIds) {
        return queryActiveDisplaySettings(displayIds);
    }

}
