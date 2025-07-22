package com.dhk.controller;

import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * Controls the combo box for the number of active hot key slots. Listeners are added to the corresponding view
 * component so that when a new number of active hot key slots is selected, the number of visibly active hot key slots
 * is reflected in the application window.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class NumberOfSlotsController implements IController {

    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the NumberOfSlotsController class.
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

    /**
     * Creates a new frame updater.
     */
    @Override
    public void initController() {
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * Initializes the listeners for the number of slots combo box.
     */
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

        frameUpdater.updateUI();
    }

}
