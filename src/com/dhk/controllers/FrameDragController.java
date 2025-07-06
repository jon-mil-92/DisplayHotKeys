package com.dhk.controllers;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import com.dhk.ui.DhkView;

/**
 * This class controls the frame of the application. Listeners are added to the frame that enables the user to click,
 * hold, and drag on any empty spot in the frame to move the application window around on the desktop.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class FrameDragController implements Controller {
	private final JFrame frame;
	private Point mousePressedCoords;

	/**
	 * Constructor for the FrameDragController class.
	 *
	 * @param view - The view for the application.
	 */
	public FrameDragController(DhkView view) {
		// Get the view's frame.
		this.frame = view.getFrame();
		
		// Initialize the mouse pointer coordinates.
		mousePressedCoords = null;
	}
	
	/**
	 * This method initializes the mouse listeners for the frame to enable window dragging.
	 */
	public void initListeners() {
		// Set the mouse listener for the frame.
		frame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				// Reset the mouse pointer coordinates when the left mouse button is released.
				mousePressedCoords = null;
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				// Get the coordinates of the mouse pointer when the left mouse button is pressed.
				mousePressedCoords = MouseInfo.getPointerInfo().getLocation();
			}
		});
		
		// Set the mouse movement listener for the frame.
		frame.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(final MouseEvent e) {
				// Get the current coordinates of the frame.
				Point frameCoords = frame.getLocationOnScreen();
				
				// Get the new coordinates of the mouse pointer.
				Point newMousePressedCoords = MouseInfo.getPointerInfo().getLocation();
				
				// The distance the mouse pointer moved horizontally.
				int dragDistanceX = newMousePressedCoords.x - mousePressedCoords.x;
				
				// The distance the mouse pointer moved vertically.
				int dragDistanceY = newMousePressedCoords.y - mousePressedCoords.y;
				
				// Move the frame with the mouse pointer.
				frame.setLocation(frameCoords.x + dragDistanceX, frameCoords.y + dragDistanceY);
				
				// Update the coordinates of the mouse pointer.
				mousePressedCoords = newMousePressedCoords;
			}
		});
	}
}
