package com.dhk.io;

import java.awt.DisplayMode;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class gets the current information for the connected displays.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DisplayConfig {
    private String[] displayIds;
    private EnumDisplayModes enumDisplayModes;
    private EnumDisplayIds enumDisplayIds;
    private DisplayMode[] displayModes;
    private ConcurrentHashMap<String, DisplayMode[]> displayModesMap;
    private int numOfConnectedDisplays;

    /**
     * Constructor for the DisplayConfig class.
     */
    public DisplayConfig() {
        // Initialize the object that will call the JNI fuction to enumerate all display IDs.
        enumDisplayIds = new EnumDisplayIds();

        // Initialize the object that will call the JNI fuction to enumerate all display modes for a given display.
        enumDisplayModes = new EnumDisplayModes();
    }

    /**
     * This method calls the methods that utilize JNI functions to get the current display configuration.
     */
    public void updateDisplayConfig() {
        // Get the current display configuration.
        updateDisplayIds();
        updateDisplayModes();
    }

    /**
     * This method gets the current number of connected displays.
     */
    public void checkNumOfConnectedDisplays() {
        numOfConnectedDisplays = enumDisplayIds.getNumOfConnectedDisplays();
    }

    /**
     * This method gets the current array of unique display IDs and stores the number of connected displays.
     */
    private void updateDisplayIds() {
        displayIds = enumDisplayIds.getDisplayIds();
        numOfConnectedDisplays = displayIds.length;
    }

    /**
     * This method gets the current array of display modes for each connected display and stores it in the display modes
     * map.
     */
    private void updateDisplayModes() {
        // Initialize the map of display IDs to array of supported display modes.
        displayModesMap = new ConcurrentHashMap<String, DisplayMode[]>();

        // For each connected display...
        for (String displayId : displayIds) {
            // Get the current array of supported display modes.
            displayModes = enumDisplayModes.getDisplayModes(displayId);

            // Add the array of supported display modes to the hash map.
            displayModesMap.put(displayId, displayModes);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the number of connected displays.
     * 
     * @return The number of connected displays.
     */
    public int getNumOfConnectedDisplays() {
        return numOfConnectedDisplays;
    }

    /**
     * Getter for the array of display IDs.
     * 
     * @return The array of display IDs.
     */
    public String[] getDisplayIds() {
        return displayIds;
    }

    /**
     * Getter for the array of supported display modes for the given display ID.
     * 
     * @param displayId - The ID of the display to get the supported display modes for.
     * @return The array of supported display modes for the given display ID.
     */
    public DisplayMode[] getDisplayModes(String displayId) {
        return displayModesMap.get(displayId);
    }
}
