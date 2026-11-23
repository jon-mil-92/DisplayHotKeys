/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
package com.dhk.controller;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.dhk.controller.button.ApplySlotButtonController;
import com.dhk.controller.button.ClearAllButtonController;
import com.dhk.controller.button.ClearHotKeyButtonController;
import com.dhk.controller.button.ClearSlotButtonController;
import com.dhk.io.DisplayConfigUpdater;
import com.dhk.io.DisplayEventNotifier;
import com.dhk.io.NativeInputHook;
import com.dhk.io.SettingsManager;
import com.dhk.io.ShellChangeHandler;
import com.dhk.localization.Language;
import com.dhk.localization.Localizer;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.theme.ThemeUpdater;
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;
import com.dhk.view.DisplayInfoOverlayView;
import com.dhk.view.MinimizeToTray;

/**
 * The main controller for Display Hot Keys. Creates all of the controllers for the application.
 *
 * @author Jonathan R. Miller
 */
public class DhkController implements IController {

    private DhkModel model;
    private DhkView view;
    private SettingsManager settingsMgr;
    private NativeInputHook inputHook;
    private boolean inputHookRecoveryAttempted;
    private HotKeyDispatcher hotKeyDispatcher;
    private HotKeysController hotKeysController;
    private MenuController menuController;
    private List<IController> controllers;
    private DisplayConfigUpdater displayConfigUpdater;
    private DisplayEventNotifier displayNotifications;
    private MinimizeToTray minimizeToTray;
    private DisplayInfoOverlayView displayInfoOverlayView;
    private boolean displayInfoAutoShowPending;

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
    public DhkController(DhkModel model, DhkView view, SettingsManager settingsMgr, NativeHookInstaller hookInstaller) {
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;

        model.initModel(settingsMgr);
        view.initView(0);

        // Adopt the JVM-lifetime hooks and permanent hot key dispatcher, waiting out any install time still remaining
        hookInstaller.awaitInstall();

        inputHook = hookInstaller.getInputHook();
        hotKeyDispatcher = hookInstaller.getHotKeyDispatcher();

        // Create the minimize-to-tray object once so app refreshes reuse the live tray instead of rebuilding it
        minimizeToTray = new MinimizeToTray(model, view, "/tray_icon.svg", this::showAdditionalSettingsDialog,
                this::showAboutDialog);

        // Create the display info overlay once so app refreshes can still hide panels shown before them
        displayInfoOverlayView = new DisplayInfoOverlayView();
    }

    @Override
    public void initController() {
        controllers = new ArrayList<IController>();

        /*
         * Recover the hook only once, since it normally lives across re-inits with its listeners attached. Retrying on
         * every refresh would reinstall a system-wide hook on every display change for as long as it kept failing
         */
        if (inputHook == null && !inputHookRecoveryAttempted) {
            inputHookRecoveryAttempted = true;
            inputHook = NativeHookInstaller.createInputHook();

            if (inputHook != null) {
                inputHook.addKeyListener(hotKeyDispatcher);
                WindowUtil.trackLeftMouseButton(inputHook);
            }
        }

        // Create the hot keys controller early so other controllers can notify it
        hotKeysController = new HotKeysController(model, view, this, settingsMgr, hotKeyDispatcher);
        controllers.add(hotKeysController);

        controllers.add(new ApplySlotButtonController(model, view, this, settingsMgr));
        controllers.add(new ClearAllButtonController(model, view, this, settingsMgr));
        controllers.add(new ClearHotKeyButtonController(model, view, settingsMgr, hotKeysController));
        controllers.add(new ClearSlotButtonController(model, view, this, settingsMgr));
        controllers.add(new DisplayModeController(model, view, settingsMgr));
        controllers.add(new DpiScaleController(model, view, settingsMgr));
        menuController = new MenuController(model, view, this, settingsMgr, hotKeysController);
        controllers.add(menuController);
        controllers.add(new NumberOfSlotsController(model, view, settingsMgr));
        controllers.add(new OrientationController(model, view, this, settingsMgr));
        controllers.add(new ScalingModeController(model, view, settingsMgr));
        controllers.add(new SelectedDisplayController(view));
        controllers.add(new WindowController(model, view, minimizeToTray));

        // Initialize all sub-controllers
        for (IController controller : controllers) {
            controller.initController();
        }

        // Start event-driven display notifications
        displayConfigUpdater = new DisplayConfigUpdater(model, view, this, settingsMgr);
        ShellChangeHandler shellChangeHandler = new ShellChangeHandler();
        displayNotifications = new DisplayEventNotifier();
        displayNotifications.registerDisplayChangeListener(displayConfigUpdater);
        displayNotifications.registerShellRestartListener(shellChangeHandler);
        displayNotifications.registerWorkingAreaChangeListener(shellChangeHandler);
        displayNotifications.start();

        // The dispatcher stays on the hook across re-inits and hands every key event to the new controller from now on
        hotKeyDispatcher.setHotKeysController(hotKeysController);
    }

