package com.dhk.io;

import com.dhk.controllers.DhkController;
import com.dhk.main.AppRefresher;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class provides methods to detect if there was a display configuration change, and if there was, then the
 * settings manager, model, view, and controllers will be re-initialized to reflect the new display configuration.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class DisplayConfigUpdater {
    private DhkModel model;
    private DisplayConfig displayConfig;
    private AppRefresher appRefresher;

    /**
     * Constructor for the DisplayConfigRefresher class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public DisplayConfigUpdater(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        // Get the application's model.
        this.model = model;

        // Initialize the object that will get the current display configuration.
        displayConfig = new DisplayConfig();
        displayConfig.updateDisplayConfig();

        // Initialize the object that will re-initialize the application.
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * This method detects if there was a change in the number of connected displays, and if there was, then the
     * settings manager, model, view, and controllers will be re-initialized to reflect the new display configuration.
     */
    public void checkNumOfConnectedDisplays() {
        // Get the current number of connected displays.
        displayConfig.checkNumOfConnectedDisplays();

        // If there are connected displays and the number of connected displays has changed...
        if (displayConfig.getNumOfConnectedDisplays() != 0
                && displayConfig.getNumOfConnectedDisplays() != model.getNumOfConnectedDisplays()) {
            // Re-initialize the app to reflect the new display configuration.
            appRefresher.reInitApp();
        }
    }
}
