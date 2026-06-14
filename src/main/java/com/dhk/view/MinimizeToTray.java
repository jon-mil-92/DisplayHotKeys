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
package com.dhk.view;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.dhk.model.DhkModel;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

/**
 * Enables the application to be minimized to the system tray and restored from the system tray.
 *
 * @author Jonathan R. Miller
 */
public class MinimizeToTray {

    private DhkView view;
    private JFrame frame;
    private ViewRefresher viewRefresher;
    private SystemTray systemTray;
    private AboutDialog aboutDialog;
    private Image minimizedToTrayIcon;

    /**
     * Constructor for the {@link MinimizeToTray} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param viewRefresher
     *            - The refresher for the given view
     * @param iconResourcePath
     *            - The icon resource path for the tray icon
     */
    public MinimizeToTray(DhkModel model, DhkView view, ViewRefresher viewRefresher, String iconResourcePath) {
        this.view = view;
        this.viewRefresher = viewRefresher;

        frame = view.getFrame();
        aboutDialog = new AboutDialog(model, view);

        // Get the minimized-to-tray icon image
        minimizedToTrayIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(iconResourcePath));
    }

    /**
     * Minimizes the application to the system tray.
     */
    public void execute() {
        // Hide the taskbar icon
        frame.setVisible(false);

        startSystemTray();
        addMenuItems();
    }

    /**
     * Starts the system tray.
     */
    private void startSystemTray() {
        systemTray = SystemTray.get("Display Hot Keys");
        systemTray.setTooltip("Display Hot Keys");
        systemTray.setImage(minimizedToTrayIcon);
    }

    /**
     * Adds the menu items to the system tray pop-up menu.
     */
    private void addMenuItems() {
        // Create options for the system tray pop-up menu
        MenuItem restoreMenuItem = new MenuItem("Restore");
        MenuItem aboutMenuItem = new MenuItem("About");
        MenuItem exitMenuItem = new MenuItem("Exit");

        restoreMenuItem.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restoreAction();
            }
        });

        systemTray.getMenu().add(restoreMenuItem);

        aboutMenuItem.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aboutAction();
            }
        });

        systemTray.getMenu().add(aboutMenuItem);

        exitMenuItem.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitAction();
            }
        });

        systemTray.getMenu().add(exitMenuItem);
    }

    /**
     * Gets the system tray.
     *
     * @return The system tray
     */
    public SystemTray getSystemTray() {
        return systemTray;
    }

    /**
     * Restores the application from the system tray.
     */
    private void restoreAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setExtendedState(JFrame.NORMAL);
                frame.setVisible(true);
                view.getDefaultFocusComponent().requestFocusInWindow();
            }
        });

        hideSystemTray();
        viewRefresher.resume();
    }

    /**
     * Shows an "About App" dialog.
     */
    private void aboutAction() {
        hideSystemTray();
        aboutDialog.showAboutDialog(systemTray);
    }

    /**
     * Exits the application.
     */
    private void exitAction() {
        hideSystemTray();
        systemTray.shutdown();
        viewRefresher.stop();
        System.exit(0);
    }

    /**
     * Hides the system tray.
     */
    private void hideSystemTray() {
        if (systemTray != null) {
            systemTray.setEnabled(false);
        }
    }

}
