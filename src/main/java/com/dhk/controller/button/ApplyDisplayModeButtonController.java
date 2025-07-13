package com.dhk.controller.button;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import com.dhk.controller.Controller;
import com.dhk.controller.DhkController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the Apply Display Mode button. Listeners are added to the corresponding view component so that
 * when the Apply Display Mode button is pressed, the associated display mode is immediately applied.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ApplyDisplayModeButtonController implements Controller {
    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private SetDisplay setDisplay;
    private AppRefresher appRefresher;

    /**
     * Constructor for the ApplyDisplayModeButtonController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public ApplyDisplayModeButtonController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        // Get the application's model, view, controller, and settings manager.
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        // Initialize the set display object that will immediately apply the new display settings.
        setDisplay = new SetDisplay();

        // Initialize the app refresher that will refresh the app after applying display modes.
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * This method initializes the listeners for the apply display mode button.
     */
    @Override
    public void initListeners() {
        // For each connected display...
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            // The display index for the current display to add an action listener to.
            int displayIndex = i;

            // Set the action listener for each slot in the view.
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                // The index for the slot view to add an action listener to.
                int slotIndex = j;

                // Start the action listener for the apply display mode button action.
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton()
                        .addActionListener(e -> applyDisplayModeButtonAction(displayIndex, slotIndex));

                // Set the state change listener for the apply display mode button.
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton()
                        .addChangeListener(e -> applyDisplayModeButtonStateChangeAction(displayIndex, slotIndex));

                // Set the focus listener for the apply display mode button.
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        // Switch to the rollover state when the apply display mode button is focused.
                        view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getModel().setRollover(true);
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        // Leave the rollover state when the apply display mode button is not focused.
                        view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getModel().setRollover(false);
                    }
                });

                // Set the mouse listener for the apply display mode button.
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        // Set the focus on the apply display mode button when the mouse hovers over it.
                        view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().requestFocusInWindow();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        // Set the focus on the display IDs label when the mouse leaves the button.
                        view.getDisplayIdsLabel().requestFocusInWindow();
                    }
                });
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Immediately apply the associated display mode.
     * 
     * @param displayIndex - The index of the display to set the display settings for.
     * @param slotIndex    - The index of the slot to set the display settings for.
     */
    private void applyDisplayModeButtonAction(int displayIndex, int slotIndex) {
        // Get the current display configuration.
        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.updateDisplayIds();

        // Get the ID for the given display index.
        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed...
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            // Set the display settings.
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getBitDepth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshRate(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

            // Re-initialize the app to prevent window corruption.
            appRefresher.reInitApp();
        }
    }

    /**
     * This method changes the apply display mode button icon depending on the button's state.
     */
    private void applyDisplayModeButtonStateChangeAction(int displayIndex, int slotIndex) {
        // If the user is holding the action button on the apply display mode button...
        if (view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getModel().isArmed()) {
            // Use the pressed icon for the apply display mode button.
            view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setIcon(
                    view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getApplyDisplayModePressedIcon());
        }
        // If the user is hovering on the apply display mode button...
        else if (view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getModel().isRollover()) {
            // Use the hover icon for the apply display mode button.
            view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setIcon(
                    view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getApplyDisplayModeHoverIcon());
        }
        // Otherwise, if the user is not interacting with the apply display mode button...
        else {
            // Use the idle icon for the apply display mode button.
            view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setIcon(
                    view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().getApplyDisplayModeIdleIcon());
        }
    }
}
