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
package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.theme.ButtonThemeUpdater;
import com.dhk.theme.ThemeUpdater;
import com.dhk.view.DhkView;
import com.dhk.view.FrameUpdater;

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
    private FrameUpdater frameUpdater;

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
        frameUpdater = new FrameUpdater(view);
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
        frameUpdater.updateUI();
    }

}
