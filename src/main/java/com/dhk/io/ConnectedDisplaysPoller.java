package com.dhk.io;

import javax.swing.SwingUtilities;
import com.dhk.controller.DhkController;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Starts a display configuration updater on a new thread. When the thread is running, the number of connected displays
 * is checked at a given interval. If the number of connected displays has changed, then the application's settings
 * manager, model, view, and controllers are re-initialized to reflect the new display configuration.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
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
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param controller
     *            - The controller for the application
     * @param settingsMgr
     *            - The settings manager for the application
     * @param interval
     *            - The interval at which to poll for connected displays
     */
    public ConnectedDisplaysPoller(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr,
            int interval) {
        this.interval = interval;
        threadSuspended = false;
        threadStopped = false;
        displayConfigUpdater = new DisplayConfigUpdater(model, view, controller, settingsMgr);
    }

    /**
     * Creates and starts a new thread for this runnable to run on.
     */
    public void start() {
        refresher = new Thread(this);
        refresher.start();
    }

    /**
     * Starts running this runnable. It polls for the number of connected displays at every given interval.
     */
    @Override
    public void run() {
        while (!threadStopped) {
            try {
                Thread.sleep(interval);

                if (threadSuspended) {
                    synchronized (this) {
                        wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    displayConfigUpdater.checkNumOfConnectedDisplays();
                }
            });
        }
    }

    /**
     * Suspends the connected displays poller.
     */
    public void suspend() {
        threadSuspended = true;
    }

    /**
     * Resumes the connected displays poller.
     */
    public void resume() {
        threadSuspended = false;

        synchronized (this) {
            notify();
        }
    }

    /**
     * Stops the connected displays poller.
     */
    public void stop() {
        threadStopped = true;
    }

}