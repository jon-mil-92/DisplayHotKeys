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
package com.dhk.utility;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JFrame;

import com.dhk.model.FramePlacement;

/**
 * Provides placement utilities for frames across displays. This class preserves a previous frame's position when a new
 * frame is created, even when the display it was on has had its resolution, DPI scale, or virtual-desktop origin
 * changed (for example, immediately after applying a new display mode).
 *
 * @author Jonathan R. Miller
 */
public class FrameUtil {

    /**
     * Default constructor for the {@link FrameUtil} class.
     */
    public FrameUtil() {
    }

    /**
     * Capture the placement of a frame so it can be reproduced after the view is re-initialized. The frame's top-left
     * is recorded as a fraction of its current display's available space (bounds minus the frame size), along with that
     * display's graphics configuration, so the placement can be reproduced even after the display is resized or
     * rescaled, and so a frame anchored against an edge or corner stays anchored there.
     *
     * @param frame
     *            - The frame to capture the placement of (may be null)
     * @return The captured placement, or null if the frame is null or not displayable
     */
    public static FramePlacement capturePlacement(JFrame frame) {
        if (frame == null || !frame.isDisplayable()) {
            return null;
        }

        try {
            GraphicsConfiguration configuration = frame.getGraphicsConfiguration();

            /*
             * Use the frame's AWT bounds rather than getLocationOnScreen() so the frame position and the display bounds
             * are read from the same coordinate space. getLocationOnScreen() is a native query that, during a DPI
             * transition, can momentarily report a different scale than the cached GraphicsConfiguration; mixing the
             * two corrupts the placement and makes the rebuilt frame jump. getBounds() and the GraphicsConfiguration
             * bounds always move together, so the placement stays correct even mid-transition
             */
            Rectangle frameBounds = frame.getBounds();
            double centerX = frameBounds.x + frameBounds.width / 2.0;
            double centerY = frameBounds.y + frameBounds.height / 2.0;
            Point absoluteCenter = new Point((int) Math.round(centerX), (int) Math.round(centerY));

            if (configuration == null) {
                return new FramePlacement(null, 0.5, 0.5, absoluteCenter);
            }

            /*
             * Anchor the top-left within the available space (display bounds minus the frame size) so edge and corner
             * positioning is preserved when the bounds change size relative to the frame. Recording the center as a
             * fraction of the bounds instead would lose the fixed half-frame edge gap when the bounds grow (as on a
             * high-to-low DPI change), pulling an edge-anchored frame toward the center
             */
            Rectangle bounds = configuration.getBounds();
            int availableWidth = bounds.width - frameBounds.width;
            int availableHeight = bounds.height - frameBounds.height;
            double leftFraction = (availableWidth > 0) ? (double) (frameBounds.x - bounds.x) / availableWidth : 0.5;
            double topFraction = (availableHeight > 0) ? (double) (frameBounds.y - bounds.y) / availableHeight : 0.5;

            // Guard against an off-screen capture producing a fraction outside the placeable range
            leftFraction = Math.max(0.0, Math.min(1.0, leftFraction));
            topFraction = Math.max(0.0, Math.min(1.0, topFraction));

            return new FramePlacement(configuration, leftFraction, topFraction, absoluteCenter);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Compute a top-left placement for a new frame that reproduces the captured placement against the current bounds of
     * the target display.
     *
     * @param placement
     *            - The captured placement to reproduce (may be null to center on the default screen)
     * @param frameSize
     *            - The size of the frame to place
     * @return The top-left Point for the new frame in screen coordinates
     */
    public static Point computeLocation(FramePlacement placement, Dimension frameSize) {
        GraphicsConfiguration configuration = resolveConfiguration(placement);

        return locationForConfiguration(placement, configuration.getBounds(), frameSize);
    }

    /**
     * Re-assert the captured placement on a frame that is already showing. This reproduces the placement against the
     * current bounds of its intended display, correcting cases where the OS positioned the new frame somewhere other
     * than intended.
     *
     * @param frame
     *            - The frame to correct
     * @param placement
     *            - The captured placement to reproduce (may be null to center on the default screen)
     */
    public static void correctLocation(JFrame frame, FramePlacement placement) {
        try {
            if (frame == null || !frame.isDisplayable() || frame.getExtendedState() != JFrame.NORMAL) {
                return;
            }

            /*
             * Recompute against the frame's actual, current geometry now that it is showing. Reading the real size and
             * the frame's own display bounds from a single settled snapshot keeps the placement and the on-screen clamp
             * in the same coordinate space, even after a DPI change where the pre-show packed size differs from the
             * final scaled size. Using the stale pre-show size here was letting the frame land out of bounds
             */
            GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
            Dimension actualFrameSize = frame.getSize();

            Point desiredLocation = (configuration != null)
                    ? locationForConfiguration(placement, configuration.getBounds(), actualFrameSize)
                    : computeLocation(placement, actualFrameSize);

            Point currentLocation = frame.getLocationOnScreen();

            if (!currentLocation.equals(desiredLocation)) {
                frame.setLocation(desiredLocation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolve the graphics configuration that the placement targets. Prefers the display the frame was captured on,
     * then the display containing the captured absolute center, and finally the default screen device.
     *
     * @param placement
     *            - The captured placement to resolve a configuration for (may be null)
     * @return The graphics configuration for the target display
     */
    private static GraphicsConfiguration resolveConfiguration(FramePlacement placement) {
        if (placement != null && placement.getConfiguration() != null) {
            GraphicsConfiguration liveConfiguration = liveConfigurationFor(placement.getConfiguration());

            if (liveConfiguration != null) {
                return liveConfiguration;
            }
        }

        if (placement != null && placement.getAbsoluteCenter() != null) {
            return getTargetGraphicsConfiguration(placement.getAbsoluteCenter());
        }

        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Find the current default configuration for the same device as the captured configuration. After a display mode
     * change, AWT may replace a device's {@link GraphicsConfiguration} instance, leaving the captured reference
     * orphaned and reporting stale bounds. Re-resolving by device id returns the live configuration whose bounds
     * reflect the new display geometry.
     *
     * @param captured
     *            - The configuration captured before the display was reconfigured
     * @return The live configuration for the captured device, or null if the device can no longer be found
     */
    private static GraphicsConfiguration liveConfigurationFor(GraphicsConfiguration captured) {
        GraphicsDevice capturedDevice = captured.getDevice();

        if (capturedDevice == null) {
            return null;
        }

        String capturedId = capturedDevice.getIDstring();

        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            if (device.getIDstring().equals(capturedId)) {
                return device.getDefaultConfiguration();
            }
        }

        return null;
    }

    /**
     * Return the graphics configuration that contains the provided screen-coordinate point, or the default
     * configuration.
     *
     * @param frameLocation
     *            - Location used to select the graphics configuration
     * @return The graphics configuration for the device containing the location, or the default configuration
     */
    private static GraphicsConfiguration getTargetGraphicsConfiguration(Point frameLocation) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        if (frameLocation == null) {
            return graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration();
        }

        for (GraphicsDevice device : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration targetConfiguration = device.getDefaultConfiguration();

            if (targetConfiguration.getBounds().contains(frameLocation)) {
                return targetConfiguration;
            }
        }

        return graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Compute the clamped top-left location for a frame inside the given bounds, reproducing the placement's
     * proportional position within the available space. Falls back to the captured absolute center, then to the center
     * of the bounds, when no proportional position is available.
     *
     * @param placement
     *            - The captured placement (may be null)
     * @param bounds
     *            - The current bounds of the target display
     * @param frameSize
     *            - The size of the frame to place
     * @return The clamped top-left Point in screen coordinates
     */
    private static Point locationForConfiguration(FramePlacement placement, Rectangle bounds, Dimension frameSize) {
        int left;
        int top;

        if (placement != null && placement.getConfiguration() != null) {
            /*
             * Reproduce the top-left within the available space (bounds minus frame size) so edge and corner anchoring
             * is preserved against the current frame size, even when the bounds changed size (as on a DPI change)
             */
            int availableWidth = bounds.width - frameSize.width;
            int availableHeight = bounds.height - frameSize.height;
            left = (int) Math.round(bounds.x + placement.getLeftFraction() * availableWidth);
            top = (int) Math.round(bounds.y + placement.getTopFraction() * availableHeight);
        } else {
            double centerX;
            double centerY;

            if (placement != null && placement.getAbsoluteCenter() != null) {
                centerX = placement.getAbsoluteCenter().x;
                centerY = placement.getAbsoluteCenter().y;
            } else {
                centerX = bounds.x + bounds.width / 2.0;
                centerY = bounds.y + bounds.height / 2.0;
            }

            left = (int) Math.round(centerX - frameSize.width / 2.0);
            top = (int) Math.round(centerY - frameSize.height / 2.0);
        }

        left = Math.max(bounds.x, Math.min(left, bounds.x + bounds.width - frameSize.width));
        top = Math.max(bounds.y, Math.min(top, bounds.y + bounds.height - frameSize.height));

        return new Point(left, top);
    }

}
