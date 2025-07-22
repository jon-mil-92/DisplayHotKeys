package com.dhk.controller.button;

import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.controller.IController;
import com.dhk.io.RunOnStartupManager;

/**
 * Controls the Run On Startup button. Listeners are added to the corresponding view component so that when the Run On
 * Startup button is pressed, the application will toggle the ability for the application to launch on user login.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class RunOnStartupButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private RunOnStartupManager runOnStartupManager;

    /**
     * Constructor for the RunOnStartupButtonController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public RunOnStartupButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    /**
     * Creates a new run on startup manager.
     */
    @Override
    public void initController() {
        runOnStartupManager = new RunOnStartupManager();
    }

    /**
     * Initializes the listeners for the run on startup button.
     */
    @Override
    public void initListeners() {
        view.getRunOnStartupButton().addActionListener(e -> runOnStartupButtonAction());

        initStateChangeListeners(view.getRunOnStartupButton(), view.getDisplayIdsLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Toggles the "run on startup" state, adds or removes a batch file to the startup folder, and then saves the new
     * "run on startup" state.
     */
    private void runOnStartupButtonAction() {
        model.toggleRunOnStartup();
        view.getRunOnStartupButton().setOn(model.isRunOnStartup());

        if (model.isRunOnStartup()) {
            runOnStartupManager.addToStartup();
        } else {
            runOnStartupManager.removeFromStartup();
        }

        settingsMgr.saveIniRunOnStartup(model.isRunOnStartup());
    }

}
