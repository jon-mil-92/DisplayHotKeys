package com.dhk.io;

import java.awt.DisplayMode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.ini4j.Wini;
import com.dhk.model.HotKey;
import com.dhk.model.Key;

/**
 * Saves the application settings to an ini file. It enables the saving of the active number of slots, the orientation
 * mode, the theme state, the minimize to tray state, the run on startup state, the display modes, the scaling modes,
 * the DPI scale percentages, and the hot keys for each display
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class SettingsManager {

    private Wini ini;
    private DisplayConfig displayConfig;
    private int numOfConnectedDisplays;
    private String[] displayIds;
    private Map<String, DisplayMode[]> displayModesMap;
    private File settingsFile;

    // The max number of visible slots in the application frame
    private final int MAX_NUM_OF_SLOTS = 12;

    /**
     * Default constructor for the SettingsManager class.
     */
    public SettingsManager() {
    }

    /**
     * Initializes the display modes map and settings file for the settings manager.
     */
    public void initSettingsManager() {
        initDisplays();
        initSettingsFile();
    }

    /**
     * Gets the dark mode property value from the settings file object.
     * 
     * @return The value for the dark mode property
     */
    public boolean getIniDarkMode() {
        return ini.get("Application", "darkMode", boolean.class);
    }

    /**
     * Sets the dark mode property value in the settings file object.
     * 
     * @param darkMode
     *            - The new value for the dark mode property
     */
    public void saveIniDarkMode(boolean darkMode) {
        ini.put("Application", "darkMode", darkMode);

        updateSettingsFile();
    }

    /**
     * Gets the minimize to tray property value from the settings file object.
     * 
     * @return The value for the minimize to tray property
     */
    public boolean getIniMinimizeToTray() {
        return ini.get("Application", "minimizeToTray", boolean.class);
    }

    /**
     * Sets the minimize to tray property value in the settings file object.
     * 
     * @param minimizeToTray
     *            - The new value for the minimize to tray property
     */
    public void saveIniMinimizeToTray(boolean minimizeToTray) {
        ini.put("Application", "minimizeToTray", minimizeToTray);

        updateSettingsFile();
    }

    /**
     * Gets the run on startup property value from the settings file object.
     * 
     * @return The value for the run on startup property
     */
    public boolean getIniRunOnStartup() {
        return ini.get("Application", "runOnStartup", boolean.class);
    }

    /**
     * Sets the run on startup property value in the settings file object.
     * 
     * @param runOnStartup
     *            - The new value for the run on startup property
     */
    public void saveIniRunOnStartup(boolean runOnStartup) {
        ini.put("Application", "runOnStartup", runOnStartup);

        updateSettingsFile();
    }

    /**
     * Gets the number of slots property value for the given display from the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to get the number of slots for
     * 
     * @return The value for the number of slots property
     */
    public int getIniNumOfSlotsForDisplay(String displayId) {
        return ini.get("Application", "numOfSlotsFor--" + displayId, int.class);
    }

    /**
     * Sets the number of slots property value for the given display in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to set the number of slots for
     * @param numOfSlots
     *            - The new value for the number of slots property
     */
    public void saveIniNumOfSlotsForDisplay(String displayId, int numOfSlots) {
        ini.put("Application", "numOfSlotsFor--" + displayId, numOfSlots);

        updateSettingsFile();
    }

    /**
     * Gets the orientation mode property value for the given display from the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to get the orientation mode for
     * 
     * @return The value for the orientation mode property
     */
    public int getIniOrientationModeForDisplay(String displayId) {
        return ini.get("Application", "orientationModeFor--" + displayId, int.class);
    }

    /**
     * Sets the orientation mode property value for the given display in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to set the orientation mode for
     * @param orientationMode
     *            - The new value for the orientation mode property
     */
    public void saveIniOrientationModeForDisplay(String displayId, int orientationMode) {
        ini.put("Application", "orientationModeFor--" + displayId, orientationMode);

        updateSettingsFile();
    }

    /**
     * Gets the specified slot's display mode built from the display mode properties in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to get the display mode for
     * @param slotId
     *            - The ID of the slot to get the display mode for
     * 
     * @return The display mode from the display property values for the specified slot ID
     */
    public DisplayMode getIniSlotDisplayMode(String displayId, int slotId) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        DisplayMode slotDisplayMode = new DisplayMode(ini.get(iniSection, "displayModeWidth", int.class),
                ini.get(iniSection, "displayModeHeight", int.class),
                ini.get(iniSection, "displayModeBitDepth", int.class),
                ini.get(iniSection, "displayModeRefreshRate", int.class));

        return slotDisplayMode;
    }

    /**
     * Sets the specified slot's display mode properties in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to set the display mode for
     * @param slotId
     *            - The ID of the slot to set the display mode for
     * @param width
     *            - The new display mode width for the specified slot
     * @param height
     *            - The new display mode height for the specified slot
     * @param bitDepth
     *            - The new display mode bit depth for the specified slot
     * @param refreshRate
     *            - The new display mode refresh rate for the specified slot
     */
    public void saveIniSlotDisplayMode(String displayId, int slotId, int width, int height, int bitDepth,
            int refreshRate) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        ini.put(iniSection, "displayModeWidth", width);
        ini.put(iniSection, "displayModeHeight", height);
        ini.put(iniSection, "displayModeBitDepth", bitDepth);
        ini.put(iniSection, "displayModeRefreshRate", refreshRate);

        updateSettingsFile();
    }

    /**
     * Gets the specified slot's scaling mode property value from the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to get the scaling mode for
     * @param slotId
     *            - The ID of the slot to get the scaling mode for
     * 
     * @return The specified slot's scaling mode property value
     */
    public int getIniSlotScalingMode(String displayId, int slotId) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        return ini.get(iniSection, "scalingMode", int.class);
    }

    /**
     * Sets the specified slot's scaling mode property value in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to set the scaling mode for
     * @param slotId
     *            - The ID of the slot to set the scaling mode for
     * @param scalingMode
     *            - The specified slot's new value for the scaling mode property
     */
    public void saveIniSlotScalingMode(String displayId, int slotId, int scalingMode) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);
        ini.put(iniSection, "scalingMode", scalingMode);

        updateSettingsFile();
    }

    /**
     * Gets the specified slot's DPI scale percentage property value from the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to get the DPI scale percentage for
     * @param slotId
     *            - The ID of the slot to get the DPI scale percentage for
     * 
     * @return The specified slot's DPI scale percentage property value
     */
    public int getIniSlotDpiScalePercentage(String displayId, int slotId) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        return ini.get(iniSection, "dpiScalePercentage", int.class);
    }

    /**
     * Sets the specified slot's DPI scale percentage property value in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to set the DPI scale percentage for
     * @param slotId
     *            - The ID of the slot to set the DPI scale percentage for
     * @param dpiScalePercentage
     *            - The specified slot's new value for the DPI scale percentage property
     */
    public void saveIniSlotDpiScalePercentage(String displayId, int slotId, int dpiScalePercentage) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);
        ini.put(iniSection, "dpiScalePercentage", dpiScalePercentage);

        updateSettingsFile();
    }

    /**
     * Gets the specified slot's hot key built from the hot key properties in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to get the hot key for
     * @param slotId
     *            - The ID of the slot to get the hot key for
     * 
     * @return The specified slot's hot key
     */
    public HotKey getIniSlotHotKey(String displayId, int slotId) {
        HotKey hotKey = new HotKey(new ArrayList<Key>());
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);
        int hotKeySize = ini.get(iniSection, "hotKeySize", int.class);

        for (int keyId = 1; keyId <= hotKeySize; keyId++) {
            int keyCode = ini.get(iniSection, "key" + Integer.toString(keyId), int.class);
            hotKey.getKeys().add(new Key(keyCode, KeyText.getKeyCodeText(keyCode), false));
        }

        return hotKey;
    }

    /**
     * Sets the specified slot's hot key properties in the settings file object.
     * 
     * @param displayId
     *            - The ID of the display to set the hot key for
     * @param slotId
     *            - The ID of the slot to set the hot key for
     * @param hotKey
     *            - The specified slot's hot key
     */
    public void saveIniSlotHotKey(String displayId, int slotId, HotKey hotKey) {
        int hotKeySize = hotKey.getKeys().size();
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);
        ini.put(iniSection, "hotKeySize", hotKeySize);

        for (int keyIndex = 0; keyIndex < 3; keyIndex++) {
            // Only update key codes for the active keys in the hot key
            if (keyIndex < hotKeySize) {
                int keyCode = hotKey.getKeys().get(keyIndex).getKey();
                ini.put(iniSection, "key" + Integer.toString(keyIndex + 1), keyCode);
            } else {
                ini.put(iniSection, "key" + Integer.toString(keyIndex + 1), 0);
            }
        }

        updateSettingsFile();
    }

    /**
     * Gets the ini file that holds the application settings.
     * 
     * @return The settings ini file.
     */
    public Wini getIni() {
        return ini;
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
     * Gets the number of actively connected display devices.
     * 
     * @return The number of actively connected display devices
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
     * Gets the map of display IDs to supported display modes array.
     * 
     * @return The map of display IDs to supported display modes array
     */
    public Map<String, DisplayMode[]> getDisplayModesMap() {
        return displayModesMap;
    }

    /**
     * Gets the max number of slots.
     * 
     * @return The max number of slots
     */
    public int getMaxNumOfSlots() {
        return MAX_NUM_OF_SLOTS;
    }

    /**
     * Initializes the display modes map.
     */
    private void initDisplays() {
        displayConfig = new DisplayConfig();
        displayConfig.updateDisplayConfig();
        numOfConnectedDisplays = displayConfig.getNumOfConnectedDisplays();
        displayIds = displayConfig.getDisplayIds();
        displayModesMap = new HashMap<String, DisplayMode[]>();

        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            displayModesMap.put(displayIds[displayIndex], displayConfig.getDisplayModes(displayIds[displayIndex]));
        }
    }

    /**
     * Initializes the settings file.
     */
    private void initSettingsFile() {
        checkSettingsFileVersion();
        String settingsPath = System.getProperty("user.home") + "\\Documents\\DisplayHotKeys\\";
        settingsPath = settingsPath + "settings.ini";

        try {
            settingsFile = new File(settingsPath);
            settingsFile.getParentFile().mkdirs();
            settingsFile.createNewFile();

            ini = new Wini(settingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SettingsValidator validator = new SettingsValidator(this);
        validator.validateAllProperties();
    }

    /**
     * Deletes the settings file if it is from a previous application version so it can be rebuilt for the new
     * application version.
     */
    private void checkSettingsFileVersion() {
        boolean oldSettingsFileVersion = false;
        String settingsPath = System.getProperty("user.home") + "\\Documents\\DisplayHotKeys\\";
        settingsPath = settingsPath + "settings.ini";
        File settingsFile = new File(settingsPath);

        // If the settings file has already been created from previously starting the application
        if (settingsFile.exists()) {
            BufferedReader settingsFileReader;

            try {
                settingsFileReader = new BufferedReader(new FileReader(settingsFile));
                String line;

                while ((line = settingsFileReader.readLine()) != null) {
                    if (line.equals("[Slot1]")) {
                        oldSettingsFileVersion = true;
                    }
                }
                settingsFileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (oldSettingsFileVersion) {
                try {
                    Files.deleteIfExists(settingsFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Wraps the Wini store call in a try/catch block.
     */
    private void updateSettingsFile() {
        try {
            ini.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
