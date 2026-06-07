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
import com.dhk.view.FrameUpdater;

/**
 * Controls the combo box for the number of active hot key slots. Listeners are added to the corresponding view
 * component so that when a new number of active hot key slots is selected, the number of visibly active hot key slots
 * is reflected in the application window.
 *
 * @author Jonathan R. Miller
 */
public class NumberOfSlotsController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the {@link NumberOfSlotsController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public NumberOfSlotsController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.view = view;
        this.model = model;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        frameUpdater = new FrameUpdater(view);
    }

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            view.getNumberOfActiveSlots(displayIndex).addActionListener(_ -> saveNumberOfSlots(displayIndex));
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Updates the model's visible number of slots with the selected number of slots from the view.
     *
     * @param displayIndex
     *            - The index of the display to update the number of slots for
     */
    private void saveNumberOfSlots(int displayIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int oldNumOfSlots = model.getNumOfSlotsForDisplay(displayIndex);
        int newNumOfSlots = (int) view.getNumberOfActiveSlots(displayIndex).getSelectedItem();
        int slotsToRemove = oldNumOfSlots - newNumOfSlots;

        model.setNumOfSlotsForDisplay(displayIndex, newNumOfSlots);
        settingsMgr.saveIniNumOfSlotsForDisplay(displayId, newNumOfSlots);

        // If decreasing the number of slots
        if (oldNumOfSlots > newNumOfSlots) {
            view.popSlots(slotsToRemove);
        }
        // Else, if increasing the number of slots
        else if (oldNumOfSlots < newNumOfSlots) {
            // Add slots to the view starting at the last ending slot index
            view.pushSlots(displayIndex, oldNumOfSlots);
        }

        frameUpdater.updateUI();
    }

}
