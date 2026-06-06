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
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan R. Miller
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
        view.getThemeButton().addActionListener(_ -> themeButtonAction());

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
