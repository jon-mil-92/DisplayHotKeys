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
