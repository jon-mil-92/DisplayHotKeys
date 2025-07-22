package com.dhk.main;

import com.dhk.controller.DhkController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

/**
 * Gets the application's model, view, controller, and settings manager, and then it re-initializes them.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class AppRefresher {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the AppRefresher class.
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
     * and feel" for the GUI.
     */
    public void reInitApp() {
        int previousFrameState = view.getFrame().getExtendedState();

        settingsMgr.initSettingsManager();
        model.initModel(settingsMgr);
        view.reInitView();
        controller.reInitController(previousFrameState);

        if (model.isDarkMode()) {
            FlatDarculaLaf.setup();
        } else {
            FlatIntelliJLaf.setup();
        }

        FlatLaf.updateUI();
    }

}
