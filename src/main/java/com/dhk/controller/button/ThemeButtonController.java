package com.dhk.controller.button;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.controller.Controller;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.theme.ButtonThemeUpdater;
import com.dhk.theme.ThemeUpdater;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Theme button. Listeners are added to the corresponding view component so that when the Theme
 * button is pressed, the application's theme will be toggled between "Light" and "Dark."
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ThemeButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private ThemeUpdater themeUpdater;
    private ButtonThemeUpdater buttonThemesUpdater;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the ThemeButtonController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public ThemeButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates a new theme chooser, button themes updater, and frame updater.
     */
    @Override
    public void initController() {
        // Initialize the objects that will control the active theme.
        themeUpdater = new ThemeUpdater();
        buttonThemesUpdater = new ButtonThemeUpdater(model, view);

        // Initialize the frame updater that will update the application's view.
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * This method initializes the listeners for the theme button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the theme button action.
        view.getThemeButton().addActionListener(e -> themeButtonAction());

        // Set the state change listener for the theme button.
        view.getThemeButton().addChangeListener(e -> themeButtonStateChangeAction());

        // Set the focus listener for the theme button.
        view.getThemeButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the theme button is focused.
                view.getThemeButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the theme button is not focused.
                view.getThemeButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the theme button.
        view.getThemeButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the theme button when the mouse hovers over it.
                view.getThemeButton().requestFocusInWindow();
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
     * Toggle the "dark mode" state and update the UI, and then save the new settings.
     */
    private void themeButtonAction() {
        // Toggle the dark mode state of the UI.
        model.toggleDarkMode();

        // Update the theme according to the current "dark mode" state of the UI.
        themeUpdater.useDarkMode(model.isDarkMode());

        // Update the themeable buttons to relfect the new "dark mode" state of the UI.
        buttonThemesUpdater.updateIdleButtonThemes();

        // Update the view's frame.
        frameUpdater.updateUI();

        // Save the new UI mode into the settings file.
        settingsMgr.saveIniDarkMode(model.isDarkMode());
    }

    /**
     * This method changes the theme button icon depending on the button's state.
     */
    private void themeButtonStateChangeAction() {
        // If the user is holding the action button on the theme button...
        if (view.getThemeButton().getModel().isArmed()) {
            // Use the corresponding pressed icon.
            setPressedIcon();
        }
        // If the user is hovering on the theme button...
        else if (view.getThemeButton().getModel().isRollover()) {
            // Use the corresponding hover icon.
            setHoverIcon();
        }
        // Otherwise, if the user is not interacting with the theme button...
        else {
            // Use the corresponding idle icon.
            setIdleIcon();
        }
    }

    /**
     * This method sets the pressed icon corresponding to the "dark mode" state.
     */
    private void setPressedIcon() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
            // Use the pressed icon for dark mode.
            view.getThemeButton().setIcon(view.getThemeButton().getDarkModePressedIcon());
        } else {
            // Use the pressed icon for light mode.
            view.getThemeButton().setIcon(view.getThemeButton().getLightModePressedIcon());
        }
    }

    /**
     * This method sets the hover icon corresponding to the "dark mode" state.
     */
    private void setHoverIcon() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
            // Use the hover icon for dark mode.
            view.getThemeButton().setIcon(view.getThemeButton().getDarkModeHoverIcon());
        } else {
            // Use the hover icon for light mode.
            view.getThemeButton().setIcon(view.getThemeButton().getLightModeHoverIcon());
        }
    }

    /**
     * This method sets the idle icon corresponding to the "dark mode" state.
     */
    private void setIdleIcon() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
            // Use the idle icon for dark mode.
            view.getThemeButton().setIcon(view.getThemeButton().getDarkModeIdleIcon());
        } else {
            // Use the idle icon for light mode.
            view.getThemeButton().setIcon(view.getThemeButton().getLightModeIdleIcon());
        }
    }
}
