package com.dhk.controllers.buttons;

import com.dhk.controllers.Controller;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Clear Hot Key buttons. Listeners are added to the corresponding view components so that when
 * the Clear Hot Key button is pressed, the corresponding hot key is cleared.
 * 
 * @author Jonathan Miller
 * @version 1.3.0
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
        // Create the frame updater object that will be used to refresh the frame once all slots are cleared.
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * This method initializes the application's listeners.
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
        String displayId = settingsMgr.getDisplayIds()[displayIndex];

        // The ID for the slot starts at 1.
        int slotId = slotIndex + 1;

        // Clear the hot key's keys from the slot's model.
        model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();

        // Update the hot key in the view for the slot.
        view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");

        // Update the view's frame.
        frameUpdater.updateUI();

        // Save the hot key in the settings.
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
    }
}
