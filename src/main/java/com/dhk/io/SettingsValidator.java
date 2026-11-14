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
package com.dhk.io;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ini4j.Wini;

import com.dhk.model.DisplayMode;

/**
 * Validates all of the property values in the settings file. It makes sure that each property value is either a
 * positive integer or a boolean, and that it is in the correct range of valid values. If a property value fails
 * validation, then it is reset to the default value in the in-memory settings file object.
 *
 * @author Jonathan R. Miller
 */
public class SettingsValidator {

    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private Wini ini;
    private String[] displayIds;
    private Map<String, DisplayMode[]> landscapeDisplayModesMap;
    private Map<String, DisplayMode[]> portraitDisplayModesMap;
    private List<Integer> validkeyCodes;
    private boolean repairedProperty;

    /**
     * Key code saved in place of each key a hot key does not use.
     */
    private static final int UNSET_KEY_CODE = 0;

    /**
     * Scaling mode values a slot may store, which stand for preserved, stretched, and centered.
     */
    private static final String[] VALID_SCALING_MODES = {"0", "1", "2"};

    /**
     * Display mode written to a repaired slot when its display reports no modes for the slot's orientation.
     */
    private static final DisplayMode DEFAULT_DISPLAY_MODE = new DisplayMode(0, 0, 0, 0);

    /**
     * Constructor for the {@link SettingsValidator} class.
     *
     * @param settingsMgr
     *            - The manager for the settings file
     */
    public SettingsValidator(SettingsManager settingsMgr) {
        this.settingsMgr = settingsMgr;
        displayConfig = settingsMgr.getDisplayConfig();
        ini = settingsMgr.getIni();
        displayIds = settingsMgr.getDisplayIds();
        landscapeDisplayModesMap = settingsMgr.getLandscapeDisplayModesMap();
        portraitDisplayModesMap = settingsMgr.getPortraitDisplayModesMap();
        validkeyCodes = buildValidKeyCodes();
    }

    /**
     * Validates all properties in the settings file. If the value for any property fails validation, then the
     * corresponding default value is repaired in the in-memory settings file object; the caller persists the repairs.
     *
     * @return Whether or not any property was repaired
     */
    public boolean validateAllProperties() {
        validateDarkMode();
        validateMinimizeToTray();
        validateCloseToTray();
        validateRunOnStartup();
        validateAutoShowInfoOverlay();
        validateNumOfSlots();
        validateOrientationModes();
        validateDisplayModes();
        validateDpiScalePercentages();
        validateScalingModes();
        validateHotKeys();

        return repairedProperty;
    }

    /**
     * Builds a list of valid key codes from the NativeKeyEvent class.
     */
    private List<Integer> buildValidKeyCodes() {
        /*
         * Every public constant on the event type is taken as a key code, so that type must hold nothing but the key
         * code constants. Anything else added there would silently become a hot key value the settings file accepts
         */
        Field[] keyEventFields = NativeKeyEvent.class.getDeclaredFields();
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
     * Repairs a property by writing the given value to the in-memory settings file object, and remembers that a repair
     * was made so the caller knows to persist the settings file.
     *
     * @param section
     *            - The settings file section that holds the property
     * @param key
     *            - The name of the property to repair
     * @param value
     *            - The value to write for the property
     */
    private void repairProperty(String section, String key, Object value) {
        ini.put(section, key, value);

        repairedProperty = true;
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
            repairProperty("Application", "darkMode", false);
        }
    }

    /**
     * Validates the value for the minimizeToTray property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the minimizeToTray property.
     */
    private void validateMinimizeToTray() {
        String minimizeToTray = ini.get("Application", "minimizeToTray");

        if (minimizeToTray == null || !(minimizeToTray.equals("false") || minimizeToTray.equals("true"))) {
            repairProperty("Application", "minimizeToTray", false);
        }
    }

    /**
     * Validates the value for the closeToTray property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the closeToTray property.
     */
    private void validateCloseToTray() {
        String closeToTray = ini.get("Application", "closeToTray");

        if (closeToTray == null || !(closeToTray.equals("false") || closeToTray.equals("true"))) {
            repairProperty("Application", "closeToTray", false);
        }
    }

