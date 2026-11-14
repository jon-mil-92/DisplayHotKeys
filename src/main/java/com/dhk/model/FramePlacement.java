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

import java.awt.GraphicsConfiguration;
import java.awt.Point;

/**
 * Captured placement intent for a frame.
 *
 * @author Jonathan R. Miller
 */
public final class FramePlacement {

    private final GraphicsConfiguration configuration;
    private final double leftFraction;
    private final double topFraction;
    private final Point absoluteCenter;

    /**
     * Constructor for the {@link FramePlacement} class.
     *
     * @param configuration
     *            - The graphics configuration of the display the frame was on (may be null)
     * @param leftFraction
     *            - The frame's left edge as a fraction of the available horizontal space (0 = flush left, 1 = flush
     *            right, 0.5 = centered)
     * @param topFraction
     *            - The frame's top edge as a fraction of the available vertical space (0 = flush top, 1 = flush bottom,
     *            0.5 = centered)
     * @param absoluteCenter
     *            - The absolute screen center of the frame, used as a fallback (may be null)
     */
    public FramePlacement(GraphicsConfiguration configuration, double leftFraction, double topFraction,
            Point absoluteCenter) {
        this.configuration = configuration;
        this.leftFraction = leftFraction;
        this.topFraction = topFraction;
        this.absoluteCenter = absoluteCenter;
    }

    /**
     * Gets the graphics configuration of the display the frame was on.
     *
     * @return The graphics configuration of the display the frame was on (may be null)
     */
    public GraphicsConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Gets the frame's left edge as a fraction of the available horizontal space.
     *
     * @return The frame's left edge as a fraction of the available horizontal space
     */
    public double getLeftFraction() {
        return leftFraction;
    }

    /**
     * Gets the frame's top edge as a fraction of the available vertical space.
     *
     * @return The frame's top edge as a fraction of the available vertical space
     */
    public double getTopFraction() {
        return topFraction;
    }

    /**
     * Gets the absolute screen center of the frame, used as a fallback.
     *
     * @return The absolute screen center of the frame (may be null)
     */
    public Point getAbsoluteCenter() {
        return absoluteCenter;
    }

}
