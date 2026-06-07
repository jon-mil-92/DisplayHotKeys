/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
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
                        .addActionListener(_ -> saveSlotScalingMode(displayIndex, slotIndex));
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
