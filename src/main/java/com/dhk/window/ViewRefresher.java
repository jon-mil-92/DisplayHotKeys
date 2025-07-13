package com.dhk.window;

import javax.swing.SwingUtilities;
import com.dhk.ui.DhkView;

/**
 * This class starts a view refresher on a new thread. When the thread is running, the frame of the application is
 * refreshed at a given interval.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ViewRefresher implements Runnable {
    private Thread refresher;
    private FrameUpdater frameUpdater;
    private volatile boolean threadSuspended;
    private volatile boolean threadStopped;
    private int interval;

    /**
     * Constructor for the ViewRefresher class.
     * 
     * @param view     - The view for the application.
     * @param interval - The interval at which to refresh the view in milliseconds.
     */
    public ViewRefresher(DhkView view, int interval) {
        // Get the refresh interval.
        this.interval = interval;

        // Do not start the thread in a "suspended" state.
        threadSuspended = false;

        // Do not start the thread in a "stopped" state.
        threadStopped = false;

        // Initialize the frame updater object that will update the view's frame.
        frameUpdater = new FrameUpdater(view);
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
     * This method starts running this runnable. It updates the given frame at every given interval.
     */
    public void run() {
        // Continuously run while the thread is not suspended.
        while (!threadStopped) {
            try {
                // Put the thread to sleep for the given interval.
                Thread.sleep(interval);

                // If the thread executing this runnable is in the "suspended" state...
                if (threadSuspended) {
                    synchronized (this) {
                        // Make the frame refresher wait.
                        wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update the frame using the AWT event dispatch thread.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Update the view's frame.
                    frameUpdater.update();
                }
            });
        }
    }

    /**
     * Suspend the view refresher.
     */
    public void suspend() {
        // Set the "suspended" state for this runnable's thread.
        this.threadSuspended = true;
    }

    /**
     * Resume the view refresher.
     */
    public void resume() {
        // Leave the "suspended" state for this runnable's thread.
        this.threadSuspended = false;

        // Notify the frame refresher to leave the "waiting" state.
        synchronized (this) {
            notify();
        }
    }

    /**
     * Stop the view refresher.
     */
    public void stop() {
        // Stop the view refresher.
        threadStopped = true;
    }
}