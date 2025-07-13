package com.dhk.controller.button;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.controller.Controller;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the Minimize To Tray button. Listeners are added to the corresponding view component so that when
 * the Minimize To Tray button is pressed, the application will toggle the ability for the application to minimize to
 * the system tray.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class MinimizeToTrayButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the MinimizeToTrayButtonController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public MinimizeToTrayButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates a new...
     */
    @Override
    public void initController() {
    }

    /**
     * This method initializes the listeners for the minimize to tray button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the minimize to tray button action.
        view.getMinimizeToTrayButton().addActionListener(e -> minimizeToTrayButtonAction());

        // Set the state change listener for the minimize to tray button.
        view.getMinimizeToTrayButton().addChangeListener(e -> minimizeToTrayButtonStateChangeAction());

        // Set the focus listener for the minimize to tray button.
        view.getMinimizeToTrayButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the minimize to tray button is focused.
                view.getMinimizeToTrayButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the minimize to tray button is not focused.
                view.getMinimizeToTrayButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the minimize to tray button.
        view.getMinimizeToTrayButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the minimize to tray button when the mouse hovers over it.
                view.getMinimizeToTrayButton().requestFocusInWindow();
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
     * This method toggles the "minimize to tray" state and then saves the new "minimize to tray" state.
     */
    private void minimizeToTrayButtonAction() {
        // Toggle the "minimize to tray" state.
        model.toggleMinimizeToTray();

        // Save the new "minimize to tray" state into the settings file.
        settingsMgr.saveIniMinimizeToTray(model.isMinimizeToTray());
    }

    /**
     * This method changes the minimize to tray button icon depending on the button's state.
     */
    private void minimizeToTrayButtonStateChangeAction() {
        // If the user is holding the action button on the minimize to tray button...
        if (view.getMinimizeToTrayButton().getModel().isArmed()) {
            // Use the corresponding pressed icon.
            setPressedIcon();
        }
        // If the user is hovering on the minimize to tray button...
        else if (view.getMinimizeToTrayButton().getModel().isRollover()) {
            // Use the corresponding hover icon.
            setHoverIcon();
        }
        // Otherwise, if the user is not interacting with the minimize to tray button...
        else {
            // Use the corresponding idle icon.
            setIdleIcon();
        }
    }

    /**
     * This method sets the pressed icon corresponding to the "minimize to tray" state and "dark mode" state of the UI.
     */
    private void setPressedIcon() {
        // If the application should minimize to tray...
        if (model.isMinimizeToTray()) {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the minimize to tray enabled dark pressed button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayEnabledDarkPressedIcon());
            } else {
                // Use the minimize to tray enabled light pressed button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayEnabledLightPressedIcon());
            }
            // Otherwise, if the application should not minimize to tray...
        } else {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the minimize to tray disabled dark pressed button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayDisabledDarkPressedIcon());
            } else {
                // Use the minimize to tray disabled light pressed button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayDisabledLightPressedIcon());
            }
        }
    }

    /**
     * This method sets the hover icon corresponding to the "minimize to tray" state and "dark mode" state of the UI.
     */
    private void setHoverIcon() {
        // If the application should minimize to tray...
        if (model.isMinimizeToTray()) {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the minimize to tray enabled dark hover button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayEnabledDarkHoverIcon());
            } else {
                // Use the minimize to tray enabled light hover button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayEnabledLightHoverIcon());
            }
            // Otherwise, if the application should not minimize to tray...
        } else {
            // If the UI is in dark mode...
            if (model.isDarkMode()) {
                // Use the minimize to tray disabled dark hover button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayDisabledDarkHoverIcon());
            } else {
                // Use the minimize to tray disabled light hover button icon.
                view.getMinimizeToTrayButton()
                        .setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayDisabledLightHoverIcon());
            }
        }
    }

    /**
     * This method sets the idle icon corresponding to the "minimize to tray" state.
     */
    private void setIdleIcon() {
        // If the application should minimize to tray...
        if (model.isMinimizeToTray()) {
            // Use the minimize to tray enabled idle button icon.
            view.getMinimizeToTrayButton().setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayEnabledIdleIcon());
            // Otherwise, if the application should not minimize to tray...
        } else {
            // Use the minimize to tray disabled idle button icon.
            view.getMinimizeToTrayButton().setIcon(view.getMinimizeToTrayButton().getMinimizeToTrayDisabledIdleIcon());
        }
    }
}
