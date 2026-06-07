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
 * @author Jonathan R. Miller
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
     * Constructor for the {@link FrameDragController} class.
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

    @Override
    public void initListeners() {
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                if (mousePressedEvent != null) {
                    mousePressedX = mousePressedEvent.getXOnScreen();
                    mousePressedY = mousePressedEvent.getYOnScreen();
                }

                view.getDefaultFocusComponent().requestFocusInWindow();
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
