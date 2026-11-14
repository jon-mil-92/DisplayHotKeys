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
#pragma once

/**
 * The settings currently in effect on a visible display, read from its active path. A display without a visible active
 * path keeps every field at zero.
 */
struct ActiveDisplaySettings {
    /**
     * Horizontal resolution in pixels, swapped with the height for a display rotated by 90 or 270 degrees.
     */
    int width;

    /**
     * Vertical resolution in pixels, swapped with the width for a display rotated by 90 or 270 degrees.
     */
    int height;

    /**
     * Numerator of the exact refresh rate as a rational, where Hz = refreshNumerator / refreshDenominator.
     */
    int refreshNumerator;

    /**
     * Denominator of the exact refresh rate as a rational, where Hz = refreshNumerator / refreshDenominator.
     */
    int refreshDenominator;

    /**
     * DPI scale percentage in effect, or 0 when it could not be read.
     */
    int dpiScalePercentage;

    /**
     * Scaling mode that was set (0 = aspect ratio, 1 = stretched, 2 = centered).
     */
    int scalingMode;

    /**
     * Orientation (0 = landscape, 1 = portrait, 2 = inverted landscape, 3 = inverted portrait).
     */
    int orientation;

    /**
     * Desktop x-coordinate of the display's top-left corner, in physical pixels.
     */
    int desktopX;

    /**
     * Desktop y-coordinate of the display's top-left corner, in physical pixels.
     */
    int desktopY;
};
