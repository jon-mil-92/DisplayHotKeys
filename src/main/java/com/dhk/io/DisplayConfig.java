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
 * Gets the current information for the connected displays.
 *
 * @author Jonathan R. Miller
 */
public class DisplayConfig {

    private String[] displayIds;
    private GetDisplay getDisplay;
    private Map<String, DisplayMode[]> landscapeDisplayModesMap;
    private Map<String, DisplayMode[]> portraitDisplayModesMap;
    private int numOfConnectedDisplays;

    /**
     * Constructor for the {@link DisplayConfig} class.
     */
    public DisplayConfig() {
        getDisplay = new GetDisplay();
        landscapeDisplayModesMap = new HashMap<String, DisplayMode[]>();
        portraitDisplayModesMap = new HashMap<String, DisplayMode[]>();
    }

    /**
     * Updates the current display configuration.
     */
    public void updateDisplayConfig() {
        updateDisplayIds();
        updateDisplayModes();
    }

    /**
     * Gets the current number of connected displays.
     */
    public void checkNumOfConnectedDisplays() {
        numOfConnectedDisplays = getDisplay.getNumOfConnectedDisplays();
    }

    /**
     * Updates the current array of unique display IDs and stores the number of connected displays.
     */
    public void updateDisplayIds() {
        displayIds = getDisplay.getDisplayIds();
        numOfConnectedDisplays = displayIds.length;
    }

    /**
     * Gets the current array of landscape and portrait display modes for each connected display.
     */
    private void updateDisplayModes() {
        int displayIndex = 0;
        landscapeDisplayModesMap = new HashMap<String, DisplayMode[]>();
        portraitDisplayModesMap = new HashMap<String, DisplayMode[]>();

        for (String displayId : displayIds) {
            int orientation = getDisplay.getDisplayOrientation(displayIndex);
            boolean landscapeOrientation = orientation == 1 || orientation == 3;
            DisplayMode[] displayModes = getDisplay.getDisplayModes(displayId);

            Arrays.sort(displayModes, Comparator.comparing(DisplayMode::getWidth).thenComparing(DisplayMode::getHeight)
                    .thenComparing(DisplayMode::getBitDepth).thenComparing(DisplayMode::getRefreshRate).reversed());

            DisplayMode[] landscapeDisplayModes = landscapeOrientation
                    ? displayModes
                    : DisplayModeInverter.invertDisplayModes(displayModes);
            DisplayMode[] portraitDisplayModes = landscapeOrientation
                    ? DisplayModeInverter.invertDisplayModes(displayModes)
                    : displayModes;

            landscapeDisplayModesMap.put(displayId, landscapeDisplayModes);
            portraitDisplayModesMap.put(displayId, portraitDisplayModes);

            displayIndex++;
        }
    }

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
     * Gets the number of connected displays.
     *
     * @return The number of connected displays
     */
    public int getNumOfConnectedDisplays() {
        return numOfConnectedDisplays;
    }

}
