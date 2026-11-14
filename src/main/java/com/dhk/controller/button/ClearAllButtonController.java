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

import javax.swing.JOptionPane;

import com.dhk.controller.DhkController;
import com.dhk.controller.IController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.model.DisplayMode;
import com.dhk.view.ConfirmationDialog;
import com.dhk.view.DhkView;

/**
 * Controls the Clear All button. Listeners are added to the corresponding view component so that when the Clear All
 * button is pressed, the display mode, DPI scale percentage, scaling mode, and hot key for each slot for the selected
 * display is set to default.
 *
 * @author Jonathan R. Miller
 */
public class ClearAllButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private AppRefresher appRefresher;

    /**
     * Question the confirmation dialog asks before clearing every slot of the selected display.
     */
    private static final String CONFIRMATION_MESSAGE = "Are you sure you want to clear all slots for the selected display?";

    /**
     * Title of the confirmation dialog shown before clearing all slots.
     */
    private static final String TITLE_BAR_MESSAGE = "Confirm Clear All Slots";

    /**
     * Constructor for the {@link ClearAllButtonController} class.
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
    public ClearAllButtonController(DhkModel model, DhkView view, DhkController controller,
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
        view.getClearAllButton().addActionListener(e -> clearAllButtonAction());

        initStateChangeListeners(view.getClearAllButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Clears all slots for the selected display and then update the UI.
     */
    private void clearAllButtonAction() {
        view.getDefaultFocusComponent().requestFocusInWindow();

        if (getUserConfirmation() == JOptionPane.YES_OPTION) {
            clearAllOrientationModes();
            clearAllDisplayModes();
            clearAllDpiScalePercentages();
            clearAllScalingModes();
            clearAllHotKeys();

            appRefresher.reInitApp();
        }
    }

    /**
     * Shows a confirmation window that asks if the user wants to clear all slots.
     *
     * @return The return value from the option pane
     */
    private int getUserConfirmation() {
        ConfirmationDialog confirmationDialog = new ConfirmationDialog(view.getFrame(), TITLE_BAR_MESSAGE,
                CONFIRMATION_MESSAGE);

        return confirmationDialog.showConfirmationDialog();
    }

    /**
     * Clears all orientation modes for the selected display and save the changes.
     */
    private void clearAllOrientationModes() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[displayIndex];

        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            int slotId = slotIndex + 1;

            // Flag the slot as clearing so the orientation combo box change does not raise the confirmation dialog
            model.getSlot(displayIndex, slotIndex).setClearingSlot(true);
            model.getSlot(displayIndex, slotIndex).setOrientationMode(0);
            view.getSlot(displayIndex, slotIndex).getOrientationModes().setSelectedIndex(0);
            settingsMgr.saveIniSlotOrientationMode(displayId, slotId, 0);
            model.getSlot(displayIndex, slotIndex).setClearingSlot(false);
        }
    }

    /**
     * Clears all display modes for the selected display and save the changes.
     */
    private void clearAllDisplayModes() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[displayIndex];

        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            int slotId = slotIndex + 1;
            DisplayMode[] displayModes = displayConfig.getLandscapeDisplayModes(displayId);
            DisplayMode defaultDisplayMode = displayModes[0];

            model.getSlot(displayIndex, slotIndex).setDisplayMode(defaultDisplayMode);
            view.getSlot(displayIndex, slotIndex).getResolutions().setSelectedIndex(0);
            view.getSlot(displayIndex, slotIndex).getRefreshRates().setSelectedIndex(0);
            settingsMgr.saveIniSlotDisplayMode(displayId, slotId, defaultDisplayMode);
        }
    }

    /**
     * Clears all DPI scale percentages for the selected display and save the changes.
     */
    private void clearAllDpiScalePercentages() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[displayIndex];

        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            int slotId = slotIndex + 1;

            model.getSlot(displayIndex, slotIndex).setDpiScalePercentage(100);
            view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setSelectedIndex(0);
            settingsMgr.saveIniSlotDpiScalePercentage(displayId, slotId, 100);
        }
    }

    /**
     * Clears all scaling modes for the selected display and save the changes.
     */
    private void clearAllScalingModes() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[displayIndex];

        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            int slotId = slotIndex + 1;

            model.getSlot(displayIndex, slotIndex).setScalingMode(0);
            view.getSlot(displayIndex, slotIndex).getScalingModes().setSelectedIndex(0);
            settingsMgr.saveIniSlotScalingMode(displayId, slotId, 0);
        }
    }

    /**
     * Clears all hot keys for the selected display and save the changes.
     */
    private void clearAllHotKeys() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[displayIndex];

        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            int slotId = slotIndex + 1;

            model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().clear();
            view.getSlot(displayIndex, slotIndex).getHotKey().setText("Not Set");
            view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
            settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(displayIndex, slotIndex).getHotKey());
        }
    }

}
