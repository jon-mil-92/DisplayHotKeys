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

import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.dhk.model.DhkModel;
import com.dhk.utility.FrameUtil;
import com.dhk.utility.TimingLog;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

/**
 * Enables the application to be minimized to the system tray and restored from the system tray. A single instance
 * lives for the whole application lifetime so re-initializations reuse the live tray instead of rebuilding it.
 *
 * @author Jonathan R. Miller
 */
public class MinimizeToTray {

    private DhkModel model;
    private DhkView view;
    private SystemTray systemTray;
    private Image minimizedToTrayIcon;
    private boolean disposingTrayMenuWindows;

    /**
     * Tray name shown in the system tray, also given by the tray library to its hidden menu anchor dialog's title.
     */
    private static final String TRAY_NAME = "Display Hot Keys";

    /**
     * Constructor for the {@link MinimizeToTray} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param iconResourcePath
     *            - The icon resource path for the tray icon
     */
    public MinimizeToTray(DhkModel model, DhkView view, String iconResourcePath) {
        this.model = model;
        this.view = view;

        // Get the minimized-to-tray icon image
        minimizedToTrayIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(iconResourcePath));
    }

    /**
     * Minimizes the application to the system tray.
     */
    public void execute() {
        // Hide the taskbar icon; the frame is resolved lazily since every re-initialization replaces it
        view.getFrame().setVisible(false);

        if (systemTray == null) {
            startSystemTray();
            addMenuItems();
        } else {
            systemTray.setEnabled(true);

            // A re-initialization with a live tray follows a display change, so refresh the menu's hidden windows
            disposeStaleTrayMenuWindows();
        }
    }

    /**
     * Starts the system tray.
     */
    private void startSystemTray() {
        long trayGetStart = TimingLog.start();
        systemTray = SystemTray.get(TRAY_NAME);
        TimingLog.end("SystemTray.get", trayGetStart);

        long trayTooltipStart = TimingLog.start();
        systemTray.setTooltip(TRAY_NAME);
        TimingLog.end("tray tooltip setup", trayTooltipStart);

        long trayImageStart = TimingLog.start();
        systemTray.setImage(minimizedToTrayIcon);
        TimingLog.end("tray image setup", trayImageStart);
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
     * Disposes the tray menu's hidden anchor dialog, and with it the popup windows it owns, so the next menu open
     * recreates their native peers under the current display geometry. Hidden windows receive no DPI-change events,
     * so a display change while minimized would otherwise leave the menu misplaced and clipped.
     */
    public void disposeStaleTrayMenuWindows() {
        // Without a live tray there are no live menu windows to go stale
        if (systemTray == null) {
            return;
        }

        // A disposed window lingers until it is collected, so an overlapping pass would queue a redundant disposal
        if (disposingTrayMenuWindows) {
            return;
        }

        for (Window window : Window.getWindows()) {
            /*
             * The tray library titles its hidden menu anchor dialog with the tray name it was started with; scan all
             * windows since a null-owner JDialog gets Swing's shared owner frame and is never ownerless
             */
            if (window instanceof JDialog && TRAY_NAME.equals(((JDialog) window).getTitle())) {
                dismissShowingTrayMenu(window);

                disposingTrayMenuWindows = true;

                disposeTrayMenuWindow(window);
            }
        }
    }

    /**
     * Disposes the given tray menu anchor dialog on a later event so the disposal can never land inside an in-progress
     * menu opening. The tray library opens the menu from its own message-pump thread, and tearing the native peers out
     * from under that opening leaves the menu drawn as an empty outline.
     *
     * @param anchorDialog
     *            - The tray menu's hidden anchor dialog to retire
     */
    private void disposeTrayMenuWindow(Window anchorDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimingLog.log("stale tray menu windows disposed for the new display geometry");

                anchorDialog.dispose();

                disposingTrayMenuWindows = false;
            }
        });
    }

    /**
     * Dismisses the tray menu when it is showing over the old geometry, going through the pop-up's own hide path so
     * its internal visible state resets; disposing a showing menu's window directly would leave that state set and
     * make the menu's next show a silent no-op.
     *
     * @param anchorDialog
     *            - The tray menu's hidden anchor dialog whose owned windows host a showing menu
     */
    private void dismissShowingTrayMenu(Window anchorDialog) {
        for (Window ownedWindow : anchorDialog.getOwnedWindows()) {
            JPopupMenu trayMenu = findTrayMenu(ownedWindow);

            if (trayMenu != null) {
                trayMenu.setVisible(false);
            }
        }
    }

    /**
     * Finds the tray menu pop-up within the given container's component tree.
     *
     * @param container
     *            - The container to search
     *
     * @return The tray menu pop-up, or null if the container does not host it
     */
    private JPopupMenu findTrayMenu(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPopupMenu) {
                return (JPopupMenu) component;
            }

            if (component instanceof Container) {
                JPopupMenu trayMenu = findTrayMenu((Container) component);

                if (trayMenu != null) {
                    return trayMenu;
                }
            }
        }

        return null;
    }

    /**
     * Restores the application from the system tray.
     */
    private void restoreAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = view.getFrame();
                frame.setExtendedState(JFrame.NORMAL);
                frame.setVisible(true);
                view.getDefaultFocusComponent().requestFocusInWindow();
            }
        });

        // Re-fit after the frame is shown so layout staleness accrued while hidden does not surface as scroll bars
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FrameUtil.refreshFrame(view.getFrame());
            }
        });

        shutDownSystemTray();
    }

    /**
     * Shows an "About App" dialog.
     */
    private void aboutAction() {
        hideSystemTray();

        // Build the dialog per show so it binds to the current frame, since every re-initialization replaces it
        new AboutDialog(model, view).showAboutDialog(systemTray);
    }

    /**
     * Exits the application.
     */
    private void exitAction() {
        shutDownSystemTray();
        System.exit(0);
    }

    /**
     * Hides the system tray, keeping it alive so dialogs can re-enable the same instance on close.
     */
    private void hideSystemTray() {
        if (systemTray != null) {
            systemTray.setEnabled(false);
        }
    }

    /**
     * Shuts down the system tray and clears it so the next minimize rebuilds it with the correct theme.
     */
    public void shutDownSystemTray() {
        if (systemTray != null) {
            long trayShutdownStart = TimingLog.start();
            systemTray.setEnabled(false);
            systemTray.shutdown();
            TimingLog.end("systemTray.shutdown (MinimizeToTray)", trayShutdownStart);

            systemTray = null;
        }
    }

}
