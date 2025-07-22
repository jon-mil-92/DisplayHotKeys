package com.dhk.theme;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

/**
 * Sets the theme for the application. It allows the theme of the application to be switched between Light and Dark
 * themes.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ThemeUpdater {

    /**
     * Constructor for the ThemeUpdater class.
     */
    public ThemeUpdater() {
        // Set the Roboto font style
        FlatRobotoFont.install();
        FlatLaf.setPreferredFontFamily(FlatRobotoFont.FAMILY);

        // Set the package for theme properties
        FlatLaf.registerCustomDefaultsSource("com.dhk.theme");
    }

    /**
     * Updates the "look and feel" of the app.
     * 
     * @param darkMode
     *            - Whether or not the dark mode "look and feel" should be applied or not
     */
    public void useDarkMode(boolean darkMode) {
        FlatAnimatedLafChange.showSnapshot();

        if (darkMode) {
            // Apply the dark theme
            FlatDarculaLaf.setup();
        } else {
            // Apply the light theme
            FlatIntelliJLaf.setup();
        }

        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

}
