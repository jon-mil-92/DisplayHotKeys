package com.dhk.controllers;

import java.util.ArrayList;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This is the main controller class for the application. The model and view is initialized, and the controller for each
 * view component is created.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DhkController implements Controller {
    private ArrayList<Controller> controllers;

    /**
     * Constructor for the DhkController class.
     * 
     * @param model    - The model for the application.
     * @param view     - The view for the application.
     * @param settings - The settings manager for the application.
     */
    public DhkController(DhkModel model, DhkView view, SettingsManager settings) {
        // Initialize each slot in the model from the settings file.
        model.initSlots(settings);

        // Initialize the interactive view components.
        view.initComponents();

        // Create the array list of controllers.
        controllers = new ArrayList<Controller>();

        // Create the controllers and add them to the array list of controllers.
        controllers.add(new WindowController(view));
        controllers.add(new FrameDragController(view));
        controllers.add(new MenuController(model, view, settings));
        controllers.add(new NumberOfSlotsController(model, view, settings));
        controllers.add(new DisplayModeController(model, view, settings));
        controllers.add(new ScalingModeController(model, view, settings));
        controllers.add(new DisplayScaleController(model, view, settings));
        controllers.add(new HotKeysController(model, view, settings));
        controllers.add(new ClearHotKeyButtonController(model, view, settings));
    }

    /**
     * This method initializes the listeners for each view component controller.
     */
    @Override
    public void initListeners() {
        for (Controller controller : controllers) {
            controller.initListeners();
        }
    }
}