package com.dhk.model;

import java.awt.DisplayMode;
import java.util.ArrayList;
import java.util.List;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;

/**
 * Defines the primary model of Display Hot Keys. Each slot in the application is initialized here.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DhkModel {

    private DisplayConfig displayConfig;
    private String[] displayIds;
    private List<Display> displays;
    private int numOfConnectedDisplays;
    private int maxNumOfSlots;
    private boolean darkMode;
    private boolean minimizeToTray;
    private boolean runOnStartup;

    /**
     * Default constructor for the DhkModel class.
     */
    public DhkModel() {
    }

    /**
     * Initializes the slots for each display from the settings file.
     * 
     * @param settingsMgr
     *            - The manager of the application's settings file
     */
    public void initModel(SettingsManager settingsMgr) {
        darkMode = settingsMgr.getIniDarkMode();
        minimizeToTray = settingsMgr.getIniMinimizeToTray();
        runOnStartup = settingsMgr.getIniRunOnStartup();
        displayConfig = settingsMgr.getDisplayConfig();
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();
        displayIds = settingsMgr.getDisplayIds();
        displays = new ArrayList<Display>(maxNumOfSlots);
        numOfConnectedDisplays = displayIds.length;

        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            String displayId = displayIds[displayIndex];
            List<Slot> slots = new ArrayList<Slot>(maxNumOfSlots);

            for (int slotId = 1; slotId <= maxNumOfSlots; slotId++) {
                DisplayMode displayMode = settingsMgr.getIniSlotDisplayMode(displayId, slotId);
                int scalingMode = settingsMgr.getIniSlotScalingMode(displayId, slotId);
                int dpiScalePercentage = settingsMgr.getIniSlotDpiScalePercentage(displayId, slotId);
                int orientationMode = settingsMgr.getIniSlotOrientationMode(displayId, slotId);
                HotKey hotKey = settingsMgr.getIniSlotHotKey(displayId, slotId);

                slots.add(new Slot(displayMode, scalingMode, dpiScalePercentage, orientationMode, false, hotKey));
            }

            int numOfSlots = settingsMgr.getIniNumOfSlotsForDisplay(displayId);

            displays.add(new Display(displayId, numOfSlots, slots));
        }
    }

    /**
     * Gets the object containing the current display configuration.
     * 
     * @return The object containing the current display configuration
     */
    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    /**
     * Gets the array of display IDs for the connected displays.
     * 
     * @return The array of display IDs for the connected displays
     */
    public String[] getDisplayIds() {
        return displayIds;
    }

    /**
     * Gets the active number of connected displays.
     * 
     * @return The active number of connected displays
     */
    public int getNumOfConnectedDisplays() {
        return numOfConnectedDisplays;
    }

    /**
     * Gets the active number of slots for the given display.
     * 
     * @param displayIndex
     *            - The index of the display to get the active number of slots for
     *
     * @return The active number of slots for the given display
     */
    public int getNumOfSlotsForDisplay(int displayIndex) {
        return displays.get(displayIndex).getNumOfActiveSlots();
    }

    /**
     * Sets the active number of slots for the given display.
     * 
     * @param displayIndex
     *            - The index of the display to set the active number of slots for
     * @param numberOfSlots
     *            - The new active number of slots for the given display
     */
    public void setNumOfSlotsForDisplay(int displayIndex, int numberOfSlots) {
        displays.get(displayIndex).setNumOfActiveSlots(numberOfSlots);
    }

    /**
     * Gets the specified slot for the given display.
     * 
     * @param displayIndex
     *            - The index of the display to get the slot for
     * @param slotIndex
     *            - The index of the slot to get
     *
     * @return The specified slot for the given display
     */
    public Slot getSlot(int displayIndex, int slotIndex) {
        return displays.get(displayIndex).getSlot(slotIndex);
    }

    /**
     * Gets the max number of slots.
     * 
     * @return The max number of slots
     */
    public int getMaxNumOfSlots() {
        return maxNumOfSlots;
    }

    /**
     * Gets the current "dark mode" state of the UI.
     * 
     * @return The current "dark mode" state of the UI
     */
    public boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Toggles the "dark mode" state of the UI.
     */
    public void toggleDarkMode() {
        darkMode = !darkMode;
    }

    /**
     * Toggles the "minimize to tray" state.
     */
    public void toggleMinimizeToTray() {
        minimizeToTray = !minimizeToTray;
    }

    /**
     * Gets the "minimize to tray" state.
     * 
     * @return The "minimize to tray" state
     */
    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }

    /**
     * Toggles the "run on startup" state.
     */
    public void toggleRunOnStartup() {
        runOnStartup = !runOnStartup;
    }

    /**
     * Gets the "run on startup" state.
     * 
     * @return The "run on startup" state
     */
    public boolean isRunOnStartup() {
        return runOnStartup;
    }

}
