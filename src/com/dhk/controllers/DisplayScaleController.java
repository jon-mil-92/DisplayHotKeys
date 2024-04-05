package com.dhk.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the Display Scale combo boxes. Listeners are added to the corresponding view components so that
 * when a new display scale is selected from a Display Scale combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DisplayScaleController implements Controller {
    private DhkView view;
    private DhkModel model;
    private SettingsManager settings;

    /**
     * Constructor for the DisplayScaleController class.
     *
     * @param model    - The model for the application.
     * @param view     - The view for the application.
     * @param settings - The settings manager for the application.
     */
    public DisplayScaleController(DhkModel model, DhkView view, SettingsManager settings) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settings = settings;
    }

    /**
     * This method initializes the listeners for the display scale combo boxes.
     */
    public void initListeners() {
        // Set the action listener for each slot in the view.
        for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
            // The index for the slot view to add an action listener to.
            int slotIndex = i;

            // Set action listeners for display scale changes from the view.
            view.getSlot(slotIndex).getDisplayScales().addActionListener(e -> saveSlotDisplayScale(slotIndex));

            // Set mouse listeners for the display scale combo boxes from the view.
            view.getSlot(slotIndex).getDisplayScales().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    // Set the focus on the display scale combo boxes when the mouse hovers over it.
                    view.getSlot(slotIndex).getDisplayScales().requestFocusInWindow();
                }
            });
        }
    }

    /**
     * This method updates the model's display scale for the specified slot with the selected display scale from the
     * view.
     */
    private void saveSlotDisplayScale(int slotIndex) {
        // Get the slot's selected display scale from the view.
        int selectedDisplayScale = (int) view.getSlot(slotIndex).getDisplayScales().getSelectedItem();

        // Update the slot's display scale in the model.
        model.getSlot(slotIndex).setDisplayScale(selectedDisplayScale);

        // Save the slot's display mode in the settings ini file.
        settings.saveIniSlotDisplayScale(slotIndex + 1, selectedDisplayScale);
    }
}
