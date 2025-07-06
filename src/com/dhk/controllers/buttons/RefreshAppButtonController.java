package com.dhk.controllers.buttons;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.controllers.Controller;
import com.dhk.controllers.DhkController;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the Refresh App button. Listeners are added to the corresponding view component so that when the
 * Refresh App button is pressed, the application is re-initialized.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class RefreshAppButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private AppRefresher appRefresher;

    /**
     * Constructor for the RefreshAppButtonController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public RefreshAppButtonController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        // Get the application's model, view, controller, and settings manager.
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates a new app refresher.
     */
    @Override
    public void initController() {
        // Initialize the object that will re-initialize the app.
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * This method initializes the listeners for the refresh app button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the refresh app button action.
        view.getRefreshAppButton().addActionListener(e -> refreshAppButtonAction());

        // Set the state change listener for the refresh app button.
        view.getRefreshAppButton().addChangeListener(e -> refreshAppButtonStateChangeAction());

        // Set the focus listener for the refresh app button.
        view.getRefreshAppButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the refresh app button is focused.
                view.getRefreshAppButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the refresh app button is not focused.
                view.getRefreshAppButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the refresh app button.
        view.getRefreshAppButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the refresh app button when the mouse hovers over it.
                view.getRefreshAppButton().requestFocusInWindow();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Set the focus on the display IDs label when the mouse leaves the button.
                view.getDisplayIdsLabel().requestFocusInWindow();
            }
        });
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Refresh the app to reflect any changes in the display configuration.
     */
    private void refreshAppButtonAction() {
        // Re-initialize the application to reflect any changes in the display configuration.
        appRefresher.reInitApp();
    }

    /**
     * This method changes the refresh app button icon depending on the button's state.
     */
    private void refreshAppButtonStateChangeAction() {
        // If the user is holding the action button on the refresh app button...
        if (view.getRefreshAppButton().getModel().isArmed()) {
            // Use the pressed icon for the refresh app button.
            view.getRefreshAppButton().setIcon(view.getRefreshAppButton().getRefreshAppPressedIcon());
        }
        // If the user is hovering on the refresh app button...
        else if (view.getRefreshAppButton().getModel().isRollover()) {
            // Use the hover icon for the refresh app button.
            view.getRefreshAppButton().setIcon(view.getRefreshAppButton().getRefreshAppHoverIcon());
        }
        // Otherwise, if the user is not interacting with the refresh app button...
        else {
            // Use the idle icon for the refresh app button.
            view.getRefreshAppButton().setIcon(view.getRefreshAppButton().getRefreshAppIdleIcon());
        }
    }
}
