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
package com.dhk.io;

import java.awt.DisplayMode;
import java.util.Arrays;

/**
 * Utilizes the GetDisplay JNI library to retrieve current display settings.
 *
 * @author Jonathan R. Miller
 */
public class GetDisplay {

    /**
     * Default constructor for the {@link GetDisplay} class.
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
