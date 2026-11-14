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
package com.dhk.main;

import com.dhk.controller.DhkController;
import com.dhk.io.DisplayConfig;
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
    private DisplayConfig displayConfig;

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

        displayConfig = settingsMgr.getDisplayConfig();
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
     * and feel" for the GUI. The app is only re-initialized if there is an active display to prevent a JVM error from
     * occurring.
     *
     * @param capturedPlacement
     *            - A frame placement captured before a display reconfiguration, used to reproduce the frame's position;
     *            or null to have the view capture the placement live during re-initialization. Callers that change the
     *            display geometry (resolution or DPI scale) must capture before applying, because the OS relocates the
     *            existing frame as part of the change, making a live capture during re-initialization unreliable
     */
    public void reInitApp(FramePlacement capturedPlacement) {
        displayConfig.updateConnectedDisplays();

        if (displayConfig.getNumOfConnectedDisplays() == 0) {
            return;
        }

        int previousFrameState = view.getFrame().getExtendedState();

        controller.cleanUp();
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

        // Place the tray icon and any display info panels last, against the settled displays and the new theme
        controller.displayConfigurationSettled();
    }

}
