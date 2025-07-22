package com.dhk.controller;

import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the DPI Scale Percentage combo boxes. Listeners are added to the corresponding view components so that when
 * a new DPI scale percentage is selected from a DPI Scale Percentage combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DpiScaleController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the DpiScaleController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public DpiScaleController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the DPI scale percentage combo boxes.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            // Set the action listener for each slot in the view.
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages()
                        .addActionListener(e -> saveSlotDpiScalePercentage(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Updates the model's DPI scale percentage for the specified slot with the selected DPI scale percentage from the
     * view.
     * 
     * @param displayIndex
     *            - The index of the display to update the DPI scale percentage for
     * @param slotIndex
     *            - The index of the slot update the DPI scale percentage for
     */
    private void saveSlotDpiScalePercentage(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;
        int selectedDpiScalePercentage = (int) view.getSlot(displayIndex, slotIndex).getDpiScalePercentages()
                .getSelectedItem();

        model.getSlot(displayIndex, slotIndex).setDpiScalePercentage(selectedDpiScalePercentage);
        settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, selectedDpiScalePercentage);
    }

}
