package com.dhk.controller;

import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * Controls the combo box for the selected display. Listeners are added to the corresponding view component so that when
 * a new display is selected, the view components are changed to those for the selected display.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class SelectedDisplayController implements IController {

    private DhkView view;
    private DhkModel model;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the SelectedDisplayController class.
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

    /**
     * Creates a new frame updater.
     */
    @Override
    public void initController() {
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * Initializes the listeners for the display IDs combo box.
     */
    @Override
    public void initListeners() {
        view.getDisplayIds().addActionListener(e -> updateSlots());
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
            view.showOrientationModesForDisplay(displayIndex);
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

    @Override
    public void cleanUp() {
    }

}
