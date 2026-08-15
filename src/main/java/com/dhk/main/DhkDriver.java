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
package com.dhk.main;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.dhk.controller.DhkController;
import com.dhk.io.SettingsManager;
import com.dhk.io.SingleInstanceLock;
import com.dhk.model.DhkModel;
import com.dhk.theme.ThemeUpdater;
import com.dhk.utility.LaunchTaskUtility;
import com.dhk.view.AlreadyRunningDialog;
import com.dhk.view.DhkView;

/**
 * The main driver for Display Hot Keys that starts the model, view, and controller on the AWT event dispatch thread.
 *
 * @author Jonathan R. Miller
 */
public class DhkDriver {

    /**
     * Default constructor for the {@link DhkDriver} class.
     */
    public DhkDriver() {
    }

    /**
     * The main method for the application driver.
     *
     * @param args
     *            - Command line arguments
     */
    public static void main(final String[] args) {
        /*
         * Disable the Direct3D pipeline before any UI/graphics init latches it, or frame corruption may occur after
         * interacting with components in the view
         */
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.noddraw", "true");
        ToolTipManager.sharedInstance().setEnabled(false);

        // Apply the saved theme before the single-instance check so the already-running dialog matches the app
        ThemeUpdater themeUpdater = new ThemeUpdater();
        themeUpdater.useDarkMode(SettingsManager.getSavedDarkMode());

        // Exit after theming when another instance already holds the lock so only one instance ever runs
        if (!new SingleInstanceLock().tryLock()) {
            new AlreadyRunningDialog().showAlreadyRunningDialog();

            return;
        }

        SettingsManager settingsMgr = new SettingsManager();
        settingsMgr.initSettingsManager();

        /*
         * Register the task that runs this application elevated, whether or not it should start on login, so the
         * launcher can start it without a consent prompt after the first launch. Registration shells out to the task
         * command line utility, which costs seconds, so it runs off the startup path where nothing waits on it
         */
        Thread taskRegistration = new Thread(new Runnable() {
            @Override
            public void run() {
                LaunchTaskUtility.registerTask(settingsMgr.getIniRunOnStartup());
            }
        });

        taskRegistration.setDaemon(true);
        taskRegistration.start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initDhk(settingsMgr);
            }
        });
    }

    /**
     * Initializes the model, view, and controller for this application.
     *
     * @param settingsMgr
     *            - The settings file manager that retrieves the saved configuration for this application
     */
    private static void initDhk(SettingsManager settingsMgr) {
        DhkModel model = new DhkModel();
        DhkView view = new DhkView(model);
        DhkController controller = new DhkController(model, view, settingsMgr);

        controller.initController();
        controller.initListeners();
    }

}
