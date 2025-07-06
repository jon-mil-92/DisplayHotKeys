package com.dhk.controllers;

import com.dhk.io.ConnectedDisplaysPoller;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the active display configuration. A connected displays poller is started to check for changes in
 * the number of connected displays.
 *
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ConnectedDisplaysController implements Controller {
    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private ConnectedDisplaysPoller connectedDisplaysPoller;

    // Poll for the number of connected displays every 1000 ms.
    private final int POLL_INTERVAL = 1000;

    /**
     * Constructor for the ConnectedDisplaysController class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public ConnectedDisplaysController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        // Get the application's model, view, controller, and settings manager.
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates and starts a new connected display poller to check for display configuration changes.
     */
    @Override
    public void initController() {
        // Initialize and start the object that will poll for number of connected displays.
        connectedDisplaysPoller = new ConnectedDisplaysPoller(model, view, controller, settingsMgr, POLL_INTERVAL);
        connectedDisplaysPoller.start();
    }

    @Override
    public void initListeners() {
    }

    /**
     * Stop polling for the number of connected displays.
     */
    @Override
    public void cleanUp() {
        connectedDisplaysPoller.stop();
    }
}
