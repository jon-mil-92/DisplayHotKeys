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
package com.dhk.model;

/**
 * The immutable settings currently in effect on a connected display: the same display mode, DPI scale percentage,
 * scaling mode, and orientation mode a slot holds, plus where the display sits on the desktop.
 *
 * @author Jonathan R. Miller
 */
public class ActiveDisplaySettings {

    private final DisplayMode displayMode;
    private final int dpiScalePercentage;
    private final int scalingMode;
    private final int orientationMode;
    private final int desktopX;
    private final int desktopY;

    /**
     * Constructor for the {@link ActiveDisplaySettings} class.
     *
     * @param displayMode
     *            - The display mode in effect, with the resolution in the display's current orientation
     * @param dpiScalePercentage
     *            - The DPI scale percentage in effect, or 0 when it could not be read
     * @param scalingMode
     *            - The scaling mode in effect (0 = preserved, 1 = stretched, 2 = centered)
     * @param orientationMode
     *            - The orientation mode in effect (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted
     *            portrait)
     * @param desktopX
     *            - The desktop x-coordinate of the display's top-left corner, in physical pixels
     * @param desktopY
     *            - The desktop y-coordinate of the display's top-left corner, in physical pixels
     */
    public ActiveDisplaySettings(DisplayMode displayMode, int dpiScalePercentage, int scalingMode, int orientationMode,
            int desktopX, int desktopY) {
        this.displayMode = displayMode;
        this.dpiScalePercentage = dpiScalePercentage;
        this.scalingMode = scalingMode;
        this.orientationMode = orientationMode;
        this.desktopX = desktopX;
        this.desktopY = desktopY;
    }

    /**
     * Gets the display mode in effect, with the resolution in the display's current orientation.
     *
     * @return The display mode in effect
     */
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Gets the DPI scale percentage in effect.
     *
     * @return The DPI scale percentage in effect, or 0 when it could not be read
     */
    public int getDpiScalePercentage() {
        return dpiScalePercentage;
    }

    /**
     * Gets the scaling mode in effect.
     *
     * @return The scaling mode in effect (0 = preserved, 1 = stretched, 2 = centered)
     */
    public int getScalingMode() {
        return scalingMode;
    }

    /**
     * Gets the orientation mode in effect.
     *
     * @return The orientation mode in effect (0 = landscape, 1 = portrait, 2 = iLandscape, 3 = iPortrait)
     */
    public int getOrientationMode() {
        return orientationMode;
    }

    /**
     * Gets the desktop x-coordinate of the display's top-left corner.
     *
     * @return The desktop x-coordinate of the display's top-left corner, in physical pixels
     */
    public int getDesktopX() {
        return desktopX;
    }

    /**
     * Gets the desktop y-coordinate of the display's top-left corner.
     *
     * @return The desktop y-coordinate of the display's top-left corner, in physical pixels
     */
    public int getDesktopY() {
        return desktopY;
    }

}
