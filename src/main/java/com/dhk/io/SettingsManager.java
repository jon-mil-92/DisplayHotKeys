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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.ini4j.Wini;

import com.dhk.localization.Language;
import com.dhk.model.DisplayMode;
import com.dhk.model.HotKey;
import com.dhk.model.Key;

/**
 * Saves the application settings to an ini file. It enables the saving of the active number of slots, the orientation
 * mode, the theme state, the language, the minimize to tray state, the close to tray state, the run on startup state,
 * the display modes, the DPI scale percentages, the scaling modes, and the hot keys for each display
 *
 * @author Jonathan R. Miller
 */
public class SettingsManager {

    private Wini ini;
    private DisplayConfig displayConfig;
    private int numOfConnectedDisplays;
    private String[] displayIds;
    private int[] displayNumbers;
    private Map<String, DisplayMode[]> landscapeDisplayModesMap;
    private Map<String, DisplayMode[]> portraitDisplayModesMap;
    private File settingsFile;

    /**
     * Most slots each display can show in the slot table.
     */
    private static final int MAX_NUM_OF_SLOTS = 12;

    /**
     * File beside the installed application where the installer records the default language picked during setup.
     */
    private static final String INSTALLER_DEFAULTS_FILE_NAME = "defaults.ini";

    /**
     * Settings file section for the Display Info hot key, which reuses the property names of a slot's hot key so both
     * are read, saved, and validated alike.
     */
    static final String DISPLAY_INFO_HOT_KEY_SECTION = "DisplayInfoHotKey";

    /**
     * Constructor for the {@link SettingsManager} class.
     */
    public SettingsManager() {
        displayConfig = new DisplayConfig();
    }

    /**
     * Initializes the displays and settings file for the settings manager.
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
     * Reads only the saved dark mode preference straight from the settings file, without initializing displays or
     * validating the file, so an early-exit path can match the app theme cheaply.
     *
     * @return The saved dark mode value, or false when it cannot be read
     */
    public static boolean getSavedDarkMode() {
        return Boolean.parseBoolean(readApplicationSetting(new File(getSettingsFilePath()), "darkMode"));
    }

    /**
     * Sets the language property value in the settings file object.
     *
     * @param language
     *            - The new value for the language property
     */
    public void saveIniLanguage(Language language) {
        ini.put("Application", "language", language.getTag());

        updateSettingsFile();
    }

