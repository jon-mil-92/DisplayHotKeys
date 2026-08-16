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

import javax.swing.SwingUtilities;

import com.dhk.controller.IController;
import com.dhk.io.RunOnStartupManager;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.model.button.ThemeableToggleButton;
import com.dhk.view.DhkView;
import com.dhk.view.RunOnStartupNoticeDialog;

/**
 * Controls the Run On Startup button, which toggles whether the application launches on user login. The button and the
 * saved value both follow what is really starting the application, so a state that could not be applied is corrected
 * rather than left showing.
 *
 * @author Jonathan R. Miller
 */
public class RunOnStartupButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private RunOnStartupManager runOnStartupManager;
    private boolean applyingRunOnStartup;
    private boolean runOnStartupRequestPending;

    /**
     * Constructor for the {@link RunOnStartupButtonController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public RunOnStartupButtonController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        runOnStartupManager = new RunOnStartupManager();

        applySavedRunOnStartup();
    }

    @Override
    public void initListeners() {
        view.getRunOnStartupButton().addActionListener(e -> runOnStartupButtonAction());

        initStateChangeListeners(view.getRunOnStartupButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Shows the toggled "run on startup" state and writes it, holding the write while an earlier one is still in flight
     * so every click shows while only the state clicked last is written.
     */
    private void runOnStartupButtonAction() {
        /*
         * The button repaints from the state change that ends this click, which reads the on state as it is right now,
         * so the toggle has to show here rather than after the write finishes or the icon keeps the old state
         */
        boolean runOnStartup = !model.isRunOnStartup();

        showRunOnStartup(runOnStartup);

        /*
         * A click landing mid-write is held rather than written beside the one in flight, since the two spawn processes
         * that would race to leave the startup state whichever finished last. Only the state shown by the click that
         * lands last is written, so clicks collapse into one follow-up write no matter how many arrive
         */
        if (applyingRunOnStartup) {
            runOnStartupRequestPending = true;

            return;
        }

        writeRunOnStartup(runOnStartup);
    }

    /**
     * Applies the saved "run on startup" state on launch and shows what came of it, since the button is built from the
     * saved state while only the apply can tell whether anything is left carrying it.
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
     * Shows what will really start the application upon login, correcting the button when the saved state could not be
     * put in place. A click that landed while the apply ran asked for a state of its own, which supersedes this one.
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
        repaintRunOnStartupButton();
        settingsMgr.saveIniRunOnStartup(startsOnLogon);

        new RunOnStartupNoticeDialog().showChangedNotice(startsOnLogon);
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
                        finishRunOnStartupButtonAction(runOnStartup, applied);
                    }
                });
            }
        });

        // Closing the application mid-write must not be held up by the processes this spawns
        runOnStartupWriter.setDaemon(true);
        runOnStartupWriter.start();
    }

    /**
     * Saves the applied "run on startup" state, or puts the button back and reports that it could not be applied.
     *
     * @param runOnStartup
     *            - The state the user asked for
     * @param applied
     *            - Whether that state was applied
     */
    private void finishRunOnStartupButtonAction(boolean runOnStartup, boolean applied) {
        applyingRunOnStartup = false;

        /*
         * Clicks that landed during the write asked for a newer state than this one, so its outcome is theirs to
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
            repaintRunOnStartupButton();

            // The saved value follows the button, so store the state left in place rather than the one asked for
            settingsMgr.saveIniRunOnStartup(!runOnStartup);

            new RunOnStartupNoticeDialog().showFailedNotice(runOnStartup);

            return;
        }

        settingsMgr.saveIniRunOnStartup(runOnStartup);
    }

    /**
     * Shows the given "run on startup" state in the model and on the button.
     *
     * @param runOnStartup
     *            - The state to show
     */
    private void showRunOnStartup(boolean runOnStartup) {
        model.setRunOnStartup(runOnStartup);
        view.getRunOnStartupButton().setOn(runOnStartup);
    }

    /**
     * Repaints the button in the icon set its shown state now selects. No click is ending here to drive that repaint,
     * so the icon matching where the cursor sits is chosen the same way the button itself would choose it.
     */
    private void repaintRunOnStartupButton() {
        ThemeableToggleButton runOnStartupButton = view.getRunOnStartupButton();

        if (runOnStartupButton.getModel().isRollover()) {
            runOnStartupButton.updateHoverIcon();

            return;
        }

        runOnStartupButton.updateIdleIcon();
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
