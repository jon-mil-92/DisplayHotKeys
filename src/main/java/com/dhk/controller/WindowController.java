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

import javax.swing.SwingUtilities;

import com.dhk.model.DhkModel;
import com.dhk.utility.FrameUtil;
import com.dhk.view.DhkView;
import com.dhk.view.MinimizeToTray;

/**
 * Controls the application's window. The window listener is initialized with this class, and it brings up the shared
 * minimize-to-tray object when the frame starts hidden for the tray.
 *
 * @author Jonathan R. Miller
 */
public class WindowController implements IController, WindowListener {

    private MinimizeToTray minimizeToTray;
    private DhkModel model;
    private DhkView view;

    /**
     * Constructor for the {@link WindowController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param minimizeToTray
     *            - The application-lifetime minimize-to-tray object shared across re-initializations
     */
    public WindowController(DhkModel model, DhkView view, MinimizeToTray minimizeToTray) {
        this.model = model;
        this.view = view;
        this.minimizeToTray = minimizeToTray;
    }

    @Override
    public void initController() {
        /*
         * A frame held back for the tray is never shown, so it raises no iconified event to hand it off. Bring the tray
         * up here instead, since the tray menu wires its own callbacks and needs nothing from the window listener
         */
        if (view.isStartMinimizedToTray()) {
            minimizeToTray.execute();
        }
    }

    @Override
    public void initListeners() {
        view.getFrame().addWindowListener(this);
    }

    @Override
    public void cleanUp() {
        // The tray deliberately survives re-initialization; it only shuts down on restore or application exit
    }

    @Override
    public void windowIconified(WindowEvent e) {
        if (model.isMinimizeToTray()) {
            minimizeToTray.execute();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // Re-fit after the frame is shown so staleness accrued while iconified does not surface as scroll bars
        SwingUtilities.invokeLater(() -> FrameUtil.refreshFrame(view.getFrame()));
    }

    @Override
    public void windowClosing(WindowEvent e) {
        minimizeToTray.shutDownSystemTray();
        System.exit(0);
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
