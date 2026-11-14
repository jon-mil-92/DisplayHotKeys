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
