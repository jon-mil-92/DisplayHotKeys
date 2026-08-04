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
import com.dhk.model.DisplayMode;
import com.dhk.model.RefreshRate;
import com.dhk.model.Resolution;
import com.dhk.view.DhkView;

/**
 * Controls the Resolution and Refresh Rate combo boxes, which together select a display mode. Listeners are added to
 * the corresponding view components so that selecting a resolution refreshes the rates it supports and, along with
 * selecting a refresh rate, updates the model with the recombined display mode.
 *
 * @author Jonathan R. Miller
 */
public class DisplayModeController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;

    /**
     * Constructor for the {@link DisplayModeController} class.
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

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            // Set the action listeners for each slot in the view
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getResolutions()
                        .addActionListener(e -> selectSlotResolution(displayIndex, slotIndex));

                view.getSlot(displayIndex, slotIndex).getRefreshRates()
                        .addActionListener(e -> saveSlotDisplayMode(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Reacts to a new resolution selection by refreshing the rates and DPI scale percentages it supports, then saving
     * the recombined display mode. Repopulating the rates does not reliably fire the refresh rate listener, so the save
     * is performed here rather than relied upon as a side effect.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to react to the resolution selection for
     */
    private void selectSlotResolution(int displayIndex, int slotIndex) {
        view.updateSlotRefreshRates(displayIndex, slotIndex);

        // Refresh the DPI Scale combo box for the new resolution, falling back to a supported percentage if needed
        view.updateSlotDpiScalePercentages(displayIndex, slotIndex);

        // Persist the recombined display mode, since repopulating the rate combo box may not fire its listener
        saveSlotDisplayMode(displayIndex, slotIndex);
    }

    /**
     * Updates the model's display mode for the specified slot with the display mode recombined from the selected
     * resolution and refresh rate in the view.
     *
     * @param displayIndex
     *            - The index of the display to update the display mode for
     * @param slotIndex
     *            - The index of the slot update the display mode for
     */
    private void saveSlotDisplayMode(int displayIndex, int slotIndex) {
        Resolution selectedResolution = (Resolution) view.getSlot(displayIndex, slotIndex).getResolutions()
                .getSelectedItem();
        RefreshRate selectedRefreshRate = (RefreshRate) view.getSlot(displayIndex, slotIndex).getRefreshRates()
                .getSelectedItem();

        // The refresh rates are repopulated as the resolution changes, so guard against a momentarily empty selection
        if (selectedResolution == null || selectedRefreshRate == null) {
            return;
        }

        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;
        DisplayMode selectedDisplayMode = new DisplayMode(selectedResolution.getWidth(), selectedResolution.getHeight(),
                selectedRefreshRate.getNumerator(), selectedRefreshRate.getDenominator());

        model.getSlot(displayIndex, slotIndex).setDisplayMode(selectedDisplayMode);
        settingsMgr.saveIniSlotDisplayMode(displayId, slotId, selectedDisplayMode);
    }

}
