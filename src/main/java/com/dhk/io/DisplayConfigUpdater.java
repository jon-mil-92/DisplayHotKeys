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
package com.dhk.io;

import com.dhk.controller.DhkController;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Provides methods to detect if there was a display configuration change, and if there was, then the settings manager,
 * model, view, and controllers will be re-initialized to reflect the new display configuration.
 *
 * @author Jonathan R. Miller
 */
public class DisplayConfigUpdater {

    private DhkModel model;
    private DisplayConfig displayConfig;
    private AppRefresher appRefresher;

    /**
     * Constructor for the {@link DisplayConfigUpdater} class.
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
    public DisplayConfigUpdater(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;

        displayConfig = new DisplayConfig();
        displayConfig.updateDisplayConfig();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Detects if there was a change in the number of connected displays, and if there was, then the settings manager,
     * model, view, and controllers will be re-initialized to reflect the new display configuration.
     */
    public void checkNumOfConnectedDisplays() {
        displayConfig.checkNumOfConnectedDisplays();

        // If there are connected displays and the number of connected displays has changed
        if (displayConfig.getNumOfConnectedDisplays() != 0
                && displayConfig.getNumOfConnectedDisplays() != model.getNumOfConnectedDisplays()) {
            appRefresher.reInitApp();
        }
    }

}
