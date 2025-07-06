package com.dhk.controllers;

import java.util.ArrayList;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class adds menu button controllers to the menu controller. Each menu button controller is created, and their
 * listeners are initialized through this menu controller.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class MenuController implements Controller {
    private ArrayList<Controller> menuButtonControllers;

    /**
     * Constructor for the MenuController class.
     * 
     * @param model    - The model for the application.
     * @param view     - The view for the application.
     * @param settings - The settings manager of the application.
     */
    public MenuController(DhkModel model, DhkView view, SettingsManager settings) {
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
        for (Controller menuButtonController : menuButtonControllers) {
            menuButtonController.initListeners();
        }
    }
}
