package com.dhk.main;

import javax.swing.SwingUtilities;
import com.dhk.controller.DhkController;
import com.dhk.io.RunOnStartupManager;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.theme.ThemeUpdater;
import com.dhk.view.DhkView;

/**
 * The main driver for Display Hot Keys that starts the model, view, and controller on the AWT event dispatch thread.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DhkDriver {

    /**
     * Default constructor for the DhkDriver class.
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
        SettingsManager settingsMgr = new SettingsManager();
        settingsMgr.initSettingsManager();
        RunOnStartupManager runOnStartupManager = new RunOnStartupManager();
        ThemeUpdater themeUpdater = new ThemeUpdater();
        themeUpdater.useDarkMode(settingsMgr.getIniDarkMode());

        if (settingsMgr.getIniRunOnStartup()) {
            runOnStartupManager.addToStartup();
        } else {
            runOnStartupManager.removeFromStartup();
        }

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
