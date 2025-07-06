package com.dhk.controllers;

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
 * @version 1.3.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class FrameDragController implements Controller {
    private DhkView view;
    private JFrame frame;
    private Point frameLocation;
    private int frameX;
    private int frameY;
    private int mousePressedX;
    private int mousePressedY;
    private int mouseDraggedX;
    private int mouseDraggedY;

    /**
     * Constructor for the FrameDragController class.
     *
     * @param view - The view for the application.
     */
    public FrameDragController(DhkView view) {
        // Get the application's view.
        this.view = view;
    }

    @Override
    public void initController() {
        // Get the current frame of the application.
        frame = view.getFrame();

        // Initialize the frame coordinates.
        frameLocation = new Point();
        frameX = 0;
        frameY = 0;

        // Initialize the mouse coordinates.
        mousePressedX = 0;
        mousePressedY = 0;
        mouseDraggedX = 0;
        mouseDraggedY = 0;
    }

    /**
     * This method initializes the mouse listeners for the frame to enable window dragging.
     */
    @Override
    public void initListeners() {
        // Set the mouse listener for the frame.
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                // Only get the mouse pressed event coordinates when they are not null.
                if (mousePressedEvent != null) {
                    mousePressedX = mousePressedEvent.getXOnScreen();
                    mousePressedY = mousePressedEvent.getYOnScreen();
                }

                // Focus on the display IDs label to remove the selection outline around the last selected component.
                view.getDisplayIdsLabel().requestFocusInWindow();
            }
        });

        // Set the mouse movement listener for the frame.
        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent mouseDraggedEvent) {
                // Get the current coordinates of the frame.
                frameLocation = frame.getLocationOnScreen();

                // Only get the frame location coordinates when they are not null.
                if (frameLocation != null) {
                    frameX = frameLocation.x;
                    frameY = frameLocation.y;
                }

                // Only get the mouse dragged event coordinates when they are not null.
                if (mouseDraggedEvent != null) {
                    mouseDraggedX = mouseDraggedEvent.getXOnScreen();
                    mouseDraggedY = mouseDraggedEvent.getYOnScreen();
                }

                // The distance the mouse pointer moved horizontally.
                int dragDistanceX = mouseDraggedX - mousePressedX;

                // The distance the mouse pointer moved vertically.
                int dragDistanceY = mouseDraggedY - mousePressedY;

                // Move the frame with the mouse pointer.
                frame.setLocation(frameX + dragDistanceX, frameY + dragDistanceY);

                // Update the coordinates of the mouse pointer.
                mousePressedX = mouseDraggedX;
                mousePressedY = mouseDraggedY;
            }
        });
    }

    @Override
    public void cleanUp() {
    }
}
