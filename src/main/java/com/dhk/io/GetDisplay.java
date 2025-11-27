package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;

/**
 * Utilizes the GetDisplay JNI library to retrieve current display settings.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class GetDisplay {

    /**
     * Default constructor for the GetDisplay class.
     */
    public GetDisplay() {
    }

    static {
        try {
            System.loadLibrary("GetDisplay");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Defines a JNI function to enumerate the supported display modes for the given display.
     * 
     * @param displayId
     *            - The ID of the display to get the array of supported display modes for
     * 
     * @return The current array of supported display modes for the given display
     */
    private native DisplayMode[] enumDisplayModes(String displayId);

    /**
     * Defines a JNI function to get the current number of connected displays.
     * 
     * @return The current number of connected displays
     */
    private native int queryNumOfConnectedDisplays();

    /**
     * Defines a JNI function to enumerate the display IDs for the connected displays.
     * 
     * @return The array of display IDs for the connected displays
     */
    private native String[] enumDisplayIds();

    /**
     * Defines a JNI function to get the current orientation for the given display.
     * 
     * @param displayIndex
     *            - The index of the display to get the orientation for
     * 
     * @return The current orientation for the given display
     */
    private native int queryDisplayOrientation(int displayIndex);

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

    /**
     * Gets the current orientation for the given display.
     * 
     * @param displayIndex
     *            - The index of the display to get the orientation for
     * 
     * @return The current orientation for the given display
     */
    public int getDisplayOrientation(int displayIndex) {
        return queryDisplayOrientation(displayIndex);
    }

}
