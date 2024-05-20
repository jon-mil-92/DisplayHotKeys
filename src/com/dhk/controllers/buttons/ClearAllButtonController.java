package com.dhk.controllers.buttons;

import java.awt.DisplayMode;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import com.dhk.controllers.Controller;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Clear All button. Listeners are added to the corresponding view component so that when the
 * Clear All button is pressed, the display mode, scaling mode, DPI scale percentage, and hot key for each slot for the
 * selected display is set to default.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ClearAllButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private FrameUpdater frameUpdater;
    private DisplayConfig displayConfig;

    /**
     * Constructor for the ClearAllButtonController class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public ClearAllButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method creates a new frame updater.
     */
    @Override
    public void initController() {
        // Create the frame updater object that will be used to refresh the frame once all slots are cleared.
        frameUpdater = new FrameUpdater(view);

        // Get the current display config from the model.
        displayConfig = model.getDisplayConfig();
    }

    /**
     * This method initializes the listeners for the clear all button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the clear all button action.
        view.getClearAllButton().addActionListener(e -> clearAllButtonAction());

        // Set the state change listener for the clear all button.
        view.getClearAllButton().addChangeListener(e -> clearAllButtonStateChangeAction());

        // Set the focus listener for the clear all button from the view.
        view.getClearAllButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the clear all button is focused.
                view.getClearAllButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the clear all button is not focused.
                view.getClearAllButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the clear all button.
        view.getClearAllButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the clear all button when the mouse hovers over it.
                view.getClearAllButton().requestFocusInWindow();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Set the focus on the display IDs label when the mouse leaves the button.
                view.getDisplayIdsLabel().requestFocusInWindow();
            }
        });
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Clear all slots for the selected display and then update the UI.
     */
    private void clearAllButtonAction() {
        // Confirm whether or not all slots should be cleared.
        int confirmationResult = getUserConfirmation();

        // Only clear all slots if the user clicked on the "Yes" option.
        if (confirmationResult == JOptionPane.YES_OPTION) {
            // Set the default values for all slots.
            clearAllDisplayModes();
            clearAllScalingModes();
            clearAllDpiScalePercentages();
            clearAllHotKeys();

            // Update the view's frame.
            frameUpdater.updateUI();
        }

        // Set the focus on the display IDs label so the clear all slots button does not flash red after confirmation.
        view.getDisplayIdsLabel().requestFocusInWindow();
    }

    /**
     * This method shows a confirmation window that asks if the user wants to clear all slots.
     * 
     * @return The return value from the option pane.
     */
    private int getUserConfirmation() {
        // Create a confirmation message string to be used in the confirmation window.
        String confirmationMessage = "Are you sure you want to clear all slots?";

        // Create a message to show in the title bar of the confirmation window.
        String titleBarMessage = "Confirm clear all slots.";

        // Create an option pane to confirm if the user wants to clear all slots.
        int confirmationResult = JOptionPane.showConfirmDialog(view.getFrame(), confirmationMessage, titleBarMessage,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        return confirmationResult;
    }

    /**
     * Clear all display modes for the selected display and save the changes.
     */
    private void clearAllDisplayModes() {
        // Get the display index for the selected display from the view.
        int displayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the ID for the selected display.
        String displayId = model.getDisplayIds()[displayIndex];

        // Get the array of supported display modes for the selected display.
        DisplayMode[] displayModes = displayConfig.getDisplayModes(displayId);

        // Get the default highest display mode.
        DisplayMode defaultDisplayMode = displayModes[0];

        // For all slots...
        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            // The ID for the slot starts at 1.
            int slotId = slotIndex + 1;

            // Set the slot's display mode to the default highest value.
            model.getSlot(displayIndex, slotIndex).setDisplayMode(defaultDisplayMode);

            // Update the display mode in the view for the slot.
            view.getSlot(displayIndex, slotIndex).getDisplayModes().setSelectedIndex(0);

            // Save the display mode in the settings.
            settingsMgr.saveIniSlotDisplayMode(displayId, slotId, defaultDisplayMode.getWidth(),
                    defaultDisplayMode.getHeight(), defaultDisplayMode.getBitDepth(),
                    defaultDisplayMode.getRefreshRate());
        }
    }

    /**
     * Clear all scaling modes for the selected display and save the changes.
     */
    private void clearAllScalingModes() {
        // Get the display index for the selected display from the view.
        int displayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the ID for the selected display.
        String displayId = model.getDisplayIds()[displayIndex];

        // For all slots...
        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            // The ID for the slot starts at 1.
            int slotId = slotIndex + 1;

            // Reset the scaling mode from the slot's model.
            model.getSlot(displayIndex, slotIndex).setScalingMode(0);

            // Update the scaling mode in the view for the slot.
            view.getSlot(displayIndex, slotIndex).getScalingModes().setSelectedIndex(0);

            // Save the scaling mode in the settings.
            settingsMgr.saveIniSlotScalingMode(displayId, slotId, 0);
        }
    }

    /**
     * Clear all DPI scale percentages for the selected display and save the changes.
     */
    private void clearAllDpiScalePercentages() {
        // Get the display index for the selected display from the view.
        int displayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the ID for the selected display.
        String displayId = model.getDisplayIds()[displayIndex];

        // For all slots...
        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            // The ID for the slot starts at 1.
            int slotId = slotIndex + 1;

            // Reset the DPI scale percentage from the slot's model.
            model.getSlot(displayIndex, slotIndex).setDpiScalePercentage(100);

            // Update the DPI scale percentage in the view for the slot.
            view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setSelectedIndex(0);

            // Save the DPI scale percentage in the settings.
            settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, 100);
        }
    }

    /**
     * Clear all hot keys for the selected display and save the changes.
     */
    private void clearAllHotKeys() {
        // Get the display index for the selected display from the view.
        int displayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the ID for the selected display.
        String displayId = model.getDisplayIds()[displayIndex];

        // For all slots...
        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            // The ID for the slot starts at 1.
            int slotId = slotIndex + 1;

            // Clear the hot key's keys from the slot's model.
            model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();

            // Update the hot key in the view for the slot.
            view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");

            // Disable the clear hot key button after clearing the hot key.
            view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);

            // Save the hot key in the settings.
            settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
        }
    }

    /**
     * This method changes the clear all button icon depending on the button's state.
     */
    private void clearAllButtonStateChangeAction() {
        // If the user is holding the action button on the clear all button...
        if (view.getClearAllButton().getModel().isArmed()) {
            // Use the pressed icon for the clear all button.
            view.getClearAllButton().setIcon(view.getClearAllButton().getClearAllPressedIcon());
        }
        // If the user is hovering on the clear all button...
        else if (view.getClearAllButton().getModel().isRollover()) {
            // Use the hover icon for the clear all button.
            view.getClearAllButton().setIcon(view.getClearAllButton().getClearAllHoverIcon());
        }
        // Otherwise, if the user is not interacting with the clear all button...
        else {
            // Use the idle icon for the clear all button.
            view.getClearAllButton().setIcon(view.getClearAllButton().getClearAllIdleIcon());
        }
    }
}
