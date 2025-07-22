package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * Controls the Clear Hot Key buttons. Listeners are added to the corresponding view components so that when the Clear
 * Hot Key button is pressed, the corresponding hot key is cleared.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ClearHotKeyButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private FrameUpdater frameUpdater;

    /**
     * Constructor for the ClearHotKeyButtonController class.
     * 
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public ClearHotKeyButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
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
     * Initializes the listeners for the clear hot key buttons.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton()
                        .addActionListener(e -> slotClearHotKeyEvent(displayIndex, slotIndex));

                initStateChangeListeners(view.getSlot(displayIndex, slotIndex).getClearHotKeyButton(),
                        view.getDisplayIdsLabel());

                // Enable the clear hot key buttons for the hot keys that are set
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
     * Clears the hot key for the specified slot for the given display.
     * 
     * @param displayIndex
     *            - The index of the display to clear the hot key for
     * @param slotIndex
     *            - The index of the slot to clear the hot key for
     */
    private void slotClearHotKeyEvent(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();
        view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");
        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
        frameUpdater.updateUI();

        // Focus on the display IDs label after clearing the hot key so the change hot key button doesn't gain focus
        view.getDisplayIdsLabel().requestFocusInWindow();
    }

}
