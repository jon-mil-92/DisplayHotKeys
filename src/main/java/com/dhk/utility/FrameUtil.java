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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.dhk.model.FramePlacement;

/**
 * Provides frame placement and refresh utilities across displays. This class preserves a previous frame's position when
 * a new frame is created, even when the display it was on has had its resolution, DPI scale, or virtual-desktop origin
 * changed (for example, immediately after applying a new display mode).
 *
 * @author Jonathan R. Miller
 */
public class FrameUtil {

    /**
     * Client-property key under which a frame's most recently cached graphics configuration is stored.
     */
    private static final String CACHED_DISPLAY_CONFIGURATION_PROPERTY = FrameUtil.class.getName()
            + ".cachedDisplayConfiguration";

    /**
     * Client-property key under which a frame's most recently cached working-area size is stored.
     */
    private static final String CACHED_WORKING_AREA_SIZE_PROPERTY = FrameUtil.class.getName()
            + ".cachedWorkingAreaSize";

    /**
     * Default constructor for the {@link FrameUtil} class.
     */
    public FrameUtil() {
    }

    /**
     * Refreshes a frame's UI, then re-fits it to the working area of the display it currently occupies.
     *
     * @param frame
     *            - The frame to refresh
     */
    public static void refreshFrame(JFrame frame) {
        if (frame == null || !frame.isDisplayable()) {
            return;
        }

        JScrollPane scrollPane = frameScrollPane(frame);
        JPanel mainPanel = scrollContentPanel(scrollPane);

        updateCachedDisplayMetrics(frame);

        SwingUtilities.updateComponentTreeUI(frame);
        mainPanel.revalidate();
        scrollPane.revalidate();
        repackAndFitToScreen(frame, scrollPane);
        frame.validate();
        frame.repaint();
    }

