package com.dhk.controllers;

import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the combo box for the number of active hot key slots. Listeners are added to the corresponding
 * view component so that when a new number of active hot key slots is selected, the number of visibly active hot key
 * slots is reflected in the application window.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class NumberOfSlotsController implements Controller {
    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the NumberOfSlotsController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public NumberOfSlotsController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.view = view;
        this.model = model;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates a new frame updater.
     */
    @Override
    public void initController() {
        // Initialize the object that will update the view's frame.
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * This method initializes the listeners for the number of slots combo box.
     */
    @Override
    public void initListeners() {
        // For each connected display...
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            // The display index for the current display to add an action listener to.
            int displayIndex = i;

            // Start the action listener for a number of slots change.
            view.getNumberOfActiveSlots(displayIndex).addActionListener(e -> saveNumberOfSlots(displayIndex));
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * This method updates the model's visible number of slots with the selected number of slots from the view.
     * 
     * @param displayIndex - The index of the display to update the number of slots for.
     */
    private void saveNumberOfSlots(int displayIndex) {
        // Get the ID for the given display.
        String displayId = model.getDisplayIds()[displayIndex];

        // The previous number of slots to be displayed.
        int oldNumOfSlots = model.getNumOfSlotsForDisplay(displayIndex);

        // The new number of slots to be displayed.
        int newNumOfSlots = (int) view.getNumberOfActiveSlots(displayIndex).getSelectedItem();

        // Calculate the number of slots to remove.
        int slotsToRemove = oldNumOfSlots - newNumOfSlots;

        // Update the new number of slots in the model.
        model.setNumOfSlotsForDisplay(displayIndex, newNumOfSlots);

        // Save the new number of slots in the settings file.
        settingsMgr.saveIniNumOfSlotsForDisplay(displayId, newNumOfSlots);

        // If decreasing the number of slots...
        if (oldNumOfSlots > newNumOfSlots) {
            // Remove the specified number of slots from the view.
            view.popSlots(slotsToRemove);
        }
        // If increasing the number of slots...
        else if (oldNumOfSlots < newNumOfSlots) {
            // Add slots to the view starting at the last ending slot index.
            view.pushSlots(displayIndex, oldNumOfSlots);
        }

        // Update the view's frame.
        frameUpdater.updateUI();
    }
}
