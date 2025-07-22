package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;

/**
 * Utilizes the EnumDisplayModes JNI library to retrieve the current array of supported display modes for a given
 * display.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class EnumDisplayModes {

    /**
     * Default constructor for the EnumDisplayModes class.
     */
    public EnumDisplayModes() {
    }

    static {
        try {
            System.loadLibrary("EnumDisplayModes");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /*
     * Defines a JNI function to enumerate the supported display modes for the given display.
     */
    private native DisplayMode[] enumDisplayModes(String displayID);

    /**
     * Gets the current supported display modes for the given display.
     * 
     * @param displayId
     *            - The ID of the display to get the array of supported display modes for
     * 
     * @return The current array of supported display modes for the given display
     */
    public DisplayMode[] getDisplayModes(String displayId) {
        // Only return unique supported display modes, as the EnumDisplaySettings function from Windows.h may not
        return Arrays.stream(enumDisplayModes(displayId)).distinct().toArray(DisplayMode[]::new);
    }

}
