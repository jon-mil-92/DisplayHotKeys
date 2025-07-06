package com.dhk.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the Scaling Mode combo boxes. Listeners are added to the corresponding view components so that
 * when a new scaling mode is selected from a Scaling Mode combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @version 1.2.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ScalingModeController implements Controller {
    private DhkView view;
    private DhkModel model;
    private SettingsManager settings;

    /**
     * Constructor for the ScalingModeController class.
     *
     * @param model    - The model for the application.
     * @param view     - The view for the application.
     * @param settings - The settings manager for the application.
     */
    public ScalingModeController(DhkModel model, DhkView view, SettingsManager settings) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settings = settings;
    }

    /**
     * This method initializes the listeners for the scaling mode combo boxes.
     */
    public void initListeners() {
        // Set the action listener for each slot in the view.
        for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
            // The index for the slot view to add an action listener to.
            int slotIndex = i;

            // Set action listeners for scaling mode changes from the view.
            view.getSlot(slotIndex).getScalingModes().addActionListener(e -> saveSlotScalingMode(slotIndex));

            // Set mouse listeners for the scaling mode combo boxes from the view.
            view.getSlot(slotIndex).getScalingModes().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    // Set the focus on the scaling mode combo boxes when the mouse hovers over it.
                    view.getSlot(slotIndex).getScalingModes().requestFocusInWindow();
                }
            });
        }
    }

    /**
     * This method updates the model's scaling mode for the specified slot with the selected scaling mode from the view.
     */
    private void saveSlotScalingMode(int slotIndex) {
        // Get the slot's selected scaling mode from the view.
        int selectedScalingMode = view.getSlot(slotIndex).getScalingModes().getSelectedIndex();

        // Update the slot's scaling mode in the model.
        model.getSlot(slotIndex).setScalingMode(selectedScalingMode);

        // Save the slot's scaling mode in the settings ini file.
        settings.saveIniSlotScalingMode(slotIndex + 1, selectedScalingMode);
    }
}
