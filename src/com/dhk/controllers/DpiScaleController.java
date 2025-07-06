package com.dhk.controllers;

import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the DPI Scale Percentage combo boxes. Listeners are added to the corresponding view components so
 * that when a new DPI scale percentage is selected from a DPI Scale Percentage combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DpiScaleController implements Controller {
    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the DpiScaleController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public DpiScaleController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
    }

    /**
     * This method initializes the listeners for the DPI scale percentage combo boxes.
     */
    @Override
    public void initListeners() {
        // For each connected display...
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            // The display index for the current display to add an action listener to.
            int displayIndex = i;

            // Set the action listener for each slot in the view.
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                // The index for the slot view to add an action listener to.
                int slotIndex = j;

                // Set action listeners for DPI scale percentage changes from the view.
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages()
                        .addActionListener(e -> saveSlotDpiScalePercentage(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * This method updates the model's DPI scale percentage for the specified slot with the selected DPI scale
     * percentage from the view.
     * 
     * @param displayIndex - The index of the display to update the DPI scale percentage for.
     * @param slotIndex    - The index of the slot update the DPI scale percentage for.
     */
    private void saveSlotDpiScalePercentage(int displayIndex, int slotIndex) {
        // Get the ID for the given display.
        String displayId = model.getDisplayIds()[displayIndex];

        // The ID for the slot starts at 1.
        int slotId = slotIndex + 1;

        // Get the slot's selected DPI scale percentage from the view.
        int selectedDpiScalePercentage = (int) view.getSlot(displayIndex, slotIndex).getDpiScalePercentages()
                .getSelectedItem();

        // Update the slot's DPI scale percentage in the model.
        model.getSlot(displayIndex, slotIndex).setDpiScalePercentage(selectedDpiScalePercentage);

        // Save the slot's DPI scale percentage in the settings ini file.
        settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, selectedDpiScalePercentage);
    }
}
