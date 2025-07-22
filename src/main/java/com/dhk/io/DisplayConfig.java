package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
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
    private EnumDisplayModes enumDisplayModes;
    private EnumDisplayIds enumDisplayIds;
    private DisplayMode[] displayModes;
    private Map<String, DisplayMode[]> displayModesMap;
    private int numOfConnectedDisplays;

    /**
     * Constructor for the DisplayConfig class.
     */
    public DisplayConfig() {
        enumDisplayIds = new EnumDisplayIds();
        enumDisplayModes = new EnumDisplayModes();
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
        numOfConnectedDisplays = enumDisplayIds.getNumOfConnectedDisplays();
    }

    /**
     * Updates the current array of unique display IDs and stores the number of connected displays.
     */
    public void updateDisplayIds() {
        displayIds = enumDisplayIds.getDisplayIds();
        numOfConnectedDisplays = displayIds.length;
    }

    /**
     * Gets the current array of display modes for each connected display and stores it in the display modes map.
     */
    private void updateDisplayModes() {
        displayModesMap = new HashMap<String, DisplayMode[]>();

        for (String displayId : displayIds) {
            displayModes = enumDisplayModes.getDisplayModes(displayId);

            Arrays.sort(displayModes, Comparator.comparing(DisplayMode::getWidth).thenComparing(DisplayMode::getHeight)
                    .thenComparing(DisplayMode::getBitDepth).thenComparing(DisplayMode::getRefreshRate).reversed());

            displayModesMap.put(displayId, displayModes);
        }
    }

    /**
     * Gets the number of connected displays.
     * 
     * @return The number of connected displays
     */
    public int getNumOfConnectedDisplays() {
        return numOfConnectedDisplays;
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
     * Gets the array of supported display modes for the given display ID.
     * 
     * @param displayId
     *            - The ID of the display to get the supported display modes for
     * 
     * @return The array of supported display modes for the given display ID
     */
    public DisplayMode[] getDisplayModes(String displayId) {
        return displayModesMap.get(displayId);
    }

}
