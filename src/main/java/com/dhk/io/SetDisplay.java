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

import java.util.List;

/**
 * Utilizes the SetDisplay JNI library to immediately apply the given display mode, DPI scale percentage, scaling mode,
 * and orientation mode to one or more displays.
 *
 * @author Jonathan R. Miller
 */
public class SetDisplay {

    /**
     * Index of the horizontal resolution in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_WIDTH = 0;

    /**
     * Index of the vertical resolution in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_HEIGHT = 1;

    /**
     * Index of the refresh rate's numerator in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_REFRESH_NUMERATOR = 2;

    /**
     * Index of the refresh rate's denominator in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_REFRESH_DENOMINATOR = 3;

    /**
     * Index of the DPI scale percentage in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_DPI_SCALE_PERCENTAGE = 4;

    /**
     * Index of the scaling mode in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_SCALING_MODE = 5;

    /**
     * Index of the orientation mode in each display's settings record passed to {@link #setDisplays}.
     */
    private static final int DISPLAY_SETTING_ORIENTATION = 6;

    /**
     * Number of values in each display's settings record passed to {@link #setDisplays}, which the native library reads
     * through the same generated constant.
     */
    private static final int DISPLAY_SETTING_COUNT = 7;

    /**
     * Default constructor for the {@link SetDisplay} class.
     */
    public SetDisplay() {
    }

    static {
        try {
            System.loadLibrary("SetDisplay");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Defines a JNI function to immediately apply the given settings to one or more displays.
     *
     * @param displayIds
     *            - The IDs of the displays to apply the display settings for
     * @param displaySettings
     *            - The settings of each display, {@link #DISPLAY_SETTING_COUNT} values per display in displayIds order
     */
    private native void setDisplays(String[] displayIds, int[] displaySettings);

    /**
     * Defines a JNI function to reflow the multi-display arrangement after a batch of display changes.
     *
     * @param arrangementSnapshot
     *            - The arrangement captured before the batch, one encoded rectangle per display
     */
    private native void preserveDisplayArrangement(String[] arrangementSnapshot);

    /**
     * Immediately applies the given settings to each display, rotating each display before applying its display mode,
     * scaling mode, and DPI scale percentage.
     *
     * @param displaySettingsRequests
     *            - The settings to apply, one request per display
     */
    public void applyDisplaySettings(List<DisplaySettingsRequest> displaySettingsRequests) {
        String[] displayIds = new String[displaySettingsRequests.size()];
        int[] displaySettings = new int[displaySettingsRequests.size() * DISPLAY_SETTING_COUNT];

        for (int requestIndex = 0; requestIndex < displaySettingsRequests.size(); requestIndex++) {
            DisplaySettingsRequest request = displaySettingsRequests.get(requestIndex);
            int recordStart = requestIndex * DISPLAY_SETTING_COUNT;

            displayIds[requestIndex] = request.displayId();
            displaySettings[recordStart + DISPLAY_SETTING_WIDTH] = request.width();
            displaySettings[recordStart + DISPLAY_SETTING_HEIGHT] = request.height();
            displaySettings[recordStart + DISPLAY_SETTING_REFRESH_NUMERATOR] = request.refreshNumerator();
            displaySettings[recordStart + DISPLAY_SETTING_REFRESH_DENOMINATOR] = request.refreshDenominator();
            displaySettings[recordStart + DISPLAY_SETTING_DPI_SCALE_PERCENTAGE] = request.dpiScalePercentage();
            displaySettings[recordStart + DISPLAY_SETTING_SCALING_MODE] = request.scalingMode();
            displaySettings[recordStart + DISPLAY_SETTING_ORIENTATION] = request.orientationMode();
        }

        setDisplays(displayIds, displaySettings);
    }

    /**
     * Reflows the multi-display arrangement against the given snapshot (from GetDisplay's captureArrangement), so every
     * display keeps its relative position and alignment after one or more displays were resized.
     *
     * @param arrangementSnapshot
     *            - The arrangement captured before applying the batch, one encoded rectangle per display
     */
    public void preserveArrangement(String[] arrangementSnapshot) {
        preserveDisplayArrangement(arrangementSnapshot);
    }

}