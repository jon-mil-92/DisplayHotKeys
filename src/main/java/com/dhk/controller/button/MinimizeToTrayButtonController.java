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
import com.dhk.view.DhkView;

/**
 * Controls the Minimize To Tray button. Listeners are added to the corresponding view component so that when the
 * Minimize To Tray button is pressed, the application will toggle the ability for the application to minimize to the
 * system tray.
 *
 * @author Jonathan R. Miller
 */
public class MinimizeToTrayButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the {@link MinimizeToTrayButtonController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public MinimizeToTrayButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        view.getMinimizeToTrayButton().addActionListener(e -> minimizeToTrayButtonAction());

        initStateChangeListeners(view.getMinimizeToTrayButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Toggles the "minimize to tray" state and then saves the new "minimize to tray" state.
     */
    private void minimizeToTrayButtonAction() {
        model.toggleMinimizeToTray();
        view.getMinimizeToTrayButton().setOn(model.isMinimizeToTray());
        settingsMgr.saveIniMinimizeToTray(model.isMinimizeToTray());
    }

}
