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
