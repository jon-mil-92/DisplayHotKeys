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
 * @author Jonathan R. Miller
 */
public class ThemeUpdater {

    /**
     * Whether the JVM-wide theme setup has run, since repeating its expensive font install on every construction is
     * wasted work.
     */
    private static boolean globalThemeSetupDone;

    /**
     * Constructor for the {@link ThemeUpdater} class.
     */
    public ThemeUpdater() {
        // The font install and defaults-source registration are global and idempotent, so only run them once per JVM
        if (globalThemeSetupDone) {
            return;
        }

        globalThemeSetupDone = true;

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
