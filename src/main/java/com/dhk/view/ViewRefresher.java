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

import javax.swing.SwingUtilities;

/**
 * Starts a view refresher on a new thread. When the thread is running, the frame of the application is refreshed at a
 * given interval.
 *
 * @author Jonathan R. Miller
 */
public class ViewRefresher implements Runnable {

    private Thread refresherThread;
    private FrameUpdater frameUpdater;
    private volatile boolean threadSuspended;
    private volatile boolean threadStopped;
    private int interval;

    /**
     * Constructor for the {@link ViewRefresher} class.
     *
     * @param view
     *            - The view for the application
     * @param interval
     *            - The interval at which to refresh the view in milliseconds
     */
    public ViewRefresher(DhkView view, int interval) {
        this.interval = interval;

        threadSuspended = false;
        threadStopped = false;
        frameUpdater = new FrameUpdater(view);
    }

    /**
     * Creates and starts a new thread for this runnable to run on.
     */
    public void start() {
        refresherThread = new Thread(this);
        refresherThread.setDaemon(true);
        refresherThread.start();
    }

    /**
     * Starts running this runnable. It updates the given frame at every given interval.
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
                    frameUpdater.update();
                }
            });
        }
    }

    /**
     * Suspends the view refresher.
     */
    public void suspend() {
        this.threadSuspended = true;
    }

    /**
     * Resumes the view refresher.
     */
    public void resume() {
        this.threadSuspended = false;

        // Notify the frame refresher to leave the "waiting" state
        synchronized (this) {
            notify();
        }
    }

    /**
     * Stops the view refresher.
     */
    public void stop() {
        threadStopped = true;

        // Interrupt the thread in case it's sleeping so it can exit promptly
        if (refresherThread != null) {
            refresherThread.interrupt();

            try {
                // Wait for the thread to terminate to avoid thread leaks on re-init
                refresherThread.join(1000);
            } catch (InterruptedException e) {
                // Restore interrupt status
                Thread.currentThread().interrupt();
            }

            refresherThread = null;
        }
    }

}