    /**
     * Picks the language to show at launch, which is the saved language, then the default language picked in the
     * installer, and otherwise the Windows display language. It reads the files straight away, without initializing
     * displays or validating the settings file, since the language is needed before anything is shown.
     *
     * @return The language to show at launch
     */
    public static Language getStartupLanguage() {
        Language savedLanguage = Language.fromTag(readApplicationSetting(new File(getSettingsFilePath()), "language"));

        if (savedLanguage != null) {
            return savedLanguage;
        }

        // Only the packaged launcher sets the executable path, and the installer writes its defaults beside it
        String appExePath = System.getProperty("jpackage.app-path");

        if (appExePath != null) {
            File defaultsFile = new File(new File(appExePath).getParentFile(), INSTALLER_DEFAULTS_FILE_NAME);
            Language installerLanguage = Language.fromTag(readApplicationSetting(defaultsFile, "defaultLanguage"));

            if (installerLanguage != null) {
                return installerLanguage;
            }
        }

        return Language.fromLocale(Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * Determines whether a settings file has been saved.
     *
     * @return True if the settings file exists, otherwise false
     */
    public static boolean savedSettingsExist() {
        return new File(getSettingsFilePath()).exists();
    }

    /**
     * Deletes the saved settings file, then its folder when nothing else is left in it.
     *
     * @return True if the settings file is gone, otherwise false
     */
    public static boolean deleteSavedSettings() {
        File savedSettingsFile = new File(getSettingsFilePath());

        try {
            Files.deleteIfExists(savedSettingsFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        // Deleting a folder only succeeds once it is empty, so a folder still holding other files is left alone
        savedSettingsFile.getParentFile().delete();

        return true;
    }

    /**
     * Reads a property of the application section straight from an ini file.
     *
     * @param iniFile
     *            - The ini file to read
     * @param key
     *            - The name of the property to read
     *
     * @return The property value, or null when the file or the property is missing or cannot be read
     */
    private static String readApplicationSetting(File iniFile, String key) {
        if (!iniFile.exists()) {
            return null;
        }

        try {
            return new Wini(iniFile).get("Application", key);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
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
     * Gets the close to tray property value from the settings file object.
     *
     * @return The value for the close to tray property
     */
    public boolean getIniCloseToTray() {
        return ini.get("Application", "closeToTray", boolean.class);
    }

    /**
     * Sets the close to tray property value in the settings file object.
     *
     * @param closeToTray
     *            - The new value for the close to tray property
     */
    public void saveIniCloseToTray(boolean closeToTray) {
        ini.put("Application", "closeToTray", closeToTray);

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
     * Gets the auto show info overlay property value from the settings file object.
     *
     * @return The value for the auto show info overlay property
     */
    public boolean getIniAutoShowInfoOverlay() {
        return ini.get("Application", "autoShowInfoOverlay", boolean.class);
    }

    /**
     * Sets the auto show info overlay property value in the settings file object.
     *
     * @param autoShowInfoOverlay
     *            - The new value for the auto show info overlay property
     */
    public void saveIniAutoShowInfoOverlay(boolean autoShowInfoOverlay) {
        ini.put("Application", "autoShowInfoOverlay", autoShowInfoOverlay);

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

        /*
         * The validator writes the exact refresh rate as a numerator/denominator pair before the model is built (a slot
         * missing it, such as a pre-fractional one, is reset to the default), so both are present here
         */
        DisplayMode slotDisplayMode = new DisplayMode(ini.get(iniSection, "displayModeWidth", int.class),
                ini.get(iniSection, "displayModeHeight", int.class),
                ini.get(iniSection, "displayModeRefreshNumerator", int.class),
                ini.get(iniSection, "displayModeRefreshDenominator", int.class));

        return slotDisplayMode;
    }

    /**
     * Sets the specified slot's display mode properties in the settings file object, persisting the exact refresh rate
     * as a numerator/denominator pair.
     *
     * @param displayId
     *            - The ID of the display to set the display mode for
     * @param slotId
     *            - The ID of the slot to set the display mode for
     * @param displayMode
     *            - The new display mode for the specified slot
     */
    public void saveIniSlotDisplayMode(String displayId, int slotId, DisplayMode displayMode) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        ini.put(iniSection, "displayModeWidth", displayMode.getWidth());
        ini.put(iniSection, "displayModeHeight", displayMode.getHeight());
        ini.put(iniSection, "displayModeRefreshNumerator", displayMode.getRefreshNumerator());
        ini.put(iniSection, "displayModeRefreshDenominator", displayMode.getRefreshDenominator());

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
     * Gets the specified slot's orientation mode property value from the settings file object.
     *
     * @param displayId
     *            - The ID of the display to get the orientation mode for
     * @param slotId
     *            - The ID of the slot to get the orientation mode for
     *
     * @return The specified slot's orientation mode property value
     */
    public int getIniSlotOrientationMode(String displayId, int slotId) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);

        return ini.get(iniSection, "orientationMode", int.class);
    }

    /**
     * Sets the specified slot's orientation mode property value in the settings file object.
     *
     * @param displayId
     *            - The ID of the display to set the orientation mode for
     * @param slotId
     *            - The ID of the slot to set the orientation mode for
     * @param orientationMode
     *            - The specified slot's new value for the orientation mode property
     */
    public void saveIniSlotOrientationMode(String displayId, int slotId, int orientationMode) {
        String iniSection = displayId + "--Slot" + Integer.toString(slotId);
        ini.put(iniSection, "orientationMode", orientationMode);

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
        return getIniHotKey(displayId + "--Slot" + Integer.toString(slotId));
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
        saveIniHotKey(displayId + "--Slot" + Integer.toString(slotId), hotKey);
    }

    /**
     * Gets the Display Info hot key built from the hot key properties in the settings file object.
     *
     * @return The Display Info hot key
     */
    public HotKey getIniDisplayInfoHotKey() {
        return getIniHotKey(DISPLAY_INFO_HOT_KEY_SECTION);
    }

    /**
     * Sets the Display Info hot key properties in the settings file object.
     *
     * @param hotKey
     *            - The Display Info hot key
     */
    public void saveIniDisplayInfoHotKey(HotKey hotKey) {
        saveIniHotKey(DISPLAY_INFO_HOT_KEY_SECTION, hotKey);
    }

    /**
     * Gets a hot key built from the hot key properties in the given section of the settings file object.
     *
     * @param iniSection
     *            - The section of the settings file holding the hot key properties
     *
     * @return The hot key held in the given section
     */
    private HotKey getIniHotKey(String iniSection) {
        HotKey hotKey = new HotKey(new ArrayList<Key>());
        int hotKeySize = ini.get(iniSection, "hotKeySize", int.class);

        for (int keyId = 1; keyId <= hotKeySize; keyId++) {
            int keyCode = ini.get(iniSection, "key" + Integer.toString(keyId), int.class);
            hotKey.getKeys().add(new Key(keyCode, KeyText.getKeyCodeText(keyCode), false));
        }

        return hotKey;
    }

    /**
     * Sets the hot key properties in the given section of the settings file object.
     *
     * @param iniSection
     *            - The section of the settings file to hold the hot key properties
     * @param hotKey
     *            - The hot key to save
     */
    private void saveIniHotKey(String iniSection, HotKey hotKey) {
        int hotKeySize = hotKey.getKeys().size();
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
     * Gets the Windows Display Settings number of each connected display, aligned index-for-index with getDisplayIds.
     *
     * @return The Windows display number of each connected display, in getDisplayIds order
     */
    public int[] getDisplayNumbers() {
        return displayNumbers;
    }

    /**
     * Gets the map of display IDs to supported landscape display modes array.
     *
     * @return The map of display IDs to supported landscape display modes array
     */
    public Map<String, DisplayMode[]> getLandscapeDisplayModesMap() {
        return landscapeDisplayModesMap;
    }

    /**
     * Gets the map of display IDs to supported portrait display modes array.
     *
     * @return The map of display IDs to supported portrait display modes array
     */
    public Map<String, DisplayMode[]> getPortraitDisplayModesMap() {
        return portraitDisplayModesMap;
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
     * Refreshes the connected displays and their supported display modes on the shared display configuration, then
     * caches the landscape and portrait display modes for each connected display by its ID.
     */
    private void initDisplays() {
        displayConfig.updateDisplayConfig();
        numOfConnectedDisplays = displayConfig.getNumOfConnectedDisplays();
        displayIds = displayConfig.getDisplayIds();
        displayNumbers = displayConfig.getDisplayNumbers();
        landscapeDisplayModesMap = new HashMap<String, DisplayMode[]>();
        portraitDisplayModesMap = new HashMap<String, DisplayMode[]>();

        for (int displayIndex = 0; displayIndex < numOfConnectedDisplays; displayIndex++) {
            landscapeDisplayModesMap.put(displayIds[displayIndex],
                    displayConfig.getLandscapeDisplayModes(displayIds[displayIndex]));
            portraitDisplayModesMap.put(displayIds[displayIndex],
                    displayConfig.getPortraitDisplayModes(displayIds[displayIndex]));
        }
    }

    /**
     * Builds the absolute path to the settings file in the user's Documents folder.
     *
     * @return The settings file path
     */
    private static String getSettingsFilePath() {
        return System.getProperty("user.home") + "\\Documents\\DisplayHotKeys\\settings.ini";
    }

    /**
     * Initializes the settings file.
     */
    private void initSettingsFile() {
        checkSettingsFileVersion();

        try {
            settingsFile = new File(getSettingsFilePath());
            settingsFile.getParentFile().mkdirs();
            settingsFile.createNewFile();

            ini = new Wini(settingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SettingsValidator validator = new SettingsValidator(this);
        boolean repaired = validator.validateAllProperties();

        // Repairs only edit the in-memory settings file object, so persist them here in a single write
        if (repaired) {
            updateSettingsFile();
        }
    }

    /**
     * Deletes the settings file if it is from a previous application version so it can be rebuilt for the new
     * application version.
     */
    private void checkSettingsFileVersion() {
        boolean oldSettingsFileVersion = false;
        File settingsFile = new File(getSettingsFilePath());

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
