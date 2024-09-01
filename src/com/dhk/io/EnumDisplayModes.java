package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;

/**
 * This class utilizes the EnumDisplayModes JNI library to retreive the current array of supported display modes for a
 * given display.
 * 
 * @author Jonathan Miller
 * @version 1.4.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class EnumDisplayModes {

    /**
     * Default constructor for the EnumDisplayModes class.
     */
    public EnumDisplayModes() {
    }

    // Load the EnumDisplayModes.dll file.
    static {
        try {
            System.loadLibrary("EnumDisplayModes");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    // Define a JNI function to enumerate the supported display modes for the given display.
    private native DisplayMode[] enumDisplayModes(String displayID);

    /**
     * This method gets the current supported display modes for the given display.
     * 
     * @param displayId - The ID of the display to get the array of supported display modes for.
     * @return The current array of supported display modes for the given display.
     */
    public DisplayMode[] getDisplayModes(String displayId) {
        // Only return unique supported display modes, as the EnumDisplaySettings function from Windows.h may not.
        return Arrays.stream(enumDisplayModes(displayId)).distinct().toArray(DisplayMode[]::new);
    }
}
