package com.dhk.controller;

import java.awt.DisplayMode;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Display Mode combo boxes. Listeners are added to the corresponding view components so that when a new
 * display mode is selected from a Display Mode combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DisplayModeController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the DisplayModeController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public DisplayModeController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the display mode combo boxes.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            // Set the action listener for each slot in the view
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getDisplayModes()
                        .addActionListener(e -> saveSlotDisplayMode(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Updates the model's display mode for the specified slot with the selected display mode from the view.
     * 
     * @param displayIndex
     *            - The index of the display to update the display mode for
     * @param slotIndex
     *            - The index of the slot update the display mode for
     */
    private void saveSlotDisplayMode(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;
        DisplayMode selectedDisplayMode = (DisplayMode) view.getSlot(displayIndex, slotIndex).getDisplayModes()
                .getSelectedItem();

        model.getSlot(displayIndex, slotIndex).setDisplayMode(selectedDisplayMode);
        settingsMgr.saveIniSlotDisplayMode(displayId, slotId, selectedDisplayMode.getWidth(),
                selectedDisplayMode.getHeight(), selectedDisplayMode.getBitDepth(),
                selectedDisplayMode.getRefreshRate());
    }

}
