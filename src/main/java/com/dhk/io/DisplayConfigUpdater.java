package com.dhk.io;

import com.dhk.controller.DhkController;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Provides methods to detect if there was a display configuration change, and if there was, then the settings manager,
 * model, view, and controllers will be re-initialized to reflect the new display configuration.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DisplayConfigUpdater {

    private DhkModel model;
    private DisplayConfig displayConfig;
    private AppRefresher appRefresher;

    /**
     * Constructor for the DisplayConfigRefresher class.
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
