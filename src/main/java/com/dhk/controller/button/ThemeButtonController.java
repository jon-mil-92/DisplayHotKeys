package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.theme.ButtonThemeUpdater;
import com.dhk.theme.ThemeUpdater;
import com.dhk.view.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * Controls the Theme button. Listeners are added to the corresponding view component so that when the Theme button is
 * pressed, the application's theme will be toggled between "Light" and "Dark."
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ThemeButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private ThemeUpdater themeUpdater;
    private ButtonThemeUpdater buttonThemesUpdater;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the ThemeButtonController class.
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

    /**
     * Creates a new theme chooser, button themes updater, and frame updater.
     */
    @Override
    public void initController() {
        themeUpdater = new ThemeUpdater();
        buttonThemesUpdater = new ButtonThemeUpdater(model, view);
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * Initializes the listeners for the theme button.
     */
    @Override
    public void initListeners() {
        view.getThemeButton().addActionListener(e -> themeButtonAction());

        initStateChangeListeners(view.getThemeButton(), view.getSelectedDisplayLabel());
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
