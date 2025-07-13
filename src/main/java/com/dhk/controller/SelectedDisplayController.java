package com.dhk.controller;

import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the combo box for the selected display. Listeners are added to the corresponding view component
 * so that when a new display is selected, the view components are changed to those for the selected display.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class SelectedDisplayController implements Controller {
    private DhkView view;
    private DhkModel model;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the SelectedDisplayController class.
     *
     * @param model - The model for the application.
     * @param view  - The view for the application.
     */
    public SelectedDisplayController(DhkModel model, DhkView view) {
        // Get the application's model and view
        this.model = model;
        this.view = view;
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
     * This method initializes the listeners for the display IDs combo box.
     */
    @Override
    public void initListeners() {
        // Start the action listener for a display ID change.
        view.getDisplayIds().addActionListener(e -> updateSlots());
    }

    /**
     * This method updates the view with the slot components for the selected display ID.
     */
    private void updateSlots() {
        // Get the display index for the selected display from the view.
        int displayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the previously selected display index from the view.
        int prevSelectedDisplayIndex = view.getPreviouslySelectedDisplayIndex();

        // Only perform the event action if the user selected a different display in the combo box.
        if (displayIndex != prevSelectedDisplayIndex) {
            // The number of active slots to be displayed for the newly selected display.
            int newNumOfActiveSlots = model.getNumOfSlotsForDisplay(displayIndex);

            // The number of active slots displayed for the previously selected display.
            int oldNumOfActiveSlots = model.getNumOfSlotsForDisplay(prevSelectedDisplayIndex);

            // Show the number of active slots combo box for the selected display.
            view.showNumberOfActiveSlotsForDisplay(displayIndex);

            // Show the orientation modes combo box for the selected display.
            view.showOrientationModesForDisplay(displayIndex);

            // Replace the slots that will be in the view with the slots for the newly selected display.
            view.replaceActiveSlots();

            // If there were more active slots for the previously selected display...
            if (oldNumOfActiveSlots > newNumOfActiveSlots) {
                // Remove the remaining slots from the view.
                view.popSlots(oldNumOfActiveSlots - newNumOfActiveSlots);
            }
            // Otherwise, add the remaining slots to the view.
            else {
                view.pushSlots(displayIndex, oldNumOfActiveSlots);
            }

            // Update the previously selected display index.
            view.setPreviouslySelectedDisplayIndex(view.getDisplayIds().getSelectedIndex());

            // Update the view's frame.
            frameUpdater.updateUI();
        }
    }

    @Override
    public void cleanUp() {
    }
}
