package com.dhk.controller;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import com.dhk.view.DhkView;

/**
 * Controls the frame of the application. Listeners are added to the frame that enables the user to click, hold, and
 * drag on any empty spot in the frame to move the application window around on the desktop.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class FrameDragController implements IController {

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
     * @param view
     *            - The view for the application
     */
    public FrameDragController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
        frame = view.getFrame();

        frameLocation = new Point();
        frameX = 0;
        frameY = 0;

        mousePressedX = 0;
        mousePressedY = 0;
        mouseDraggedX = 0;
        mouseDraggedY = 0;
    }

    /**
     * Initializes the mouse listeners for the frame to enable window dragging.
     */
    @Override
    public void initListeners() {
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                if (mousePressedEvent != null) {
                    mousePressedX = mousePressedEvent.getXOnScreen();
                    mousePressedY = mousePressedEvent.getYOnScreen();
                }

                // Focus on the display IDs label to remove the selection outline around the last selected component
                view.getDisplayIdsLabel().requestFocusInWindow();
            }
        });

        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent mouseDraggedEvent) {
                frameLocation = frame.getLocationOnScreen();

                if (frameLocation != null) {
                    frameX = frameLocation.x;
                    frameY = frameLocation.y;
                }

                if (mouseDraggedEvent != null) {
                    mouseDraggedX = mouseDraggedEvent.getXOnScreen();
                    mouseDraggedY = mouseDraggedEvent.getYOnScreen();
                }

                int dragDistanceX = mouseDraggedX - mousePressedX;
                int dragDistanceY = mouseDraggedY - mousePressedY;

                // Move the frame with the mouse pointer
                frame.setLocation(frameX + dragDistanceX, frameY + dragDistanceY);

                mousePressedX = mouseDraggedX;
                mousePressedY = mouseDraggedY;
            }
        });
    }

    @Override
    public void cleanUp() {
    }

}
