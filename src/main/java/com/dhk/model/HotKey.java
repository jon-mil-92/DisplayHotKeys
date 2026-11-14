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
package com.dhk.model;

import java.util.List;

/**
 * Defines the model for a Hot Key. States and the string representation of the Hot Key are defined here.
 *
 * @author Jonathan R. Miller
 */
public class HotKey {

    private List<Key> keys;
    private boolean hotKeyPressed;
    private boolean hotKeyHeldDown;
    private boolean changingHotKey;

    /**
     * Constructor for the {@link HotKey} class.
     *
     * @param hotKey
     *            - The array list of keys that make up the hot key
     */
    public HotKey(List<Key> hotKey) {
        this.keys = hotKey;
        this.changingHotKey = false;
        this.hotKeyPressed = false;
        this.hotKeyHeldDown = false;
    }

    /**
     * Builds a hot key string from an array list of keys.
     *
     * @return The hot key string
     */
    public String getHotKeyString() {
        String hotKeyString = "";

        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            if (keyIndex != (keys.size() - 1)) {
                hotKeyString += (keys.get(keyIndex).getName() + " + ");
            } else {
                hotKeyString += (keys.get(keyIndex).getName());
            }
        }

        if (keys.size() == 0) {
            hotKeyString = "Not Set";
        }

        return hotKeyString;
    }

    /**
     * Gets the key array list that makes up the hot key.
     *
     * @return The key array list that makes up the hot key
     */
    public List<Key> getKeys() {
        return keys;
    }

    /**
     * Sets the key array list that makes up the hot key.
     *
     * @param keys
     *            - The new key array list that makes up the hot key
     */
    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    /**
     * Gets the "changing hot key" state of the slot.
     *
     * @return Whether or not the user is changing the hot key for the slot
     */
    public boolean isChangingHotKey() {
        return changingHotKey;
    }

    /**
     * Sets the "changing hot key" state of the slot.
     *
     * @param changingHotKey
     *            - The new boolean for the "changing hot key" state of the slot
     */
    public void setChangingHotKey(boolean changingHotKey) {
        this.changingHotKey = changingHotKey;
    }

    /**
     * Gets the hot key's "pressed" state.
     *
     * @return Whether or not the hot key is pressed
     */
    public boolean isHotKeyPressed() {
        return hotKeyPressed;
    }

    /**
     * Sets the hot key's "pressed" state.
     *
     * @param hotKeyPressed
     *            - The new "pressed" state for the hot key
     */
    public void setHotKeyPressed(boolean hotKeyPressed) {
        this.hotKeyPressed = hotKeyPressed;
    }

    /**
     * Gets the hot key's "held down" state.
     *
     * @return The "held down" state of the hot key
     */
    public boolean isHotKeyHeldDown() {
        return hotKeyHeldDown;
    }

    /**
     * Sets the hot key's "held down" state.
     *
     * @param hotKeyHeldDown
     *            - The new "held down" state for the hot key
     */
    public void setHotKeyHeldDown(boolean hotKeyHeldDown) {
        this.hotKeyHeldDown = hotKeyHeldDown;
    }

}
