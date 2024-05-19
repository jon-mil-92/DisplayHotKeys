package com.dhk.controllers.buttons;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.controllers.Controller;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.ui.buttons.ClearHotKeyButton;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Clear Hot Key buttons. Listeners are added to the corresponding view components so that when
 * the Clear Hot Key button is pressed, the corresponding hot key is cleared.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ClearHotKeyButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the ClearHotKeyButtonController class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public ClearHotKeyButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
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
        // Create the frame updater object that will be used to refresh the frame once the hot key is cleared.
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * This method initializes the listeners for the clear hot key buttons.
     */
    @Override
    public void initListeners() {
        // For each connected display...
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            // The display index for the current display to add an action listener to.
            int displayIndex = i;

            // Set the action listener for each clear hot key button in the view.
            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                // The index for the slot view to add an action listener to.
                int slotIndex = j;

                // Set action listeners for the clear hot key button presses from the view.
                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton()
                        .addActionListener(e -> slotClearHotKeyEvent(displayIndex, slotIndex));

                // Set the state change listener for the clear hot key button.
                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton()
                        .addChangeListener(e -> clearHotKeyButtonStateChangeAction(displayIndex, slotIndex));

                // Set the focus listener for the clear hot key button from the view.
                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        // Switch to the rollover state when the clear hot key button is focused.
                        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().getModel().setRollover(true);
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        // Leave the rollover state when the clear hot key button is not focused.
                        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().getModel().setRollover(false);
                    }
                });

                // Set the mouse listener for the clear hot key button.
                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        // Set the focus on the clear hot key button when the mouse hovers over it.
                        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().requestFocusInWindow();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        // Set the focus on the display IDs label when the mouse leaves the button.
                        view.getDisplayIdsLabel().requestFocusInWindow();
                    }
                });

                // Enable the clear hot key buttons for the hot keys that are set.
                if (model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().size() > 0) {
                    view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(true);
                }
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * This method clears the hot key for the specified slot for the given display.
     * 
     * @param displayIndex - The index of the display to clear the hot key for.
     * @param slotIndex    - The index of the slot to clear the hot key for.
     */
    private void slotClearHotKeyEvent(int displayIndex, int slotIndex) {
        // Get the ID for the given display.
        String displayId = model.getDisplayIds()[displayIndex];

        // The ID for the slot starts at 1.
        int slotId = slotIndex + 1;

        // Clear the hot key's keys from the slot's model.
        model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();

        // Update the hot key in the view for the slot.
        view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");

        // Disable the clear hot key button after clearing the hot key.
        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);

        // Update the view's frame.
        frameUpdater.updateUI();

        // Save the hot key in the settings.
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
        
        // Focus on the display IDs label after clearing the hot key so the change hot key button doesn't gain focus.
        view.getDisplayIdsLabel().requestFocusInWindow();
    }

    /**
     * This method changes the clear hot key button icon depending on the button's state.
     * 
     * @param displayIndex - The index of the display to change the clear hot key button icon for.
     * @param slotIndex    - The index of the slot to change the clear hot key button icon for.
     */
    private void clearHotKeyButtonStateChangeAction(int displayIndex, int slotIndex) {
        // Get the clear hot key button for the given slot.
        ClearHotKeyButton clearHotKeyButton = view.getSlot(displayIndex, slotIndex).getClearHotKeyButton();

        // If the user is holding the action button on the clear hot key button...
        if (clearHotKeyButton.getModel().isArmed()) {
            // Use the pressed icon for the clear hot key button.
            clearHotKeyButton.setIcon(clearHotKeyButton.getClearHotKeyPressedIcon());
        }
        // If the user is hovering on the clear hot key button...
        else if (clearHotKeyButton.getModel().isRollover()) {
            // Use the hover icon for the clear hot key button.
            clearHotKeyButton.setIcon(clearHotKeyButton.getClearHotKeyHoverIcon());
        }
        // Otherwise, if the user is not interacting with the clear hot key button...
        else {
            // Use the idle icon for the clear hot key button.
            clearHotKeyButton.setIcon(clearHotKeyButton.getClearHotKeyIdleIcon());
        }
    }
}
