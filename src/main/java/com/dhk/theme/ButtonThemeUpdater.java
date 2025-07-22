package com.dhk.theme;

import com.dhk.model.DhkModel;
import com.dhk.model.button.ThemeableButton;
import com.dhk.view.DhkView;

/**
 * Sets the theme for all of the themeable buttons in the view. It aids in setting the theme of the application.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ButtonThemeUpdater {

    private DhkModel model;
    private DhkView view;

    /**
     * Constructor for the ButtonThemesUpdater class.
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
        for (ThemeableButton button : view.getThemeableButtons()) {
            button.setDarkMode(model.isDarkMode());
            button.updateIdleIcon();
        }
    }

}
