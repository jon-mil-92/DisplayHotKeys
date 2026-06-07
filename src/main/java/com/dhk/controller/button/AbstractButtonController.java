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
package com.dhk.controller.button;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.dhk.model.button.AbstractButton;

/**
 * Provides a method to initialize the state change listeners for a button.
 *
 * @author Jonathan R. Miller
 */
public abstract class AbstractButtonController {

    /**
     * Default constructor for the {@link AbstractButtonController} class
     */
    public AbstractButtonController() {
    }

    /**
     * Initializes the state change listeners for the given button.
     *
     * @param button
     *            - The button to initialize the state change listeners for
     * @param defaultFocusComponent
     *            - The component to give focus to when the cursor leaves the button
     */
    public void initStateChangeListeners(AbstractButton button, Component defaultFocusComponent) {
        button.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                button.getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                button.getModel().setRollover(false);
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.requestFocusInWindow();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                defaultFocusComponent.requestFocusInWindow();
            }
        });
    }

}
