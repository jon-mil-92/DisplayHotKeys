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

import com.dhk.controller.DhkController;
import com.dhk.controller.IController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.model.DisplayMode;
import com.dhk.view.DhkView;

/**
 * Controls the Clear Slot buttons. Listeners are added to the corresponding view components so that when a Clear Slot
 * button is pressed, the display mode, DPI scale percentage, scaling mode, orientation mode, and hot key for that slot
 * are reset to their default values.
 *
 * @author Jonathan R. Miller
 */
public class ClearSlotButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private AppRefresher appRefresher;

    /**
     * Constructor for the {@link ClearSlotButtonController} class.
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
    public ClearSlotButtonController(DhkModel model, DhkView view, DhkController controller,
            SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        displayConfig = settingsMgr.getDisplayConfig();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getClearSlotButton()
                        .addActionListener(e -> clearSlotButtonAction(displayIndex, slotIndex));

                initStateChangeListeners(view.getSlot(displayIndex, slotIndex).getClearSlotButton(),
                        view.getDefaultFocusComponent());
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Clears the specified slot for the given display by resetting every setting to default and updating the UI.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to clear
     */
    private void clearSlotButtonAction(int displayIndex, int slotIndex) {
        clearOrientationMode(displayIndex, slotIndex);
        clearDisplayMode(displayIndex, slotIndex);
        clearDpiScalePercentage(displayIndex, slotIndex);
        clearScalingMode(displayIndex, slotIndex);
        clearHotKey(displayIndex, slotIndex);

        appRefresher.reInitApp();
    }

    /**
     * Clears the orientation mode for the given slot and saves the change.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to clear the orientation mode for
     */
    private void clearOrientationMode(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        // Flag the slot as clearing so the orientation combo box change does not raise the confirmation dialog
        model.getSlot(displayIndex, slotIndex).setClearingSlot(true);
        model.getSlot(displayIndex, slotIndex).setOrientationMode(0);
        view.getSlot(displayIndex, slotIndex).getOrientationModes().setSelectedIndex(0);
        settingsMgr.saveIniSlotOrientationMode(displayId, slotId, 0);
        model.getSlot(displayIndex, slotIndex).setClearingSlot(false);
    }

    /**
     * Clears the display mode for the given slot and saves the change.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to clear the display mode for
     */
    private void clearDisplayMode(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;
        DisplayMode[] displayModes = displayConfig.getLandscapeDisplayModes(displayId);
        DisplayMode defaultDisplayMode = displayModes[0];

        model.getSlot(displayIndex, slotIndex).setDisplayMode(defaultDisplayMode);
        view.getSlot(displayIndex, slotIndex).getResolutions().setSelectedIndex(0);
        view.getSlot(displayIndex, slotIndex).getRefreshRates().setSelectedIndex(0);
        settingsMgr.saveIniSlotDisplayMode(displayId, slotId, defaultDisplayMode);
    }

    /**
     * Clears the DPI scale percentage for the given slot and saves the change.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to clear the DPI scale percentage for
     */
    private void clearDpiScalePercentage(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        model.getSlot(displayIndex, slotIndex).setDpiScalePercentage(100);
        view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setSelectedIndex(0);
        settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, 100);
    }

    /**
     * Clears the scaling mode for the given slot and saves the change.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to clear the scaling mode for
     */
    private void clearScalingMode(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        model.getSlot(displayIndex, slotIndex).setScalingMode(0);
        view.getSlot(displayIndex, slotIndex).getScalingModes().setSelectedIndex(0);
        settingsMgr.saveIniSlotScalingMode(displayId, slotId, 0);
    }

    /**
     * Clears the hot key for the given slot and saves the change.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to clear the hot key for
     */
    private void clearHotKey(int displayIndex, int slotIndex) {
        String displayId = model.getDisplayIds()[displayIndex];
        int slotId = slotIndex + 1;

        model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();
        view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");
        view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
    }

}
