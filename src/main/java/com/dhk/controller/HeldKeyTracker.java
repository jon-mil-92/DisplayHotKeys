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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;

/**
 * Tracks which virtual key codes are currently held down on the global keyboard hook. This listener stays attached
 * across app re-initializations and holds no model, view, or controller references, so held keys survive the rebuild
 * that follows applying a hot key and it can never leak the old object graph.
 *
 * @author Jonathan R. Miller
 */
public class HeldKeyTracker implements GlobalKeyListener {

    private final Set<Integer> heldKeyCodes;

    /**
     * Default constructor for the {@link HeldKeyTracker} class.
     */
    public HeldKeyTracker() {
        // Synchronized because the hook delivers events on its native thread while seeding reads on the EDT
        heldKeyCodes = Collections.synchronizedSet(new HashSet<Integer>());
    }

    @Override
    public void keyPressed(GlobalKeyEvent keyEvent) {
        if (keyEvent != null) {
            heldKeyCodes.add(keyEvent.getVirtualKeyCode());
        }
    }

    @Override
    public void keyReleased(GlobalKeyEvent keyEvent) {
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
