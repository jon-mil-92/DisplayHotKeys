package com.dhk.controllers;

import java.util.ArrayList;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class adds menu button controllers to the menu controller. Each menu button controller is created, and their 
 * listeners are initialized through this menu controller.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class MenuController implements Controller {
	private ArrayList<Controller> menuButtonControllers;
	
	public MenuController (DhkModel model, DhkView view, SettingsManager settings) {
		// Create the array list of menu button controllers.
		menuButtonControllers = new ArrayList<Controller>();
		
		// Create the menu button controllers and add them to the array list of menu button controllers.
		menuButtonControllers.add(new RunOnStartupButtonController(view, settings));
		menuButtonControllers.add(new ThemeButtonController(view, settings));
		menuButtonControllers.add(new ClearAllButtonController(model, view, settings));
		menuButtonControllers.add(new MinimizeButtonController(view));
		menuButtonControllers.add(new ExitButtonController(view));
	}

	// This method initializes the listeners for each menu button controller.
	@Override
	public void initListeners() {
		// Initialize the listeners for each menu button controller.
		for (Controller menuButtonController: menuButtonControllers) {
			menuButtonController.initListeners();
		}
	}
}
