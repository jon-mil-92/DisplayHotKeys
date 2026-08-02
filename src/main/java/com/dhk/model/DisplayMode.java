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

import java.util.Objects;

/**
 * An immutable supported display mode: a resolution and exact refresh rate. The refresh rate is held as a rational
 * (numerator / denominator) so a fractional rate such as 119.88 Hz (120000 / 1001) is represented without loss and
 * applied by selecting the existing mode.
 *
 * @author Jonathan R. Miller
 */
public class DisplayMode {

    private final int width;
    private final int height;
    private final int refreshNumerator;
    private final int refreshDenominator;

    /**
     * Constructor for the {@link DisplayMode} class.
     *
     * @param width
     *            - The horizontal resolution in pixels
     * @param height
     *            - The vertical resolution in pixels
     * @param refreshNumerator
     *            - The numerator of the exact refresh rate (Hz = numerator / denominator)
     * @param refreshDenominator
     *            - The denominator of the exact refresh rate (Hz = numerator / denominator)
     */
    public DisplayMode(int width, int height, int refreshNumerator, int refreshDenominator) {
        this.width = width;
        this.height = height;
        this.refreshNumerator = refreshNumerator;
        this.refreshDenominator = refreshDenominator;
    }

    /**
     * Gets the horizontal resolution in pixels.
     *
     * @return The horizontal resolution in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the vertical resolution in pixels.
     *
     * @return The vertical resolution in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the numerator of the exact refresh rate, where Hz = numerator / denominator.
     *
     * @return The numerator of the exact refresh rate
     */
    public int getRefreshNumerator() {
        return refreshNumerator;
    }

    /**
     * Gets the denominator of the exact refresh rate, where Hz = numerator / denominator.
     *
     * @return The denominator of the exact refresh rate
     */
    public int getRefreshDenominator() {
        return refreshDenominator;
    }

    /**
     * Gets the exact refresh rate in hertz.
     *
     * @return The exact refresh rate in hertz
     */
    public double getRefreshRateHz() {
        return refreshDenominator == 0 ? 0.0 : (double) refreshNumerator / refreshDenominator;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof DisplayMode)) {
            return false;
        }

        DisplayMode other = (DisplayMode) object;

        return width == other.width && height == other.height && refreshNumerator == other.refreshNumerator
                && refreshDenominator == other.refreshDenominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, refreshNumerator, refreshDenominator);
    }

    @Override
    public String toString() {
        return width + "x" + height + "@" + formatRefreshRate() + "Hz";
    }

    /**
     * Formats the refresh rate as a whole number when it is exactly integral, otherwise to two decimal places so a
     * fractional NTSC rate reads the way Windows Display Settings shows it (for example 119.88).
     *
     * @return The formatted refresh rate
     */
    private String formatRefreshRate() {
        double refreshRateHz = getRefreshRateHz();

        if (refreshDenominator == 1 || refreshRateHz == Math.rint(refreshRateHz)) {
            return Integer.toString((int) Math.rint(refreshRateHz));
        }

        return String.format("%.2f", refreshRateHz);
    }

}
