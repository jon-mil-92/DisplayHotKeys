package com.dhk.main;

import javax.swing.SwingUtilities;
import com.dhk.controller.DhkController;
import com.dhk.io.RunOnStartupManager;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.theme.ThemeUpdater;
import com.dhk.ui.DhkView;

/**
 * This class is the main driver for Display Hot Keys that starts the model, view, and controller on the AWT event
 * dispatch thread.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
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
     * @param args - Command line arguments.
     */
    public static void main(final String[] args) {
        // Initialize a settings manager object.
        SettingsManager settingsMgr = new SettingsManager();
        settingsMgr.initSettingsManager();

        // Initialize a run on startup manager object.
        RunOnStartupManager runOnStartupManager = new RunOnStartupManager();

        // Set up the "look and feel" for the GUI.
        ThemeUpdater themeUpdater = new ThemeUpdater();

        // Start the application in the UI mode defined in the settings file.
        themeUpdater.useDarkMode(settingsMgr.getIniDarkMode());

        // Create or delete the "run on startup" batch file depending on the value in the settings file.
        if (settingsMgr.getIniRunOnStartup()) {
            runOnStartupManager.addToStartup();
        } else {
            runOnStartupManager.removeFromStartup();
        }

        // Start Display Hot Keys on the AWT event dispatch thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initDhk(settingsMgr);
            }
        });
    }

    /**
     * This method initializes the model, view, and controller for this application.
     * 
     * @param settingsMgr - The settings file manager that retrieves the saved configuration for this application.
     */
    private static void initDhk(SettingsManager settingsMgr) {
        // Initialize the Display Hot Keys MVC modules.
        DhkModel model = new DhkModel();
        DhkView view = new DhkView(model);
        DhkController controller = new DhkController(model, view, settingsMgr);

        // Initialize the main controller and all sub-controllers.
        controller.initController();
        controller.initListeners();
    }
}
