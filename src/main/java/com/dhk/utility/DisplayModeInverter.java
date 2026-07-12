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
package com.dhk.utility;

import java.awt.DisplayMode;

/**
 * Inverts a given display mode by swapping the width and height of the resolution.
 *
 * @author Jonathan R. Miller
 */
public class DisplayModeInverter {

    /**
     * Default constructor for the {@link DisplayModeInverter} class.
     */
    public DisplayModeInverter() {
    }

    /**
     * Swap the width and height of the given display mode.
     *
     * @param displayMode
     *            - The display mode to invert
     *
     * @return The inverted display mode
     */
    public static DisplayMode invertDisplayMode(DisplayMode displayMode) {
        int width = displayMode.getWidth();
        int height = displayMode.getHeight();
        int bitDepth = displayMode.getBitDepth();
        int refreshRate = displayMode.getRefreshRate();

        // Swap the width and height for the inverted display mode
        DisplayMode invertedDisplayMode = new DisplayMode(height, width, bitDepth, refreshRate);

        return invertedDisplayMode;
    }

    /**
     * Swap the width and height of the given display modes.
     *
     * @param displayModes
     *            - The display modes to invert
     *
     * @return The inverted display modes
     */
    public static DisplayMode[] invertDisplayModes(DisplayMode[] displayModes) {
        DisplayMode[] invertedDisplayModes = new DisplayMode[displayModes.length];

        for (int i = 0; i < displayModes.length; i++) {
            invertedDisplayModes[i] = invertDisplayMode(displayModes[i]);
        }

        return invertedDisplayModes;
    }

}
