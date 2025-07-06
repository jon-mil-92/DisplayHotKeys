package com.dhk.controllers.buttons;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.controllers.Controller;
import com.dhk.io.RunOnStartupManager;
import com.dhk.ui.DhkView;

/**
 * This class controls the Run On Startup button. Listeners are added to the corresponding view component so that when
 * the Run On Startup button is pressed, the application will toggle the ability for the application to launch on user
 * login.
 * 
 * @author Jonathan Miller
 * @version 1.4.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class RunOnStartupButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private RunOnStartupManager runOnStartupManager;

    /**
     * Constructor for the RunOnStartupButtonController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public RunOnStartupButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates a new run on startup manager.
     */
    @Override
    public void initController() {
        // Initialize the run on startup manager.
        runOnStartupManager = new RunOnStartupManager();
    }

    /**
     * This method initializes the listeners for the run on startup button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the run on startup button action.
        view.getRunOnStartupButton().addActionListener(e -> runOnStartupButtonAction());

        // Set the state change listener for the run on startup button.
        view.getRunOnStartupButton().addChangeListener(e -> startupButtonStateChangeAction());

        // Set the focus listener for the run on startup button.
        view.getRunOnStartupButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the run on startup button is focused.
                view.getRunOnStartupButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the run on startup button is not focused.
                view.getRunOnStartupButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the run on startup button.
        view.getRunOnStartupButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the run on startup button when the mouse hovers over it.
                view.getRunOnStartupButton().requestFocusInWindow();
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
     * This method toggles the "run on startup" state, adds or removes a batch file to the startup folder, and then
     * saves the new "run on startup" state.
     */
    private void runOnStartupButtonAction() {
        // Toggle the "run on startup" state.
        model.toggleRunOnStartup();

        // If the application should run on startup...
        if (model.isRunOnStartup()) {
            // Add the startup batch file to run this application on Windows login.
            runOnStartupManager.addToStartup();
        } else {
            // Remove the startup batch file so this application does not run on Windows login.
            runOnStartupManager.removeFromStartup();
        }

        // Save the new "run on startup" state into the settings file.
        settingsMgr.saveIniRunOnStartup(model.isRunOnStartup());
    }

    /**
     * This method changes the run on startup button icon depending on the button's state.
     */
    private void startupButtonStateChangeAction() {
        // If the user is holding the action button on the run on startup button...
        if (view.getRunOnStartupButton().getModel().isArmed()) {
            // Use the corresponding pressed icon.
            setPressedIcon();
        }
        // If the user is hovering on the run on startup button...
        else if (view.getRunOnStartupButton().getModel().isRollover()) {
            // Use the corresponding hover icon.
            setHoverIcon();
        }
        // Otherwise, if the user is not interacting with the run on startup button...
        else {
            // Use the corresponding idle icon.
            setIdleIcon();
        }
    }

    /**
     * This method sets the pressed icon corresponding to the "run on startup" state and "dark mode" state of the UI.
     */
    private void setPressedIcon() {
        // If the application should run on startup...
        if (model.isRunOnStartup()) {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the run on startup enabled dark pressed button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledDarkPressedIcon());
            } else {
                // Use the run on startup enabled light pressed button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledLightPressedIcon());
            }
            // Otherwise, if the application should not run on startup...
        } else {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the run on startup disabled dark pressed button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledDarkPressedIcon());
            } else {
                // Use the run on startup disabled light pressed button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledLightPressedIcon());
            }
        }
    }

    /**
     * This method sets the hover icon corresponding to the "run on startup" state and "dark mode" state of the UI.
     */
    private void setHoverIcon() {
        // If the application should run on startup...
        if (model.isRunOnStartup()) {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the run on startup enabled dark hover button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledDarkHoverIcon());
            } else {
                // Use the run on startup enabled light hover button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledLightHoverIcon());
            }
            // Otherwise, if the application should not run on startup...
        } else {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the run on startup disabled dark hover button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledDarkHoverIcon());
            } else {
                // Use the run on startup disabled light hover button icon.
                view.getRunOnStartupButton()
                        .setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledLightHoverIcon());
            }
        }
    }

    /**
     * This method sets the idle icon corresponding to the "run on startup" state.
     */
    private void setIdleIcon() {
        // If the application should run on startup...
        if (model.isRunOnStartup()) {
            // Use the run on startup enabled idle button icon.
            view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledIdleIcon());
            // Otherwise, if the application should not run on startup...
        } else {
            // Use the run on startup disabled idle button icon.
            view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledIdleIcon());
        }
    }
}
