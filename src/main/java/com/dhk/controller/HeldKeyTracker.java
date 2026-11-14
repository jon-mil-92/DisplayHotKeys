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
package com.dhk.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.dhk.io.NativeKeyEvent;
import com.dhk.io.NativeKeyListener;

/**
 * Tracks which keys are currently held down, recording the side a modifier was pressed on. This listener stays attached
 * across app refreshes and holds no model, view, or controller references, so held keys survive an app refresh that
 * follows applying a hot key and it can never leak the old object graph.
 *
 * @author Jonathan R. Miller
 */
public class HeldKeyTracker implements NativeKeyListener {

    private final Set<Integer> heldKeyCodes;

    /**
     * Default constructor for the {@link HeldKeyTracker} class.
     */
    public HeldKeyTracker() {
        // Synchronized because events are delivered on the native reporting thread while seeding reads on the EDT
        heldKeyCodes = Collections.synchronizedSet(new HashSet<Integer>());
    }

    @Override
    public void keyPressed(NativeKeyEvent keyEvent) {
        if (keyEvent != null) {
            heldKeyCodes.add(keyEvent.getVirtualKeyCode());
        }
    }

    @Override
    public void keyReleased(NativeKeyEvent keyEvent) {
        if (keyEvent != null) {
            heldKeyCodes.remove(keyEvent.getVirtualKeyCode());
        }
    }

    /**
     * Reports whether the key with the given virtual key code is currently held down.
     *
     * @param keyCode
     *            - The virtual key code to check
     *
     * @return Whether the key is currently held down
     */
    public boolean isKeyHeld(int keyCode) {
        return heldKeyCodes.contains(keyCode);
    }

}
