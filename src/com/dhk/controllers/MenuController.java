package com.dhk.controllers;

import java.util.ArrayList;
import com.dhk.controllers.buttons.ClearAllButtonController;
import com.dhk.controllers.buttons.ExitButtonController;
import com.dhk.controllers.buttons.MinimizeButtonController;
import com.dhk.controllers.buttons.RefreshAppButtonController;
import com.dhk.controllers.buttons.RunOnStartupButtonController;
import com.dhk.controllers.buttons.ThemeButtonController;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class adds menu button controllers to the menu controller. Each menu button controller is created, and their
 * listeners are initialized through this menu controller.
 * 
 * @author Jonathan Miller
 * @version 1.3.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class MenuController implements Controller {
    private DhkModel model;
    private DhkView view;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private ArrayList<Controller> menuButtonControllers;

    /**
     * Constructor for the MenuController class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public MenuController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        // Get the application's model, view, controller, and settings manager.
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * This method initializes the button controllers for the menu.
     */
    @Override
    public void initController() {
        // Create the array list of menu button controllers.
        menuButtonControllers = new ArrayList<Controller>();

        // Create the menu button controllers and add them to the array list of menu button controllers.
        menuButtonControllers.add(new RunOnStartupButtonController(view, settingsMgr));
        menuButtonControllers.add(new ThemeButtonController(view, settingsMgr));
        menuButtonControllers.add(new RefreshAppButtonController(model, view, controller, settingsMgr));
        menuButtonControllers.add(new ClearAllButtonController(model, view, settingsMgr));
        menuButtonControllers.add(new MinimizeButtonController(view));
        menuButtonControllers.add(new ExitButtonController(view));

        // Initialize all of the menu button controllers.
        for (Controller menuButtonController : menuButtonControllers) {
            menuButtonController.initController();
        }
    }

    // This method initializes the listeners for each menu button controller.
    @Override
    public void initListeners() {
        // Initialize the listeners for each menu button controller.
        for (Controller menuButtonController : menuButtonControllers) {
            menuButtonController.initListeners();
        }
    }

    @Override
    public void cleanUp() {
    }
}
