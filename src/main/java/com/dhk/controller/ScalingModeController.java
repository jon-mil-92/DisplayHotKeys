/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
package com.dhk.controller;

import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Scaling Mode combo boxes. Listeners are added to the corresponding view components so that when a new
 * scaling mode is selected from a Scaling Mode combo box, the model is updated.
 *
 * @author Jonathan R. Miller
 */
public class ScalingModeController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the {@link ScalingModeController} class.
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