    @Override
    public void initListeners() {
        for (IController controller : controllers) {
            controller.initListeners();
        }
    }

    @Override
    public void cleanUp() {
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

        // Retire the updater so a late notification callback cannot re-initialize a disposed view
        if (displayConfigUpdater != null) {
            try {
                displayConfigUpdater.cleanUp();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                displayConfigUpdater = null;
            }
        }
    }

    /**
     * Re-initializes all sub-controllers and attaches their listeners, after the caller has cleaned up the previous
     * ones. The frame survives the re-initialization, so it keeps its own state.
     */
    public void reInitController() {
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

    /**
     * Shows the "Additional Settings" dialog through the current menu controller, which wires its settings, so the tray
     * can open it even though every app refresh replaces that controller.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes
     */
    public void showAdditionalSettingsDialog(Runnable onCloseAction) {
        menuController.getAdditionalSettingsButtonController().showAdditionalSettingsDialog(onCloseAction);
    }

    /**
     * Shows the application in the given language by rebuilding it, which rebuilds the "Additional Settings" dialog the
     * language was chosen in.
     *
     * @param language
     *            - The language to show the application in
     */
    public void changeLanguage(Language language) {
        Localizer.setLanguage(language);

        // The theme is set up before the rebuild, so the view measures its text in the new language's font
        new ThemeUpdater().useDarkMode(model.isDarkMode());
        new AppRefresher(model, view, this, settingsMgr).reInitApp();

        minimizeToTray.languageChanged();
    }

    /**
     * Shows an "About Display Hot Keys" dialog through the current menu controller, which builds it per show, so the
     * tray can open it even though every app refresh replaces that controller.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes
     */
    public void showAboutDialog(Runnable onCloseAction) {
        menuController.getAboutButtonController().showAboutDialog(onCloseAction);
    }

    /**
     * Gets the application-lifetime display info overlay, which shows the applied settings of every connected display.
     *
     * @return The display info overlay
     */
    public DisplayInfoOverlayView getDisplayInfoOverlayView() {
        return displayInfoOverlayView;
    }

    /**
     * Dismisses everything placed against the current display geometry, which is a showing tray menu and any display
     * info panels, since a display configuration change leaves both misplaced. The panels are shown again once the
     * change settles when they are set to show automatically.
     */
    public void displayConfigurationChanged() {
        minimizeToTray.displayConfigurationChanged();
        displayInfoOverlayView.hide();

        displayInfoAutoShowPending = true;
    }

    /**
     * Places everything that depends on the display geometry against the settled display configuration. The display
     * info panels show on every display when a change is pending and they are set to show automatically.
     */
    public void displayConfigurationSettled() {
        // A slot apply and a reported display change both set the flag, and each is followed by one app refresh
        if (displayInfoAutoShowPending) {
            displayInfoAutoShowPending = false;

            if (model.isAutoShowInfoOverlay()) {
                displayInfoOverlayView.show(model);
            }
        }

        // Rescale the tray icon last, since the shell answers for the task bar slowly while still rebuilding it
        minimizeToTray.displayConfigurationSettled();
    }

}