package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Minimize To Tray button. Listeners are added to the corresponding view component so that when the
 * Minimize To Tray button is pressed, the application will toggle the ability for the application to minimize to the
 * system tray.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class MinimizeToTrayButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the MinimizeToTrayButtonController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public MinimizeToTrayButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the minimize to tray button.
     */
    @Override
    public void initListeners() {
        view.getMinimizeToTrayButton().addActionListener(e -> minimizeToTrayButtonAction());

        initStateChangeListeners(view.getMinimizeToTrayButton(), view.getDisplayIdsLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Toggles the "minimize to tray" state and then saves the new "minimize to tray" state.
     */
    private void minimizeToTrayButtonAction() {
        model.toggleMinimizeToTray();
        view.getMinimizeToTrayButton().setOn(model.isMinimizeToTray());
        settingsMgr.saveIniMinimizeToTray(model.isMinimizeToTray());
    }

}
