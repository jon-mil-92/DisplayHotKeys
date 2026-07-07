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
package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.dhk.utility.DisplayModeInverter;

/**
 * Gets the current information for the connected displays, including display IDs, supported display modes, and
 * orientation-aware landscape/portrait mode mappings. Uses visible display IDs to reflect the current desktop
 * configuration.
 *
 * @author Jonathan R. Miller
 */
public class DisplayConfig {

    private String[] displayIds;
    private GetDisplay getDisplay;
    private Map<String, DisplayMode[]> landscapeDisplayModesMap;
    private Map<String, DisplayMode[]> portraitDisplayModesMap;
    private Map<Long, Integer[]> supportedDpiScalePercentages;
    private int numOfConnectedDisplays;

    /**
     * Constructor for the {@link DisplayConfig} class.
     */
    public DisplayConfig() {
        getDisplay = new GetDisplay();
        landscapeDisplayModesMap = new HashMap<String, DisplayMode[]>();
        portraitDisplayModesMap = new HashMap<String, DisplayMode[]>();
        supportedDpiScalePercentages = new HashMap<Long, Integer[]>();
    }

    /**
     * Updates the current display configuration, including connected displays and their supported display modes.
     */
    public void updateDisplayConfig() {
        updateConnectedDisplays();
        updateDisplayModes();
    }

    /**
     * Updates the current array of unique visible display IDs and stores the number of connected (visible) displays.
     */
    public void updateConnectedDisplays() {
        String[] rawDisplayIds = getDisplay.getVisibleDisplayIds();
        int count = 0;

        for (String displayId : rawDisplayIds) {
            if (displayId != null && !displayId.isBlank()) {
                count++;
            }
        }

        String[] filteredDisplayIds = new String[count];
        int filteredDisplayIdsIndex = 0;

        for (String s : rawDisplayIds) {
            if (s != null && !s.isBlank()) {
                filteredDisplayIds[filteredDisplayIdsIndex++] = s;
            }
        }

        displayIds = filteredDisplayIds;
        numOfConnectedDisplays = filteredDisplayIds.length;
    }

    /**
     * Gets the current array of landscape and portrait display modes for each connected (visible) display.
     */
    private void updateDisplayModes() {
        landscapeDisplayModesMap = new HashMap<String, DisplayMode[]>(numOfConnectedDisplays);
        portraitDisplayModesMap = new HashMap<String, DisplayMode[]>(numOfConnectedDisplays);
        int[] orientations = getDisplay.getDisplayOrientations();

        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            String displayId = displayIds[displayIndex];

            // The orientation array is aligned with the visible display IDs; default to landscape if it is ever shorter
            int orientation = displayIndex < orientations.length ? orientations[displayIndex] : 1;
            boolean landscapeOrientation = (orientation == 1 || orientation == 3);

            DisplayMode[] displayModes = getDisplay.getDisplayModes(displayId);
            Arrays.sort(displayModes, DISPLAY_MODE_COMPARATOR);
            DisplayMode[] invertedDisplayModes = DisplayModeInverter.invertDisplayModes(displayModes);

            landscapeDisplayModesMap.put(displayId, landscapeOrientation ? displayModes : invertedDisplayModes);
            portraitDisplayModesMap.put(displayId, landscapeOrientation ? invertedDisplayModes : displayModes);
        }
    }

    /**
     * Comparator for display mode sorting.
     */
    private static final Comparator<DisplayMode> DISPLAY_MODE_COMPARATOR = Comparator
            .comparingInt(DisplayMode::getWidth).thenComparingInt(DisplayMode::getHeight)
            .thenComparingInt(DisplayMode::getBitDepth).thenComparingInt(DisplayMode::getRefreshRate).reversed();

    /**
     * Gets the array of display IDs.
     *
     * @return The array of display IDs
     */
    public String[] getDisplayIds() {
        return displayIds;
    }

    /**
     * Gets the array of supported landscape display modes for the given display ID.
     *
     * @param displayId
     *            - The ID of the display to get the supported landscape display modes for
     *
     * @return The array of supported landscape display modes for the given display ID
     */
    public DisplayMode[] getLandscapeDisplayModes(String displayId) {
        return landscapeDisplayModesMap.get(displayId);
    }

    /**
     * Gets the array of supported portrait display modes for the given display ID.
     *
     * @param displayId
     *            - The ID of the display to get the supported portrait display modes for
     *
     * @return The array of supported portrait display modes for the given display ID
     */
    public DisplayMode[] getPortraitDisplayModes(String displayId) {
        return portraitDisplayModesMap.get(displayId);
    }

    /**
     * Gets the number of connected (visible) displays.
     *
     * @return The number of connected (visible) displays
     */
    public int getNumOfConnectedDisplays() {
        return numOfConnectedDisplays;
    }

    /**
     * Gets the array of supported DPI scale percentages for the given resolution. The supported set for a slot reflects
     * the DPI scale percentages Windows would offer for its selected resolution. The result is orientation-independent.
     *
     * @param width
     *            - The horizontal resolution to get the supported DPI scale percentages for
     * @param height
     *            - The vertical resolution to get the supported DPI scale percentages for
     *
     * @return The array of supported DPI scale percentages for the given resolution
     */
    public Integer[] getSupportedDpiScalePercentages(int width, int height) {
        // The supported set is a pure function of the resolution, so cache it to avoid repeated native queries
        long cacheKey = ((long) width << 32) | (height & 0xffffffffL);
        Integer[] cachedPercentages = supportedDpiScalePercentages.get(cacheKey);

        if (cachedPercentages != null) {
            return cachedPercentages.clone();
        }

        Integer[] supportedPercentages = getDisplay.getDpiScalePercentages(width, height);
        supportedDpiScalePercentages.put(cacheKey, supportedPercentages);

        return supportedPercentages.clone();
    }

}
