package com.dhk.controller.button;

import java.awt.DisplayMode;
import javax.swing.JOptionPane;
import com.dhk.controller.DhkController;
import com.dhk.controller.IController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Clear All button. Listeners are added to the corresponding view component so that when the Clear All
 * button is pressed, the display mode, scaling mode, DPI scale percentage, and hot key for each slot for the selected
 * display is set to default.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ClearAllButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private AppRefresher appRefresher;

    private final String CONFIRMATION_MESSAGE = "Are you sure you want to clear all slots for the selected display?";
    private final String TITLE_BAR_MESSAGE = "Confirm Clear All Slots";

    /**
     * Constructor for the ClearAllButtonController class.
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

    /**
     * Creates a new frame updater.
     */
    @Override
    public void initController() {
        displayConfig = model.getDisplayConfig();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Initializes the listeners for the clear all button.
     */
    @Override
    public void initListeners() {
        view.getClearAllButton().addActionListener(e -> clearAllButtonAction());

        initStateChangeListeners(view.getClearAllButton(), view.getSelectedDisplayLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Clears all slots for the selected display and then update the UI.
     */
    private void clearAllButtonAction() {
        // Focus on the selected display label so the clear all slots button does not flash red after confirmation
        view.getSelectedDisplayLabel().requestFocusInWindow();

        if (getUserConfirmation() == JOptionPane.YES_OPTION) {
            clearAllOrientationModes();
            clearAllDisplayModes();
            clearAllScalingModes();
            clearAllDpiScalePercentages();
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
        return JOptionPane.showConfirmDialog(view.getFrame(), CONFIRMATION_MESSAGE, TITLE_BAR_MESSAGE,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Clears all orientation modes for the selected display and save the changes.
     */
    private void clearAllOrientationModes() {
        int displayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[displayIndex];

        for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
            int slotId = slotIndex + 1;

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
            view.getSlot(displayIndex, slotIndex).getDisplayModes().setSelectedIndex(0);
            settingsMgr.saveIniSlotDisplayMode(displayId, slotId, defaultDisplayMode.getWidth(),
                    defaultDisplayMode.getHeight(), defaultDisplayMode.getBitDepth(),
                    defaultDisplayMode.getRefreshRate());
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
