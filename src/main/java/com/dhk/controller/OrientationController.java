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

import javax.swing.JOptionPane;

import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.model.DisplayMode;
import com.dhk.utility.DisplayModeInverter;
import com.dhk.view.ConfirmationDialog;
import com.dhk.view.DhkView;

/**
 * Controls the orientation mode combo boxes. Listeners are added to the corresponding view components so that when a
 * new orientation mode is selected from an orientation mode combo box, the model is updated.
 *
 * @author Jonathan R. Miller
 */
public class OrientationController implements IController {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private AppRefresher appRefresher;

    /**
     * Question the confirmation dialog asks before changing a slot's orientation.
     */
    private static final String CONFIRMATION_QUESTION = "Are you sure you want to change the display orientation for the slot?";

    /**
     * Warning shown in the confirmation dialog, since a rotated image is only usable on a display that can be
     * physically rotated.
     */
    private static final String CONFIRMATION_WARNING = "Only do this if you can rotate your display!";

    /**
     * Title of the confirmation dialog shown before changing a slot's orientation.
     */
    private static final String TITLE_BAR_MESSAGE = "Confirm Display Orientation Change";

    /**
     * Constructor for the {@link OrientationController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param controller
     *            - The controller for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public OrientationController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            // Set the action listener for each slot in the view.
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getOrientationModes()
                        .addActionListener(e -> orientationModeAction(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Changes the orientation mode of the display upon user confirmation.
     *
     * @param displayIndex
     *            - The index of the display to change the orientation mode for
     * @param slotIndex
     *            - The index of the slot to update the orientation mode for
     */
    private void orientationModeAction(int displayIndex, int slotIndex) {
        view.getDefaultFocusComponent().requestFocusInWindow();

        int previouslySelectedOrientationMode = model.getSlot(displayIndex, slotIndex).getOrientationMode();
        int selectedOrientationMode = view.getSlot(displayIndex, slotIndex).getOrientationModes().getSelectedIndex();

        if (previouslySelectedOrientationMode != selectedOrientationMode) {
            if (model.getSlot(displayIndex, slotIndex).isClearingSlot()
                    || getUserConfirmation() == JOptionPane.YES_OPTION) {
                saveSlotOrientationMode(displayIndex, slotIndex);
            } else {
                view.getSlot(displayIndex, slotIndex).getOrientationModes()
                        .setSelectedIndex(previouslySelectedOrientationMode);
            }
        }
    }

    /**
     * Shows a confirmation window that asks if the user wants to change the display orientation for the slot.
     *
     * @return The return value from the option pane
     */
    private int getUserConfirmation() {
        ConfirmationDialog confirmationDialog = new ConfirmationDialog(view.getFrame(), TITLE_BAR_MESSAGE,
                CONFIRMATION_QUESTION, CONFIRMATION_WARNING);

        return confirmationDialog.showConfirmationDialog();
    }

    /**
     * Updates the given slot for the selected orientation mode.
     *
     * @param displayIndex
     *            - The index of the display to change the orientation mode for
     * @param slotIndex
     *            - The index of the slot to update the orientation mode for
     */
    private void saveSlotOrientationMode(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;
        int previouslySelectedOrientationMode = model.getSlot(displayIndex, slotIndex).getOrientationMode();
        int selectedOrientationMode = view.getSlot(displayIndex, slotIndex).getOrientationModes().getSelectedIndex();

        if (previouslySelectedOrientationMode != selectedOrientationMode) {
            // Determine if the previous orientation mode was a landscape orientation mode
            boolean previouslyLandscapeOrientation = previouslySelectedOrientationMode == 0
                    || previouslySelectedOrientationMode == 2;

            // Determine if the selected orientation mode is a portrait orientation mode
            boolean selectedPortraitOrientation = selectedOrientationMode == 1 || selectedOrientationMode == 3;

            // Determine if going from a landscape orientation mode to a portrait orientation mode
            boolean landscapeToPortrait = previouslyLandscapeOrientation && selectedPortraitOrientation;

            // Determine if going from a portrait orientation mode to a landscape orientation mode
            boolean portraitToLandscape = (!previouslyLandscapeOrientation && !selectedPortraitOrientation);

            model.getSlot(displayIndex, slotIndex).setOrientationMode(selectedOrientationMode);
            settingsMgr.saveIniSlotOrientationMode(displayId, slotId, selectedOrientationMode);

            // Only invert the display modes if going from landscape to portrait or vice versa
            if (landscapeToPortrait || portraitToLandscape) {
                DisplayMode invertedDisplayMode = DisplayModeInverter
                        .invertDisplayMode(model.getSlot(displayIndex, slotIndex).getDisplayMode());

                settingsMgr.saveIniSlotDisplayMode(displayId, slotId, invertedDisplayMode);
            }

            appRefresher.reInitApp();
        }
    }

}
