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
 * @author Jonathan R. Miller
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
