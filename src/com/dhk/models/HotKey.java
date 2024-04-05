package com.dhk.models;

import java.util.ArrayList;

/**
 * This class represents the model for a Hot Key. States and the string representation of the Hot Key are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class HotKey {
    private ArrayList<Key> keys;
    private boolean hotKeyPressed;
    private boolean hotKeyHeldDown;
    private boolean changingHotKey;

    /**
     * Constructor for the HotKey class.
     * 
     * @param hotKey - The array list of keys that make up the hotkey.
     */
    public HotKey(ArrayList<Key> hotKey) {
        this.keys = hotKey;
        this.changingHotKey = false;
        this.hotKeyPressed = false;
        this.hotKeyHeldDown = false;
    }

    /**
     * This method builds a hotkey string from an array list of keys.
     * 
     * @return The hotkey string.
     */
    public String getHotKeyString() {
        // Start building the string with an empty string.
        String hotKeyString = "";

        // For each HotKey, append its key to the hot key string.
        for (int i = 0; i < keys.size(); i++) {
            // If not on the last element of the array...
            if (i != (keys.size() - 1)) {
                // Build the string with a plus at the end.
                hotKeyString += (keys.get(i).getName() + " + ");
            } else {
                // Build the string without the plus since it is the last key in the hotkey.
                hotKeyString += (keys.get(i).getName());
            }
        }

        // If the hotkey's array list of keys is empty...
        if (keys.size() == 0) {
            // The hotkey is not set.
            hotKeyString = "Not Set!";
        }

        return hotKeyString;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the key array list that makes up the hotkey.
     * 
     * @return The key array list that makes up the hotkey.
     */
    public ArrayList<Key> getKeys() {
        return keys;
    }

    /**
     * Setter for the key array list that makes up the hotkey.
     * 
     * @param keys - The new key array list that makes up the hotkey.
     */
    public void setKeys(ArrayList<Key> keys) {
        this.keys = keys;
    }

    /**
     * Getter for the "changing hotkey" state of the slot.
     * 
     * @return Whether or not the user is changing the hotkey for the slot.
     */
    public boolean isChangingHotKey() {
        return changingHotKey;
    }

    /**
     * Setter for the "changing hotkey" state of the slot.
     * 
     * @param changingHotKey - The new boolean for the "changing hotkey" state of the slot.
     */
    public void setChangingHotKey(boolean changingHotKey) {
        this.changingHotKey = changingHotKey;
    }

    /**
     * Setter for the hotkey's "pressed" state.
     * 
     * @return Whether or not the hotkey is pressed.
     */
    public boolean isHotKeyPressed() {
        return hotKeyPressed;
    }

    /**
     * Setter for the hotkey's "pressed" state.
     * 
     * @param hotKeyPressed - The new "pressed" state for the hotkey.
     */
    public void setHotKeyPressed(boolean hotKeyPressed) {
        this.hotKeyPressed = hotKeyPressed;
    }

    /**
     * Getter for the hotkey's "held down" state.
     * 
     * @return The "held down" state of the hotkey.
     */
    public boolean isHotKeyHeldDown() {
        return hotKeyHeldDown;
    }

    /**
     * Setter for the hotkey's "held down" state.
     * 
     * @param hotKeyHeldDown - The new "held down" state for the hotkey.
     */
    public void setHotKeyHeldDown(boolean hotKeyHeldDown) {
        this.hotKeyHeldDown = hotKeyHeldDown;
    }
}
