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
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;

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
    }

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            view.getNumberOfActiveSlots(displayIndex).addActionListener(e -> saveNumberOfSlots(displayIndex));
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

        WindowUtil.refreshWindow(view.getFrame());
    }

}
