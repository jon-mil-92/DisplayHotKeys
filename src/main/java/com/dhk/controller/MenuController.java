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
import com.dhk.controller.button.ClearAllButtonController;
import com.dhk.controller.button.ExitButtonController;
import com.dhk.controller.button.MinimizeButtonController;
import com.dhk.controller.button.MinimizeToTrayButtonController;
import com.dhk.controller.button.RefreshAppButtonController;
import com.dhk.controller.button.RunOnStartupButtonController;
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
    private DhkController controller;
    private SettingsManager settingsMgr;
    private List<IController> menuButtonControllers;

    /**
     * Constructor for the {@link MenuController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param controller
     *            - The controller for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public MenuController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        menuButtonControllers = new ArrayList<IController>();
        menuButtonControllers.add(new AboutButtonController(model, view));
        menuButtonControllers.add(new ThemeButtonController(model, view, settingsMgr));
        menuButtonControllers.add(new MinimizeToTrayButtonController(model, view, settingsMgr));
        menuButtonControllers.add(new RunOnStartupButtonController(model, view, settingsMgr));
        menuButtonControllers.add(new RefreshAppButtonController(model, view, controller, settingsMgr));
        menuButtonControllers.add(new ClearAllButtonController(model, view, controller, settingsMgr));
        menuButtonControllers.add(new MinimizeButtonController(view));
        menuButtonControllers.add(new ExitButtonController(view));

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

}
