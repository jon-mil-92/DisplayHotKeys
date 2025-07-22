package com.dhk.controller.button;

import java.util.Arrays;
import com.dhk.controller.IController;
import com.dhk.controller.DhkController;
import com.dhk.io.DisplayConfig;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Controls the Apply Display Mode button. Listeners are added to the corresponding view component so that when the
 * Apply Display Mode button is pressed, the associated display mode is immediately applied.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ApplyDisplayModeButtonController extends AbstractButtonController implements IController {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private SetDisplay setDisplay;
    private AppRefresher appRefresher;

    /**
     * Constructor for the ApplyDisplayModeButtonController class.
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
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Initializes the listeners for the apply display mode button.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            for (int j = 0; j < model.getMaxNumOfSlots(); j++) {
                int slotIndex = j;

                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton()
                        .addActionListener(e -> applyDisplayModeButtonAction(displayIndex, slotIndex));

                initStateChangeListeners(view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton(),
                        view.getDisplayIdsLabel());
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
        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.updateDisplayIds();

        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getBitDepth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshRate(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

            // Re-initialize the app to prevent window corruption
            appRefresher.reInitApp();
        }
    }

}
