package com.dhk.controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Clear Hot Key buttons. Listeners are added to the corresponding view components so that when
 * the Clear Hot Key button is pressed, the corresponding hot key is cleared.
 * 
 * @author Jonathan Miller
 * @version 1.2.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ClearHotKeyButtonController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settings;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the ClearHotKeyButtonController class.
     * 
     * @param model    - The model for the application.
     * @param view     - The view for the application.
     * @param settings - The settings manager of the application.
     */
    public ClearHotKeyButtonController(DhkModel model, DhkView view, SettingsManager settings) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settings = settings;

        // Create the frame updater object that will be used to refresh the frame once a hot key is cleared.
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * This method initializes the application's listeners.
     */
    public void initListeners() {
        // Set the action listener for each clear hot key button in the view.
        for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
            // The index for the slot view to add an action listener to.
            int slotIndex = i;

            // Set action listeners for the clear hot key button presses from the view.
            view.getSlot(slotIndex).getClearHotKeyButton().addActionListener(e -> slotClearHotKeyEvent(slotIndex));

            // Set mouse listeners for the clear hot key buttons from the view.
            view.getSlot(slotIndex).getClearHotKeyButton().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    // Set the focus on the clear hot key buttons when the mouse hovers over it.
                    view.getSlot(slotIndex).getClearHotKeyButton().requestFocusInWindow();
                }
            });
        }
    }

    /**
     * This method clears the hot key for the specified slot.
     * 
     * @param slotIndex - The index for the slot to clear the hot key for.
     */
    private void slotClearHotKeyEvent(int slotIndex) {
        // Clear the hot key's keys from the slot's model.
        model.getSlot(slotIndex).getHotKey().getKeys().clear();

        // Update the hot key in the view for the slot.
        view.getSlot(slotIndex).getHotKey().setText("Not Set!");

        // Update the frame.
        frameUpdater.updateUI();

        // Save the hot key in the settings.
        settings.saveIniSlotHotKey(slotIndex + 1, model.getSlot(slotIndex).getHotKey());
    }
}
