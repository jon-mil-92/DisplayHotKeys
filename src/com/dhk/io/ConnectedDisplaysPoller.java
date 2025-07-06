package com.dhk.io;

import javax.swing.SwingUtilities;
import com.dhk.controllers.DhkController;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class starts a display configuration updater on a new thread. When the thread is running, the number of
 * connected displays is checked at a given interval. If the number of connected displays has changed, then the
 * application's settings manager, model, view, and controllers are re-initialized to reflect the new display
 * configuration.
 * 
 * @author Jonathan Miller
 * @version 1.4.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ConnectedDisplaysPoller implements Runnable {
    private Thread refresher;
    private DisplayConfigUpdater displayConfigUpdater;
    private volatile boolean threadSuspended;
    private volatile boolean threadStopped;
    private int interval;

    /**
     * Constructor for the ConnectedDisplaysPoller class.
     * 
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param controller  - The controller for the application.
     * @param settingsMgr - The settings manager for the application.
     * @param interval    - The interval at which to poll for connected displays.
     */
    public ConnectedDisplaysPoller(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr,
            int interval) {
        // Get the polling interval.
        this.interval = interval;

        // Do not start the thread in a "suspended" state.
        threadSuspended = false;

        // Do not start the thread in a "stopped" state.
        threadStopped = false;

        // Initialize the object that will check for number of connected display changes.
        displayConfigUpdater = new DisplayConfigUpdater(model, view, controller, settingsMgr);
    }

    /**
     * This method creates and starts a new thread for this runnable to run on.
     */
    public void start() {
        // Create and then start the thread for this runnable.
        refresher = new Thread(this);
        refresher.start();
    }

    /**
     * This method starts running this runnable. It polls for the number of connected displays at every given interval.
     */
    public void run() {
        // Continuously run while the thread is not stopped.
        while (!threadStopped) {
            try {
                // Put the thread to sleep for the given interval.
                Thread.sleep(interval);

                // If the thread executing this runnable is in the "suspended" state...
                if (threadSuspended) {
                    synchronized (this) {
                        // Make the connected displays poller wait.
                        wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check for number of connected display changes using the AWT event dispatch thread.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Poll for connected displays, and refresh the app if the number of connected displays has changed.
                    displayConfigUpdater.checkNumOfConnectedDisplays();
                }
            });
        }
    }

    /**
     * Suspend the connected displays poller.
     */
    public void suspend() {
        // Set the "suspended" state for this runnable's thread.
        threadSuspended = true;
    }

    /**
     * Resume the connected displays poller.
     */
    public void resume() {
        // Leave the "suspended" state for this runnable's thread.
        threadSuspended = false;

        // Notify the connected displays poller to leave the "waiting" state.
        synchronized (this) {
            notify();
        }
    }

    /**
     * Stop the connected displays poller.
     */
    public void stop() {
        // Stop the connected displays poller.
        threadStopped = true;
    }
}