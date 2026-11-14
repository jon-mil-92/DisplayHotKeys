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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.SwingUtilities;

import com.dhk.model.DhkModel;
import com.dhk.utility.WindowUtil;
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
        SwingUtilities.invokeLater(() -> WindowUtil.refreshWindow(view.getFrame()));
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // Closing to the tray is a refinement of minimizing to the tray, so it only applies while that is on
        if (model.isMinimizeToTray() && model.isCloseToTray()) {
            minimizeToTray.execute();

            return;
        }

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
