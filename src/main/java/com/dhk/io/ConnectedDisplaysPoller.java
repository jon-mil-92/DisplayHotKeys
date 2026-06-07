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
 * @author Jonathan R. Miller
 */
public class ConnectedDisplaysPoller implements Runnable {

    private Thread pollingThread;
    private DisplayConfigUpdater displayConfigUpdater;
    private volatile boolean threadSuspended;
    private volatile boolean threadStopped;
    private int interval;

    /**
     * Constructor for the {@link ConnectedDisplaysPoller} class.
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
        pollingThread = new Thread(this);
        pollingThread.setDaemon(true);
        pollingThread.start();
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
                // Expected when force stopping the thread
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

        // Interrupt the thread in case it's sleeping so it can exit promptly
        if (pollingThread != null) {
            pollingThread.interrupt();

            try {
                // Wait for the thread to terminate to avoid thread leaks on re-init
                pollingThread.join(1000);
            } catch (InterruptedException e) {
                // Restore interrupt status
                Thread.currentThread().interrupt();
            }

            pollingThread = null;
        }
    }

}