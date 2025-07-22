package com.dhk.controller;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.window.MinimizeToTray;
import com.dhk.window.ViewRefresher;

/**
 * Controls the application's window. The window listener is initialized with this class. It defines how often the view
 * should be refreshed, and it initializes the object that allows the application to be minimized to the system tray and
 * restored from the system tray.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class WindowController implements IController, WindowListener {

    private ViewRefresher viewRefresher;
    private MinimizeToTray minimizeToTray;
    private DhkModel model;
    private DhkView view;

    private final int REFRESH_INTERVAL = 250;

    /**
     * Constructor for the WindowController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public WindowController(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
    }

    /**
     * Creates and starts a new view refresher along with a new minimize-to-tray object.
     */
    @Override
    public void initController() {
        viewRefresher = new ViewRefresher(view, REFRESH_INTERVAL);
        viewRefresher.start();
        minimizeToTray = new MinimizeToTray(view, viewRefresher, "/tray_icon.png");
    }

    /**
     * Initializes the listener for the view's frame.
     */
    @Override
    public void initListeners() {
        view.getFrame().addWindowListener(this);
    }

    /**
     * Stops refreshing the view and remove the app from the system tray if it is minimized to the system tray.
     */
    @Override
    public void cleanUp() {
        viewRefresher.stop();

        if (minimizeToTray.getSystemTray() != null) {
            minimizeToTray.getSystemTray().shutdown();
        }
    }

    /**
     * Defines what occurs when the window is minimized to an icon.
     */
    @Override
    public void windowIconified(WindowEvent e) {
        if (model.isMinimizeToTray()) {
            minimizeToTray.execute();
        }

        viewRefresher.suspend();
    }

    /**
     * Defines what occurs when the window is closed.
     */
    @Override
    public void windowClosed(WindowEvent e) {
    }

    /**
     * Defines what occurs when the window is restored from an icon.
     */
    @Override
    public void windowDeiconified(WindowEvent e) {
        viewRefresher.resume();
    }

    /**
     * Defines what occurs right before the application closes.
     */
    @Override
    public void windowClosing(WindowEvent e) {
    }

    /**
     * Defines what occurs when the window is opened.
     */
    @Override
    public void windowOpened(WindowEvent e) {
    }

    /**
     * Defines what occurs when the window is activated.
     */
    @Override
    public void windowActivated(WindowEvent e) {
    }

    /**
     * Defines what occurs when the window is deactivated.
     */
    @Override
    public void windowDeactivated(WindowEvent e) {
    }

}
