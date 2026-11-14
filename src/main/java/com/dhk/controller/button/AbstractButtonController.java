/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
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
