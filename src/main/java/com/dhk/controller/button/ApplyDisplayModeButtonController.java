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

import java.util.Arrays;

import javax.swing.Timer;

import com.dhk.controller.DhkController;
import com.dhk.controller.IController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.model.FramePlacement;
import com.dhk.utility.FrameUtil;
import com.dhk.view.DhkView;

/**
 * Controls the Apply Display Mode button. Listeners are added to the corresponding view component so that when the
 * Apply Display Mode button is pressed, the associated display mode is immediately applied.
 *
 * @author Jonathan R. Miller
 */
public class ApplyDisplayModeButtonController extends AbstractButtonController implements IController {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private SetDisplay setDisplay;
    private AppRefresher appRefresher;

    private static final int REINIT_DELAY_MS = 400;

    /**
     * Constructor for the {@link ApplyDisplayModeButtonController} class.
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
    public ApplyDisplayModeButtonController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        setDisplay = new SetDisplay();
        displayConfig = settingsMgr.getDisplayConfig();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton()
                        .addActionListener(e -> applyDisplayModeButtonAction(displayIndex, slotIndex));

                initStateChangeListeners(view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton(),
                        view.getDefaultFocusComponent());
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Immediately applies the associated display mode.
     *
     * @param displayIndex
     *            - The index of the display to set the display settings for
     * @param slotIndex
     *            - The index of the slot to set the display settings for
     */
    private void applyDisplayModeButtonAction(int displayIndex, int slotIndex) {
        displayConfig.updateConnectedDisplays();

        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            // Capture the frame placement before the display reconfiguration relocates the window
            FramePlacement placement = FrameUtil.capturePlacement(view.getFrame());

            setDisplay.applyDisplayOrientation(displayId, model.getSlot(displayIndex, slotIndex).getOrientationMode());
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getBitDepth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshRate(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

            /*
             * Re-initialize the app to prevent window corruption, but defer briefly so the display reconfiguration
             * settles first; otherwise the rebuilt frame is placed against stale display bounds and jumps up and to the
             * left. The Timer fires once on the EDT. The placement captured above is reproduced, since the OS will have
             * moved the existing frame
             */
            Timer reInitTimer = new Timer(REINIT_DELAY_MS, e -> appRefresher.reInitApp(placement));
            reInitTimer.setRepeats(false);
            reInitTimer.start();
        }
    }

}
