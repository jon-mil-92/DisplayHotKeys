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
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.JFrame;

/**
 * Provides placement and mapping utilities for frames across displays and DPI transforms. This class preserves the
 * visual center of a previous frame when creating a new frame, mapping across graphics configurations and DPI
 * transforms. It uses affine mapping with a scale-ratio fallback and clamps the resulting top-left to the target
 * configuration bounds.
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
     * Compute a placement that preserves the previous frame's visual center across graphics configurations.
     *
     * @param previousFrame
     *            - Previous frame
     * @param expectedCenterLocation
     *            - Expected location for the center of the frame
     * @param expectedFrameSize
     *            - Expected size of the frame
     * @return Top-left Point for new frame placement in screen coordinates
     */
    public static Point computeLocation(JFrame previousFrame, Point expectedCenterLocation,
            Dimension expectedFrameSize) {
        double centerX;
        double centerY;
        GraphicsConfiguration previousConfiguration = null;

        if (expectedCenterLocation != null) {
            centerX = expectedCenterLocation.x;
            centerY = expectedCenterLocation.y;
        } else if (previousFrame != null && previousFrame.isDisplayable()) {
            try {
                Point previousLocation = previousFrame.getLocationOnScreen();
                Dimension previousSize = previousFrame.getSize();
                centerX = previousLocation.x + previousSize.width / 2.0;
                centerY = previousLocation.y + previousSize.height / 2.0;
                previousConfiguration = previousFrame.getGraphicsConfiguration();
            } catch (IllegalComponentStateException e) {
                e.printStackTrace();
                Rectangle previousBounds = previousFrame.getBounds();
                centerX = previousBounds.x + previousBounds.width / 2.0;
                centerY = previousBounds.y + previousBounds.height / 2.0;
                previousConfiguration = previousFrame.getGraphicsConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
                centerX = 0;
                centerY = 0;
            }
        } else {
            GraphicsConfiguration defaultConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle defaultBounds = defaultConfiguration.getBounds();
            centerX = defaultBounds.x + defaultBounds.width / 2.0;
            centerY = defaultBounds.y + defaultBounds.height / 2.0;
            previousConfiguration = defaultConfiguration;
        }

        GraphicsConfiguration targetConfiguration = (previousConfiguration != null)
                ? previousConfiguration
                : getTargetGraphicsConfiguration(new Point((int) Math.round(centerX), (int) Math.round(centerY)));

        if (targetConfiguration == null) {
            targetConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }

        if (previousConfiguration != null) {
            Rectangle previousBounds = previousConfiguration.getBounds();
            Rectangle targetBounds = targetConfiguration.getBounds();
            boolean boundsDiffer = (previousBounds == null && targetBounds != null)
                    || (previousBounds != null && !previousBounds.equals(targetBounds));

            if (boundsDiffer) {
                try {
                    double previousLocalX = centerX - previousBounds.x;
                    double previousLocalY = centerY - previousBounds.y;

                    AffineTransform previousToDevice = previousConfiguration.getDefaultTransform();
                    Point2D devicePoint = previousToDevice.transform(new Point2D.Double(previousLocalX, previousLocalY),
                            null);

                    AffineTransform targetToDevice = targetConfiguration.getDefaultTransform();
                    AffineTransform deviceToTarget = targetToDevice.createInverse();
                    Point2D targetUserPoint = deviceToTarget.transform(devicePoint, null);

                    centerX = targetUserPoint.getX() + targetBounds.x;
                    centerY = targetUserPoint.getY() + targetBounds.y;
                } catch (NoninvertibleTransformException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Rectangle finalTargetBounds = targetConfiguration.getBounds();
        int left = (int) Math.round(centerX - expectedFrameSize.width / 2.0);
        int top = (int) Math.round(centerY - expectedFrameSize.height / 2.0);

        left = Math.max(finalTargetBounds.x,
                Math.min(left, finalTargetBounds.x + finalTargetBounds.width - expectedFrameSize.width));
        top = Math.max(finalTargetBounds.y,
                Math.min(top, finalTargetBounds.y + finalTargetBounds.height - expectedFrameSize.height));

        return new Point(left, top);
    }

    /**
     * Return the graphics configuration that contains the provided screen-coordinate point, or the default
     * configuration.
     *
     * @param frameLocation
     *            - Location of the frame used to select the graphics configuration
     * @return The graphics configuration for the device containing the frame location, or the default configuration
     */
    public static GraphicsConfiguration getTargetGraphicsConfiguration(Point frameLocation) {
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
     * Corrects the location for a frame so it stays in the screen bounds and preserves visual-center intent when the OS
     * reassigns the window.
     *
     * @param frameToCorrect
     *            - Frame to correct
     * @param intendedFrameCenter
     *            - Intended center of the frame in screen coordinates
     * @param expectedFrameSize
     *            - Expected size of the frame
     * @param expectedConfiguration
     *            - Expected graphics configuration (may be null)
     * @param expectedBounds
     *            - Bounds of the expected graphics configuration (may be null)
     */
    public static void correctLocation(JFrame frameToCorrect, Point intendedFrameCenter, Dimension expectedFrameSize,
            GraphicsConfiguration expectedConfiguration, Rectangle expectedBounds) {
        try {
            if (frameToCorrect != null && frameToCorrect.isDisplayable()
                    && frameToCorrect.getExtendedState() == JFrame.NORMAL) {
                Point currentLocationOnScreen = frameToCorrect.getLocationOnScreen();
                GraphicsConfiguration actualConfiguration = frameToCorrect.getGraphicsConfiguration();
                Rectangle actualBounds = (actualConfiguration != null)
                        ? actualConfiguration.getBounds()
                        : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                                .getDefaultConfiguration().getBounds();

                boolean configurationChanged = (expectedBounds != null && !expectedBounds.equals(actualBounds));

                if (configurationChanged && expectedConfiguration != null && actualConfiguration != null) {
                    mapLocationOnScreen(frameToCorrect, intendedFrameCenter, expectedFrameSize, expectedConfiguration,
                            actualConfiguration, actualBounds);
                } else {
                    clampLocationOnScreen(frameToCorrect, currentLocationOnScreen, actualBounds);
                }
            }
        } catch (IllegalComponentStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Maps the intended frame center from an expected graphics configuration into the actual graphics configuration and
     * set the frame location so the visual center is preserved. This method performs the affine mapping (with fallback)
     * and clamps the resulting top-left to the actual bounds.
     *
     * @param frame
     *            - Frame to position
     * @param intendedFrameCenter
     *            - Intended center of the frame in screen coordinates (relative to the expected configuration)
     * @param expectedFrameSize
     *            - Expected size of the frame
     * @param expectedConfiguration
     *            - Expected graphics configuration
     * @param actualConfiguration
     *            - Graphics configuration currently assigned to the frame
     * @param actualBounds
     *            - Bounds of the actual configuration
     */
    private static void mapLocationOnScreen(JFrame frame, Point intendedFrameCenter, Dimension expectedFrameSize,
            GraphicsConfiguration expectedConfiguration, GraphicsConfiguration actualConfiguration,
            Rectangle actualBounds) {
        Insets frameInsets = frame.getInsets();
        Point2D mappedCenter = mapCenterBetweenConfigs(intendedFrameCenter.x, intendedFrameCenter.y,
                expectedConfiguration, actualConfiguration);
        int mappedLeft = (int) Math.round(mappedCenter.getX() - expectedFrameSize.width / 2.0);
        int mappedTop = (int) Math.round(mappedCenter.getY() - expectedFrameSize.height / 2.0);

        mappedLeft = Math.max(actualBounds.x,
                Math.min(mappedLeft, actualBounds.x + actualBounds.width - expectedFrameSize.width));
        mappedTop = Math.max(actualBounds.y,
                Math.min(mappedTop, actualBounds.y + actualBounds.height - expectedFrameSize.height));

        if (mappedTop + frameInsets.top < actualBounds.y + 1) {
            mappedTop = actualBounds.y + 1 - frameInsets.top;
        }

        frame.setLocation(mappedLeft, mappedTop);
    }

    /**
     * Clamps the current frame location so the frame remains fully inside the provided bounds. This corrects cases
     * where the OS assigned the frame to the same graphics configuration but the top-left would be off-screen.
     *
     * @param frame
     *            - Frame to clamp
     * @param currentLocationOnScreen
     *            - Current top-left location of the frame in screen coordinates
     * @param actualBounds
     *            - Bounds to clamp against
     */
    private static void clampLocationOnScreen(JFrame frame, Point currentLocationOnScreen, Rectangle actualBounds) {
        boolean isOffTop = currentLocationOnScreen.y < actualBounds.y + 1;
        boolean isOffLeft = currentLocationOnScreen.x < actualBounds.x;
        boolean isOffRight = currentLocationOnScreen.x + frame.getWidth() > actualBounds.x + actualBounds.width;
        boolean isOffBottom = currentLocationOnScreen.y + frame.getHeight() > actualBounds.y + actualBounds.height;

        if (isOffTop || isOffLeft || isOffRight || isOffBottom) {
            Insets frameInsets = frame.getInsets();
            int clampedLeft = Math.max(actualBounds.x,
                    Math.min(currentLocationOnScreen.x, actualBounds.x + actualBounds.width - frame.getWidth()));
            int clampedTop = Math.max(actualBounds.y,
                    Math.min(currentLocationOnScreen.y, actualBounds.y + actualBounds.height - frame.getHeight()));

            if (clampedTop + frameInsets.top < actualBounds.y + 1) {
                clampedTop = actualBounds.y + 1 - frameInsets.top;
            }

            frame.setLocation(clampedLeft, clampedTop);
        }
    }

    /**
     * Map a screen-space center from one graphics configuration to another using transforms. Falls back to scale-based
     * mapping if affine inversion fails.
     *
     * @param centerX
     *            - Screen X in source configuration
     * @param centerY
     *            - Screen Y in source configuration
     * @param sourceConfiguration
     *            - Source graphics configuration
     * @param targetConfiguration
     *            - Target graphics configuration
     * @return Mapped center as Point2D.Double
     */
    public static Point2D mapCenterBetweenConfigs(double centerX, double centerY,
            GraphicsConfiguration sourceConfiguration, GraphicsConfiguration targetConfiguration) {
        try {
            Rectangle sourceBounds = sourceConfiguration.getBounds();
            Rectangle targetBounds = targetConfiguration.getBounds();

            double sourceLocalX = centerX - sourceBounds.x;
            double sourceLocalY = centerY - sourceBounds.y;

            AffineTransform sourceToDevice = sourceConfiguration.getDefaultTransform();
            Point2D devicePoint = sourceToDevice.transform(new Point2D.Double(sourceLocalX, sourceLocalY), null);

            AffineTransform targetToDevice = targetConfiguration.getDefaultTransform();
            AffineTransform deviceToTarget = targetToDevice.createInverse();
            Point2D targetUserPoint = deviceToTarget.transform(devicePoint, null);

            double mappedX = targetUserPoint.getX() + targetBounds.x;
            double mappedY = targetUserPoint.getY() + targetBounds.y;

            return new Point2D.Double(mappedX, mappedY);
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();

            try {
                AffineTransform sourceTransform = sourceConfiguration.getDefaultTransform();
                AffineTransform targetTransform = targetConfiguration.getDefaultTransform();
                double scaleXRatio = sourceTransform.getScaleX() / Math.max(1e-6, targetTransform.getScaleX());
                double scaleYRatio = sourceTransform.getScaleY() / Math.max(1e-6, targetTransform.getScaleY());

                Rectangle sourceBounds = sourceConfiguration.getBounds();
                Rectangle targetBounds = targetConfiguration.getBounds();

                double sourceLocalX = centerX - sourceBounds.x;
                double sourceLocalY = centerY - sourceBounds.y;

                double targetLocalX = sourceLocalX * scaleXRatio;
                double targetLocalY = sourceLocalY * scaleYRatio;

                double mappedX = targetLocalX + targetBounds.x;
                double mappedY = targetLocalY + targetBounds.y;

                return new Point2D.Double(mappedX, mappedY);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new Point2D.Double(centerX, centerY);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Point2D.Double(centerX, centerY);
        }
    }

}
