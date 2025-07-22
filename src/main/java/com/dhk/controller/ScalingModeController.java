package com.dhk.controller;

import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Scaling Mode combo boxes. Listeners are added to the corresponding view components so that when a new
 * scaling mode is selected from a Scaling Mode combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ScalingModeController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the ScalingModeController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public ScalingModeController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the scaling mode combo boxes.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getScalingModes()
                        .addActionListener(e -> saveSlotScalingMode(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Updates the model's scaling mode for the specified slot with the selected scaling mode from the view.
     * 
     * @param displayIndex
     *            - The index of the display to update the scaling mode for
     * @param slotIndex
     *            - The index of the slot update the scaling mode for
     */
    private void saveSlotScalingMode(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;
        int selectedScalingMode = view.getSlot(displayIndex, slotIndex).getScalingModes().getSelectedIndex();

        model.getSlot(displayIndex, slotIndex).setScalingMode(selectedScalingMode);
        settingsMgr.saveIniSlotScalingMode(displayId, slotId, selectedScalingMode);
    }

}
