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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import com.dhk.io.NativeKeyEvent;
import com.dhk.io.NativeKeyListener;

/**
 * Hands every key event that can affect a hot key to the current hot keys controller, on the EDT in the order the keys
 * were pressed. It listens for the whole application lifetime, so no key event is lost while a re-initialization
 * replaces the controller, and the controller it builds starts from the keys still held.
 *
 * @author Jonathan R. Miller
 */
public class HotKeyDispatcher implements NativeKeyListener {

    private final Set<Integer> heldKeyCodes;
    private final Set<Integer> dispatchedKeyCodes;
    private volatile HotKeysController hotKeysController;

    /**
     * Default constructor for the {@link HotKeyDispatcher} class.
     */
    public HotKeyDispatcher() {
        // The held keys are only touched on the EDT, while the dispatched keys are read and written on the input thread
        heldKeyCodes = new HashSet<Integer>();
        dispatchedKeyCodes = ConcurrentHashMap.newKeySet();
    }

    /**
     * Sets the hot keys controller that handles the key events from now on, which every re-initialization replaces.
     *
     * @param hotKeysController
     *            - The current hot keys controller
     */
    public void setHotKeysController(HotKeysController hotKeysController) {
        this.hotKeysController = hotKeysController;
    }

    @Override
    public void keyPressed(NativeKeyEvent keyEvent) {
        if (keyEvent == null || !isRelevant(keyEvent.getVirtualKeyCode())) {
            return;
        }

        int keyCode = keyEvent.getVirtualKeyCode();
        dispatchedKeyCodes.add(keyCode);

        // The held keys change in the same EDT task that handles the press, so a re-initialization sees both or neither
        SwingUtilities.invokeLater(() -> {
            heldKeyCodes.add(keyCode);
            hotKeysController.hotKeyPressed(keyEvent);
        });
    }

    @Override
    public void keyReleased(NativeKeyEvent keyEvent) {
        if (keyEvent == null) {
            return;
        }

        int keyCode = keyEvent.getVirtualKeyCode();

        // A key whose press was handed on has its release handed on as well, even if it stopped being relevant since
        if (!dispatchedKeyCodes.remove(keyCode) && !isRelevant(keyCode)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            heldKeyCodes.remove(keyCode);
            hotKeysController.hotKeyReleased(keyEvent);
        });
    }

    /**
     * Reports whether the key with the given virtual key code is held down, as of the key events the EDT has handled.
     * Only call this on the EDT.
     *
     * @param keyCode
     *            - The virtual key code to check
     *
     * @return Whether the key is currently held down
     */
    public boolean isKeyHeld(int keyCode) {
        return heldKeyCodes.contains(keyCode);
    }

    /**
     * Checks on the input thread whether a key can affect a hot key, so the EDT is not woken for every key typed in
     * other applications.
     *
     * @param keyCode
     *            - The virtual key code of the key event
     *
     * @return Whether the current hot keys controller has a use for the key event
     */
    private boolean isRelevant(int keyCode) {
        HotKeysController currentController = hotKeysController;

        return currentController != null && currentController.isKeyEventRelevant(keyCode);
    }

}
