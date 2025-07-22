package com.dhk.window;

import javax.swing.SwingUtilities;
import com.dhk.view.DhkView;

/**
 * Starts a view refresher on a new thread. When the thread is running, the frame of the application is refreshed at a
 * given interval.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
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
        refresher = new Thread(this);
        refresher.start();
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
                e.printStackTrace();
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
    }

}