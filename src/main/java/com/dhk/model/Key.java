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
