package com.dhk.window;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

/**
 * This class enables the application to be minimized to the system tray and restored from the system tray.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class MinimizeToTray {
    private Frame frame;
    private AppRefresher appRefresher;
    private SystemTray systemTray;
    private Image minimizedToTrayIcon;

    /**
     * Constructor for the MinimizeToTray class.
     * 
     * @param frame            - The frame for the view to minimize.
     * @param appRefresher     - The refresher for the given frame.
     * @param iconResourcePath - The icon resource path for the tray icon.
     */
    public MinimizeToTray(Frame frame, AppRefresher appRefresher, String iconResourcePath) {
        // Get the frame for the application.
        this.frame = frame;

        // Get the refresher for the frame.
        this.appRefresher = appRefresher;

        // Get the minimized-to-tray icon.
        minimizedToTrayIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(iconResourcePath));
    }

    /**
     * This method adds the menu items to the system tray pop-up menu.
     */
    private void addMenuItems() {
        // Create a restore option for the system tray pop-up menu.
        MenuItem restore = new MenuItem("Restore");

        // Create an exit option for the system tray pop-up menu.
        MenuItem exit = new MenuItem("Exit");

        // Create the action listener for the restore option.
        restore.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Restore the frame.
                frame.setExtendedState(JFrame.NORMAL);

                // Un-hide the frame.
                frame.setVisible(true);

                // Shut down the system tray after restoring the frame.
                systemTray.shutdown();

                // Resume the app refresher so the frame continues to update.
                appRefresher.resume();
            }
        });

        // Add the restore pop-up menu item.
        systemTray.getMenu().add(restore);

        // Create the action listener for the exit option.
        exit.setCallback(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Disable the system tray before exiting the program so it does not stay in the taskbar.
                systemTray.setEnabled(false);
                
                System.exit(0);
            }
        });

        // Add the exit pop-up menu item.
        systemTray.getMenu().add(exit);
    }

    /**
     * This method minimizes the application to the system tray.
     */
    public void execute() {
        // Hide the taskbar icon.
        frame.setVisible(false);

        startSystemTray();

        // Add the menu items to the system tray pop-up menu.
        addMenuItems();
    }

    /**
     * This method starts the system tray.
     */
    public void startSystemTray() {
        // Get the system tray for Windows with the given name.
        systemTray = SystemTray.get("Display Hot Keys");

        // Create a tooltip for the system tray with the given string.
        systemTray.setTooltip("Display Hot Keys");

        // Use the minimized-to-tray image for the system tray icon.
        systemTray.setImage(minimizedToTrayIcon);
    }
}