    /**
     * Validates the value for the runOnStartup property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the runOnStartup property.
     */
    private void validateRunOnStartup() {
        String runOnStartup = ini.get("Application", "runOnStartup");

        // Only repair the stored value here; the repaired state reaches the system through applySavedRunOnStartup
        if (runOnStartup == null || !(runOnStartup.equals("false") || runOnStartup.equals("true"))) {
            repairProperty("Application", "runOnStartup", false);
        }
    }

    /**
     * Validates the value for the autoShowInfoOverlay property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the autoShowInfoOverlay property.
     */
    private void validateAutoShowInfoOverlay() {
        String autoShowInfoOverlay = ini.get("Application", "autoShowInfoOverlay");

        if (autoShowInfoOverlay == null
                || !(autoShowInfoOverlay.equals("false") || autoShowInfoOverlay.equals("true"))) {
            repairProperty("Application", "autoShowInfoOverlay", false);
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
                repairProperty("Application", iniProperty, 4);
            }
        }
    }

    /**
     * Validates the value for each orientationMode property from the settings file. If the value is not a valid value,
     * then it writes the default value for the orientationMode property.
     */
    private void validateOrientationModes() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String orientationMode = ini.get(iniSection, "orientationMode");

                if (orientationMode == null || !isPositiveInt(orientationMode)
                        || Integer.valueOf(orientationMode) > 3) {
                    repairProperty(iniSection, "orientationMode", 0);
                }
            }
        }
    }

    /**
     * Validates the display mode for each slot in the settings file, resetting a missing or malformed mode (such as a
     * pre-fractional one without the numerator/denominator pair) to the default. A well-formed mode is kept even when
     * the display does not currently offer it, since a display that is off or still waking reports only some modes.
     */
    private void validateDisplayModes() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String width = ini.get(iniSection, "displayModeWidth");
                String height = ini.get(iniSection, "displayModeHeight");
                String refreshNumerator = ini.get(iniSection, "displayModeRefreshNumerator");
                String refreshDenominator = ini.get(iniSection, "displayModeRefreshDenominator");

                if (width != null && isPositiveInt(width) && height != null && isPositiveInt(height)
                        && refreshNumerator != null && isPositiveInt(refreshNumerator) && refreshDenominator != null
                        && isPositiveInt(refreshDenominator)) {
                    continue;
                }

                // Repair with a default matching the slot's own orientation
                int slotOrientationMode = settingsMgr.getIniSlotOrientationMode(displayId, slotId);
                boolean landscapeOrientation = slotOrientationMode == 0 || slotOrientationMode == 2;
                writeDefaultDisplayMode(landscapeOrientation, displayId, slotId);
            }
        }
    }

    /**
     * Writes the default display mode property values to the give slot section in the settings file.
     *
     * @param landscapeOrientation
     *            - Whether or not to write a landscape display mode
     * @param displayId
     *            - The ID of the display to get the display mode for
     * @param slotId
     *            - The ID of the slot to get the display mode for
     */
    private void writeDefaultDisplayMode(boolean landscapeOrientation, String displayId, int slotId) {
        DisplayMode[] landscapeDisplayModes = landscapeDisplayModesMap.get(displayId);
        DisplayMode[] portraitDisplayModes = portraitDisplayModesMap.get(displayId);
        DisplayMode defaultDisplayMode = DEFAULT_DISPLAY_MODE;

        if (landscapeOrientation && landscapeDisplayModes.length > 0) {
            defaultDisplayMode = landscapeDisplayModes[0];
        } else if (!landscapeOrientation && portraitDisplayModes.length > 0) {
            defaultDisplayMode = portraitDisplayModes[0];
        }

        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        repairProperty(iniSection, "displayModeWidth", defaultDisplayMode.getWidth());
        repairProperty(iniSection, "displayModeHeight", defaultDisplayMode.getHeight());
        repairProperty(iniSection, "displayModeRefreshNumerator", defaultDisplayMode.getRefreshNumerator());
        repairProperty(iniSection, "displayModeRefreshDenominator", defaultDisplayMode.getRefreshDenominator());
    }

    /**
     * Validates the value for each dpiScalePercentage property from the settings file. The set of valid DPI scale
     * percentages depends on the slot's resolution, so each value is validated against the percentages Windows supports
     * for that slot's stored resolution. If the value is not a supported value, then it writes the default value for
     * the dpiScalePercentage property. This runs after the display modes are validated, so each slot's stored
     * resolution is already a valid resolution for its display.
     */
    private void validateDpiScalePercentages() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                String iniSection = displayId + "--Slot" + Integer.toString(slotId);
                String dpiScalePercentage = ini.get(iniSection, "dpiScalePercentage");

                if (dpiScalePercentage == null || !isPositiveInt(dpiScalePercentage)
                        || !isSupportedDpiScalePercentage(iniSection, dpiScalePercentage)) {
                    repairProperty(iniSection, "dpiScalePercentage", 100);
                }
            }
        }
    }

    /**
     * Determines whether the given DPI scale percentage is supported for the resolution stored in the given slot
     * section. The supported set depends on the slot's resolution, so the stored display mode width and height are used
     * to look up the percentages Windows supports for that resolution.
     *
     * @param iniSection
     *            - The slot section to read the stored resolution from
     * @param dpiScalePercentage
     *            - The string representation of the DPI scale percentage to check
     *
     * @return Whether or not the given DPI scale percentage is supported for the slot's stored resolution
     */
    private boolean isSupportedDpiScalePercentage(String iniSection, String dpiScalePercentage) {
        String width = ini.get(iniSection, "displayModeWidth");
        String height = ini.get(iniSection, "displayModeHeight");

        if (width == null || !isPositiveInt(width) || height == null || !isPositiveInt(height)) {
            return false;
        }

        Integer[] supportedDpiScalePercentages = displayConfig.getSupportedDpiScalePercentages(Integer.valueOf(width),
                Integer.valueOf(height));

        return Arrays.asList(supportedDpiScalePercentages).contains(Integer.valueOf(dpiScalePercentage));
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
                    repairProperty(iniSection, "scalingMode", 0);
                }
            }
        }
    }

    /**
     * Validates the hot key of every slot of every display, and then the Display Info hot key. Each hot key is held in
     * its own section with the same property names, so all of them are validated the same way.
     */
    private void validateHotKeys() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];

            for (int slotId = 1; slotId <= settingsMgr.getMaxNumOfSlots(); slotId++) {
                validateHotKey(displayId + "--Slot" + Integer.toString(slotId));
            }
        }

        validateHotKey(SettingsManager.DISPLAY_INFO_HOT_KEY_SECTION);
    }

    /**
     * Validates the keys of the hot key in the given section, and then its hotKeySize property. A hotKeySize out of
     * range is reset to its default, and a hotKeySize that does not match the number of set keys is corrected to it.
     *
     * @param iniSection
     *            - The section of the settings file holding the hot key properties
     */
    private void validateHotKey(String iniSection) {
        validateKeys(iniSection);

        String hotKeySize = ini.get(iniSection, "hotKeySize");

        if (hotKeySize == null || !isPositiveInt(hotKeySize) || Integer.valueOf(hotKeySize) < 0
                || Integer.valueOf(hotKeySize) > 3) {
            repairProperty(iniSection, "hotKeySize", 0);
        }

        int validatedHotKeySize = ini.get(iniSection, "hotKeySize", int.class);
        int numOfSetKeys = 0;

        for (int keyId = 1; keyId <= 3; keyId++) {
            int validatedKeyCode = ini.get(iniSection, "key" + keyId, int.class);

            if (validatedKeyCode != UNSET_KEY_CODE) {
                numOfSetKeys++;
            }

            if (numOfSetKeys > validatedHotKeySize) {
                repairProperty(iniSection, "key" + keyId, UNSET_KEY_CODE);
            }
        }

        if (validatedHotKeySize > numOfSetKeys) {
            repairProperty(iniSection, "hotKeySize", numOfSetKeys);
        }
    }

    /**
     * Validates the value for each key property of the hot key in the given section. A key value is valid when it is
     * the unset key code or a supported key code; otherwise the default unset value is written for the key property.
     *
     * @param iniSection
     *            - The section of the settings file holding the hot key properties
     */
    private void validateKeys(String iniSection) {
        for (int keyId = 1; keyId <= 3; keyId++) {
            String key = ini.get(iniSection, "key" + keyId);

            // An unset key persists as UNSET_KEY_CODE, so that value must pass validation without a repair
            if (key == null || !isPositiveInt(key)
                    || !(Integer.valueOf(key) == UNSET_KEY_CODE || validkeyCodes.contains(Integer.valueOf(key)))) {
                repairProperty(iniSection, "key" + keyId, UNSET_KEY_CODE);
            }
        }
    }

}