    /**
     * Capture the placement of a frame so it can be reproduced after the view is re-initialized. The frame's top-left
     * is recorded as a fraction of its current display's available space (bounds minus the frame size), along with that
     * display's graphics configuration, so the placement can be reproduced even after the display is resized or
     * rescaled, and so a frame anchored against an edge or corner stays anchored there.
     *
     * @param frame
     *            - The frame to capture the placement of (may be null)
     *
     * @return The captured placement, or null if the frame is null or not displayable
     */
    public static FramePlacement capturePlacement(JFrame frame) {
        if (frame == null || !frame.isDisplayable()) {
            return null;
        }

        try {
            GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
            Rectangle frameBounds = frame.getBounds();
            double centerX = frameBounds.x + frameBounds.width / 2.0;
            double centerY = frameBounds.y + frameBounds.height / 2.0;
            Point absoluteCenter = new Point((int) Math.round(centerX), (int) Math.round(centerY));

            if (configuration == null) {
                return new FramePlacement(null, 0.5, 0.5, absoluteCenter);
            }

            // Anchor the top-left within the available space so edge and corner positioning stay preserved.
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
     *
     * @return The top-left Point for the new frame in screen coordinates
     */
    public static Point computeLocation(FramePlacement placement, Dimension frameSize) {
        GraphicsConfiguration configuration = resolveConfiguration(placement);
        return locationForConfiguration(placement, configuration.getBounds(), frameSize);
    }

    /**
     * Resolves the placement's target display and returns its working area size.
     *
     * @param placement
     *            - The captured placement whose target display to measure (may be null to use the default screen)
     *
     * @return The working area size of the target display
     */
    public static Dimension workingAreaSize(FramePlacement placement) {
        return workingAreaSize(resolveConfiguration(placement));
    }

    /**
     * Returns the configuration's bounds minus the screen insets (task bar, menu bar) so a frame sized to it does not
     * extend behind the task bar; queries the screen insets natively, so hot-path callers should cache the result and
     * only recompute it when the configuration changes.
     *
     * @param configuration
     *            - The configuration to measure (may be null)
     *
     * @return The working area size, or null if no configuration is available
     */
    public static Dimension workingAreaSize(GraphicsConfiguration configuration) {
        if (configuration == null) {
            return null;
        }

        Rectangle bounds = workingAreaBounds(configuration);
        int maxWidth = bounds.width;
        int maxHeight = bounds.height;

        return new Dimension(maxWidth, maxHeight);
    }

    /**
     * Returns the usable working-area bounds for a graphics configuration (display bounds minus native screen insets).
     *
     * @param configuration
     *            - The configuration to measure (may be null)
     *
     * @return The working-area bounds in screen coordinates, or null if no configuration is available
     */
    public static Rectangle workingAreaBounds(GraphicsConfiguration configuration) {
        if (configuration == null) {
            return null;
        }

        Rectangle bounds = new Rectangle(configuration.getBounds());

        try {
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bounds;
    }

    /**
     * Returns the preferred size clamped to the working area, reserving room for whichever scroll bars are needed:
     * because a scroll bar on one axis consumes space on the perpendicular axis, when an axis overflows the
     * perpendicular scroll bar's thickness is added to the other axis before clamping (so capping the width does not
     * force an unnecessary vertical scroll bar, and vice versa); a size that already fits is returned unchanged.
     *
     * @param preferred
     *            - The frame's preferred (packed) size
     * @param scrollPane
     *            - The scroll pane that is the frame's content (may be null to skip scroll-bar reservation)
     * @param workingArea
     *            - The display's working area size (may be null to return the preferred size unchanged)
     *
     * @return The size to give the frame
     */
    public static Dimension fitToWorkingArea(Dimension preferred, JScrollPane scrollPane, Dimension workingArea) {
        if (workingArea == null) {
            return preferred;
        }

        int maxWidth = workingArea.width;
        int maxHeight = workingArea.height;
        int desiredWidth = preferred.width;
        int desiredHeight = preferred.height;

        if (scrollPane != null) {
            int verticalScrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
            int horizontalScrollBarHeight = scrollPane.getHorizontalScrollBar().getPreferredSize().height;

            boolean needsHorizontal = desiredWidth > maxWidth;
            boolean needsVertical = desiredHeight > maxHeight;

            if (needsHorizontal) {
                desiredHeight += horizontalScrollBarHeight;
            }

            if (needsVertical) {
                desiredWidth += verticalScrollBarWidth;
            }

            // Reserving space for one scroll bar can push the perpendicular axis over, requiring the other as well
            if (!needsVertical && desiredHeight > maxHeight) {
                desiredWidth += verticalScrollBarWidth;
            }

            if (!needsHorizontal && desiredWidth > maxWidth) {
                desiredHeight += horizontalScrollBarHeight;
            }
        }

        return new Dimension(Math.min(desiredWidth, maxWidth), Math.min(desiredHeight, maxHeight));
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

            // Use the live frame size and display bounds so placement stays in the correct coordinate space.
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
     * Clamps the preferred top-left location for a frame into the current working area of the provided graphics
     * configuration.
     *
     * @param configuration
     *            - The configuration whose working area should contain the frame
     * @param frameSize
     *            - The frame size to keep within the working area
     * @param preferredLocation
     *            - The desired top-left location before clamping
     *
     * @return The clamped top-left location, or the preferred location unchanged if no configuration is available
     */
    public static Point clampToWorkingArea(GraphicsConfiguration configuration, Dimension frameSize,
            Point preferredLocation) {
        if (configuration == null || frameSize == null || preferredLocation == null) {
            return preferredLocation;
        }

        Rectangle workingArea = workingAreaBounds(configuration);

        if (workingArea == null) {
            return preferredLocation;
        }

        int maxLeft = workingArea.x + Math.max(0, workingArea.width - frameSize.width);
        int maxTop = workingArea.y + Math.max(0, workingArea.height - frameSize.height);
        int clampedLeft = Math.max(workingArea.x, Math.min(preferredLocation.x, maxLeft));
        int clampedTop = Math.max(workingArea.y, Math.min(preferredLocation.y, maxTop));

        return new Point(clampedLeft, clampedTop);
    }

    /**
     * Refreshes the cached graphics configuration and working area when the frame moves to another display, or when
     * those metrics have not been cached yet.
     *
     * @param frame
     *            - The frame whose cached display metrics to refresh
     */
    private static void updateCachedDisplayMetrics(JFrame frame) {
        GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
        GraphicsConfiguration cachedConfiguration = cachedDisplayConfiguration(frame);
        Dimension cachedWorkingArea = cachedWorkingAreaSize(frame);

        if (configuration != cachedConfiguration || cachedWorkingArea == null) {
            frame.getRootPane().putClientProperty(CACHED_DISPLAY_CONFIGURATION_PROPERTY, configuration);
            frame.getRootPane().putClientProperty(CACHED_WORKING_AREA_SIZE_PROPERTY, workingAreaSize(configuration));
        }
    }

    /**
     * Re-packs the frame for the current display, then caps its size to the display's working area and keeps the title
     * bar reachable.
     *
     * @param frame
     *            - The frame to re-pack and fit to the current display
     * @param scrollPane
     *            - The scroll pane used to reserve room for scroll bars when fitting
     */
    private static void repackAndFitToScreen(JFrame frame, JScrollPane scrollPane) {
        Point preferredLocation = frame.getLocation();

        frame.pack();
        resizeToFitScreen(frame, scrollPane);

        Point clampedLocation = clampToWorkingArea(frame.getGraphicsConfiguration(), frame.getSize(),
                preferredLocation);

        if (clampedLocation != null && !clampedLocation.equals(frame.getLocation())) {
            frame.setLocation(clampedLocation);
        }
    }

    /**
     * Sizes the frame to its preferred size capped to the cached working area when needed.
     *
     * @param frame
     *            - The frame to resize
     * @param scrollPane
     *            - The scroll pane used to reserve room for scroll bars when fitting
     */
    private static void resizeToFitScreen(JFrame frame, JScrollPane scrollPane) {
        Dimension target = fitToWorkingArea(frame.getSize(), scrollPane, cachedWorkingAreaSize(frame));

        if (!target.equals(frame.getSize())) {
            frame.setSize(target);
        }
    }

    /**
     * Returns the frame's content pane as a scroll pane.
     *
     * @param frame
     *            - The frame whose content pane to return
     *
     * @return The frame's content pane cast to a scroll pane
     */
    private static JScrollPane frameScrollPane(JFrame frame) {
        return (JScrollPane) frame.getContentPane();
    }

    /**
     * Returns the main panel shown in the frame's content scroll pane.
     *
     * @param scrollPane
     *            - The scroll pane whose viewport view to return
     *
     * @return The main panel shown in the scroll pane's viewport
     */
    private static JPanel scrollContentPanel(JScrollPane scrollPane) {
        Component content = scrollPane.getViewport().getView();

        return (JPanel) content;
    }

    /**
     * Returns the cached graphics configuration stored on the frame.
     *
     * @param frame
     *            - The frame whose cached graphics configuration to return
     *
     * @return The cached graphics configuration, or null if none has been cached
     */
    private static GraphicsConfiguration cachedDisplayConfiguration(JFrame frame) {
        return (GraphicsConfiguration) frame.getRootPane().getClientProperty(CACHED_DISPLAY_CONFIGURATION_PROPERTY);
    }

    /**
     * Returns the cached working-area size stored on the frame.
     *
     * @param frame
     *            - The frame whose cached working-area size to return
     *
     * @return The cached working-area size, or null if none has been cached
     */
    private static Dimension cachedWorkingAreaSize(JFrame frame) {
        return (Dimension) frame.getRootPane().getClientProperty(CACHED_WORKING_AREA_SIZE_PROPERTY);
    }

    /**
     * Resolve the graphics configuration that the placement targets. Prefers the display the frame was captured on,
     * then the display containing the captured absolute center, and finally the default screen device.
     *
     * @param placement
     *            - The captured placement to resolve a configuration for (may be null)
     *
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
     *
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
     *
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
     *
     * @return The clamped top-left Point in screen coordinates
     */
    private static Point locationForConfiguration(FramePlacement placement, Rectangle bounds, Dimension frameSize) {
        int left;
        int top;

        if (placement != null && placement.getConfiguration() != null) {
            // Reproduce the top-left within the available space so edge and corner anchoring stays preserved.
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
