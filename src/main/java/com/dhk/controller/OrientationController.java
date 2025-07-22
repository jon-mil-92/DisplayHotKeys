package com.dhk.controller;

import java.awt.DisplayMode;
import java.util.Arrays;
import javax.swing.JOptionPane;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.utility.DisplayModeInverter;
import com.dhk.view.DhkView;

/**
 * Controls the combo box for the orientation of the selected display. Listeners are added to the corresponding view
 * component so that when a new display is selected, the view components are changed to those for the selected display.
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
    private DisplayModeInverter displayModeInverter;
    private SetDisplay setDisplay;
    private AppRefresher appRefresher;

    private final String CONFIRMATION_MESSAGE = "Are you sure you want to change the display orientation?"
            + " Only do this if you can rotate your display!";
    private final String TITLE_BAR_MESSAGE = "Confirm display orientation change.";

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
        displayModeInverter = new DisplayModeInverter();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
        setDisplay = new SetDisplay();
    }

    /**
     * Initializes the listeners for the orientation modes combo box.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            view.getOrientationModes(displayIndex).addActionListener(e -> orientationModeAction(displayIndex));
        }
    }

    /**
     * Changes the orientation of the display upon user confirmation.
     * 
     * @param displayIndex
     *            - The index of the dislay to change the orientation for
     */
    private void orientationModeAction(int displayIndex) {
        // Set the focus on the display IDs label so the clear all slots button does not flash red after confirmation
        view.getDisplayIdsLabel().requestFocusInWindow();

        if (getUserConfirmation() == JOptionPane.YES_OPTION) {
            saveOrientationMode(displayIndex);
        } else {
            int previouslySelectedOrientationMode = model.getOrientationModeForDisplay(displayIndex);
            view.getOrientationModes(displayIndex).setSelectedIndex(previouslySelectedOrientationMode);
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
     * Updates the view with the slot components for the selected orientation mode.
     * 
     * @param displayIndex
     *            - The index of the display to change the orientation for
     */
    private void saveOrientationMode(int displayIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int previouslySelectedOrientationMode = model.getOrientationModeForDisplay(displayIndex);
        int selectedOrientationMode = view.getOrientationModes(displayIndex).getSelectedIndex();

        if (previouslySelectedOrientationMode != selectedOrientationMode) {
            model.setOrientationModeForDisplay(displayIndex, selectedOrientationMode);
            settingsMgr.saveIniOrientationModeForDisplay(displayId, selectedOrientationMode);

            // Determine if going from a landscape orientation mode to a portrait orientation mode
            boolean landscapeToPortrait = (previouslySelectedOrientationMode == 0
                    || previouslySelectedOrientationMode == 2)
                    && (selectedOrientationMode == 1 || selectedOrientationMode == 3);

            // Determine if going from a portrait orientation mode to a landscape orientation mode
            boolean portraitToLandscape = ((previouslySelectedOrientationMode == 1
                    || previouslySelectedOrientationMode == 3)
                    && (selectedOrientationMode == 0 || selectedOrientationMode == 2));

            // Only invert the display modes if going from landscape to portrait or vice versa
            if (landscapeToPortrait || portraitToLandscape) {

                // Invert the saved display mode for each slot for the current display
                for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
                    int slotId = slotIndex + 1;

                    DisplayMode invertedDisplayMode = displayModeInverter
                            .invert(model.getSlot(displayIndex, slotIndex).getDisplayMode());

                    settingsMgr.saveIniSlotDisplayMode(displayId, slotId, invertedDisplayMode.getWidth(),
                            invertedDisplayMode.getHeight(), invertedDisplayMode.getBitDepth(),
                            invertedDisplayMode.getRefreshRate());
                }
            }

            setOrientation(displayIndex);
            appRefresher.reInitApp();
        }
    }

    /**
     * Sets the orientation if the display is connected.
     * 
     * @param displayIndex
     *            - The index of the display to set the orientation mode for
     */
    private void setOrientation(int displayIndex) {
        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.updateDisplayIds();

        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            setDisplay.applyDisplayOrientation(displayId, model.getOrientationModeForDisplay(displayIndex));
        }
    }

    @Override
    public void cleanUp() {
    }

}
