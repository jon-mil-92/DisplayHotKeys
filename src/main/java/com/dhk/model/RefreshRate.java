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
