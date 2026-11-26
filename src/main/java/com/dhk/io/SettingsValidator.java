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

import com.dhk.localization.Localizer;
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
        validateInfoOverlayPosition();
        validateInfoOverlayScale();
        validateInfoOverlayOpacity();
        validateInfoOverlayDuration();
        validateLanguage();
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
        String darkMode = ini.get(SettingsManager.APPLICATION_SECTION, SettingsManager.DARK_MODE_PROPERTY);

        if (darkMode == null || !(darkMode.equals("false") || darkMode.equals("true"))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.DARK_MODE_PROPERTY, false);
        }
    }

    /**
     * Validates the value for the minimizeToTray property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the minimizeToTray property.
     */
    private void validateMinimizeToTray() {
        String minimizeToTray = ini.get(SettingsManager.APPLICATION_SECTION, SettingsManager.MINIMIZE_TO_TRAY_PROPERTY);

        if (minimizeToTray == null || !(minimizeToTray.equals("false") || minimizeToTray.equals("true"))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.MINIMIZE_TO_TRAY_PROPERTY, false);
        }
    }

    /**
     * Validates the value for the closeToTray property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the closeToTray property.
     */
    private void validateCloseToTray() {
        String closeToTray = ini.get(SettingsManager.APPLICATION_SECTION, SettingsManager.CLOSE_TO_TRAY_PROPERTY);

        if (closeToTray == null || !(closeToTray.equals("false") || closeToTray.equals("true"))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.CLOSE_TO_TRAY_PROPERTY, false);
        }
    }

    /**
     * Validates the value for the runOnStartup property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the runOnStartup property.
     */
    private void validateRunOnStartup() {
        String runOnStartup = ini.get(SettingsManager.APPLICATION_SECTION, SettingsManager.RUN_ON_STARTUP_PROPERTY);

        // Only repair the stored value here; the repaired state reaches the system through applySavedRunOnStartup
        if (runOnStartup == null || !(runOnStartup.equals("false") || runOnStartup.equals("true"))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.RUN_ON_STARTUP_PROPERTY, false);
        }
    }

    /**
     * Validates the value for the autoShowInfoOverlay property from the settings file. If the value is not a string
     * representation of a boolean, then it writes the default value for the autoShowInfoOverlay property.
     */
    private void validateAutoShowInfoOverlay() {
        String autoShowInfoOverlay = ini.get(SettingsManager.APPLICATION_SECTION,
                SettingsManager.AUTO_SHOW_INFO_OVERLAY_PROPERTY);

        if (autoShowInfoOverlay == null
                || !(autoShowInfoOverlay.equals("false") || autoShowInfoOverlay.equals("true"))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.AUTO_SHOW_INFO_OVERLAY_PROPERTY, false);
        }
    }

    /**
     * Validates the value for the infoOverlayPosition property from the settings file. If the value is not one of the
     * overlay's positions, then it writes the default top-right position for the infoOverlayPosition property.
     */
    private void validateInfoOverlayPosition() {
        String infoOverlayPosition = ini.get(SettingsManager.APPLICATION_SECTION,
                SettingsManager.INFO_OVERLAY_POSITION_PROPERTY);

        if (infoOverlayPosition == null || !isPositiveInt(infoOverlayPosition)
                || Integer.valueOf(infoOverlayPosition) >= DisplayInfoOverlay.OVERLAY_POSITION_COUNT) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.INFO_OVERLAY_POSITION_PROPERTY,
                    DisplayInfoOverlay.OVERLAY_POSITION_TOP_RIGHT);
        }
    }

    /**
     * Validates the value for the infoOverlayScale property from the settings file. If the value is not a step of the
     * info overlay's scale range, then it writes the default 130% scale for the infoOverlayScale property.
     */
    private void validateInfoOverlayScale() {
        String infoOverlayScale = ini.get(SettingsManager.APPLICATION_SECTION,
                SettingsManager.INFO_OVERLAY_SCALE_PROPERTY);

        if (infoOverlayScale == null || !isPositiveInt(infoOverlayScale)
                || Integer.valueOf(infoOverlayScale) < SettingsManager.MIN_INFO_OVERLAY_SCALE
                || Integer.valueOf(infoOverlayScale) > SettingsManager.MAX_INFO_OVERLAY_SCALE
                || Integer.valueOf(infoOverlayScale) % SettingsManager.INFO_OVERLAY_SCALE_STEP != 0) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.INFO_OVERLAY_SCALE_PROPERTY, 130);
        }
    }

    /**
     * Validates the value for the infoOverlayOpacity property from the settings file. If the value is not a step of the
     * info overlay's opacity range, then it writes the default 80% opacity for the infoOverlayOpacity property.
     */
    private void validateInfoOverlayOpacity() {
        String infoOverlayOpacity = ini.get(SettingsManager.APPLICATION_SECTION,
                SettingsManager.INFO_OVERLAY_OPACITY_PROPERTY);

        if (infoOverlayOpacity == null || !isPositiveInt(infoOverlayOpacity)
                || Integer.valueOf(infoOverlayOpacity) < SettingsManager.MIN_INFO_OVERLAY_OPACITY
                || Integer.valueOf(infoOverlayOpacity) > SettingsManager.MAX_INFO_OVERLAY_OPACITY
                || Integer.valueOf(infoOverlayOpacity) % SettingsManager.INFO_OVERLAY_OPACITY_STEP != 0) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.INFO_OVERLAY_OPACITY_PROPERTY, 80);
        }
    }

    /**
     * Validates the value for the infoOverlayDuration property from the settings file. If the value is not within the
     * info overlay's duration range, then it writes the default 6 seconds for the infoOverlayDuration property.
     */
    private void validateInfoOverlayDuration() {
        String infoOverlayDuration = ini.get(SettingsManager.APPLICATION_SECTION,
                SettingsManager.INFO_OVERLAY_DURATION_PROPERTY);

        if (infoOverlayDuration == null || !isPositiveInt(infoOverlayDuration)
                || Integer.valueOf(infoOverlayDuration) < SettingsManager.MIN_INFO_OVERLAY_DURATION
                || Integer.valueOf(infoOverlayDuration) > SettingsManager.MAX_INFO_OVERLAY_DURATION) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.INFO_OVERLAY_DURATION_PROPERTY, 6);
        }
    }

    /**
     * Validates the language property, which must always hold the tag of the language the application is showing, so a
     * missing or unsupported tag is replaced as well. It also records the install whose language was adopted, so that
     * install never overrides a later pick in the app.
     */
    private void validateLanguage() {
        String shownLanguageTag = Localizer.getLanguage().getTag();

        // The shown language is always supported, and launch may have picked it over the saved one for a newer install
        if (!shownLanguageTag.equals(ini.get(SettingsManager.APPLICATION_SECTION, SettingsManager.LANGUAGE_PROPERTY))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.LANGUAGE_PROPERTY, shownLanguageTag);
        }

        String installId = SettingsManager.getInstallId();

        // A copy that no installer set up, such as a portable one, has no install to record
        if (installId == null) {
            return;
        }

        if (!installId.equals(ini.get(SettingsManager.APPLICATION_SECTION, SettingsManager.INSTALL_ID_PROPERTY))) {
            repairProperty(SettingsManager.APPLICATION_SECTION, SettingsManager.INSTALL_ID_PROPERTY, installId);
        }
    }

    /**
     * Validates the value for the number of slots property from the settings file. If the value is not in the correct
     * range, then it writes the default value for the number of slots property.
     */
    private void validateNumOfSlots() {
        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            String displayId = displayIds[displayIndex];
            String iniProperty = SettingsManager.numOfSlotsProperty(displayId);
            String numOfSlots = ini.get(SettingsManager.APPLICATION_SECTION, iniProperty);

            if (numOfSlots == null || !isPositiveInt(numOfSlots) || Integer.valueOf(numOfSlots) < 1
                    || Integer.valueOf(numOfSlots) > settingsMgr.getMaxNumOfSlots()) {
                repairProperty(SettingsManager.APPLICATION_SECTION, iniProperty, 4);
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
                String iniSection = SettingsManager.slotSection(displayId, slotId);
                String orientationMode = ini.get(iniSection, SettingsManager.ORIENTATION_MODE_PROPERTY);

                if (orientationMode == null || !isPositiveInt(orientationMode)
                        || Integer.valueOf(orientationMode) > 3) {
                    repairProperty(iniSection, SettingsManager.ORIENTATION_MODE_PROPERTY, 0);
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
                String iniSection = SettingsManager.slotSection(displayId, slotId);
                String width = ini.get(iniSection, SettingsManager.DISPLAY_MODE_WIDTH_PROPERTY);
                String height = ini.get(iniSection, SettingsManager.DISPLAY_MODE_HEIGHT_PROPERTY);
                String refreshNumerator = ini.get(iniSection, SettingsManager.DISPLAY_MODE_REFRESH_NUMERATOR_PROPERTY);
                String refreshDenominator = ini.get(iniSection,
                        SettingsManager.DISPLAY_MODE_REFRESH_DENOMINATOR_PROPERTY);

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

        String iniSection = SettingsManager.slotSection(displayId, slotId);

        repairProperty(iniSection, SettingsManager.DISPLAY_MODE_WIDTH_PROPERTY, defaultDisplayMode.getWidth());
        repairProperty(iniSection, SettingsManager.DISPLAY_MODE_HEIGHT_PROPERTY, defaultDisplayMode.getHeight());
        repairProperty(iniSection, SettingsManager.DISPLAY_MODE_REFRESH_NUMERATOR_PROPERTY,
                defaultDisplayMode.getRefreshNumerator());
        repairProperty(iniSection, SettingsManager.DISPLAY_MODE_REFRESH_DENOMINATOR_PROPERTY,
                defaultDisplayMode.getRefreshDenominator());
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
                String iniSection = SettingsManager.slotSection(displayId, slotId);
                String dpiScalePercentage = ini.get(iniSection, SettingsManager.DPI_SCALE_PERCENTAGE_PROPERTY);

                if (dpiScalePercentage == null || !isPositiveInt(dpiScalePercentage)
                        || !isSupportedDpiScalePercentage(iniSection, dpiScalePercentage)) {
                    repairProperty(iniSection, SettingsManager.DPI_SCALE_PERCENTAGE_PROPERTY, 100);
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
        String width = ini.get(iniSection, SettingsManager.DISPLAY_MODE_WIDTH_PROPERTY);
        String height = ini.get(iniSection, SettingsManager.DISPLAY_MODE_HEIGHT_PROPERTY);

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
                String iniSection = SettingsManager.slotSection(displayId, slotId);
                String scalingMode = ini.get(iniSection, SettingsManager.SCALING_MODE_PROPERTY);

                if (scalingMode == null || !isPositiveInt(scalingMode)
                        || !Arrays.asList(VALID_SCALING_MODES).contains(scalingMode)) {
                    repairProperty(iniSection, SettingsManager.SCALING_MODE_PROPERTY, 0);
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
                validateHotKey(SettingsManager.slotSection(displayId, slotId));
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

        String hotKeySize = ini.get(iniSection, SettingsManager.HOT_KEY_SIZE_PROPERTY);

        if (hotKeySize == null || !isPositiveInt(hotKeySize) || Integer.valueOf(hotKeySize) < 0
                || Integer.valueOf(hotKeySize) > 3) {
            repairProperty(iniSection, SettingsManager.HOT_KEY_SIZE_PROPERTY, 0);
        }

        int validatedHotKeySize = ini.get(iniSection, SettingsManager.HOT_KEY_SIZE_PROPERTY, int.class);
        int numOfSetKeys = 0;

        for (int keyId = 1; keyId <= 3; keyId++) {
            int validatedKeyCode = ini.get(iniSection, SettingsManager.keyProperty(keyId), int.class);

            if (validatedKeyCode != UNSET_KEY_CODE) {
                numOfSetKeys++;
            }

            if (numOfSetKeys > validatedHotKeySize) {
                repairProperty(iniSection, SettingsManager.keyProperty(keyId), UNSET_KEY_CODE);
            }
        }

        if (validatedHotKeySize > numOfSetKeys) {
            repairProperty(iniSection, SettingsManager.HOT_KEY_SIZE_PROPERTY, numOfSetKeys);
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
            String key = ini.get(iniSection, SettingsManager.keyProperty(keyId));

            // An unset key persists as UNSET_KEY_CODE, so that value must pass validation without a repair
            if (key == null || !isPositiveInt(key)
                    || !(Integer.valueOf(key) == UNSET_KEY_CODE || validkeyCodes.contains(Integer.valueOf(key)))) {
                repairProperty(iniSection, SettingsManager.keyProperty(keyId), UNSET_KEY_CODE);
            }
        }
    }

}
