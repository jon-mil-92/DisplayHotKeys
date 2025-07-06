package com.dhk.models;

/**
 * This class represents the model for a Key. The key code, name, and pressed state of the Key is defined here.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class Key {
    private int key;
    private String name;
    private boolean keyPressed;

    /**
     * Constructor for the Key class.
     * 
     * @param key        - The native key even key code for the key.
     * @param name       - The name for the key.
     * @param keyPressed - Whether or not the key is pressed down or not.
     */
    public Key(int key, String name, boolean keyPressed) {
        // Initialize fields.
        this.key = key;
        this.name = name;
        this.keyPressed = false;
    }

    @Override
    public boolean equals(Object object) {
        // If the object is compared with itself, return true.
        if (object == this) {
            return true;
        }

        // If the object is not an instance of Key, return false.
        if (!(object instanceof Key)) {
            return false;
        }

        // Typecast the object to Key so that the data members can be compared.
        Key keyToCompare = (Key) object;

        // Compare the data members and return accordingly
        return key == keyToCompare.getKey();
    }

    @Override
    public int hashCode() {
        return hash(this.key);
    }

    /**
     * This function hashes on the key's key code and returns the result.
     * 
     * @param key - The key code for the key.
     * @return The hashed result.
     */
    private int hash(int key) {
        key = ((key >>> 16) ^ key) * 0x45d9f3b;
        key = ((key >>> 16) ^ key) * 0x45d9f3b;
        key = (key >>> 16) ^ key;
        return key;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the key's key code.
     * 
     * @return The key code for the key.
     */
    public int getKey() {
        return key;
    }

    /**
     * Getter for the key's name.
     * 
     * @return The name for the key.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the "pressed" state of the key.
     * 
     * @return Whether or not the key is pressed.
     */
    public boolean isKeyPressed() {
        return keyPressed;
    }

    /**
     * Setter for the "pressed" state of the key.
     * 
     * @param keyPressed - The new "pressed" state of the key.
     */
    public void setKeyPressed(boolean keyPressed) {
        this.keyPressed = keyPressed;
    }
}
