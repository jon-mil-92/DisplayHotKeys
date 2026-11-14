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

import com.dhk.model.DhkModel;
import com.dhk.model.button.Button;
import com.dhk.model.button.ThemeableButton;
import com.dhk.view.DhkView;

/**
 * Sets the theme for all of the themeable buttons in the view. It aids in setting the theme of the application.
 *
 * @author Jonathan R. Miller
 */
public class ButtonThemeUpdater {

    private DhkModel model;
    private DhkView view;

    /**
     * Constructor for the {@link ButtonThemeUpdater} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public ButtonThemeUpdater(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
    }

    /**
     * Applies the current theme to all of the themeable buttons in the view.
     */
    public void updateButtonThemes() {
        for (Button button : view.getButtons()) {
            if (button instanceof ThemeableButton themeableButton) {
                themeableButton.setDarkMode(model.isDarkMode());
                themeableButton.updateIdleIcon();
            }
        }
    }

}
