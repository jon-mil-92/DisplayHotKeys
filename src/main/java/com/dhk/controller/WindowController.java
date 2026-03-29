package com.dhk.controller;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;
import com.dhk.view.MinimizeToTray;
import com.dhk.view.ViewRefresher;

/**
 * Controls the application's window. The window listener is initialized with this class. It defines how often the view
 * should be refreshed, and it initializes the object that allows the application to be minimized to the system tray and
 * restored from the system tray.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan Miller
 */
public class WindowController implements IController, WindowListener {

    private ViewRefresher viewRefresher;
    private MinimizeToTray minimizeToTray;
    private DhkModel model;
    private DhkView view;

    private static final int REFRESH_INTERVAL = 250;

    /**
     * Constructor for the {@link WindowController} class.
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

    @Override
    public void initController() {
        viewRefresher = new ViewRefresher(view, REFRESH_INTERVAL);
        viewRefresher.start();
        minimizeToTray = new MinimizeToTray(model, view, viewRefresher, "/tray_icon.png");
    }

    @Override
    public void initListeners() {
        view.getFrame().addWindowListener(this);
    }

    @Override
    public void cleanUp() {
        viewRefresher.stop();

        if (minimizeToTray.getSystemTray() != null) {
            minimizeToTray.getSystemTray().shutdown();
        }
    }

    @Override
    public void windowIconified(WindowEvent e) {
        if (model.isMinimizeToTray()) {
            minimizeToTray.execute();
        }

        viewRefresher.suspend();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        viewRefresher.resume();
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

}
