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
package com.dhk.controller;

import java.util.ArrayList;
import java.util.List;

import com.dhk.controller.button.AboutButtonController;
import com.dhk.controller.button.AdditionalSettingsButtonController;
import com.dhk.controller.button.ThemeButtonController;
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
    private AdditionalSettingsButtonController additionalSettingsButtonController;
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
     */
    public MenuController(DhkModel model, DhkView view, SettingsManager settingsMgr,
            HotKeysController hotKeysController) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
        this.hotKeysController = hotKeysController;
    }

    @Override
    public void initController() {
        additionalSettingsButtonController = new AdditionalSettingsButtonController(model, view, settingsMgr,
                hotKeysController);

        menuButtonControllers = new ArrayList<IController>();
        menuButtonControllers.add(additionalSettingsButtonController);
        menuButtonControllers.add(new AboutButtonController(model, view));
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

}
