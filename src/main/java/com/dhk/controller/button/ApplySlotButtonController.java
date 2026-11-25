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

import java.util.Arrays;

import com.dhk.controller.DhkController;
import com.dhk.controller.IController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Apply Slot button. Listeners are added to the corresponding view component so that when the Apply Slot
 * button is pressed, the associated slot display settings are immediately applied.
 *
 * @author Jonathan R. Miller
 */
public class ApplySlotButtonController extends AbstractButtonController implements IController {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private SetDisplay setDisplay;
    private AppRefresher appRefresher;

    /**
     * Constructor for the {@link ApplySlotButtonController} class.
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
    public ApplySlotButtonController(DhkModel model, DhkView view, DhkController controller,
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

                view.getSlot(displayIndex, slotIndex).getApplySlotButton()
                        .addActionListener(e -> applySlotButtonAction(displayIndex, slotIndex));

                initStateChangeListeners(view.getSlot(displayIndex, slotIndex).getApplySlotButton(),
                        view.getDefaultFocusComponent());
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Immediately applies the associated slot display settings.
     *
     * @param displayIndex
     *            - The index of the display to set the display settings for
     * @param slotIndex
     *            - The index of the slot to set the display settings for
     */
    private void applySlotButtonAction(int displayIndex, int slotIndex) {
        displayConfig.updateConnectedDisplays();

        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            // Dismiss a showing tray menu and display info panels, since both were placed against the old geometry
            controller.displayConfigurationChanged();

            // Capture the display arrangement before the display reconfiguration rearranges the display
            String[] arrangementSnapshot = displayConfig.captureArrangement();

            setDisplay.applyDisplayOrientation(displayId, model.getSlot(displayIndex, slotIndex).getOrientationMode());
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshNumerator(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshDenominator(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode());
            setDisplay.preserveArrangement(arrangementSnapshot);

            // The apply returns once it is in effect, and each window waits for AWT to catch up before it is re-fitted
            appRefresher.reInitApp();
        }
    }

}
