package com.dhk.controller;

import java.util.ArrayList;
import javax.swing.JFrame;
import com.dhk.controller.button.ApplyDisplayModeButtonController;
import com.dhk.controller.button.ClearHotKeyButtonController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;
import lc.kra.system.keyboard.GlobalKeyboardHook;

/**
 * This is the main controller class for the application. It creates all of the controllers for the application.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class DhkController implements Controller {
    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private GlobalKeyboardHook keyboardHook;
    private HotKeysController hotKeysController;
    private ArrayList<Controller> controllers;
    private int frameState;

    /**
     * Constructor for the DhkController class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public DhkController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;

        // Initialize each slot in the model from the settings file.
        model.initModel(settingsMgr);

        // Initialize the view.
        view.initView(null);

        // Initialize the frame state.
        if (settingsMgr.getIniMinimizeToTray()) {
            frameState = JFrame.ICONIFIED;
        } else {
            frameState = JFrame.NORMAL;
        }

        // Initialize the global keyboard input hook.
        keyboardHook = new GlobalKeyboardHook(true);
    }

    /**
     * This method initializes the controllers for the application.
     */
    @Override
    public void initController() {
        // Create the array list of controllers.
        controllers = new ArrayList<Controller>();

        // Create the controllers and add them to the array list of controllers.
        controllers.add(new ApplyDisplayModeButtonController(model, view, this, settingsMgr));
        controllers.add(new ClearHotKeyButtonController(model, view, settingsMgr));
        controllers.add(new ConnectedDisplaysController(model, view, this, settingsMgr));
        controllers.add(new DisplayModeController(model, view, settingsMgr));
        controllers.add(new DpiScaleController(model, view, settingsMgr));
        controllers.add(new FrameDragController(view));

        hotKeysController = new HotKeysController(model, view, this, settingsMgr);
        controllers.add(hotKeysController);

        controllers.add(new MenuController(model, view, this, settingsMgr));
        controllers.add(new NumberOfSlotsController(model, view, settingsMgr));
        controllers.add(new OrientationController(model, view, this, settingsMgr));
        controllers.add(new ScalingModeController(model, view, settingsMgr));
        controllers.add(new SelectedDisplayController(model, view));
        controllers.add(new WindowController(model, view));

        // Initialize all sub-controllers.
        for (Controller controller : controllers) {
            controller.initController();
        }

        // Add the hot keys controller to the global keyboard listener.
        keyboardHook.addKeyListener(hotKeysController);

        // Set the state of the frame.
        view.getFrame().setExtendedState(frameState);
    }

    /**
     * This method initializes the listeners for each controller.
     */
    @Override
    public void initListeners() {
        for (Controller controller : controllers) {
            controller.initListeners();
        }
    }

    // Remove the hot keys controller from the global keyboard listener and clean up after sub-controllers.
    @Override
    public void cleanUp() {
        // Remove the hot keys controller from the global keyboard listener
        keyboardHook.removeKeyListener(hotKeysController);

        // For each sub-controller...
        for (Controller controller : controllers) {
            // Clean up and stop any opened threads.
            controller.cleanUp();

            // Set the controller to null to remove references and allow it to be collected by the garbage collector.
            controller = null;
        }
    }

    /**
     * This method cleans up after previously created controllers and re-initializes all sub-controllers.
     * 
     * @param previousFrameState - The state the frame was in before re-initialization.
     */
    public void reInitController(int previousFrameState) {
        // Get the previous frame state so the frame remains in the same state upon re-initialization.
        frameState = previousFrameState;

        cleanUp();
        initController();
        initListeners();

        // Clean up memory after re-initializing the controller.
        System.gc();
    }
}