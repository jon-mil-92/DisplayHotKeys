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

import com.dhk.controller.button.ApplySlotButtonController;
import com.dhk.controller.button.ClearAllButtonController;
import com.dhk.controller.button.ClearHotKeyButtonController;
import com.dhk.controller.button.ClearSlotButtonController;
import com.dhk.io.DisplayConfigUpdater;
import com.dhk.io.DisplayEventNotifier;
import com.dhk.io.SettingsManager;
import com.dhk.io.ShellRestartHandler;
import com.dhk.model.DhkModel;
import com.dhk.utility.TimingLog;
import com.dhk.view.DhkView;
import com.dhk.view.MinimizeToTray;

import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.mouse.GlobalMouseHook;

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
    private GlobalMouseHook mouseHook;
    private HeldKeyTracker heldKeyTracker;
    private HotKeysController hotKeysController;
    private List<IController> controllers;
    private int frameState;
    private DisplayConfigUpdater displayConfigUpdater;
    private DisplayEventNotifier displayNotifications;
    private ShellRestartHandler shellRestartHandler;
    private MinimizeToTray minimizeToTray;

    /**
     * Constructor for the {@link DhkController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param settingsMgr
     *            - The settings manager for the application
     * @param hookInstaller
     *            - The installer whose background global hook install began at launch
     */
    public DhkController(DhkModel model, DhkView view, SettingsManager settingsMgr,
            GlobalHookInstaller hookInstaller) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;

        long modelInitStart = TimingLog.start();
        model.initModel(settingsMgr);
        TimingLog.end("ctor model.initModel", modelInitStart);

        long viewInitStart = TimingLog.start();
        view.initView(null, 0);
        TimingLog.end("ctor view.initView", viewInitStart);

        if (settingsMgr.getIniMinimizeToTray()) {
            frameState = JFrame.ICONIFIED;
        } else {
            frameState = JFrame.NORMAL;
        }

        // Adopt the JVM-lifetime hooks and permanent held-key tracker, waiting out any install time still remaining
        long hookWaitStart = TimingLog.start();
        hookInstaller.awaitInstall();
        TimingLog.end("ctor hook install wait", hookWaitStart);

        keyboardHook = hookInstaller.getKeyboardHook();
        heldKeyTracker = hookInstaller.getHeldKeyTracker();
        mouseHook = hookInstaller.getMouseHook();

        // Create the minimize-to-tray object once so app refreshes reuse the live tray instead of rebuilding it
        minimizeToTray = new MinimizeToTray(model, view, "/tray_icon.png");
    }

    @Override
    public void initController() {
        controllers = new ArrayList<IController>();

        // Recreate the mouse hook only if it never existed; normally it stays alive across re-inits
        if (mouseHook == null) {
            mouseHook = GlobalHookInstaller.createMouseHook();
        }

        // Create the hot keys controller early so other controllers can notify it
        hotKeysController = new HotKeysController(model, view, this, settingsMgr, heldKeyTracker);
        controllers.add(hotKeysController);

        controllers.add(new ApplySlotButtonController(model, view, this, settingsMgr));
        controllers.add(new ClearAllButtonController(model, view, this, settingsMgr));
        controllers.add(new ClearHotKeyButtonController(model, view, settingsMgr, hotKeysController));
        controllers.add(new ClearSlotButtonController(model, view, this, settingsMgr));
        controllers.add(new DisplayModeController(model, view, settingsMgr));
        controllers.add(new DpiScaleController(model, view, settingsMgr));
        controllers.add(new FrameDragController(view, mouseHook));
        controllers.add(new MenuController(model, view, settingsMgr));
        controllers.add(new NumberOfSlotsController(model, view, settingsMgr));
        controllers.add(new OrientationController(model, view, this, settingsMgr));
        controllers.add(new ScalingModeController(model, view, settingsMgr));
        controllers.add(new SelectedDisplayController(model, view));
        controllers.add(new WindowController(model, view, minimizeToTray));

        // Initialize all sub-controllers
        for (IController controller : controllers) {
            long initStart = TimingLog.start();
            controller.initController();
            TimingLog.end(controller.getClass().getSimpleName() + ".initController", initStart);
        }

        // Start event-driven display notifications
        displayConfigUpdater = new DisplayConfigUpdater(model, view, this, settingsMgr);
        shellRestartHandler = new ShellRestartHandler(view);
        displayNotifications = new DisplayEventNotifier();
        displayNotifications.registerDisplayChangeListener(displayConfigUpdater);
        displayNotifications.registerShellRestartListener(shellRestartHandler);

        long notifierStart = TimingLog.start();
        displayNotifications.start();
        TimingLog.end("displayNotifications.start", notifierStart);

        // Recreate the hook only if it never existed; normally it stays alive across re-inits with the tracker attached
        if (keyboardHook == null) {
            keyboardHook = new GlobalKeyboardHook(true);
            keyboardHook.addKeyListener(heldKeyTracker);
        }

        // Attach the new dispatch listener; the persistent held-key tracker remains attached across re-inits
        keyboardHook.addKeyListener(hotKeysController);

        // A frame held back for the tray is handed off by the window controller, so leave its state alone
        if (!view.isStartMinimizedToTray()) {
            view.getFrame().setExtendedState(frameState);
        }
    }

    @Override
    public void initListeners() {
        for (IController controller : controllers) {
            controller.initListeners();
        }
    }

    @Override
    public void cleanUp() {
        /*
         * Detach only the current dispatch listener so the old hot keys controller can be garbage collected. The hook
         * and its held-key tracker stay alive so keys held across app refreshes are never missed, and the process exit
         * reclaims the native hook
         */
        if (keyboardHook != null) {
            try {
                keyboardHook.removeKeyListener(hotKeysController);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Ensure EDT tasks that may access controllers have been processed before we clean up references
        if (!SwingUtilities.isEventDispatchThread()) {
            long edtDrainStart = TimingLog.start();

            try {
                SwingUtilities.invokeAndWait(() -> {
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            TimingLog.end("EDT drain before cleanUp", edtDrainStart);
        }

        if (controllers != null) {
            for (IController controller : controllers) {
                long cleanUpStart = TimingLog.start();

                try {
                    controller.cleanUp();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TimingLog.end(controller.getClass().getSimpleName() + ".cleanUp", cleanUpStart);
            }

            // Remove references to allow GC
            controllers.clear();
            controllers = null;
        }

        // Stop native display notifications
        if (displayNotifications != null) {
            long notifierStopStart = TimingLog.start();

            try {
                displayNotifications.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                displayNotifications = null;
            }

            TimingLog.end("displayNotifications.stop", notifierStopStart);
        }

        // Stop any pending deferred re-initialization so the Timer cannot fire against a disposed view
        if (displayConfigUpdater != null) {
            try {
                displayConfigUpdater.cleanUp();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                displayConfigUpdater = null;
            }
        }

        // Stop any pending deferred re-fit so the Timer cannot fire against a disposed view
        if (shellRestartHandler != null) {
            try {
                shellRestartHandler.cleanUp();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                shellRestartHandler = null;
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

        initController();
        initListeners();
    }

    /**
     * Gets the application-lifetime minimize-to-tray object.
     *
     * @return The minimize-to-tray object
     */
    public MinimizeToTray getMinimizeToTray() {
        return minimizeToTray;
    }

}