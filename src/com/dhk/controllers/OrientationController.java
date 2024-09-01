package com.dhk.controllers;

import java.awt.DisplayMode;
import java.util.Arrays;

import javax.swing.JOptionPane;

import com.dhk.io.DisplayConfig;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.utility.DisplayModeInverter;

/**
 * This class controls the combo box for the orientation of the selected display. Listeners are added to the
 * corresponding view component so that when a new display is selected, the view components are changed to those for the
 * selected display.
 * 
 * @author Jonathan Miller
 * @version 1.0.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class OrientationController implements Controller {
    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayModeInverter displayModeInverter;
    private SetDisplay setDisplay;
    private AppRefresher appRefresher;

    /**
     * Constructor for the OrientationController class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public OrientationController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        // Get the application's model, view, controller, and settings manager.
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method initializes fields for the controller.
     */
    @Override
    public void initController() {
        displayModeInverter = new DisplayModeInverter();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
        setDisplay = new SetDisplay();
    }

    /**
     * This method initializes the listeners for the orientation modes combo box.
     */
    @Override
    public void initListeners() {
        // For each connected display...
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            // The display index for the current display to add an action listener to.
            int displayIndex = i;

            // Start the action listener for a orientation mode change.
            view.getOrientationModes(displayIndex).addActionListener(e -> orientationModeAction(displayIndex));
        }
    }

    /**
     * Change the orientation of the display upon user confirmation.
     * 
     * @param displayIndex - The index of the dislay to change the orientation for.
     */
    private void orientationModeAction(int displayIndex) {
        // Set the focus on the display IDs label so the clear all slots button does not flash red after confirmation.
        view.getDisplayIdsLabel().requestFocusInWindow();

        // Confirm whether or not the display orientation should be changed.
        int confirmationResult = getUserConfirmation();

        // Only change the display orientation if the user clicked on the "Yes" option.
        if (confirmationResult == JOptionPane.YES_OPTION) {
            saveOrientationMode(displayIndex);
        } else {
            // Get the previously selected orientation mode for the given display.
            int previouslySelectedOrientationMode = model.getOrientationModeForDisplay(displayIndex);

            // Restore the previously selected orientation mode.
            view.getOrientationModes(displayIndex).setSelectedIndex(previouslySelectedOrientationMode);
        }
    }

    /**
     * This method shows a confirmation window that asks if the user wants to clear all slots.
     * 
     * @return The return value from the option pane.
     */
    private int getUserConfirmation() {
        // Create a confirmation message string to be used in the confirmation window.
        String confirmationMessage = "Are you sure you want to change the display orientation?"
                + " Only do this if you can rotate your display!";

        // Create a message to show in the title bar of the confirmation window.
        String titleBarMessage = "Confirm display orientation change.";

        // Create an option pane to confirm if the user wants to change the display orientation.
        int confirmationResult = JOptionPane.showConfirmDialog(view.getFrame(), confirmationMessage, titleBarMessage,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        return confirmationResult;
    }

    /**
     * This method updates the view with the slot components for the selected orientation mode.
     * 
     * @param displayIndex - The index of the dislay to change the orientation for.
     */
    private void saveOrientationMode(int displayIndex) {
        // Get the ID for the given display.
        String displayId = model.getDisplayIds()[displayIndex];

        // Get the previously selected orientation mode for the given display.
        int previouslySelectedOrientationMode = model.getOrientationModeForDisplay(displayIndex);

        // Get the orientation mode for the given display.
        int selectedOrientationMode = view.getOrientationModes(displayIndex).getSelectedIndex();

        // Skip the processing for changing the orientation mode if selecting the same mode.
        if (previouslySelectedOrientationMode != selectedOrientationMode) {
            // Update the orientation mode in the model.
            model.setOrientationModeForDisplay(displayIndex, selectedOrientationMode);

            // Save the orientation mode in the settings file.
            settingsMgr.saveIniOrientationModeForDisplay(displayId, selectedOrientationMode);

            // Determine if going from a landscape orientation mode to a portrait orientation mode.
            boolean landscapeToPortrait = (previouslySelectedOrientationMode == 0
                    || previouslySelectedOrientationMode == 2)
                    && (selectedOrientationMode == 1 || selectedOrientationMode == 3);

            // Determine if going from a portrait orientation mode to a landscape orientation mode.
            boolean portraitToLandscape = ((previouslySelectedOrientationMode == 1
                    || previouslySelectedOrientationMode == 3)
                    && (selectedOrientationMode == 0 || selectedOrientationMode == 2));

            // Only invert the display modes if going from landscape to portrait or vice versa.
            if (landscapeToPortrait || portraitToLandscape) {

                // Invert the saved display mode for each slot for the current display.
                for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
                    int slotId = slotIndex + 1;

                    // Invert the resolution for the current display mode.
                    DisplayMode invertedDisplayMode = displayModeInverter
                            .invert(model.getSlot(displayIndex, slotIndex).getDisplayMode());

                    // Save the inverted display mode for the current slot of the selected display.
                    settingsMgr.saveIniSlotDisplayMode(displayId, slotId, invertedDisplayMode.getWidth(),
                            invertedDisplayMode.getHeight(), invertedDisplayMode.getBitDepth(),
                            invertedDisplayMode.getRefreshRate());
                }
            }

            setOrientation(displayIndex);
            appRefresher.reInitApp();
        }
    }

    /**
     * This method sets the orientation if the display is connected.
     * 
     * @param displayIndex - The index of the display to set the orientation mode for.
     */
    private void setOrientation(int displayIndex) {
        // Update the number of connected displays.
        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.updateDisplayIds();

        // Get the ID for the given display index.
        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed...
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            // Set the new orientation mode for the given display.
            setDisplay.applyDisplayOrientation(displayId, model.getOrientationModeForDisplay(displayIndex));
        }
    }

    @Override
    public void cleanUp() {
    }
}
