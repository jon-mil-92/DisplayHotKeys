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
import com.dhk.theme.ThemeUpdater;
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;
import com.dhk.view.OverlayDialog;
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
     * Re-initializes the settings manager, model, view, and controllers for the application, rebuilding the frame and
     * any open dialog inside their existing windows in the theme the settings file holds. The app is only
     * re-initialized if there is an active display to prevent a JVM error from occurring.
     */
    public void reInitApp() {
        displayConfig.updateConnectedDisplays();

        if (displayConfig.getNumOfConnectedDisplays() == 0) {
            return;
        }

        try {
            controller.cleanUp();
            settingsMgr.initSettingsManager();
            model.initModel(settingsMgr);

            // A theme changed in the settings file is applied before the view is rebuilt, so new content starts in it
            if (model.isDarkMode() != FlatLaf.isLafDark()) {
                new ThemeUpdater().useDarkMode(model.isDarkMode());
            }

            view.reInitView();
            controller.reInitController();

            // The open dialog is rebuilt after the controllers, so its content wires to the ones that replaced them
            OverlayDialog.rebuildShownDialog();
        } finally {
            // Each window is re-fitted once to keep its placement, even after a failed step so none is left unlaid-out
            WindowUtil.refitRebuiltWindows();
        }

        // Place the tray icon and any display info panels last, against the settled displays
        controller.displayConfigurationSettled();
    }

}
