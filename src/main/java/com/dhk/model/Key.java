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

/**
 * Defines the model for a Key. The key code, name, and pressed state of the Key is defined here.
 *
 * @author Jonathan R. Miller
 */
public class Key {

    private int key;
    private String name;
    private boolean keyPressed;

    /**
     * Constructor for the {@link Key} class.
     *
     * @param key
     *            - The native key even key code for the key
     * @param name
     *            - The name for the key
     * @param keyPressed
     *            - Whether or not the key is pressed down or not
     */
    public Key(int key, String name, boolean keyPressed) {
        this.key = key;
        this.name = name;
        this.keyPressed = keyPressed;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Key)) {
            return false;
        }

        Key keyToCompare = (Key) object;

        return key == keyToCompare.getKey();
    }

    @Override
    public int hashCode() {
        return hash(this.key);
    }

    /**
     * Hashes on the key's key code and returns the result.
     *
     * @param key
     *            - The key code for the key
     *
     * @return The hashed result
     */
    private int hash(int key) {
        key = ((key >>> 16) ^ key) * 0x45d9f3b;
        key = ((key >>> 16) ^ key) * 0x45d9f3b;
        key = (key >>> 16) ^ key;

        return key;
    }

    /**
     * Gets the key's key code.
     *
     * @return The key code for the key
     */
    public int getKey() {
        return key;
    }

    /**
     * Gets the key's name.
     *
     * @return The name for the key
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the "pressed" state of the key.
     *
     * @return Whether or not the key is pressed
     */
    public boolean isKeyPressed() {
        return keyPressed;
    }

    /**
     * Sets the "pressed" state of the key.
     *
     * @param keyPressed
     *            - The new "pressed" state of the key
     */
    public void setKeyPressed(boolean keyPressed) {
        this.keyPressed = keyPressed;
    }

}
