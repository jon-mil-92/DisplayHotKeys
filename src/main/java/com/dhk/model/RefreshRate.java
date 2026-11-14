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
 * An immutable exact refresh rate held as a rational (numerator / denominator) so a fractional rate such as 119.88 Hz
 * (120000 / 1001) is represented without loss. It is the refresh-rate half of a {@link DisplayMode}, letting the view
 * present resolution and refresh rate as separate selections that recombine into a display mode.
 *
 * @author Jonathan R. Miller
 */
public class RefreshRate {

    private final int numerator;
    private final int denominator;

    /**
     * Constructor for the {@link RefreshRate} class.
     *
     * @param numerator
     *            - The numerator of the exact refresh rate (Hz = numerator / denominator)
     * @param denominator
     *            - The denominator of the exact refresh rate (Hz = numerator / denominator)
     */
    public RefreshRate(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Gets the numerator of the exact refresh rate, where Hz = numerator / denominator.
     *
     * @return The numerator of the exact refresh rate
     */
    public int getNumerator() {
        return numerator;
    }

    /**
     * Gets the denominator of the exact refresh rate, where Hz = numerator / denominator.
     *
     * @return The denominator of the exact refresh rate
     */
    public int getDenominator() {
        return denominator;
    }

    /**
     * Gets the exact refresh rate in hertz.
     *
     * @return The exact refresh rate in hertz
     */
    public double getRefreshRateHz() {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    /**
     * Formats a rational refresh rate as a whole number when it is exactly integral, otherwise to two decimal places so
     * a fractional NTSC rate reads the way Windows Display Settings shows it (for example 119.88). The returned value
     * carries no unit; callers append their own.
     *
     * @param numerator
     *            - The numerator of the refresh rate to format
     * @param denominator
     *            - The denominator of the refresh rate to format
     *
     * @return The formatted refresh rate value without a unit
     */
    static String format(int numerator, int denominator) {
        double refreshRateHz = denominator == 0 ? 0.0 : (double) numerator / denominator;

        if (denominator == 1 || refreshRateHz == Math.rint(refreshRateHz)) {
            return Integer.toString((int) Math.rint(refreshRateHz));
        }

        return String.format("%.2f", refreshRateHz);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RefreshRate)) {
            return false;
        }

        RefreshRate other = (RefreshRate) object;

        return numerator == other.numerator && denominator == other.denominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public String toString() {
        return format(numerator, denominator) + " Hz";
    }

}
