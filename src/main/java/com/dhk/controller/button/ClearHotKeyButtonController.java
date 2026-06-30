/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.dhk.controller.button;

import com.dhk.controller.HotKeysController;
import com.dhk.controller.IController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.utility.FrameUtil;
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
                        .addActionListener(e -> slotClearHotKeyEvent(displayIndex, slotIndex));

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
    private void slotClearHotKeyEvent(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();
        view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");
        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
        FrameUtil.refreshFrame(view.getFrame());

        if (hotKeysController != null) {
            hotKeysController.rebuildActiveKeyCodes();
        }

        view.getDefaultFocusComponent().requestFocusInWindow();
    }

}
