package com.dhk.main;

import com.dhk.controllers.DhkController;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

/**
 * This class is an app refresher that gets the application's model, view, controller, and settings manager, and then it
 * re-initializes them.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class AppRefresher {
    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the AppRefresher class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public AppRefresher(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        // Get the application's model, view, controller, and settings manager.
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method re-initializes the settings manager, model, view, and controllers for the application.
     */
    public void reInitApp() {
        // Get the state of the frame so it can be used for re-initialization.
        int previousFrameState = view.getFrame().getExtendedState();

        // Re-initialize the settings manager.
        settingsMgr.initSettingsManager();

        // Re-initialize the model.
        model.initModel(settingsMgr);

        // Re-initialize the view.
        view.reInitView();

        // Re-initialize the controllers.
        controller.reInitController(previousFrameState);

        // Set up the "look and feel" for the GUI.
        if (model.isDarkMode()) {
            // Apply the dark "look and feel" for the GUI.
            FlatDarculaLaf.setup();

            // Update the UI after changing the theme.
            FlatLaf.updateUI();
        } else {
            // Apply the light "look and feel" for the GUI.
            FlatIntelliJLaf.setup();

            // Update the UI after changing the theme.
            FlatLaf.updateUI();
        }
    }
}
