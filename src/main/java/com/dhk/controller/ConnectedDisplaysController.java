package com.dhk.controller;

import com.dhk.io.ConnectedDisplaysPoller;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the active display configuration. A connected displays poller is started to check for changes in the number
 * of connected displays.
 *
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ConnectedDisplaysController implements IController {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private ConnectedDisplaysPoller connectedDisplaysPoller;

    // Poll for the number of connected displays every 1000 ms
    private final int POLL_INTERVAL = 1000;

    /**
     * Constructor for the ConnectedDisplaysController class.
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
    public ConnectedDisplaysController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * Creates and starts a new connected display poller to check for display configuration changes.
     */
    @Override
    public void initController() {
        connectedDisplaysPoller = new ConnectedDisplaysPoller(model, view, controller, settingsMgr, POLL_INTERVAL);
        connectedDisplaysPoller.start();
    }

    @Override
    public void initListeners() {
    }

    /**
     * Stops polling for the number of connected displays.
     */
    @Override
    public void cleanUp() {
        connectedDisplaysPoller.stop();
    }

}
