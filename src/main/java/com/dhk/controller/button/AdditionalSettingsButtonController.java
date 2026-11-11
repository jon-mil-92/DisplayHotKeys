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
import com.dhk.view.DhkView;
import com.dhk.view.AdditionalSettingsDialog;

/**
 * Controls the Additional Settings button. Listeners are added to the corresponding view component so that when the
 * Additional Settings button is pressed, a dialog is shown with settings that affect all displays.
 *
 * @author Jonathan R. Miller
 */
public class AdditionalSettingsButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private HotKeysController hotKeysController;
    private AdditionalSettingsDialog additionalSettingsDialog;

    /**
     * Constructor for the {@link AdditionalSettingsButtonController} class.
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
    public AdditionalSettingsButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr,
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
        view.getAdditionalSettingsButton().addActionListener(e -> additionalSettingsButtonAction());

        initStateChangeListeners(view.getAdditionalSettingsButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Shows an "Additional Settings" dialog with settings that affect all displays, wiring its Display Info hot key
     * controls before it is shown.
     */
    private void additionalSettingsButtonAction() {
        AdditionalSettingsDialog dialog = new AdditionalSettingsDialog(model, view);
        additionalSettingsDialog = dialog;

        dialog.getAutoShowInfoOverlaySwitch().addActionListener(e -> autoShowInfoOverlaySwitchAction(dialog));
        dialog.getChangeHotKeyButton().addActionListener(e -> hotKeysController.displayInfoHotKeyChangeEvent(dialog));
        dialog.getClearHotKeyButton().addActionListener(e -> clearDisplayInfoHotKeyButtonAction(dialog));

        initStateChangeListeners(dialog.getClearHotKeyButton(), dialog.getDefaultFocusComponent());

        dialog.showAdditionalSettingsDialog();
    }

    /**
     * Saves whether the info overlay is shown automatically, which takes effect at the next slot apply or display
     * change.
     *
     * @param dialog
     *            - The additional settings dialog showing the Auto Show Info Overlay switch
     */
    private void autoShowInfoOverlaySwitchAction(AdditionalSettingsDialog dialog) {
        boolean autoShowInfoOverlay = dialog.getAutoShowInfoOverlaySwitch().isSelected();

        model.setAutoShowInfoOverlay(autoShowInfoOverlay);
        settingsMgr.saveIniAutoShowInfoOverlay(autoShowInfoOverlay);

        dialog.getDefaultFocusComponent().requestFocusInWindow();
    }

    /**
     * Clears the Display Info hot key and saves the cleared hot key.
     *
     * @param dialog
     *            - The additional settings dialog showing the Display Info hot key
     */
    private void clearDisplayInfoHotKeyButtonAction(AdditionalSettingsDialog dialog) {
        model.getDisplayInfoHotKey().getKeys().clear();
        dialog.setDisplayInfoHotKeyText(model.getDisplayInfoHotKey().getHotKeyString());
        dialog.getClearHotKeyButton().setEnabled(false);
        settingsMgr.saveIniDisplayInfoHotKey(model.getDisplayInfoHotKey());
        hotKeysController.rebuildActiveKeyCodes();

        dialog.getDefaultFocusComponent().requestFocusInWindow();
    }

}
