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

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Defines an abstract draggable dialog that defines a method to initialize the mouse listeners.
 *
 * @author Jonathan R. Miller
 */
public abstract class AbstractDraggableDialog implements IView {

    protected JFrame parentFrame;
    private Point dialogLocation;
    private int dialogX;
    private int dialogY;
    private int mousePressedX;
    private int mousePressedY;
    private int mouseDraggedX;
    private int mouseDraggedY;

    /**
     * Constructor for the {@link AbstractDraggableDialog} class.
     *
     * @param parentFrame
     *            - The parent frame for the dialog
     */
    public AbstractDraggableDialog(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Initializes the mouse listeners for a dialog to enable window dragging.
     *
     * @param dialog
     *            - The dialog to initialize mouse listeners for
     */
    public void initMouseListeners(JDialog dialog) {
        dialogLocation = new Point();
        dialogX = 0;
        dialogY = 0;
        mousePressedX = 0;
        mousePressedY = 0;
        mouseDraggedX = 0;
        mouseDraggedY = 0;

        dialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                if (mousePressedEvent != null) {
                    mousePressedX = mousePressedEvent.getXOnScreen();
                    mousePressedY = mousePressedEvent.getYOnScreen();
                }

                getDefaultFocusComponent().requestFocusInWindow();
            }
        });

        dialog.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent mouseDraggedEvent) {
                dialogLocation = dialog.getLocationOnScreen();

                if (dialogLocation != null) {
                    dialogX = dialogLocation.x;
                    dialogY = dialogLocation.y;
                }

                if (mouseDraggedEvent != null) {
                    mouseDraggedX = mouseDraggedEvent.getXOnScreen();
                    mouseDraggedY = mouseDraggedEvent.getYOnScreen();
                }

                int dragDistanceX = mouseDraggedX - mousePressedX;
                int dragDistanceY = mouseDraggedY - mousePressedY;

                // Move the dialog with the mouse pointer
                dialog.setLocation(dialogX + dragDistanceX, dialogY + dragDistanceY);

                mousePressedX = mouseDraggedX;
                mousePressedY = mouseDraggedY;
            }
        });
    }

}
