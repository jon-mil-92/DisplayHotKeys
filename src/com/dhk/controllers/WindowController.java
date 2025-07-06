package com.dhk.controllers;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import com.dhk.ui.DhkView;
import com.dhk.window.AppRefresher;
import com.dhk.window.MinimizeToTray;

/**
 * This class controls the application's window. The window listener is initialized with this class. It defines how
 * often the application window should be refreshed, and it initializes the object that allows the application to be
 * minimized to the system tray and restored from the system tray.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class WindowController implements Controller, WindowListener {
    private AppRefresher appRefresher;
    private MinimizeToTray minimizeToTray;
    private DhkView view;

    /**
     * Constructor for the WindowController class.
     *
     * @param view - The view for the application.
     */
    public WindowController(DhkView view) {
        // Initialize the view.
        this.view = view;

        // Start the app refresher that will update the view's frame every 250 ms.
        appRefresher = new AppRefresher(view, 250);
        appRefresher.start();

        // Initialize the minimize-to-tray object for the apllication's frame with the specified tray icon path.
        minimizeToTray = new MinimizeToTray(view.getFrame(), appRefresher, "/tray_icon.png");
    }

    /**
     * This method initializes the listener for the view's frame.
     */
    public void initListeners() {
        // Add this window listener to the view's frame.
        view.getFrame().addWindowListener(this);
    }

    /**
     * Defines what occurs when the window is minimized to an icon.
     */
    @Override
    public void windowIconified(WindowEvent e) {
        // Minimize the application to the system tray.
        minimizeToTray.execute();

        // Suspend the app refresher while the GUI is minimized.
        appRefresher.suspend();
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
        // Do nothing when the window is opened from an icon.
    }

    /**
     * Defines what occurs right before the application closes.
     */
    @Override
    public void windowClosing(WindowEvent e) {
        // Do nothing when the window is closing.
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
