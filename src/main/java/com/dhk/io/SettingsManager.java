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
     * Identifier the installer writes on every install, read once at launch so every later validation can record it
     * without reading the installer's file again, or null when no installer set this copy up.
     */
    private static String installId;

    /**
     * Most slots each display can show in the slot table.
     */
    private static final int MAX_NUM_OF_SLOTS = 12;

    /**
     * System property holding the user's home folder, which the settings file path starts from.
     */
    private static final String USER_HOME_SYSTEM_PROPERTY = "user.home";

    /**
     * Path of the settings file below the user's home folder, so every Windows account keeps its own settings.
     */
    private static final String SETTINGS_FILE_HOME_PATH = "\\Documents\\DisplayHotKeys\\settings.ini";

    /**
     * Section line found only in the settings files of early versions, whose format is rebuilt from scratch.
     */
    private static final String OLD_FORMAT_SECTION_LINE = "[Slot1]";

    /**
     * System property the packaged launcher sets to the application executable's path, which a development run lacks.
     */
    private static final String APP_PATH_SYSTEM_PROPERTY = "jpackage.app-path";

    /**
     * File beside the installed application where the installer records the default language picked during setup.
     */
    private static final String INSTALLER_DEFAULTS_FILE_NAME = "defaults.ini";

    /**
     * Installer defaults property holding the tag of the language picked during setup.
     */
    private static final String DEFAULT_LANGUAGE_PROPERTY = "defaultLanguage";

    /**
     * Section for the properties that apply to the whole application, in the settings file and the installer defaults.
     */
    static final String APPLICATION_SECTION = "Application";

    /**
     * Application property holding whether the dark theme is shown.
     */
    static final String DARK_MODE_PROPERTY = "darkMode";

    /**
     * Application property holding the tag of the language the application is showing.
     */
    static final String LANGUAGE_PROPERTY = "language";

    /**
     * Property holding an install's identifier, which the installer writes on every install and each account records
     * once it adopts that install's language.
     */
    static final String INSTALL_ID_PROPERTY = "installId";

    /**
     * Application property holding whether minimizing the window hides the application in the system tray.
     */
    static final String MINIMIZE_TO_TRAY_PROPERTY = "minimizeToTray";

    /**
     * Application property holding whether closing the window hides the application in the system tray.
     */
    static final String CLOSE_TO_TRAY_PROPERTY = "closeToTray";

    /**
     * Application property holding whether the application starts upon login.
     */
    static final String RUN_ON_STARTUP_PROPERTY = "runOnStartup";

    /**
     * Application property holding whether the info overlay shows after every slot apply and display change.
     */
    static final String AUTO_SHOW_INFO_OVERLAY_PROPERTY = "autoShowInfoOverlay";

    /**
     * Application property holding where the info overlay sits on each display, as one of the overlay's positions.
     */
    static final String INFO_OVERLAY_POSITION_PROPERTY = "infoOverlayPosition";

    /**
     * Application property holding the percentage the info overlay's fonts and spacing are scaled by.
     */
    static final String INFO_OVERLAY_SCALE_PROPERTY = "infoOverlayScale";

    /**
     * Smallest percentage the info overlay can be scaled by, at which it is drawn at its unscaled size.
     */
    public static final int MIN_INFO_OVERLAY_SCALE = 100;

    /**
     * Largest percentage the info overlay can be scaled by.
     */
    public static final int MAX_INFO_OVERLAY_SCALE = 200;

    /**
     * Difference between neighboring info overlay scales, which every saved scale is a multiple of.
     */
    public static final int INFO_OVERLAY_SCALE_STEP = 10;

    /**
     * Application property holding the opacity percentage of the info overlay's panels.
     */
    static final String INFO_OVERLAY_OPACITY_PROPERTY = "infoOverlayOpacity";

    /**
     * Smallest opacity percentage of the info overlay, at which its panels are invisible.
     */
    public static final int MIN_INFO_OVERLAY_OPACITY = 0;

    /**
     * Largest opacity percentage of the info overlay, at which its panels are fully opaque.
     */
    public static final int MAX_INFO_OVERLAY_OPACITY = 100;

    /**
     * Difference between neighboring info overlay opacities, which every saved opacity is a multiple of.
     */
    public static final int INFO_OVERLAY_OPACITY_STEP = 10;

    /**
     * Application property holding the number of seconds the info overlay stays visible.
     */
    static final String INFO_OVERLAY_DURATION_PROPERTY = "infoOverlayDuration";

    /**
     * Fewest seconds the info overlay can stay visible.
     */
    public static final int MIN_INFO_OVERLAY_DURATION = 1;

    /**
     * Most seconds the info overlay can stay visible.
     */
    public static final int MAX_INFO_OVERLAY_DURATION = 20;

    /**
     * Start of the application property holding a display's number of active slots, which the display's ID completes.
     */
    private static final String NUM_OF_SLOTS_PROPERTY_PREFIX = "numOfSlotsFor--";

    /**
     * Text between a display's ID and a slot's number in the name of that slot's section.
     */
    private static final String SLOT_SECTION_INFIX = "--Slot";

    /**
     * Slot property holding the width of the slot's resolution.
     */
    static final String DISPLAY_MODE_WIDTH_PROPERTY = "displayModeWidth";

    /**
     * Slot property holding the height of the slot's resolution.
     */
    static final String DISPLAY_MODE_HEIGHT_PROPERTY = "displayModeHeight";

    /**
     * Slot property holding the numerator of the slot's exact refresh rate.
     */
    static final String DISPLAY_MODE_REFRESH_NUMERATOR_PROPERTY = "displayModeRefreshNumerator";

    /**
     * Slot property holding the denominator of the slot's exact refresh rate.
     */
    static final String DISPLAY_MODE_REFRESH_DENOMINATOR_PROPERTY = "displayModeRefreshDenominator";

    /**
     * Slot property holding the slot's DPI scale percentage.
     */
    static final String DPI_SCALE_PERCENTAGE_PROPERTY = "dpiScalePercentage";

    /**
     * Slot property holding the slot's scaling mode code.
     */
    static final String SCALING_MODE_PROPERTY = "scalingMode";

    /**
     * Slot property holding the slot's orientation mode code.
     */
    static final String ORIENTATION_MODE_PROPERTY = "orientationMode";

    /**
     * Hot key property holding the number of keys the hot key uses.
     */
    static final String HOT_KEY_SIZE_PROPERTY = "hotKeySize";

    /**
     * Start of a hot key property holding one key's code, which the key's 1-based number completes.
     */
    private static final String KEY_PROPERTY_PREFIX = "key";

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
        return ini.get(APPLICATION_SECTION, DARK_MODE_PROPERTY, boolean.class);
    }

    /**
     * Sets the dark mode property value in the settings file object.
     *
     * @param darkMode
     *            - The new value for the dark mode property
     */
    public void saveIniDarkMode(boolean darkMode) {
        ini.put(APPLICATION_SECTION, DARK_MODE_PROPERTY, darkMode);

        updateSettingsFile();
    }

    /**
     * Reads only the saved dark mode preference straight from the settings file, without initializing displays or
     * validating the file, so an early-exit path can match the app theme cheaply.
     *
     * @return The saved dark mode value, or false when it cannot be read
     */
    public static boolean getSavedDarkMode() {
        return Boolean.parseBoolean(readApplicationSettings(new File(getSettingsFilePath())).get(DARK_MODE_PROPERTY));
    }

    /**
     * Sets the language property value in the settings file object.
     *
     * @param language
     *            - The new value for the language property
     */
    public void saveIniLanguage(Language language) {
        ini.put(APPLICATION_SECTION, LANGUAGE_PROPERTY, language.getTag());

        updateSettingsFile();
    }

    /**
     * Picks the language to show at launch, which is the installer's language after a new install or when none is
     * saved, then the saved language, and otherwise the Windows display language. It reads the files directly, since
     * the language is needed before anything is shown.
     *
     * @return The language to show at launch
     */
    public static Language getStartupLanguage() {
        Map<String, String> savedSettings = readApplicationSettings(new File(getSettingsFilePath()));
        Map<String, String> installerDefaults = Map.of();

        // Only the packaged launcher sets the executable path, and the installer writes its defaults beside it
        String appExePath = System.getProperty(APP_PATH_SYSTEM_PROPERTY);

        if (appExePath != null) {
            installerDefaults = readApplicationSettings(
                    new File(new File(appExePath).getParentFile(), INSTALLER_DEFAULTS_FILE_NAME));
        }

        Language savedLanguage = Language.fromTag(savedSettings.get(LANGUAGE_PROPERTY));
        Language installerLanguage = Language.fromTag(installerDefaults.get(DEFAULT_LANGUAGE_PROPERTY));
        installId = installerDefaults.get(INSTALL_ID_PROPERTY);

        // Each account records the install whose language it adopted, so an install it has not recorded is a new one
        boolean newInstall = installId != null && !installId.equals(savedSettings.get(INSTALL_ID_PROPERTY));

        if (installerLanguage != null && (newInstall || savedLanguage == null)) {
            return installerLanguage;
        }

        if (savedLanguage != null) {
            return savedLanguage;
        }

        return Language.fromLocale(Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * Gets the identifier the installer wrote on its latest install, as read at launch.
     *
     * @return The identifier of the latest install, or null when no installer set this copy up
     */
    static String getInstallId() {
        return installId;
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
     * Reads the application section straight from an ini file, so a caller that needs several of its properties parses
     * the file only once.
     *
     * @param iniFile
     *            - The ini file to read
     *
     * @return The properties of the application section, empty when the file or section is missing or unreadable
     */
    private static Map<String, String> readApplicationSettings(File iniFile) {
        if (!iniFile.exists()) {
            return Map.of();
        }

        try {
            Map<String, String> applicationSettings = new Wini(iniFile).get(APPLICATION_SECTION);

            return applicationSettings != null ? applicationSettings : Map.of();
        } catch (IOException e) {
            e.printStackTrace();

            return Map.of();
        }
    }

    /**
     * Gets the minimize to tray property value from the settings file object.
     *
     * @return The value for the minimize to tray property
     */
    public boolean getIniMinimizeToTray() {
        return ini.get(APPLICATION_SECTION, MINIMIZE_TO_TRAY_PROPERTY, boolean.class);
    }

    /**
     * Sets the minimize to tray property value in the settings file object.
     *
     * @param minimizeToTray
     *            - The new value for the minimize to tray property
     */
    public void saveIniMinimizeToTray(boolean minimizeToTray) {
        ini.put(APPLICATION_SECTION, MINIMIZE_TO_TRAY_PROPERTY, minimizeToTray);

        updateSettingsFile();
    }

    /**
     * Gets the close to tray property value from the settings file object.
     *
     * @return The value for the close to tray property
     */
    public boolean getIniCloseToTray() {
        return ini.get(APPLICATION_SECTION, CLOSE_TO_TRAY_PROPERTY, boolean.class);
    }

    /**
     * Sets the close to tray property value in the settings file object.
     *
     * @param closeToTray
     *            - The new value for the close to tray property
     */
    public void saveIniCloseToTray(boolean closeToTray) {
        ini.put(APPLICATION_SECTION, CLOSE_TO_TRAY_PROPERTY, closeToTray);

        updateSettingsFile();
    }

    /**
     * Gets the run on startup property value from the settings file object.
     *
     * @return The value for the run on startup property
     */
    public boolean getIniRunOnStartup() {
        return ini.get(APPLICATION_SECTION, RUN_ON_STARTUP_PROPERTY, boolean.class);
    }

    /**
     * Sets the run on startup property value in the settings file object.
     *
     * @param runOnStartup
     *            - The new value for the run on startup property
     */
    public void saveIniRunOnStartup(boolean runOnStartup) {
        ini.put(APPLICATION_SECTION, RUN_ON_STARTUP_PROPERTY, runOnStartup);

        updateSettingsFile();
    }

    /**
     * Gets the auto show info overlay property value from the settings file object.
     *
     * @return The value for the auto show info overlay property
     */
    public boolean getIniAutoShowInfoOverlay() {
        return ini.get(APPLICATION_SECTION, AUTO_SHOW_INFO_OVERLAY_PROPERTY, boolean.class);
    }

    /**
     * Sets the auto show info overlay property value in the settings file object.
     *
     * @param autoShowInfoOverlay
     *            - The new value for the auto show info overlay property
     */
    public void saveIniAutoShowInfoOverlay(boolean autoShowInfoOverlay) {
        ini.put(APPLICATION_SECTION, AUTO_SHOW_INFO_OVERLAY_PROPERTY, autoShowInfoOverlay);

        updateSettingsFile();
    }

    /**
     * Gets the info overlay position property value from the settings file object.
     *
     * @return The value for the info overlay position property
     */
    public int getIniInfoOverlayPosition() {
        return ini.get(APPLICATION_SECTION, INFO_OVERLAY_POSITION_PROPERTY, int.class);
    }

    /**
     * Sets the info overlay position property value in the settings file object.
     *
     * @param infoOverlayPosition
     *            - The new value for the info overlay position property
     */
    public void saveIniInfoOverlayPosition(int infoOverlayPosition) {
        ini.put(APPLICATION_SECTION, INFO_OVERLAY_POSITION_PROPERTY, infoOverlayPosition);

        updateSettingsFile();
    }

    /**
     * Gets the info overlay scale property value from the settings file object.
     *
     * @return The value for the info overlay scale property
     */
    public int getIniInfoOverlayScale() {
        return ini.get(APPLICATION_SECTION, INFO_OVERLAY_SCALE_PROPERTY, int.class);
    }

    /**
     * Sets the info overlay scale property value in the settings file object.
     *
     * @param infoOverlayScale
     *            - The new value for the info overlay scale property
     */
    public void saveIniInfoOverlayScale(int infoOverlayScale) {
        ini.put(APPLICATION_SECTION, INFO_OVERLAY_SCALE_PROPERTY, infoOverlayScale);

        updateSettingsFile();
    }

    /**
     * Gets the info overlay opacity property value from the settings file object.
     *
     * @return The value for the info overlay opacity property
     */
    public int getIniInfoOverlayOpacity() {
        return ini.get(APPLICATION_SECTION, INFO_OVERLAY_OPACITY_PROPERTY, int.class);
    }

    /**
     * Sets the info overlay opacity property value in the settings file object.
     *
     * @param infoOverlayOpacity
     *            - The new value for the info overlay opacity property
     */
    public void saveIniInfoOverlayOpacity(int infoOverlayOpacity) {
        ini.put(APPLICATION_SECTION, INFO_OVERLAY_OPACITY_PROPERTY, infoOverlayOpacity);

        updateSettingsFile();
    }

    /**
     * Gets the info overlay duration property value from the settings file object.
     *
     * @return The value for the info overlay duration property
     */
    public int getIniInfoOverlayDuration() {
        return ini.get(APPLICATION_SECTION, INFO_OVERLAY_DURATION_PROPERTY, int.class);
    }

    /**
     * Sets the info overlay duration property value in the settings file object.
     *
     * @param infoOverlayDuration
     *            - The new value for the info overlay duration property
     */
    public void saveIniInfoOverlayDuration(int infoOverlayDuration) {
        ini.put(APPLICATION_SECTION, INFO_OVERLAY_DURATION_PROPERTY, infoOverlayDuration);

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
        return ini.get(APPLICATION_SECTION, numOfSlotsProperty(displayId), int.class);
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
        ini.put(APPLICATION_SECTION, numOfSlotsProperty(displayId), numOfSlots);

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
        String iniSection = slotSection(displayId, slotId);

        /*
         * The validator writes the exact refresh rate as a numerator/denominator pair before the model is built (a slot
         * missing it, such as a pre-fractional one, is reset to the default), so both are present here
         */
        DisplayMode slotDisplayMode = new DisplayMode(ini.get(iniSection, DISPLAY_MODE_WIDTH_PROPERTY, int.class),
                ini.get(iniSection, DISPLAY_MODE_HEIGHT_PROPERTY, int.class),
                ini.get(iniSection, DISPLAY_MODE_REFRESH_NUMERATOR_PROPERTY, int.class),
                ini.get(iniSection, DISPLAY_MODE_REFRESH_DENOMINATOR_PROPERTY, int.class));

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
        String iniSection = slotSection(displayId, slotId);

        ini.put(iniSection, DISPLAY_MODE_WIDTH_PROPERTY, displayMode.getWidth());
        ini.put(iniSection, DISPLAY_MODE_HEIGHT_PROPERTY, displayMode.getHeight());
        ini.put(iniSection, DISPLAY_MODE_REFRESH_NUMERATOR_PROPERTY, displayMode.getRefreshNumerator());
        ini.put(iniSection, DISPLAY_MODE_REFRESH_DENOMINATOR_PROPERTY, displayMode.getRefreshDenominator());

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
        String iniSection = slotSection(displayId, slotId);

        return ini.get(iniSection, DPI_SCALE_PERCENTAGE_PROPERTY, int.class);
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
        String iniSection = slotSection(displayId, slotId);
        ini.put(iniSection, DPI_SCALE_PERCENTAGE_PROPERTY, dpiScalePercentage);

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
        String iniSection = slotSection(displayId, slotId);

        return ini.get(iniSection, SCALING_MODE_PROPERTY, int.class);
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
        String iniSection = slotSection(displayId, slotId);
        ini.put(iniSection, SCALING_MODE_PROPERTY, scalingMode);

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
        String iniSection = slotSection(displayId, slotId);

        return ini.get(iniSection, ORIENTATION_MODE_PROPERTY, int.class);
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
        String iniSection = slotSection(displayId, slotId);
        ini.put(iniSection, ORIENTATION_MODE_PROPERTY, orientationMode);

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
        return getIniHotKey(slotSection(displayId, slotId));
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
        saveIniHotKey(slotSection(displayId, slotId), hotKey);
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
        int hotKeySize = ini.get(iniSection, HOT_KEY_SIZE_PROPERTY, int.class);

        for (int keyId = 1; keyId <= hotKeySize; keyId++) {
            int keyCode = ini.get(iniSection, keyProperty(keyId), int.class);
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
        ini.put(iniSection, HOT_KEY_SIZE_PROPERTY, hotKeySize);

        for (int keyIndex = 0; keyIndex < 3; keyIndex++) {
            // Only update key codes for the active keys in the hot key
            if (keyIndex < hotKeySize) {
                int keyCode = hotKey.getKeys().get(keyIndex).getKey();
                ini.put(iniSection, keyProperty(keyIndex + 1), keyCode);
            } else {
                ini.put(iniSection, keyProperty(keyIndex + 1), 0);
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
     * Builds the name of the section that holds a slot's properties.
     *
     * @param displayId
     *            - The ID of the display the slot belongs to
     * @param slotId
     *            - The 1-based number of the slot
     *
     * @return The name of the slot's section
     */
    static String slotSection(String displayId, int slotId) {
        return displayId + SLOT_SECTION_INFIX + slotId;
    }

    /**
     * Builds the name of the application property that holds a display's number of active slots.
     *
     * @param displayId
     *            - The ID of the display
     *
     * @return The name of the display's number of slots property
     */
    static String numOfSlotsProperty(String displayId) {
        return NUM_OF_SLOTS_PROPERTY_PREFIX + displayId;
    }

    /**
     * Builds the name of the hot key property that holds one key's code.
     *
     * @param keyId
     *            - The 1-based number of the key in the hot key
     *
     * @return The name of the key's property
     */
    static String keyProperty(int keyId) {
        return KEY_PROPERTY_PREFIX + keyId;
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
        return System.getProperty(USER_HOME_SYSTEM_PROPERTY) + SETTINGS_FILE_HOME_PATH;
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
                    if (line.equals(OLD_FORMAT_SECTION_LINE)) {
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
