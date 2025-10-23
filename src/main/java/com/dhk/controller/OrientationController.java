package com.dhk.controller;

import java.awt.DisplayMode;
import javax.swing.JOptionPane;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.utility.DisplayModeInverter;
import com.dhk.view.DhkView;

/**
 * Controls the orientation mode combo boxes. Listeners are added to the corresponding view components so that when a
 * new orientation mode is selected from an orientation mode combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class OrientationController implements IController {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private AppRefresher appRefresher;

    private final String CONFIRMATION_MESSAGE = "Are you sure you want to change the display orientation for the slot?"
            + " Only do this if you can rotate your display!";
    private final String TITLE_BAR_MESSAGE = "Confirm Display Orientation Change";

    /**
     * Constructor for the OrientationController class.
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

    /**
     * Initializes fields for the controller.
     */
    @Override
    public void initController() {
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Initializes the listeners for the orientation modes combo box.
     */
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

    /**
     * Changes the orientation mode of the display upon user confirmation.
     * 
     * @param displayIndex
     *            - The index of the display to change the orientation mode for
     * @param slotIndex
     *            - The index of the slot to update the orientation mode for
     */
    private void orientationModeAction(int displayIndex, int slotIndex) {
        // Focus on the selected display label
        view.getSelectedDisplayLabel().requestFocusInWindow();

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
     * Shows a confirmation window that asks if the user wants to clear all slots.
     * 
     * @return The return value from the option pane
     */
    private int getUserConfirmation() {
        return JOptionPane.showConfirmDialog(view.getFrame(), CONFIRMATION_MESSAGE, TITLE_BAR_MESSAGE,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
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

                settingsMgr.saveIniSlotDisplayMode(displayId, slotId, invertedDisplayMode.getWidth(),
                        invertedDisplayMode.getHeight(), invertedDisplayMode.getBitDepth(),
                        invertedDisplayMode.getRefreshRate());
            }

            appRefresher.reInitApp();
        }
    }

    @Override
    public void cleanUp() {
    }

}
