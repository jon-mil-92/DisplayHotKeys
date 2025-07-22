package com.dhk.io;

import java.awt.DisplayMode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.ini4j.Wini;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

/**
 * Validates all of the property values in the settings file. It makes sure that each property value is either a
 * positive integer or a boolean, and that it is in the correct range of valid values. If a property value fails
 * validation, then it is reset to the default value.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class SettingsValidator {

    private SettingsManager settingsMgr;
    private Wini ini;
    private String[] displayIds;
    private Map<String, DisplayMode[]> displayModesMap;
    private List<Integer> validkeyCodes;
    private RunOnStartupManager runOnStartupManager;

    private final int UNSET_KEY_CODE = 0;
    private final String[] VALID_SCALING_MODES = {"0", "1", "2"};
    private final String[] VALID_DPI_SCALE_PERCENTAGES = {"100", "125", "150", "175", "200", "225", "250", "300",
            "350"};

    /**
     * Constructor for the SettingsValidator class.
     * 
     * @param settingsMgr
     *            - The manager for the settings file
     */
    public SettingsValidator(SettingsManager settingsMgr) {
        this.settingsMgr = settingsMgr;
        ini = settingsMgr.getIni();
        displayIds = settingsMgr.getDisplayIds();
        displayModesMap = settingsMgr.getDisplayModesMap();
        validkeyCodes = buildValidKeyCodes();
        runOnStartupManager = new RunOnStartupManager();
    }

    /**
     * Validates all properties in the settings file. If the value for any property fails validation, then the
     * corresponding default value is written to the settings file.
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

    /**
     * Builds a list of valid key codes from the GlobalKeyEvent class.
     */
    private List<Integer> buildValidKeyCodes() {
        Field[] keyEventFields = GlobalKeyEvent.class.getDeclaredFields();
        List<Integer> keyCodeList = new ArrayList<Integer>();

        for (Field f : keyEventFields) {
            f.setAccessible(true);

            try {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
                    keyCodeList.add(f.getInt(f.getName()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Remove the "unknown" key code since it is the value used for a key that is not set
        keyCodeList.removeIf(n -> (n == 0));

        return keyCodeList;
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

    /**
     * Checks if a given string is a string representation of a positive integer.
     * 
     * @param intString
     *            - The supposed string representation of a positive integer
     * 
     * @return Whether or not the given string is a string representation of a positive integer
     */
    private boolean isPositiveInt(String intString) {
        String maxIntString = Integer.toString(Integer.MAX_VALUE);

        if (intString.isEmpty() || intString.length() > maxIntString.length()) {
            return false;
        }

        for (int charIndex = 0; charIndex < intString.length(); charIndex++) {
            if (Character.digit(intString.charAt(charIndex), 10) < 0) {
                return false;
            }
        }

        if (intString.length() == maxIntString.length()) {
            for (int charIndex = 0; charIndex < maxIntString.length(); charIndex++) {
                char intStringChar = intString.charAt(charIndex);
                char maxIntChar = maxIntString.charAt(charIndex);

                if (intStringChar > maxIntChar) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates the value for the darkMode property from the settings file. If the value is not a string representation
     * of a boolean, then it writes the default value for the darkMode property.
     */
    private void validateDarkMode() {
        String darkMode = ini.get("Application", "darkMode");

        if (darkMode == null || !(darkMode.equals("false") || darkMode.equals("true"))) {
            settingsMgr.saveIniDarkMode(false);
        }
    }

    /**
     * Validates the value for the minimizeToTray property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the minimizeToTray property.
     */
    private void validateMinimizeToTray() {
        String minimizeToTray = ini.get("Application", "minimizeToTray");

        if (minimizeToTray == null || !(minimizeToTray.equals("false") || minimizeToTray.equals("true"))) {
            settingsMgr.saveIniMinimizeToTray(false);
        }
    }

    /**
     * Validates the value for the runOnStartup property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the runOnStartup property.
     */
    private void validateRunOnStartup() {
        String runOnStartup = ini.get("Application", "runOnStartup");

        if (runOnStartup == null || !(runOnStartup.equals("false") || runOnStartup.equals("true"))) {
            settingsMgr.saveIniRunOnStartup(false);
            runOnStartupManager.removeFromStartup();
        }
    }

    /**
     * Validates the value for the number of slots property from the settings file. If the value is not in the correct
     * range, then it writes the default value for the number of slots property.
     */
    private void validateNumOfSlots() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];
            String iniProperty = "numOfSlotsFor--" + displayId;
            String numOfSlots = ini.get("Application", iniProperty);

            if (numOfSlots == null || !isPositiveInt(numOfSlots) || Integer.valueOf(numOfSlots) < 1
                    || Integer.valueOf(numOfSlots) > settingsMgr.getMaxNumOfSlots()) {
                settingsMgr.saveIniNumOfSlotsForDisplay(displayId, 4);
            }
        }
    }

    /**
     * Validates the value for the orientation mode property from the settings file. If the value is not in the correct
     * range, then it writes the default value for the orientation mode property.
     */
    private void validateOrientationMode() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];
            String iniProperty = "orientationModeFor--" + displayId;
            String orientationMode = ini.get("Application", iniProperty);

            if (orientationMode == null || !isPositiveInt(orientationMode) || Integer.valueOf(orientationMode) > 3) {
                settingsMgr.saveIniOrientationModeForDisplay(displayId, 0);
            }
        }
    }

    /**
     * Validates the display modes properties for each slot in the settings file. If the value for any display mode
     * property for any slot fails validation, the default display mode property values are written to the settings file
     * for that slot.
     */
    private void validateDisplayModes() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String width = ini.get(iniSection, "displayModeWidth");
                String height = ini.get(iniSection, "displayModeHeight");
                String bitDepth = ini.get(iniSection, "displayModeBitDepth");
                String refreshRate = ini.get(iniSection, "displayModeRefreshRate");

                if ((width != null && isPositiveInt(width)) && (height != null && isPositiveInt(height))
                        && (bitDepth != null && isPositiveInt(bitDepth))
                        && (refreshRate != null && isPositiveInt(refreshRate))) {
                    DisplayMode displayMode = new DisplayMode(Integer.valueOf(width), Integer.valueOf(height),
                            Integer.valueOf(bitDepth), Integer.valueOf(refreshRate));

                    if (!Arrays.asList(displayModesMap.get(displayId)).contains(displayMode)) {
                        writeDefaultDisplayMode(displayId, slotId);
                    }
                } else {
                    writeDefaultDisplayMode(displayId, slotId);
                }
            }
        }
    }

    /**
     * Writes the default display mode property values to the give slot section in the settings file.
     * 
     * @param displayId
     *            - The ID of the display to get the display mode for
     * @param slotId
     *            - The ID of the slot to get the display mode for
     */
    private void writeDefaultDisplayMode(String displayId, int slotId) {
        DisplayMode[] displayModes = displayModesMap.get(displayId);
        DisplayMode defaultDisplayMode = displayModes[0];

        settingsMgr.saveIniSlotDisplayMode(displayId, slotId, defaultDisplayMode.getWidth(),
                defaultDisplayMode.getHeight(), defaultDisplayMode.getBitDepth(), defaultDisplayMode.getRefreshRate());
    }

    /**
     * Validates the value for each scalingMode property from the settings file. If the value is not a valid value, then
     * it writes the default value for the scalingMode property.
     */
    private void validateScalingModes() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String scalingMode = ini.get(iniSection, "scalingMode");

                if (scalingMode == null || !isPositiveInt(scalingMode)
                        || !Arrays.asList(VALID_SCALING_MODES).contains(scalingMode)) {
                    settingsMgr.saveIniSlotScalingMode(displayId, slotId, 0);
                }
            }
        }
    }

    /**
     * Validates the value for each dpiScalePercentage property from the settings file. If the value is not a valid
     * value, then it writes the default value for the dpiScalePercentage property.
     */
    private void validateDpiScalePercentages() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String dpiScalePercentage = ini.get(iniSection, "dpiScalePercentage");

                if (dpiScalePercentage == null || !isPositiveInt(dpiScalePercentage)
                        || !Arrays.asList(VALID_DPI_SCALE_PERCENTAGES).contains(dpiScalePercentage)) {
                    settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, 100);
                }
            }
        }
    }

    /**
     * Calls the method to validate the keys for each hot key, and then it validates the value for each hotKeySize
     * property from the settings file. If the value is not in the correct range, then it writes the default value for
     * the hotKeySize property. If the hotKeySize property value does not match the number of set keys, then the
     * hotKeySize property value is updated to the number of set keys.
     */
    private void validateHotKeys() {
        validateKeys();

        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String hotKeySize = ini.get(iniSection, "hotKeySize");

                if (hotKeySize == null || !isPositiveInt(hotKeySize) || Integer.valueOf(hotKeySize) < 0
                        || Integer.valueOf(hotKeySize) > 3) {
                    ini.put(iniSection, "hotKeySize", 0);

                    updateSettingsFile();
                }

                int validatedHotKeySize = ini.get(iniSection, "hotKeySize", int.class);
                int numOfSetKeys = 0;

                for (int keyId = 1; keyId <= 3; keyId++) {
                    int validatedKeyCode = ini.get(iniSection, "key" + keyId, int.class);

                    if (validatedKeyCode != UNSET_KEY_CODE) {
                        numOfSetKeys++;
                    }

                    if (numOfSetKeys > validatedHotKeySize) {
                        ini.put(iniSection, "key" + keyId, UNSET_KEY_CODE);

                        updateSettingsFile();
                    }
                }

                if (validatedHotKeySize > numOfSetKeys) {
                    ini.put(iniSection, "hotKeySize", numOfSetKeys);

                    updateSettingsFile();
                }
            }
        }
    }

    /**
     * Validates the value for each key property from the settings file. If the value is not a valid value, then it
     * writes the default value for the key property.
     */
    private void validateKeys() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                for (int keyId = 1; keyId <= 3; keyId++) {
                    String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                    String key = ini.get(iniSection, "key" + keyId);

                    if (key == null || !isPositiveInt(key) || !validkeyCodes.contains(Integer.valueOf(key))) {
                        ini.put(iniSection, "key" + keyId, UNSET_KEY_CODE);

                        updateSettingsFile();
                    }
                }
            }
        }
    }

}
