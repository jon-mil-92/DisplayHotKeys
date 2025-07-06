package com.dhk.io;

/**
 * This class utilizes the EnumDisplayIds JNI library to retreive the current array of display IDs.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class EnumDisplayIds {

    // Load the EnumDisplayIds.dll file.
    static {
        try {
            System.loadLibrary("EnumDisplayIds");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    // Define a JNI function to get the current number of connected displays.
    private native int queryNumOfConnectedDisplays();

    // Define a JNI function to enumerate the display IDs for the connected displays.
    private native String[] enumDisplayIds();

    /**
     * This method gets the current number connected displays.
     * 
     * @return The current number of connected displays.
     */
    public int getNumOfConnectedDisplays() {
        return queryNumOfConnectedDisplays();
    }

    /**
     * This method gets the display IDs for the connected displays.
     * 
     * @return The array of display IDs for the connected displays.
     */
    public String[] getDisplayIds() {
        return enumDisplayIds();
    }
}