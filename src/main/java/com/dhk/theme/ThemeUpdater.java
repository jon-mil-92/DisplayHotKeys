package com.dhk.theme;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

/**
 * This class sets the theme for the application. It allows the theme of the application to be switched between Light
 * and Dark themes.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ThemeUpdater {

    /**
     * Constructor for the ThemeUpdater class.
     */
    public ThemeUpdater() {
        // Install the Roboto font family.
        FlatRobotoFont.install();

        // Enable the Roboto font family.
        FlatLaf.setPreferredFontFamily(FlatRobotoFont.FAMILY);

        // Set the package for theme properties.
        FlatLaf.registerCustomDefaultsSource("com.dhk.theme");
    }

    /**
     * This method updates the "look and feel" of the app.
     * 
     * @param darkMode - Whether or not the dark mode "look and feel" should be applied or not.
     */
    public void useDarkMode(boolean darkMode) {
        // If the user selects dark mode...
        if (darkMode) {
            // Set the snapshot that will fade to the new UI.
            FlatAnimatedLafChange.showSnapshot();

            // Apply the dark "look and feel" for the GUI.
            FlatDarculaLaf.setup();

            // Update the UI after changing the theme.
            FlatLaf.updateUI();

            // Fade the snapshot of the old UI to the new UI.
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } else {
            // Set the snapshot that will fade to the new UI.
            FlatAnimatedLafChange.showSnapshot();

            // Apply the light "look and feel" for the GUI.
            FlatIntelliJLaf.setup();

            // Update the UI after changing the theme.
            FlatLaf.updateUI();

            // Fade the snapshot of the old UI to the new UI.
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }
}
