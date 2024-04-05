package com.dhk.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * @version 1.2.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class NumberOfSlotsController implements Controller {
    private DhkView view;
    private DhkModel model;
    private SettingsManager settings;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the NumberOfSlotsController class.
     *
     * @param model    - The model for the application.
     * @param view     - The view for the application.
     * @param settings - The settings manager for the application.
     */
    public NumberOfSlotsController(DhkModel model, DhkView view, SettingsManager settings) {
        // Get the application's view, model, and settings manager.
        this.view = view;
        this.model = model;
        this.settings = settings;

        // Initialize the object that will update the view's frame.
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * This method initializes the listeners for the number of slots combo box.
     */
    public void initListeners() {
        // Start the action listener for a number of slots change.
        view.getNumberOfSlots().addActionListener(e -> saveNumberOfSlots());

        // Set mouse listeners for the number of slots combo box from the view.
        view.getNumberOfSlots().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the number of slots combo box when the mouse hovers over it.
                view.getNumberOfSlots().requestFocusInWindow();
            }
        });
    }

    /**
     * This method updates the model's visible number of slots with the selected number of slots from the view.
     */
    private void saveNumberOfSlots() {
        // The previous number of slots to be displayed.
        int oldNumOfSlots = model.getNumOfSlots();

        // The new number of slots to be displayed.
        int newNumOfSlots = (int) view.getNumberOfSlots().getSelectedItem();

        // Calculate the number of slots to remove.
        int slotsToRemove = oldNumOfSlots - newNumOfSlots;

        // Update the new number of slots in the model.
        model.setNumOfSlots(newNumOfSlots);

        // Save the new number of slots in the settings file.
        settings.saveIniNumOfSlots(newNumOfSlots);

        // If decreasing the number of slots...
        if (oldNumOfSlots > newNumOfSlots) {
            // Remove the specified number of slots from the view.
            view.removeSlots(slotsToRemove);
        }
        // If increasing the number of slots...
        else if (oldNumOfSlots < newNumOfSlots) {
            // Add slots to the view starting at the last ending slot index.
            view.addSlots(oldNumOfSlots);
        }

        // Update the view components.
        frameUpdater.updateUI();
    }
}
