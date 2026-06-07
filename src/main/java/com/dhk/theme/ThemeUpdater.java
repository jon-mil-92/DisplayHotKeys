/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
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
     * Constructor for the {@link ThemeUpdater} class.
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
