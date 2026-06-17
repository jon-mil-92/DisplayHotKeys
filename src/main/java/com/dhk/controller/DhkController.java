/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.dhk.controller;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.dhk.controller.button.ApplyDisplayModeButtonController;
import com.dhk.controller.button.ClearAllButtonController;
import com.dhk.controller.button.ClearHotKeyButtonController;
import com.dhk.io.DisplayConfigUpdater;
import com.dhk.io.DisplayEventNotifier;
import com.dhk.io.SettingsManager;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

import lc.kra.system.keyboard.GlobalKeyboardHook;

/**
 * The main controller for Display Hot Keys. Creates all of the controllers for the application.
 *
 * @author Jonathan R. Miller
 */
public class DhkController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private GlobalKeyboardHook keyboardHook;
    private HotKeysController hotKeysController;
    private List<IController> controllers;
    private int frameState;
    private DisplayConfigUpdater displayConfigUpdater;
    private DisplayEventNotifier displayNotifications;

    /**
     * Constructor for the {@link DhkController} class.
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

    @Override
    public void initController() {
        controllers = new ArrayList<IController>();

        // Create the hot keys controller early so other controllers can notify it
        hotKeysController = new HotKeysController(model, view, this, settingsMgr);

        controllers.add(new ClearAllButtonController(model, view, this, settingsMgr));
        controllers.add(new ApplyDisplayModeButtonController(model, view, this, settingsMgr));
        controllers.add(new ClearHotKeyButtonController(model, view, settingsMgr, hotKeysController));
        controllers.add(new DisplayModeController(model, view, settingsMgr));
        controllers.add(new DpiScaleController(model, view, settingsMgr));
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

        // Start event-driven display notifications
        displayConfigUpdater = new DisplayConfigUpdater(model, view, this, settingsMgr);
        displayNotifications = new DisplayEventNotifier();
        displayNotifications.registerListener(displayConfigUpdater);
        displayNotifications.start();

        // Ensure keyboard hook exists (may have been shutdown during previous cleanup)
        if (keyboardHook == null) {
            keyboardHook = new GlobalKeyboardHook(true);
        }

        keyboardHook.addKeyListener(hotKeysController);
        view.getFrame().setExtendedState(frameState);
    }

    @Override
    public void initListeners() {
        for (IController controller : controllers) {
            controller.initListeners();
        }
    }

    @Override
    public void cleanUp() {
        // Attempt to shutdown the native keyboard hook to free native resources and stop incoming events
        if (keyboardHook != null) {
            try {
                keyboardHook.removeKeyListener(hotKeysController);
                keyboardHook.shutdownHook();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                keyboardHook = null;
            }
        }

        // Ensure EDT tasks that may access controllers have been processed before we clean up references
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (controllers != null) {
            for (IController controller : controllers) {
                try {
                    controller.cleanUp();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Remove references to allow GC
            controllers.clear();
            controllers = null;
        }

        // Stop native display notifications
        if (displayNotifications != null) {
            try {
                displayNotifications.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                displayNotifications = null;
            }
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
    }

}