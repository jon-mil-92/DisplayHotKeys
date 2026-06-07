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
 * Controls the DPI Scale Percentage combo boxes. Listeners are added to the corresponding view components so that when
 * a new DPI scale percentage is selected from a DPI Scale Percentage combo box, the model is updated.
 *
 * @author Jonathan R. Miller
 */
public class DpiScaleController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the {@link DpiScaleController} class.
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

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            // Set the action listener for each slot in the view.
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages()
                        .addActionListener(_ -> saveSlotDpiScalePercentage(displayIndex, slotIndex));
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
