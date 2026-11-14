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

    /**
     * Gets the resolution half of this display mode.
     *
     * @return The resolution of this display mode
     */
    public Resolution getResolution() {
        return new Resolution(width, height);
    }

    /**
     * Gets the refresh-rate half of this display mode.
     *
     * @return The exact refresh rate of this display mode
     */
    public RefreshRate getRefreshRate() {
        return new RefreshRate(refreshNumerator, refreshDenominator);
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
        return RefreshRate.format(refreshNumerator, refreshDenominator);
    }

}
