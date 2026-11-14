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

import com.dhk.model.DhkModel;
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;

/**
 * Controls the combo box for the selected display. Listeners are added to the corresponding view component so that when
 * a new display is selected, the view components are changed to those for the selected display.
 *
 * @author Jonathan R. Miller
 */
public class SelectedDisplayController implements IController {

    private DhkView view;
    private DhkModel model;

    /**
     * Constructor for the {@link SelectedDisplayController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public SelectedDisplayController(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        view.getDisplayIds().addActionListener(e -> updateSlots());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Updates the view with the slot components for the selected display ID.
     */
    private void updateSlots() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        int prevSelectedDisplayIndex = view.getPreviouslySelectedDisplayIndex();

        if (displayIndex != prevSelectedDisplayIndex) {
            int newNumOfActiveSlots = model.getNumOfSlotsForDisplay(displayIndex);
            int oldNumOfActiveSlots = model.getNumOfSlotsForDisplay(prevSelectedDisplayIndex);

            view.showNumberOfActiveSlotsForDisplay(displayIndex);
            view.replaceActiveSlots();

            if (oldNumOfActiveSlots > newNumOfActiveSlots) {
                view.popSlots(oldNumOfActiveSlots - newNumOfActiveSlots);
            } else {
                view.pushSlots(displayIndex, oldNumOfActiveSlots);
            }

            view.setPreviouslySelectedDisplayIndex(view.getDisplayIds().getSelectedIndex());
            WindowUtil.refreshWindow(view.getFrame());
        }
    }

}
