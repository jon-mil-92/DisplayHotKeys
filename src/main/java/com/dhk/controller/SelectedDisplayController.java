package com.dhk.controller;

import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.view.FrameUpdater;

/**
 * Controls the combo box for the selected display. Listeners are added to the corresponding view component so that when
 * a new display is selected, the view components are changed to those for the selected display.
 * 
 * @author Jonathan R. Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan R. Miller
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
            frameUpdater.updateUI();
        }
    }

}
