package com.dhk.window;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import com.dhk.view.DhkView;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

/**
 * Enables the application to be minimized to the system tray and restored from the system tray.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class MinimizeToTray {

    private DhkView view;
    private Frame frame;
    private ViewRefresher viewRefresher;
    private SystemTray systemTray;
    private Image minimizedToTrayIcon;

    /**
     * Constructor for the MinimizeToTray class.
     * 
     * @param view
     *            - The view for the application
     * @param appRefresher
     *            - The refresher for the given frame.
     * @param iconResourcePath
     *            - The icon resource path for the tray icon.
     */
    public MinimizeToTray(DhkView view, ViewRefresher appRefresher, String iconResourcePath) {
        this.view = view;
        this.frame = view.getFrame();
        this.viewRefresher = appRefresher;

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
        MenuItem restore = new MenuItem("Restore");
        MenuItem exit = new MenuItem("Exit");

        restore.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setExtendedState(JFrame.NORMAL);
                frame.setVisible(true);

                // Focus on the selected display label after restoring the frame
                view.getSelectedDisplayLabel().requestFocusInWindow();

                systemTray.shutdown();
                viewRefresher.resume();
            }
        });

        systemTray.getMenu().add(restore);

        exit.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Disable the system tray before exiting the program so it does not stay in the taskbar
                systemTray.setEnabled(false);

                System.exit(0);
            }
        });

        systemTray.getMenu().add(exit);
    }

    /**
     * Gets the system tray.
     * 
     * @return The system tray
     */
    public SystemTray getSystemTray() {
        return systemTray;
    }

}
