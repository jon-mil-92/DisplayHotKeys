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

/**
 * Utilizes the GetDisplay JNI library to retrieve current display settings, including supported display modes,
 * connected display IDs, visible display IDs, and display orientations.
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
     * Defines a JNI function to get the current orientation of every display in QueryDisplayConfig path order.
     *
     * @return The current orientation of every display in QueryDisplayConfig path order
     */
    private native int[] queryDisplayOrientations();

    /**
     * Defines a JNI function to enumerate the display IDs for the displays that are currently visible.
     *
     * @return The array of display IDs for the currently visible displays
     */
    private native String[] enumVisibleDisplayIds();

    /**
     * Defines a JNI function to compute the supported DPI scale percentages for the given resolution.
     *
     * @param width
     *            - The horizontal resolution to get the supported DPI scale percentages for
     * @param height
     *            - The vertical resolution to get the supported DPI scale percentages for
     *
     * @return The array of supported DPI scale percentages for the given resolution
     */
    private native int[] getSupportedDpiScalePercentages(int width, int height);

    /**
     * Gets the current supported display modes for the given display.
     *
     * @param displayId
     *            - The ID of the display to get the array of supported display modes for
     *
     * @return The current array of supported display modes for the given display
     */
    public DisplayMode[] getDisplayModes(String displayId) {
        return enumDisplayModes(displayId);
    }

    /**
     * Gets the current orientation of every display in QueryDisplayConfig path order.
     *
     * @return The current orientation of every display in QueryDisplayConfig path order
     */
    public int[] getDisplayOrientations() {
        return queryDisplayOrientations();
    }

    /**
     * Gets the display IDs for the displays that are currently visible.
     *
     * @return The array of display IDs for the currently visible displays
     */
    public String[] getVisibleDisplayIds() {
        return enumVisibleDisplayIds();
    }

    /**
     * Gets the supported DPI scale percentages for the given resolution. The supported set matches the percentages
     * Windows would offer for that resolution.
     *
     * @param width
     *            - The horizontal resolution to get the supported DPI scale percentages for
     * @param height
     *            - The vertical resolution to get the supported DPI scale percentages for
     *
     * @return The array of supported DPI scale percentages for the given resolution
     */
    public Integer[] getDpiScalePercentages(int width, int height) {
        int[] supportedDpiScalePercentages = getSupportedDpiScalePercentages(width, height);
        Integer[] dpiScalePercentages = new Integer[supportedDpiScalePercentages.length];

        for (int i = 0; i < supportedDpiScalePercentages.length; i++) {
            dpiScalePercentages[i] = supportedDpiScalePercentages[i];
        }

        return dpiScalePercentages;
    }

}
