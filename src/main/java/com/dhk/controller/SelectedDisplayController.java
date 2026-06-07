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

import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.view.FrameUpdater;

/**
 * Controls the combo box for the selected display. Listeners are added to the corresponding view component so that when
 * a new display is selected, the view components are changed to those for the selected display.
 *
 * @author Jonathan R. Miller
 */
public class SelectedDisplayController implements IController {

    private DhkView view;
    private DhkModel model;
    private FrameUpdater frameUpdater;

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
        frameUpdater = new FrameUpdater(view);
    }

    @Override
    public void initListeners() {
        view.getDisplayIds().addActionListener(_ -> updateSlots());
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
            frameUpdater.updateUI();
        }
    }

}
