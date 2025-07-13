package com.dhk.controller;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.MinimizeToTray;
import com.dhk.window.ViewRefresher;

/**
 * This class controls the application's window. The window listener is initialized with this class. It defines how
 * often the view should be refreshed, and it initializes the object that allows the application to be minimized to the
 * system tray and restored from the system tray.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class WindowController implements Controller, WindowListener {
    private ViewRefresher viewRefresher;
    private MinimizeToTray minimizeToTray;
    private DhkModel model;
    private DhkView view;

    // Update the frame every 250 ms when it is visible.
    private final int REFRESH_INTERVAL = 250;

    /**
     * Constructor for the WindowController class.
     *
     * @param model - The model for the application.
     * @param view  - The view for the application.
     */
    public WindowController(DhkModel model, DhkView view) {
        // Get the application's model and view.
        this.model = model;
        this.view = view;
    }

    /**
     * This method creates and starts a new view refresher along with a new minimize-to-tray object.
     */
    @Override
    public void initController() {
        // Start the view refresher that will update the frame at a defined interval.
        viewRefresher = new ViewRefresher(view, REFRESH_INTERVAL);
        viewRefresher.start();

        // Initialize the minimize-to-tray object for the apllication's frame with the specified tray icon path.
        minimizeToTray = new MinimizeToTray(view, viewRefresher, "/tray_icon.png");
    }

    /**
     * This method initializes the listener for the view's frame.
     */
    @Override
    public void initListeners() {
        // Add this window listener to the view's frame.
        view.getFrame().addWindowListener(this);
    }

    /**
     * Stop refreshing the view and remove the app from the system tray if it is minimized to the system tray.
     */
    @Override
    public void cleanUp() {
        viewRefresher.stop();

        // If the app is currently minimized to the system tray...
        if (minimizeToTray.getSystemTray() != null) {
            minimizeToTray.getSystemTray().shutdown();
        }
    }

    /**
     * Defines what occurs when the window is minimized to an icon.
     */
    @Override
    public void windowIconified(WindowEvent e) {
        // Minimize the application to the system tray if "minimize to tray" is enabled.
        if (model.isMinimizeToTray()) {
            minimizeToTray.execute();
        }

        // Suspend the view refresher while the GUI is minimized.
        viewRefresher.suspend();
    }

    /**
     * Defines what occurs when the window is closed.
     */
    @Override
    public void windowClosed(WindowEvent e) {
        // Do nothing when the window is closed.
    }

    /**
     * Defines what occurs when the window is restored from an icon.
     */
    @Override
    public void windowDeiconified(WindowEvent e) {
        // Resume the view refresher so the frame continues to update.
        viewRefresher.resume();
    }

    /**
     * Defines what occurs right before the application closes.
     */
    @Override
    public void windowClosing(WindowEvent e) {
        // Do nothing right before the application closes.
    }

    /**
     * Defines what occurs when the window is opened.
     */
    @Override
    public void windowOpened(WindowEvent e) {
        // Do nothing when the window is opened.
    }

    /**
     * Defines what occurs when the window is activated.
     */
    @Override
    public void windowActivated(WindowEvent e) {
        // Do nothing when the window is activated.
    }

    /**
     * Defines what occurs when the window is deactivated.
     */
    @Override
    public void windowDeactivated(WindowEvent e) {
        // Do nothing when the window is deactivated.
    }
}
