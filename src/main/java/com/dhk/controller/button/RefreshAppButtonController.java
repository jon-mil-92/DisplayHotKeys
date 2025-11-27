package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.controller.DhkController;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Refresh App button. Listeners are added to the corresponding view component so that when the Refresh App
 * button is pressed, the application is re-initialized.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class RefreshAppButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private AppRefresher appRefresher;

    /**
     * Constructor for the RefreshAppButtonController class.
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
    public RefreshAppButtonController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * Creates a new app refresher.
     */
    @Override
    public void initController() {
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Initializes the listeners for the refresh app button.
     */
    @Override
    public void initListeners() {
        view.getRefreshAppButton().addActionListener(e -> refreshAppButtonAction());

        initStateChangeListeners(view.getRefreshAppButton(), view.getSelectedDisplayLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Refreshes the app to reflect any changes in the display configuration.
     */
    private void refreshAppButtonAction() {
        appRefresher.reInitApp();
    }

}
