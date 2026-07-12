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
