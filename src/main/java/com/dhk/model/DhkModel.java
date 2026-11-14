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
package com.dhk.model;

import java.util.ArrayList;
import java.util.List;

import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;

/**
 * Defines the primary model of Display Hot Keys. Each slot in the application is initialized here.
 *
 * @author Jonathan R. Miller
 */
public class DhkModel {

    private DisplayConfig displayConfig;
    private String[] displayIds;
    private int[] displayNumbers;
    private List<Display> displays;
    private int numOfConnectedDisplays;
    private int maxNumOfSlots;
    private boolean darkMode;
    private boolean minimizeToTray;
    private boolean closeToTray;
    private boolean runOnStartup;
    private HotKey displayInfoHotKey;
    private boolean autoShowInfoOverlay;

    /**
     * Default constructor for the {@link DhkModel} class.
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
        closeToTray = settingsMgr.getIniCloseToTray();
        runOnStartup = settingsMgr.getIniRunOnStartup();
        displayInfoHotKey = settingsMgr.getIniDisplayInfoHotKey();
        autoShowInfoOverlay = settingsMgr.getIniAutoShowInfoOverlay();
        displayConfig = settingsMgr.getDisplayConfig();
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();
        displayIds = settingsMgr.getDisplayIds();
        displayNumbers = settingsMgr.getDisplayNumbers();
        displays = new ArrayList<Display>(maxNumOfSlots);
        numOfConnectedDisplays = displayIds.length;

        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            String displayId = displayIds[displayIndex];
            List<Slot> slots = new ArrayList<Slot>(maxNumOfSlots);

            for (int slotId = 1; slotId <= maxNumOfSlots; slotId++) {
                DisplayMode displayMode = settingsMgr.getIniSlotDisplayMode(displayId, slotId);
                int dpiScalePercentage = settingsMgr.getIniSlotDpiScalePercentage(displayId, slotId);
                int scalingMode = settingsMgr.getIniSlotScalingMode(displayId, slotId);
                int orientationMode = settingsMgr.getIniSlotOrientationMode(displayId, slotId);
                HotKey hotKey = settingsMgr.getIniSlotHotKey(displayId, slotId);

                slots.add(new Slot(displayMode, dpiScalePercentage, scalingMode, orientationMode, false, hotKey));
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
     * Gets the Windows Display Settings number of each connected display, aligned index-for-index with getDisplayIds.
     * The numbers can skip values, matching the gaps Windows leaves for disconnected displays.
     *
     * @return The Windows display number of each connected display, in getDisplayIds order
     */
    public int[] getDisplayNumbers() {
        return displayNumbers;
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
     * Sets the "minimize to tray" state.
     *
     * @param minimizeToTray
     *            - The "minimize to tray" state to set
     */
    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
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
     * Gets the "close to tray" state, which hides the application in the system tray instead of exiting when its window
     * is closed. It only applies while the "minimize to tray" state is on.
     *
     * @return The "close to tray" state
     */
    public boolean isCloseToTray() {
        return closeToTray;
    }

    /**
     * Sets the "close to tray" state.
     *
     * @param closeToTray
     *            - The "close to tray" state to set
     */
    public void setCloseToTray(boolean closeToTray) {
        this.closeToTray = closeToTray;
    }

    /**
     * Sets the "run on startup" state.
     *
     * @param runOnStartup
     *            - The "run on startup" state to set
     */
    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    /**
     * Gets the "run on startup" state.
     *
     * @return The "run on startup" state
     */
    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    /**
     * Gets the global hot key that shows the applied settings of every connected display.
     *
     * @return The Display Info hot key
     */
    public HotKey getDisplayInfoHotKey() {
        return displayInfoHotKey;
    }

    /**
     * Sets the global hot key that shows the applied settings of every connected display.
     *
     * @param displayInfoHotKey
     *            - The new Display Info hot key
     */
    public void setDisplayInfoHotKey(HotKey displayInfoHotKey) {
        this.displayInfoHotKey = displayInfoHotKey;
    }

    /**
     * Gets the "auto show info overlay" state, which shows the applied settings of every connected display whenever a
     * slot is applied or the display configuration changes.
     *
     * @return The "auto show info overlay" state
     */
    public boolean isAutoShowInfoOverlay() {
        return autoShowInfoOverlay;
    }

    /**
     * Sets the "auto show info overlay" state.
     *
     * @param autoShowInfoOverlay
     *            - The "auto show info overlay" state to set
     */
    public void setAutoShowInfoOverlay(boolean autoShowInfoOverlay) {
        this.autoShowInfoOverlay = autoShowInfoOverlay;
    }

}
