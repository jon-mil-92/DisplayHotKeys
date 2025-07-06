package com.dhk.models;

import java.util.ArrayList;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;

/**
 * This class is the primary model of Display Hot Keys. Each slot in the application is initialized here.
 * 
 * @author Jonathan Miller
 * @version 1.3.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DhkModel {
    private DisplayConfig displayConfig;
    private String[] displayIds;
    private ArrayList<Display> displays;
    private int numOfConnectedDisplays;
    private int maxNumOfSlots;

    /**
     * This method initializes the slots for each display from the settings file.
     * 
     * @param settingsMgr - The manager of the application's settings file.
     */
    public void initModel(SettingsManager settingsMgr) {
        // Initialize the object that will get the current display configuration.
        displayConfig = settingsMgr.getDisplayConfig();

        // Initialize the maximum number of slots that can be displayed in the application.
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();

        // Get an array of IDs for all connected displays.
        displayIds = settingsMgr.getDisplayIds();

        // Initialize the array list of displays.
        displays = new ArrayList<Display>(maxNumOfSlots);

        // Store the number of connected displays.
        numOfConnectedDisplays = displayIds.length;

        // For each connected display...
        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // Initialize an array list of slots for the current display.
            ArrayList<Slot> slots = new ArrayList<Slot>(maxNumOfSlots);

            // Add new slots to the array list of slots.
            for (int slotId = 1; slotId <= maxNumOfSlots; slotId++) {
                slots.add(new Slot(settingsMgr.getIniSlotDisplayMode(displayId, slotId),
                        settingsMgr.getIniSlotScalingMode(displayId, slotId),
                        settingsMgr.getIniSlotDpiScalePercentage(displayId, slotId), false,
                        settingsMgr.getIniSlotHotKey(displayId, slotId)));
            }

            // Add a display to the array list of displays.
            displays.add(new Display(displayId, settingsMgr.getIniNumOfSlotsForDisplay(displayId), slots));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the object containing the current display configuration.
     * 
     * @return The object containing the current display configuration.
     */
    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    /**
     * Getter for the array of display IDs for the connected displays.
     * 
     * @return The array of display IDs for the connected displays.
     */
    public String[] getDisplayIds() {
        return displayIds;
    }

    /**
     * Getter for the active number of connected displays.
     * 
     * @return The active number of connected displays.
     */
    public int getNumOfConnectedDisplays() {
        return numOfConnectedDisplays;
    }

    /**
     * Getter for the active number of slots for the given display.
     * 
     * @param displayIndex - The index of the display to get the active number of slots for.
     * @return The active number of slots for the given display.
     */
    public int getNumOfSlotsForDisplay(int displayIndex) {
        return displays.get(displayIndex).getNumOfActiveSlots();
    }

    /**
     * Setter for the active number of slots for the given display.
     * 
     * @param displayIndex  - The index of the display to set the active number of slots for.
     * @param numberOfSlots - The new active number of slots for the given display.
     */
    public void setNumOfSlotsForDisplay(int displayIndex, int numberOfSlots) {
        displays.get(displayIndex).setNumOfActiveSlots(numberOfSlots);
    }

    /**
     * Getter for the specified slot for the given display.
     * 
     * @param displayIndex - The index of the display to get the slot for.
     * @param slotIndex    - The index of the slot to get.
     * @return The specified slot for the given display.
     */
    public Slot getSlot(int displayIndex, int slotIndex) {
        return displays.get(displayIndex).getSlot(slotIndex);
    }

    /**
     * Getter for the max number of slots.
     * 
     * @return The max number of slots.
     */
    public int getMaxNumOfSlots() {
        return maxNumOfSlots;
    }
}
