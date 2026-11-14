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
package com.dhk.controller;

import java.util.ArrayList;
import java.util.List;

import com.dhk.controller.button.AboutButtonController;
import com.dhk.controller.button.AdditionalSettingsButtonController;
import com.dhk.controller.button.ThemeButtonController;
import com.dhk.io.NativeInputHook;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Adds menu button controllers to the menu controller. Each menu button controller is created, and their listeners are
 * initialized through this menu controller.
 *
 * @author Jonathan R. Miller
 */
public class MenuController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private HotKeysController hotKeysController;
    private NativeInputHook nativeInputHook;
    private AdditionalSettingsButtonController additionalSettingsButtonController;
    private AboutButtonController aboutButtonController;
    private List<IController> menuButtonControllers;

    /**
     * Constructor for the {@link MenuController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     * @param hotKeysController
     *            - The controller for hot key management
     * @param nativeInputHook
     *            - The shared native input hook the dialogs use to re-fit after a cross-display drag (may be null)
     */
    public MenuController(DhkModel model, DhkView view, SettingsManager settingsMgr,
            HotKeysController hotKeysController, NativeInputHook nativeInputHook) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
        this.hotKeysController = hotKeysController;
        this.nativeInputHook = nativeInputHook;
    }

    @Override
    public void initController() {
        additionalSettingsButtonController = new AdditionalSettingsButtonController(model, view, settingsMgr,
                hotKeysController, nativeInputHook);
        aboutButtonController = new AboutButtonController(view, nativeInputHook);

        menuButtonControllers = new ArrayList<IController>();
        menuButtonControllers.add(additionalSettingsButtonController);
        menuButtonControllers.add(aboutButtonController);
        menuButtonControllers.add(new ThemeButtonController(model, view, settingsMgr));

        for (IController menuButtonController : menuButtonControllers) {
            menuButtonController.initController();
        }
    }

    @Override
    public void initListeners() {
        for (IController menuButtonController : menuButtonControllers) {
            menuButtonController.initListeners();
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Gets the controller of the Additional Settings button, which also shows the dialog for the tray.
     *
     * @return The Additional Settings button controller
     */
    public AdditionalSettingsButtonController getAdditionalSettingsButtonController() {
        return additionalSettingsButtonController;
    }

    /**
     * Gets the controller of the About button, which also shows the dialog for the tray.
     *
     * @return The About button controller
     */
    public AboutButtonController getAboutButtonController() {
        return aboutButtonController;
    }

}
