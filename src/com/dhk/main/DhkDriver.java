package com.dhk.main;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import com.dhk.controllers.DhkController;
import com.dhk.io.RunOnStartupManager;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.ui.Theme;

/**
 * This class is the main driver for Display Hot Keys that starts the model, view, and controller on the AWT event 
 * dispatch thread.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class DhkDriver {
	/**
	 * The main method for the application driver.
	 * 
	 * @param args - Command line arguments.
	 */
	public static void main(final String[] args) {
		// Initialize a settings manager object.
		SettingsManager settings = new SettingsManager();
		
		// Initialize a run on startup manager object.
		RunOnStartupManager runOnStartupManager = new RunOnStartupManager();
		
		// Set up the "look and feel" for the GUI.
		Theme theme = new Theme();
		
		// Start the application in the UI mode defined in the settings file.
		theme.useDarkMode(settings.getIniDarkMode());
		
		// Create or delete the "run on startup" batch file depending on the value in the settings file.
		if (settings.getIniRunOnStartup()) {
			runOnStartupManager.addToStartup();
		}
		else {
			runOnStartupManager.removeFromStartup();
		}
		
		// Start Display Hot Keys on the AWT event dispatch thread.
		SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                initDhk(settings);
            }
        });
	}
	
	/**
	 * This method initializes the model, view, and controller for this application.
	 * 
	 * @param settings - The settings file manager that retrieves the saved configuration for this application.
	 */
	private static void initDhk(SettingsManager settings) {
		// Initialize the Display HotKeys MVC modules.
		DhkModel model = new DhkModel();
		DhkView view = new DhkView(model, settings.getIniDarkMode(), settings.getIniRunOnStartup());
		DhkController controller = new DhkController(model, view, settings);
				
		// Initialize all listeners.
		controller.initListeners();
				
		// Start the program minimized.
		view.getFrame().setExtendedState(JFrame.ICONIFIED);
	}
}
