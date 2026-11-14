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
package com.dhk.main;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.dhk.controller.DhkController;
import com.dhk.controller.NativeHookInstaller;
import com.dhk.io.SettingsManager;
import com.dhk.io.SingleInstanceLock;
import com.dhk.model.DhkModel;
import com.dhk.theme.ThemeUpdater;
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

        // Check the lock before any slow setup so only the winning instance installs the native hooks
        boolean lockAcquired = new SingleInstanceLock().tryLock();

        // Start the hook install now so its potentially slow native setup overlaps the theme and settings setup
        NativeHookInstaller hookInstaller = new NativeHookInstaller();

        if (lockAcquired) {
            hookInstaller.startInstall();
        }

        // Apply the saved theme before any window can show so the already-running dialog matches the app
        ThemeUpdater themeUpdater = new ThemeUpdater();
        themeUpdater.useDarkMode(SettingsManager.getSavedDarkMode());

        // Exit after theming when another instance already holds the lock so only one instance ever runs
        if (!lockAcquired) {
            new AlreadyRunningDialog().showAlreadyRunningDialog();

            return;
        }

        SettingsManager settingsMgr = new SettingsManager();
        settingsMgr.initSettingsManager();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initDhk(settingsMgr, hookInstaller);
            }
        });
    }

    /**
     * Initializes the model, view, and controller for this application.
     *
     * @param settingsMgr
     *            - The settings file manager that retrieves the saved configuration for this application
     * @param hookInstaller
     *            - The installer whose background native hook install began at launch
     */
    private static void initDhk(SettingsManager settingsMgr, NativeHookInstaller hookInstaller) {
        DhkModel model = new DhkModel();
        DhkView view = new DhkView(model);
        DhkController controller = new DhkController(model, view, settingsMgr, hookInstaller);

        controller.initController();
        controller.initListeners();
    }

}
