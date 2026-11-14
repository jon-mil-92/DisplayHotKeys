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
package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.theme.ButtonThemeUpdater;
import com.dhk.theme.ThemeUpdater;
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;

/**
 * Controls the Theme button. Listeners are added to the corresponding view component so that when the Theme button is
 * pressed, the application's theme will be toggled between "Light" and "Dark."
 *
 * @author Jonathan R. Miller
 */
public class ThemeButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private ThemeUpdater themeUpdater;
    private ButtonThemeUpdater buttonThemesUpdater;

    /**
     * Constructor for the {@link ThemeButtonController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public ThemeButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        themeUpdater = new ThemeUpdater();
        buttonThemesUpdater = new ButtonThemeUpdater(model, view);
    }

    @Override
    public void initListeners() {
        view.getThemeButton().addActionListener(e -> themeButtonAction());

        initStateChangeListeners(view.getThemeButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Toggles the "dark mode" state and update the UI, and then save the new settings.
     */
    private void themeButtonAction() {
        model.toggleDarkMode();
        themeUpdater.useDarkMode(model.isDarkMode());
        buttonThemesUpdater.updateButtonThemes();
        settingsMgr.saveIniDarkMode(model.isDarkMode());
        WindowUtil.refreshWindow(view.getFrame());
    }

}
