package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import com.dhk.utility.DisplayModeInverter;
import java.util.HashMap;

/**
 * Gets the current information for the connected displays.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DisplayConfig {

    private String[] displayIds;
    private GetDisplay getDisplay;
    private Map<String, DisplayMode[]> landscapeDisplayModesMap;
    private Map<String, DisplayMode[]> portraitDisplayModesMap;
    private int numOfConnectedDisplays;

    /**
     * Constructor for the DisplayConfig class.
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
