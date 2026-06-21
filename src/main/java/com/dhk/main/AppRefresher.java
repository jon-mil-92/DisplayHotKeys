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
package com.dhk.main;

import com.dhk.controller.DhkController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.model.FramePlacement;
import com.dhk.view.DhkView;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

/**
 * Gets the application's model, view, controller, and settings manager, and then it re-initializes them.
 *
 * @author Jonathan R. Miller
 */
public class AppRefresher {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the {@link AppRefresher} class.
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
    public AppRefresher(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * Re-initializes the settings manager, model, view, and controllers for the application, and then sets up the "look
     * and feel" for the GUI. The view captures its own frame placement during re-initialization.
     */
    public void reInitApp() {
        reInitApp(null);
    }

    /**
     * Re-initializes the settings manager, model, view, and controllers for the application, and then sets up the "look
     * and feel" for the GUI.
     *
     * @param capturedPlacement
     *            - A frame placement captured before a display reconfiguration, used to reproduce the frame's position;
     *            or null to have the view capture the placement live during re-initialization. Callers that change the
     *            display geometry (resolution or DPI scale) must capture before applying, because the OS relocates the
     *            existing frame as part of the change, making a live capture during re-initialization unreliable
     */
    public void reInitApp(FramePlacement capturedPlacement) {
        int previousFrameState = view.getFrame().getExtendedState();

        settingsMgr.initSettingsManager();
        model.initModel(settingsMgr);
        view.reInitView(capturedPlacement);
        controller.reInitController(previousFrameState);

        if (model.isDarkMode()) {
            FlatDarculaLaf.setup();
        } else {
            FlatIntelliJLaf.setup();
        }

        FlatLaf.updateUI();

        // Clean up memory after re-initializing the app
        System.gc();
    }

}
