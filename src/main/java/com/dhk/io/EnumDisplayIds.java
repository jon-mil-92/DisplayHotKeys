package com.dhk.io;

/**
 * Utilizes the EnumDisplayIds JNI library to retrieve the current array of display IDs.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class EnumDisplayIds {

    /**
     * Default constructor for the EnumDisplayIds class.
     */
    public EnumDisplayIds() {
    }

    static {
        try {
            System.loadLibrary("EnumDisplayIds");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /*
     * Defines a JNI function to get the current number of connected displays.
     */
    private native int queryNumOfConnectedDisplays();

    /*
     * Defines a JNI function to enumerate the display IDs for the connected displays.
     */
    private native String[] enumDisplayIds();

    /**
     * This method gets the current number connected displays.
     * 
     * @return The current number of connected displays
     */
    public int getNumOfConnectedDisplays() {
        return queryNumOfConnectedDisplays();
    }

    /**
     * Gets the display IDs for the connected displays.
     * 
     * @return The array of display IDs for the connected displays
     */
    public String[] getDisplayIds() {
        return enumDisplayIds();
    }

}