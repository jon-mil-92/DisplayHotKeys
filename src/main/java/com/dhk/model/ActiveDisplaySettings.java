/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
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
