package com.dhk.io;

import java.awt.DisplayMode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.ini4j.Wini;
import com.dhk.models.HotKey;
import com.dhk.models.Key;

/**
 * This class saves the application settings in an ini file. It enables the saving of the active number of slots, the
 * theme state, the run on startup state, the display modes, the scaling modes, the DPI scale percentages, and the hot
 * keys.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class SettingsManager {
    private Wini ini;
    private DisplayConfig displayConfig;
    private int numOfConnectedDisplays;
    private String[] displayIds;
    private ConcurrentHashMap<String, DisplayMode[]> displayModesMap;
    private KeyText keyText;
    private File settingsFile;

    // The max number of visible slots in the application frame.
    private final int MAX_NUM_OF_SLOTS = 12;

    /**
     * This method initializes the key text object, display modes map, and settings file for the settings manager.
     */
    public void initSettingsManager() {
        // Initialize the key text object to get the correct text representation of a key given a key code.
        keyText = new KeyText();

        // Initialize the display modes map and settings file for the settings manager.
        initDisplays();
        initSettingsFile();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Settings File Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the number of slots property value for the given display from the settings file object.
     * 
     * @param displayId - The ID of the display to get the number of slots for.
     * @return The value for the number of slots property.
     */
    public int getIniNumOfSlotsForDisplay(String displayId) {
        // Get the number of slots property value from the settings file object.
        return ini.get("Application", "numOfSlotsFor--" + displayId, int.class);
    }

    /**
     * Setter for the number of slots property value for the given display in the settings file object.
     * 
     * @param displayId  - The ID of the display to set the number of slots for.
     * @param numOfSlots - The new value for the number of slots property.
     */
    public void saveIniNumOfSlotsForDisplay(String displayId, int numOfSlots) {
        // Write the new number of slots property value to the settings file object.
        ini.put("Application", "numOfSlotsFor--" + displayId, numOfSlots);

        updateSettingsFile();
    }

    /**
     * Getter for the dark mode property value from the settings file object.
     * 
     * @return The value for the dark mode property.
     */
    public boolean getIniDarkMode() {
        // Get the dark mode property value from the settings file object.
        return ini.get("Application", "darkMode", boolean.class);
    }

    /**
     * Setter for the dark mode property value in the settings file object.
     * 
     * @param darkMode - The new value for the dark mode property.
     */
    public void saveIniDarkMode(boolean darkMode) {
        // Write the new dark mode property value to the settings file object.
        ini.put("Application", "darkMode", darkMode);

        updateSettingsFile();
    }

    /**
     * Getter for the run on startup property value from the settings file object.
     * 
     * @return The value for the run on startup property.
     */
    public boolean getIniRunOnStartup() {
        // Get the run on startup property value from the settings file object.
        return ini.get("Application", "runOnStartup", boolean.class);
    }

    /**
     * Setter for the run on startup property value in the settings file object.
     * 
     * @param runOnStartup - The new value for the run on startup property.
     */
    public void saveIniRunOnStartup(boolean runOnStartup) {
        // Write the new run on startup property value to the settings file object.
        ini.put("Application", "runOnStartup", runOnStartup);

        updateSettingsFile();
    }

    /**
     * Getter for the specified slot's display mode built from the display mode properties in the settings file object.
     * 
     * @param displayId - The ID of the display to get the display mode for.
     * @param slotId    - The ID of the slot to get the display mode for.
     * @return The display mode from the display property values for the specified slot ID.
     */
    public DisplayMode getIniSlotDisplayMode(String displayId, int slotId) {
        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Build the display mode from the display mode properties for the specified slot in the settings file object.
        DisplayMode slotDisplayMode = new DisplayMode(ini.get(iniSection, "displayModeWidth", int.class),
                ini.get(iniSection, "displayModeHeight", int.class),
                ini.get(iniSection, "displayModeBitDepth", int.class),
                ini.get(iniSection, "displayModeRefreshRate", int.class));

        return slotDisplayMode;
    }

    /**
     * Setter for the specified slot's display mode properties in the settings file object.
     * 
     * @param displayId   - The ID of the display to set the display mode for.
     * @param slotId      - The ID of the slot to set the display mode for.
     * @param width       - The new display mode width for the specified slot.
     * @param height      - The new display mode height for the specified slot.
     * @param bitDepth    - The new display mode bit depth for the specified slot.
     * @param refreshRate - The new display mode refresh rate for the specified slot.
     */
    public void saveIniSlotDisplayMode(String displayId, int slotId, int width, int height, int bitDepth,
            int refreshRate) {
        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Write the new display mode property values for the specified slot to the settings file object.
        ini.put(iniSection, "displayModeWidth", width);
        ini.put(iniSection, "displayModeHeight", height);
        ini.put(iniSection, "displayModeBitDepth", bitDepth);
        ini.put(iniSection, "displayModeRefreshRate", refreshRate);

        updateSettingsFile();
    }

    /**
     * Getter for the specified slot's scaling mode property value from the settings file object.
     * 
     * @param displayId - The ID of the display to get the scaling mode for.
     * @param slotId    - The ID of the slot to get the scaling mode for.
     * @return The specified slot's scaling mode property value.
     */
    public int getIniSlotScalingMode(String displayId, int slotId) {
        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Get the scaling mode property value from the settings file object.
        return ini.get(iniSection, "scalingMode", int.class);
    }

    /**
     * Setter for the specified slot's scaling mode property value in the settings file object.
     * 
     * @param displayId   - The ID of the display to set the scaling mode for.
     * @param slotId      - The ID of the slot to set the scaling mode for.
     * @param scalingMode - The specified slot's new value for the scaling mode property.
     */
    public void saveIniSlotScalingMode(String displayId, int slotId, int scalingMode) {
        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Write the new scaling mode property value for the specified slot to the settings file.
        ini.put(iniSection, "scalingMode", scalingMode);

        updateSettingsFile();
    }

    /**
     * Getter for the specified slot's DPI scale percentage property value from the settings file object.
     * 
     * @param displayId - The ID of the display to get the DPI scale percentage for.
     * @param slotId    - The ID of the slot to get the DPI scale percentage for.
     * @return The specified slot's DPI scale percentage property value.
     */
    public int getIniSlotDpiScalePercentage(String displayId, int slotId) {
        // Set the ini section string for the current display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Get the DPI scale percentage property value from the settings file object.
        return ini.get(iniSection, "dpiScalePercentage", int.class);
    }

    /**
     * Setter for the specified slot's DPI scale percentage property value in the settings file object.
     * 
     * @param displayId          - The ID of the display to set the DPI scale percentage for.
     * @param slotId             - The ID of the slot to set the DPI scale percentage for.
     * @param dpiScalePercentage - The specified slot's new value for the DPI scale percentage property.
     */
    public void saveIniSlotDpiScalePercentage(String displayId, int slotId, int dpiScalePercentage) {
        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Write the new DPI scale percentage property value for the specified slot to the settings file.
        ini.put(iniSection, "dpiScalePercentage", dpiScalePercentage);

        updateSettingsFile();
    }

    /**
     * Getter for the specified slot's hot key built from the hot key properties in the settings file object.
     * 
     * @param displayId - The ID of the display to get the hot key for.
     * @param slotId    - The ID of the slot to get the hot key for.
     * @return The specified slot's hot key.
     */
    public HotKey getIniSlotHotKey(String displayId, int slotId) {
        // Create a new hot key to store the retrieved key property values.
        HotKey hotKey = new HotKey(new ArrayList<Key>());

        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Get the hot key size property value from the settings file object.
        int hotKeySize = ini.get(iniSection, "hotKeySize", int.class);

        // For each stored key that makes up the hot key...
        for (int keyId = 1; keyId <= hotKeySize; keyId++) {
            // Get the current key property value from the settings file object.
            int keyCode = ini.get(iniSection, "key" + Integer.toString(keyId), int.class);

            // Build the hot key array list of keys by retrieving each key property value from the settings file.
            hotKey.getKeys().add(new Key(keyCode, keyText.getKeyCodeText(keyCode), false));
        }

        return hotKey;
    }

    /**
     * Setter for the specified slot's hot key properties in the settings file object.
     * 
     * @param displayId - The ID of the display to set the hot key for.
     * @param slotId    - The ID of the slot to set the hot key for.
     * @param hotKey    - The specified slot's hot key.
     */
    public void saveIniSlotHotKey(String displayId, int slotId, HotKey hotKey) {
        // Get the new hot key size.
        int hotKeySize = hotKey.getKeys().size();

        // Set the ini section string for the given display ID and slot ID.
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        // Store the updated hot key size for the specified slot.
        ini.put(iniSection, "hotKeySize", hotKeySize);

        // For each key in the hot key...
        for (int keyIndex = 0; keyIndex < 3; keyIndex++) {
            // Only update key codes for the active keys in the hot key.
            if (keyIndex < hotKeySize) {
                // Get the key code for the current key in the hot key.
                int keyCode = hotKey.getKeys().get(keyIndex).getKey();

                // Write the retrieved key code value into the settings file object for the current key.
                ini.put(iniSection, "key" + Integer.toString(keyIndex + 1), keyCode);
            }
            // Otherwise, reset the key code for the unused keys.
            else {
                ini.put(iniSection, "key" + Integer.toString(keyIndex + 1), 0);
            }
        }

        updateSettingsFile();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the ini file that holds the application settings.
     * 
     * @return The settings ini file.
     */
    public Wini getIni() {
        return ini;
    }

    /**
     * Getter for the object containing the current display configuration.
     * 
     * @return The object containing the current display configuration.
     */
    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    /**
     * Getter for the number of actively connected display devices.
     * 
     * @return The number of actively connected display devices.
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
     * Getter for the map of display IDs to supported display modes array.
     * 
     * @return The map of display IDs to supported display modes array.
     */
    public ConcurrentHashMap<String, DisplayMode[]> getDisplayModesMap() {
        return displayModesMap;
    }

    /**
     * Getter for the max number of slots.
     * 
     * @return The max number of slots.
     */
    public int getMaxNumOfSlots() {
        return MAX_NUM_OF_SLOTS;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Private Methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method initializes the display modes map.
     */
    private void initDisplays() {
        // Get the current display configuration.
        displayConfig = new DisplayConfig();
        displayConfig.updateDisplayConfig();

        // Store the number of actively connected display devices.
        numOfConnectedDisplays = displayConfig.getNumOfConnectedDisplays();

        // Get the array of unique display IDs.
        displayIds = displayConfig.getDisplayIds();

        // Initialize the map of display IDs to supported display modes array.
        displayModesMap = new ConcurrentHashMap<String, DisplayMode[]>();

        // For each connected display...
        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            // Add the array of supported display modes for the current display index to the hash map.
            displayModesMap.put(displayIds[displayIndex], displayConfig.getDisplayModes(displayIds[displayIndex]));
        }
    }

    /**
     * This method initializes the settings file.
     */
    private void initSettingsFile() {
        // Delete the settings file if it is from a previous version of the application so it can be reformatted.
        checkSettingsFileVersion();

        // Get the path for the settings folder that will hold the settings file.
        String settingsPath = System.getProperty("user.home") + "\\Documents\\DisplayHotKeys\\";

        // Set the full path to the settings file.
        settingsPath = settingsPath + "settings.ini";

        // Try to create an ini object from the settings file.
        try {
            // Create the application's settings file in the user's Documents folder.
            settingsFile = new File(settingsPath);
            settingsFile.getParentFile().mkdirs();
            settingsFile.createNewFile();

            // Create the new settings file object.
            ini = new Wini(settingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize the settings validator object.
        SettingsValidator validator = new SettingsValidator(this);

        // Validate each property value in the settings file.
        validator.validateAllProperties();
    }

    /**
     * This method deletes the settings file if it is from a previous application version so it can be rebuilt for the
     * new application version.
     */
    private void checkSettingsFileVersion() {
        // Initialize a variable to determine if the settings file is from a previous version of the application.
        boolean oldSettingsFileVersion = false;

        // Get the path for the settings folder that will hold the settings file.
        String settingsPath = System.getProperty("user.home") + "\\Documents\\DisplayHotKeys\\";

        // Set the full path to the settings file.
        settingsPath = settingsPath + "settings.ini";

        // Initialize the settings file object.
        File settingsFile = new File(settingsPath);

        // If the settings file has already been created from previously starting the application.
        if (settingsFile.exists()) {
            // Define a a buffered reader for the settings file.
            BufferedReader settingsFileReader;
            try {
                settingsFileReader = new BufferedReader(new FileReader(settingsFile));

                // Parse the settings file line-by-line until the end of the file is reached.
                String line;
                while ((line = settingsFileReader.readLine()) != null) {
                    // If the current line in the settings file equals the "[Slot1]" ini section...
                    if (line.equals("[Slot1]")) {
                        // The settings file is from a previous application version.
                        oldSettingsFileVersion = true;
                    }
                }
                settingsFileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // If the settings file is from previous application versions...
            if (oldSettingsFileVersion) {
                // Delete the settings file so it can be rebuilt for the new application version.
                try {
                    Files.deleteIfExists(settingsFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This method simply wraps the Wini store method in a try/catch block.
     */
    private void updateSettingsFile() {
        // Try to write the new property values to settings file.
        try {
            ini.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
