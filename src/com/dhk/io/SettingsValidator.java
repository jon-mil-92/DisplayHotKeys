package com.dhk.io;

import java.awt.DisplayMode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.ini4j.Wini;

import lc.kra.system.keyboard.event.GlobalKeyEvent;

/**
 * This class validates all of the property values in the settings file. It makes sure that each property value is
 * either a positive integer or a boolean, and that it is in the correct range of valid values. If a property value
 * fails validation, then it is reset to the default value.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class SettingsValidator {
    private SettingsManager settingsMgr;
    private Wini ini;
    private String[] displayIds;
    private ConcurrentHashMap<String, DisplayMode[]> displayModesMap;
    private ArrayList<Integer> validkeyCodes;
    private RunOnStartupManager runOnStartupManager;

    /**
     * Constructor for the SettingsValidator class.
     * 
     * @param settingsMgr - The manager for the settings file.
     */
    public SettingsValidator(SettingsManager settingsMgr) {
        // Initialize the settings manager.
        this.settingsMgr = settingsMgr;

        // Initialize the settings file.
        ini = settingsMgr.getIni();

        // Initialize the array of display IDs.
        displayIds = settingsMgr.getDisplayIds();

        // Initialize the map of display IDs to display modes.
        displayModesMap = settingsMgr.getDisplayModesMap();

        // Initialize the array list of valid key codes.
        validkeyCodes = buildValidKeyCodes();

        // Initialize the run on startup manager.
        runOnStartupManager = new RunOnStartupManager();
    }

    /**
     * This method validates all properties in the settings file. If the value for any property fails validation, then
     * the corresponding default value is written to the settings file.
     */
    public void validateAllProperties() {
        validateDarkMode();
        validateMinimizeToTray();
        validateRunOnStartup();
        validateNumOfSlots();
        validateOrientationMode();
        validateDisplayModes();
        validateScalingModes();
        validateDpiScalePercentages();
        validateHotKeys();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Private Methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method builds an array list of valid key codes from the GlobalKeyEvent class.
     */
    private ArrayList<Integer> buildValidKeyCodes() {
        // Get all key fields from the GlobalKeyEvent class.
        Field[] keyEventFields = GlobalKeyEvent.class.getDeclaredFields();

        // Instantiate an array list that will hold all key codes.
        ArrayList<Integer> keyCodeList = new ArrayList<Integer>();

        // For each field in the GlobalKeyEvent class...
        for (Field f : keyEventFields) {
            // Permit reading of the public field value.
            f.setAccessible(true);

            try {
                // If the current field is a public static field...
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
                    // Add the integer value of the field to the array list of key codes.
                    keyCodeList.add(f.getInt(f.getName()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Remove the unknown key code since it is the value used for a key that is not set.
        keyCodeList.removeIf(n -> (n == 0));

        return keyCodeList;
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

    /**
     * This method checks if a given string is a string representation of a positive integer.
     * 
     * @param intString - The supposed string representation of a positive integer.
     * @return Whether or not the given string is a string representation of a positive integer.
     */
    private boolean isPositiveInt(String intString) {
        // Get the string representation of the max integer value.
        String maxIntString = Integer.toString(Integer.MAX_VALUE);

        // If the given string is empty or its length is greater than the length of the max integer string...
        if (intString.isEmpty() || intString.length() > maxIntString.length()) {
            // The given string is not a string representation of a positive integer.
            return false;
        }

        // For each character in the given string...
        for (int charIndex = 0; charIndex < intString.length(); charIndex++) {
            // If the current character is not a digit with a radix of 10...
            if (Character.digit(intString.charAt(charIndex), 10) < 0) {
                // The given string is not a string representation of a positive integer.
                return false;
            }
        }

        // If the given string has the same length as the max integer string...
        if (intString.length() == maxIntString.length()) {
            // Compare each character of the given string to the max integer string.
            for (int charIndex = 0; charIndex < maxIntString.length(); charIndex++) {
                char intStringChar = intString.charAt(charIndex);
                char maxIntChar = maxIntString.charAt(charIndex);

                // If the given string is larger than the max integer string...
                if (intStringChar > maxIntChar) {
                    // The given string is not a string representation of a positive integer.
                    return false;
                }
            }
        }

        // If we got here, then the given string is a string representation of a positive integer.
        return true;
    }

    /**
     * This method validates the value for the darkMode property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the darkMode property.
     */
    private void validateDarkMode() {
        // Get the string representation of the darkMode boolean value.
        String darkMode = ini.get("Application", "darkMode");

        // If the darkMode property value is null or not a string representation of a boolean value...
        if (darkMode == null || !(darkMode.equals("false") || darkMode.equals("true"))) {
            // Reset the darkMode property to the default value.
            settingsMgr.saveIniDarkMode(false);
        }
    }

    /**
     * This method validates the value for the minimizeToTray property from the settings file. If the value is not a
     * string representation of a boolean, then it writes the default value for the minimizeToTray property.
     */
    private void validateMinimizeToTray() {
        // Get the string representation of the minimizeToTray boolean value.
        String minimizeToTray = ini.get("Application", "minimizeToTray");

        // If the minimizeToTray property value is null or not a string representation of a boolean value...
        if (minimizeToTray == null || !(minimizeToTray.equals("false") || minimizeToTray.equals("true"))) {
            // Reset the minimizeToTray property to the default value.
            settingsMgr.saveIniMinimizeToTray(false);
        }
    }

    /**
     * This method validates the value for the runOnStartup property from the settings file. If the value is not a
     * string representation of a boolean, then it writes the default value for the runOnStartup property.
     */
    private void validateRunOnStartup() {
        // Get the string representation of the runOnStartup boolean value.
        String runOnStartup = ini.get("Application", "runOnStartup");

        // If the runOnStartup property value is null or not a string representation of a boolean value...
        if (runOnStartup == null || !(runOnStartup.equals("false") || runOnStartup.equals("true"))) {
            // Reset the runOnStartup property to the default value.
            settingsMgr.saveIniRunOnStartup(false);

            // Remove the startup batch file since the runOnStartup property was reset to false.
            runOnStartupManager.removeFromStartup();
        }
    }

    /**
     * This method validates the value for the number of slots property from the settings file. If the value is not in
     * the correct range, then it writes the default value for the number of slots property.
     */
    private void validateNumOfSlots() {
        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // Set the ini property string for the current display ID.
            String iniProperty = "numOfSlotsFor--" + displayId;

            // Get the string value for the number of slots property for the current display ID from the settings file.
            String numOfSlots = ini.get("Application", iniProperty);

            // If the number of slots property value for the current display is null, not a positive integer, or not in
            // the correct range...
            if (numOfSlots == null || !isPositiveInt(numOfSlots) || Integer.valueOf(numOfSlots) < 1
                    || Integer.valueOf(numOfSlots) > settingsMgr.getMaxNumOfSlots()) {
                // Reset the number of slots property for the current display to the default value.
                settingsMgr.saveIniNumOfSlotsForDisplay(displayId, 4);
            }
        }
    }

    /**
     * This method validates the value for the orientation mode property from the settings file. If the value is not in
     * the correct range, then it writes the default value for the orientation mode property.
     */
    private void validateOrientationMode() {
        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // Set the ini property string for the current display ID.
            String iniProperty = "orientationModeFor--" + displayId;

            // Get the string value for the orientation mode property for the current display ID from the settings file.
            String orientationMode = ini.get("Application", iniProperty);

            // If the orientation mode property value for the current display is null, not a positive integer, or not in
            // the correct range...
            if (orientationMode == null || !isPositiveInt(orientationMode) || Integer.valueOf(orientationMode) > 3) {
                // Reset the orientation mode property for the current display to the default value.
                settingsMgr.saveIniOrientationModeForDisplay(displayId, 0);
            }
        }
    }

    /**
     * This method validates the display modes properties for each slot in the settings file. If the value for any
     * display mode property for any slot fails validation, the default display mode property values are written to the
     * settings file for that slot.
     */
    private void validateDisplayModes() {
        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // For each slot section in the settings file...
            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                // Set the ini section string for the current display ID and slot ID.
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);

                // Get the display mode properties for the current slot.
                String width = ini.get(iniSection, "displayModeWidth");
                String height = ini.get(iniSection, "displayModeHeight");
                String bitDepth = ini.get(iniSection, "displayModeBitDepth");
                String refreshRate = ini.get(iniSection, "displayModeRefreshRate");

                // If all display mode property values are not null and positive integers...
                if ((width != null && isPositiveInt(width)) && (height != null && isPositiveInt(height))
                        && (bitDepth != null && isPositiveInt(bitDepth))
                        && (refreshRate != null && isPositiveInt(refreshRate))) {
                    // Create a new display mode object with the values from the settings file.
                    DisplayMode displayMode = new DisplayMode(Integer.valueOf(width), Integer.valueOf(height),
                            Integer.valueOf(bitDepth), Integer.valueOf(refreshRate));

                    // If the current display mode is not in the supported display modes array...
                    if (!Arrays.asList(displayModesMap.get(displayId)).contains(displayMode)) {
                        // Write the default display mode values to their corresponding properties in the settings file.
                        writeDefaultDisplayMode(displayId, slotId);
                    }
                } else {
                    // Write the default display mode values to their corresponding properties in the settings file.
                    writeDefaultDisplayMode(displayId, slotId);
                }
            }
        }
    }

    /**
     * This method writes the default display mode property values to the give slot section in the settings file.
     * 
     * @param displayId - The ID of the display to get the display mode for.
     * @param slotId    - The ID of the slot to get the display mode for.
     */
    private void writeDefaultDisplayMode(String displayId, int slotId) {
        // Get the array of supported display modes for the given display.
        DisplayMode[] displayModes = displayModesMap.get(displayId);

        // Get the default highest display mode.
        DisplayMode defaultDisplayMode = displayModes[0];

        // Reset the display mode property values to their defaults (highest display mode values).
        settingsMgr.saveIniSlotDisplayMode(displayId, slotId, defaultDisplayMode.getWidth(),
                defaultDisplayMode.getHeight(), defaultDisplayMode.getBitDepth(), defaultDisplayMode.getRefreshRate());
    }

    /**
     * This method validates the value for each scalingMode property from the settings file. If the value is not a valid
     * value, then it writes the default value for the scalingMode property.
     */
    private void validateScalingModes() {
        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // For each slot section in the settings file...
            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                // An array of valid scaling mode values for the corresponding combo box.
                String[] validScalingModes = { "0", "1", "2" };

                // Set the ini section string for the current display ID and slot ID.
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);

                // Get the scaling mode for the current slot.
                String scalingMode = ini.get(iniSection, "scalingMode");

                // If the scalingMode property value is null, not a positive integer, or not in the valid array...
                if (scalingMode == null || !isPositiveInt(scalingMode)
                        || !Arrays.asList(validScalingModes).contains(scalingMode)) {
                    // Reset the scalingMode property to the default value.
                    settingsMgr.saveIniSlotScalingMode(displayId, slotId, 0);
                }
            }
        }
    }

    /**
     * This method validates the value for each dpiScalePercentage property from the settings file. If the value is not
     * a valid value, then it writes the default value for the dpiScalePercentage property.
     */
    private void validateDpiScalePercentages() {
        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // For each slot section in the settings file...
            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                // An array of valid DPI scale percentage values for the corresponding combo box.
                String[] validDpiScalePercentages = { "100", "125", "150", "175", "200", "225", "250", "300", "350" };

                // Set the ini section string for the current display ID and slot ID.
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);

                // Get the DPI scale percentage for the current slot.
                String dpiScalePercentage = ini.get(iniSection, "dpiScalePercentage");

                // If the dpiScalePercentage property value is null, not a positive integer, or not in the valid
                // array...
                if (dpiScalePercentage == null || !isPositiveInt(dpiScalePercentage)
                        || !Arrays.asList(validDpiScalePercentages).contains(dpiScalePercentage)) {
                    // Reset the dpiScalePercentage property to the default value.
                    settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, 100);
                }
            }
        }
    }

    /**
     * This method calls the method to validate the keys for each hot key, and then it validates the value for each
     * hotKeySize property from the settings file. If the value is not in the correct range, then it writes the default
     * value for the hotKeySize property. If the hotKeySize property value does not match the number of set keys, then
     * the hotKeySize property value is updated to the number of set keys.
     */
    private void validateHotKeys() {
        // Validate the value for each key property from the settings file.
        validateKeys();

        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // For each slot section in the settings file...
            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                // Set the ini section string for the current display ID and slot ID.
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);

                // Get the string value for the hotKeySize property from the settings file object.
                String hotKeySize = ini.get(iniSection, "hotKeySize");

                // If the hotKeySize property value is null, not a positive integer, or not in the correct range...
                if (hotKeySize == null || !isPositiveInt(hotKeySize) || Integer.valueOf(hotKeySize) < 0
                        || Integer.valueOf(hotKeySize) > 3) {
                    // Reset the hotKeySize property to the default value.
                    ini.put(iniSection, "hotKeySize", 0);

                    // Write the new hotKeySize property value to the settings file.
                    updateSettingsFile();
                }

                // Get the previously validated hot key size from the settings file object.
                int validatedHotKeySize = ini.get(iniSection, "hotKeySize", int.class);

                // Create a variable to store the number of keys that are set for the current hot key.
                int numOfSetKeys = 0;

                // For each key in the hot key...
                for (int keyId = 1; keyId <= 3; keyId++) {
                    // Get the previously validated key code from the settings file.
                    int validatedKeyCode = ini.get(iniSection, "key" + keyId, int.class);

                    // If the current key's key code is set...
                    if (validatedKeyCode != 0) {
                        // Increment the number of keys that are set for the current hot key.
                        numOfSetKeys++;
                    }

                    // If the current key is not a part of the current hot key.
                    if (numOfSetKeys > validatedHotKeySize) {
                        // Reset the key to the default value.
                        ini.put(iniSection, "key" + keyId, 0);

                        // Write the new key property value to the settings file.
                        updateSettingsFile();
                    }
                }

                // If the validated hot key size exceeds the number of keys that are set for the current hot key...
                if (validatedHotKeySize > numOfSetKeys) {
                    // Set the hotKeySize property to the number of set keys.
                    ini.put(iniSection, "hotKeySize", numOfSetKeys);

                    // Write the new hotKeySize property value to the settings file.
                    updateSettingsFile();
                }
            }
        }
    }

    /**
     * This method validates the value for each key property from the settings file. If the value is not a valid value,
     * then it writes the default value for the key property.
     */
    private void validateKeys() {
        // For each connected display...
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            // Get the ID for the current display index.
            String displayId = displayIds[displayIndex];

            // For each slot section in the settings file...
            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                // For each key in the hot key, validate its key code.
                for (int keyId = 1; keyId <= 3; keyId++) {
                    // Set the ini section string for the current display ID and slot ID.
                    String iniSection = displayId + "--Slot" + Integer.toString(slotId);

                    // Get the string value for the current key property from the settings file object.
                    String key = ini.get(iniSection, "key" + keyId);

                    // If any key property value is null, not a positive integer, or not in the list of valid keys
                    // codes...
                    if (key == null || !isPositiveInt(key) || !validkeyCodes.contains(Integer.valueOf(key))) {
                        // Reset the key to the default value.
                        ini.put(iniSection, "key" + keyId, 0);

                        // Write the new key property value to the settings file.
                        updateSettingsFile();
                    }
                }
            }
        }
    }
}
