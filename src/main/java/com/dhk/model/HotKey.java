package com.dhk.model;

import java.util.List;

/**
 * Defines the model for a Hot Key. States and the string representation of the Hot Key are defined here.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class HotKey {

    private List<Key> keys;
    private boolean hotKeyPressed;
    private boolean hotKeyHeldDown;
    private boolean changingHotKey;

    /**
     * Constructor for the HotKey class.
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
