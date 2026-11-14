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
package com.dhk.io;

/**
 * Listener for key events delivered from the native input hook. The callbacks arrive on the native layer's reporting
 * thread rather than the EDT, so any model or view work must be marshaled to the EDT, and slow work must be avoided
 * since it delays every later event.
 *
 * @author Jonathan R. Miller
 */
public interface NativeKeyListener {

    /**
     * Called when a key is pressed anywhere in the system.
     *
     * @param keyEvent
     *            - The key event describing the pressed key
     */
    void keyPressed(NativeKeyEvent keyEvent);

    /**
     * Called when a key is released anywhere in the system.
     *
     * @param keyEvent
     *            - The key event describing the released key
     */
    void keyReleased(NativeKeyEvent keyEvent);

}
