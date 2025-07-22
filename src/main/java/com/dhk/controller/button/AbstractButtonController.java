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
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public abstract class AbstractButtonController {

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
