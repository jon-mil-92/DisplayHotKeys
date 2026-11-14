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

import javax.swing.SwingUtilities;

import com.dhk.controller.HotKeysController;
import com.dhk.controller.IController;
import com.dhk.io.NativeInputHook;
import com.dhk.io.RunOnStartupManager;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.AdditionalSettingsDialog;
import com.dhk.view.DhkView;
import com.dhk.view.RunOnStartupNoticeDialog;

/**
 * Controls the Additional Settings button and the dialog it shows, which holds settings that affect all displays. Each
 * setting is applied and saved as it changes, and the saved run on startup state is put in place whenever this
 * controller initializes.
 *
 * @author Jonathan R. Miller
 */
public class AdditionalSettingsButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private HotKeysController hotKeysController;
    private NativeInputHook nativeInputHook;
    private RunOnStartupManager runOnStartupManager;
    private AdditionalSettingsDialog additionalSettingsDialog;
    private boolean applyingRunOnStartup;
    private boolean runOnStartupRequestPending;

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
     * @param nativeInputHook
     *            - The shared native input hook the dialog uses to re-fit after a cross-display drag (may be null)
     */
    public AdditionalSettingsButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr,
            HotKeysController hotKeysController, NativeInputHook nativeInputHook) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
        this.hotKeysController = hotKeysController;
        this.nativeInputHook = nativeInputHook;
    }

    @Override
    public void initController() {
        runOnStartupManager = new RunOnStartupManager();

        applySavedRunOnStartup();
    }

    @Override
    public void initListeners() {
        view.getAdditionalSettingsButton().addActionListener(e -> showAdditionalSettingsDialog(null));

        initStateChangeListeners(view.getAdditionalSettingsButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Shows an "Additional Settings" dialog with its settings wired, so each one is applied and saved as it changes.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    public void showAdditionalSettingsDialog(Runnable onCloseAction) {
        AdditionalSettingsDialog dialog = new AdditionalSettingsDialog(model, view, nativeInputHook);
        additionalSettingsDialog = dialog;

        dialog.getRunOnStartupSwitch().addActionListener(e -> runOnStartupSwitchAction(dialog));
        dialog.getMinimizeToTraySwitch().addActionListener(e -> minimizeToTraySwitchAction(dialog));
        dialog.getCloseToTraySwitch().addActionListener(e -> closeToTraySwitchAction(dialog));
        dialog.getAutoShowInfoOverlaySwitch().addActionListener(e -> autoShowInfoOverlaySwitchAction(dialog));
        dialog.getChangeHotKeyButton().addActionListener(e -> hotKeysController.displayInfoHotKeyChangeEvent(dialog));
        dialog.getClearHotKeyButton().addActionListener(e -> clearDisplayInfoHotKeyButtonAction(dialog));

        initStateChangeListeners(dialog.getClearHotKeyButton(), dialog.getDefaultFocusComponent());

        dialog.showAdditionalSettingsDialog(onCloseAction);
    }

    /**
     * Saves the toggled "run on startup" state and writes it, holding the write while an earlier one is still in flight
     * so only the state switched to last is written.
     *
     * @param dialog
     *            - The additional settings dialog showing the Run on Startup switch
     */
    private void runOnStartupSwitchAction(AdditionalSettingsDialog dialog) {
        boolean runOnStartup = dialog.getRunOnStartupSwitch().isSelected();

        model.setRunOnStartup(runOnStartup);
        dialog.getDefaultFocusComponent().requestFocusInWindow();

        /*
         * A toggle landing mid-write is held rather than written beside the one in flight, since the two spawn
         * processes that would race to leave the startup state whichever finished last. Only the state switched to last
         * is written, so toggles collapse into one follow-up write no matter how many arrive
         */
        if (applyingRunOnStartup) {
            runOnStartupRequestPending = true;

            return;
        }

        writeRunOnStartup(runOnStartup);
    }

    /**
     * Saves whether minimizing the window hides the application in the system tray, and updates the Close to Tray
     * switch, which only applies while it is on.
     *
     * @param dialog
     *            - The additional settings dialog showing the Minimize to Tray switch
     */
    private void minimizeToTraySwitchAction(AdditionalSettingsDialog dialog) {
        boolean minimizeToTray = dialog.getMinimizeToTraySwitch().isSelected();

        model.setMinimizeToTray(minimizeToTray);
        settingsMgr.saveIniMinimizeToTray(minimizeToTray);
        dialog.updateCloseToTrayAvailability();

        dialog.getDefaultFocusComponent().requestFocusInWindow();
    }

    /**
     * Saves whether closing the window hides the application in the system tray instead of exiting, which takes effect
     * at the next close.
     *
     * @param dialog
     *            - The additional settings dialog showing the Close to Tray switch
     */
    private void closeToTraySwitchAction(AdditionalSettingsDialog dialog) {
        boolean closeToTray = dialog.getCloseToTraySwitch().isSelected();

        model.setCloseToTray(closeToTray);
        settingsMgr.saveIniCloseToTray(closeToTray);

        dialog.getDefaultFocusComponent().requestFocusInWindow();
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

    /**
     * Applies the saved "run on startup" state and shows what came of it, since the switch is built from the saved
     * state while only the apply can tell whether anything is left carrying it.
     */
    private void applySavedRunOnStartup() {
        boolean savedRunOnStartup = settingsMgr.getIniRunOnStartup();

        applyingRunOnStartup = true;

        // Registering the task costs seconds, so it stays off the startup path where nothing waits on it
        Thread savedStateApplier = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean startsOnLogon = runOnStartupManager.applySavedRunOnStartup(savedRunOnStartup);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        finishApplySavedRunOnStartup(savedRunOnStartup, startsOnLogon);
                    }
                });
            }
        });

        savedStateApplier.setDaemon(true);
        savedStateApplier.start();
    }

    /**
     * Shows what will really start the application upon login, correcting the switch when the saved state could not be
     * put in place. A switch toggled while the apply ran asked for a state of its own, which supersedes this one.
     *
     * @param savedRunOnStartup
     *            - The saved state the apply was given
     * @param startsOnLogon
     *            - Whether the application will start upon login
     */
    private void finishApplySavedRunOnStartup(boolean savedRunOnStartup, boolean startsOnLogon) {
        applyingRunOnStartup = false;

        if (runOnStartupRequestPending) {
            runOnStartupRequestPending = false;

            writeRunOnStartup(model.isRunOnStartup());

            return;
        }

        if (startsOnLogon == savedRunOnStartup) {
            return;
        }

        // The saved state could not be put in place, so show and store the one the account was left with
        showRunOnStartup(startsOnLogon);
        settingsMgr.saveIniRunOnStartup(startsOnLogon);

        new RunOnStartupNoticeDialog(view.getFrame()).showChangedNotice(startsOnLogon);
    }

    /**
     * Writes the given "run on startup" state off the event dispatch thread, since the processes it spawns would
     * otherwise freeze the window.
     *
     * @param runOnStartup
     *            - The state to write
     */
    private void writeRunOnStartup(boolean runOnStartup) {
        applyingRunOnStartup = true;

        Thread runOnStartupWriter = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean applied = applyRunOnStartup(runOnStartup);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        finishRunOnStartupSwitchAction(runOnStartup, applied);
                    }
                });
            }
        });

        // Closing the application mid-write must not be held up by the processes this spawns
        runOnStartupWriter.setDaemon(true);
        runOnStartupWriter.start();
    }

    /**
     * Saves the applied "run on startup" state, or puts the switch back and reports that it could not be applied.
     *
     * @param runOnStartup
     *            - The state the user asked for
     * @param applied
     *            - Whether that state was applied
     */
    private void finishRunOnStartupSwitchAction(boolean runOnStartup, boolean applied) {
        applyingRunOnStartup = false;

        /*
         * Toggles that landed during the write asked for a newer state than this one, so its outcome is theirs to
         * settle. Reporting it here would undo the state they show and report a state the user has already moved off
         */
        if (runOnStartupRequestPending) {
            runOnStartupRequestPending = false;

            writeRunOnStartup(model.isRunOnStartup());

            return;
        }

        // Only the write can tell whether the shown state is real, so undo it here when it turned out not to be
        if (!applied) {
            showRunOnStartup(!runOnStartup);

            // The saved value follows the switch, so store the state left in place rather than the one asked for
            settingsMgr.saveIniRunOnStartup(!runOnStartup);

            new RunOnStartupNoticeDialog(view.getFrame()).showFailedNotice(runOnStartup);

            return;
        }

        settingsMgr.saveIniRunOnStartup(runOnStartup);
    }

    /**
     * Shows the given "run on startup" state in the model and on the Run on Startup switch of the latest dialog.
     *
     * @param runOnStartup
     *            - The state to show
     */
    private void showRunOnStartup(boolean runOnStartup) {
        model.setRunOnStartup(runOnStartup);

        if (additionalSettingsDialog != null) {
            additionalSettingsDialog.getRunOnStartupSwitch().setSelected(runOnStartup);
        }
    }

    /**
     * Applies the wanted "run on startup" state through the startup manager.
     *
     * @param runOnStartup
     *            - Whether the application should start upon login
     *
     * @return True if the wanted state was applied, false if it could not be
     */
    private boolean applyRunOnStartup(boolean runOnStartup) {
        if (runOnStartup) {
            return runOnStartupManager.addToStartup();
        }

        return runOnStartupManager.removeFromStartup();
    }

}
