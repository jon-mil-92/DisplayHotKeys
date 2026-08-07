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
 * An immutable pixel resolution (width by height). It is the resolution half of a {@link DisplayMode}, letting the view
 * present resolution and refresh rate as separate selections that recombine into a display mode.
 *
 * @author Jonathan R. Miller
 */
public class Resolution {

    private final int width;
    private final int height;

    /**
     * Constructor for the {@link Resolution} class.
     *
     * @param width
     *            - The horizontal resolution in pixels
     * @param height
     *            - The vertical resolution in pixels
     */
    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Resolution)) {
            return false;
        }

        Resolution other = (Resolution) object;

        return width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return width + " x " + height;
    }

}
