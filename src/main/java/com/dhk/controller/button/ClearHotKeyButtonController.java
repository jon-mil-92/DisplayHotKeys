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
package com.dhk.controller.button;

import com.dhk.controller.HotKeysController;
import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;

/**
 * Controls the Clear Hot Key buttons. Listeners are added to the corresponding view components so that when the Clear
 * Hot Key button is pressed, the corresponding hot key is cleared.
 *
 * @author Jonathan R. Miller
 */
public class ClearHotKeyButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private HotKeysController hotKeysController;

    /**
     * Constructor for the {@link ClearHotKeyButtonController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     * @param hotKeysController
     *            - The controller for hot key management
     */
    public ClearHotKeyButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr,
            HotKeysController hotKeysController) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
        this.hotKeysController = hotKeysController;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton()
                        .addActionListener(e -> clearHotKeyButtonAction(displayIndex, slotIndex));

                initStateChangeListeners(view.getSlot(displayIndex, slotIndex).getClearHotKeyButton(),
                        view.getDefaultFocusComponent());

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
    private void clearHotKeyButtonAction(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();
        view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");
        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
        WindowUtil.refreshWindow(view.getFrame());

        if (hotKeysController != null) {
            hotKeysController.rebuildActiveKeyCodes();
        }

        view.getDefaultFocusComponent().requestFocusInWindow();
    }

}
