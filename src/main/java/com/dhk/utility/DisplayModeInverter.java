package com.dhk.utility;

import java.awt.DisplayMode;

/**
 * Inverts a given display mode by swapping the width and height of the resolution.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DisplayModeInverter {

    /**
     * Default constructor for the DisplayModeInverter class.
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
