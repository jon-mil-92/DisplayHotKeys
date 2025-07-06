package com.dhk.models;

import java.util.ArrayList;

/**
 * This class represents the model for a Hot Key. States and the string representation of the Hot Key are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class HotKey {
    private ArrayList<Key> keys;
    private boolean hotKeyPressed;
    private boolean hotKeyHeldDown;
    private boolean changingHotKey;

    /**
     * Constructor for the HotKey class.
     * 
     * @param hotKey - The array list of keys that make up the hot key.
     */
    public HotKey(ArrayList<Key> hotKey) {
        this.keys = hotKey;
        this.changingHotKey = false;
        this.hotKeyPressed = false;
        this.hotKeyHeldDown = false;
    }

    /**
     * This method builds a hot key string from an array list of keys.
     * 
     * @return The hot key string.
     */
    public String getHotKeyString() {
        // Start building the string with an empty string.
        String hotKeyString = "";

        // For each HotKey, append its key to the hot key string.
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            // If not on the last element of the array...
            if (keyIndex != (keys.size() - 1)) {
                // Build the string with a plus at the end.
                hotKeyString += (keys.get(keyIndex).getName() + " + ");
            } else {
                // Build the string without the plus since it is the last key in the hot key.
                hotKeyString += (keys.get(keyIndex).getName());
            }
        }

        // If the hot key's array list of keys is empty...
        if (keys.size() == 0) {
            // The hot key is not set.
            hotKeyString = "Not Set";
        }

        return hotKeyString;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the key array list that makes up the hot key.
     * 
     * @return The key array list that makes up the hot key.
     */
    public ArrayList<Key> getKeys() {
        return keys;
    }

    /**
     * Setter for the key array list that makes up the hot key.
     * 
     * @param keys - The new key array list that makes up the hot key.
     */
    public void setKeys(ArrayList<Key> keys) {
        this.keys = keys;
    }

    /**
     * Getter for the "changing hot key" state of the slot.
     * 
     * @return Whether or not the user is changing the hot key for the slot.
     */
    public boolean isChangingHotKey() {
        return changingHotKey;
    }

    /**
     * Setter for the "changing hot key" state of the slot.
     * 
     * @param changingHotKey - The new boolean for the "changing hot key" state of the slot.
     */
    public void setChangingHotKey(boolean changingHotKey) {
        this.changingHotKey = changingHotKey;
    }

    /**
     * Getter for the hot key's "pressed" state.
     * 
     * @return Whether or not the hot key is pressed.
     */
    public boolean isHotKeyPressed() {
        return hotKeyPressed;
    }

    /**
     * Setter for the hot key's "pressed" state.
     * 
     * @param hotKeyPressed - The new "pressed" state for the hot key.
     */
    public void setHotKeyPressed(boolean hotKeyPressed) {
        this.hotKeyPressed = hotKeyPressed;
    }

    /**
     * Getter for the hot key's "held down" state.
     * 
     * @return The "held down" state of the hot key.
     */
    public boolean isHotKeyHeldDown() {
        return hotKeyHeldDown;
    }

    /**
     * Setter for the hot key's "held down" state.
     * 
     * @param hotKeyHeldDown - The new "held down" state for the hot key.
     */
    public void setHotKeyHeldDown(boolean hotKeyHeldDown) {
        this.hotKeyHeldDown = hotKeyHeldDown;
    }
}
