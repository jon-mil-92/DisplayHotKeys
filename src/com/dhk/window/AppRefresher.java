package com.dhk.window;

import javax.swing.SwingUtilities;
import com.dhk.ui.DhkView;

/**
 * This class starts an app refresher on a new thread. When the thread is running, the frame of the application is 
 * refreshed at a given interval.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class AppRefresher implements Runnable {
	private Thread refresher;
	private FrameUpdater frameUpdater;
	private volatile boolean threadSuspended;
	private int interval;
	
	/**
	 * Constructor for the AppRefresher class.
	 * 
	 * @param frame - The frame for the GUI to refresh.
	 * @param interval - The interval at which to refresh the app.
	 */
	public AppRefresher (DhkView view, int interval) {
		// Initialize the interval field.
		this.interval = interval;
		
		// Do not start the thread in a "suspended" state.
		threadSuspended = false;
		
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
	    while (true) {
	    	try {
	        	// Put the thread to sleep for the given interval.
	            Thread.sleep(interval);
	
	            // If the thread executing this runnable is in the "suspended" state...
	            if (threadSuspended) {
	                synchronized(this) {
	                	// Make the frame refresher wait.
	                    wait();
	                }
	            }
	        } catch (InterruptedException e){
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
	 * Suspend the frame refresher.
	 */
	public void suspend() {
		// Set the "suspended" state for this runnable's thread.
		this.threadSuspended = true;
	}
	
	/**
	 * Resume the frame refresher.
	 */
	public void resume() {
		// Leave the "suspended" state for this runnable's thread.
		this.threadSuspended = false;
		
		// Notify the frame refresher to leave the "waiting" state.
		synchronized(this) {
			notify();
		}
	}
}