package com.dhk.controller;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import com.dhk.controller.button.ApplyDisplayModeButtonController;
import com.dhk.controller.button.ClearHotKeyButtonController;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import lc.kra.system.keyboard.GlobalKeyboardHook;

/**
 * The main controller for Display Hot Keys. Creates all of the controllers for the application.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DhkController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private GlobalKeyboardHook keyboardHook;
    private HotKeysController hotKeysController;
    private List<IController> controllers;
    private int frameState;

    /**
     * Constructor for the DhkController class.
     * 
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public DhkController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
        model.initModel(settingsMgr);
        view.initView(null);

        if (settingsMgr.getIniMinimizeToTray()) {
            frameState = JFrame.ICONIFIED;
        } else {
            frameState = JFrame.NORMAL;
        }

        keyboardHook = new GlobalKeyboardHook(true);
    }

    /**
     * Initializes the controllers for the application.
     */
    @Override
    public void initController() {
        controllers = new ArrayList<IController>();
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

        // Initialize all sub-controllers
        for (IController controller : controllers) {
            controller.initController();
        }

        keyboardHook.addKeyListener(hotKeysController);
        view.getFrame().setExtendedState(frameState);
    }

    /**
     * Initializes the listeners for each controller.
     */
    @Override
    public void initListeners() {
        for (IController controller : controllers) {
            controller.initListeners();
        }
    }

    /*
     * Removes the hot keys controller from the global keyboard listener and clean up after sub-controllers.
     */
    @Override
    public void cleanUp() {
        keyboardHook.removeKeyListener(hotKeysController);

        for (IController controller : controllers) {
            controller.cleanUp();
            controller = null;
        }
    }

    /**
     * Cleans up after previously created controllers and re-initializes all sub-controllers.
     * 
     * @param previousFrameState
     *            - The state the frame was in before re-initialization
     */
    public void reInitController(int previousFrameState) {
        // Get the previous frame state so the frame remains in the same state upon re-initialization
        frameState = previousFrameState;

        cleanUp();
        initController();
        initListeners();

        // Clean up memory after re-initializing the controller
        System.gc();
    }

}