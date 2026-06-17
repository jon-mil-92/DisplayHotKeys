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

public class FrameUtil {

    /**
     * Default constructor for the {@link FrameUtil} class.
     */
    public FrameUtil() {
    }

    /**
     * Compute a placement that preserves the previous frame's visual center across GraphicsConfigurations.
     *
     * @param previousFrame
     *            - Previous frame (may be null)
     * @param fallbackHint
     *            - Fallback screen-coordinate hint (may be null)
     * @param packedSize
     *            - Packed size of the new frame (non-null)
     * @return Top-left Point for new frame placement (screen coordinates)
     */
    public static Point computePlacementWithTransforms(JFrame previousFrame, Point fallbackHint, Dimension packedSize) {
        double centerX;
        double centerY;
        GraphicsConfiguration previousConfiguration = null;

        if (previousFrame != null && previousFrame.isDisplayable()) {
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
                centerX = (fallbackHint != null) ? fallbackHint.x : 0;
                centerY = (fallbackHint != null) ? fallbackHint.y : 0;
                previousConfiguration = null;
            }
        } else if (fallbackHint != null) {
            centerX = fallbackHint.x;
            centerY = fallbackHint.y;
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
        int left = (int) Math.round(centerX - packedSize.width / 2.0);
        int top = (int) Math.round(centerY - packedSize.height / 2.0);

        left = Math.max(finalTargetBounds.x,
                Math.min(left, finalTargetBounds.x + finalTargetBounds.width - packedSize.width));
        top = Math.max(finalTargetBounds.y,
                Math.min(top, finalTargetBounds.y + finalTargetBounds.height - packedSize.height));

        return new Point(left, top);
    }

    /**
     * Map a screen-space center from one GraphicsConfiguration to another using transforms. Falls back to scale-based
     * mapping if affine inversion fails.
     *
     * @param centerX
     *            - Screen X in source configuration
     * @param centerY
     *            - Screen Y in source configuration
     * @param sourceConfiguration
     *            - Source GraphicsConfiguration
     * @param targetConfiguration
     *            - Target GraphicsConfiguration
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

    /**
     * Corrects the location for a frame so it stays in the screen bounds and preserves visual-center intent when the OS
     * reassigns the window.
     *
     * @param frame
     *            - The frame to adjust
     * @param intendedCenter
     *            - The intended visual center in screen coordinates
     * @param packedSize
     *            - The packed size of the frame
     * @param expectedConfiguration
     *            - The GraphicsConfiguration used to compute the intended center (may be null)
     * @param expectedBounds
     *            The bounds of expectedCfg (may be null)
     */
    public static void correctPlacement(JFrame frame, Point intendedCenter, Dimension packedSize,
            GraphicsConfiguration expectedConfiguration, Rectangle expectedBounds) {
        try {
            if (frame != null && frame.isDisplayable()) {
                Point currentLocationOnScreen = frame.getLocationOnScreen();
                Insets frameInsets = frame.getInsets();
                GraphicsConfiguration actualConfiguration = frame.getGraphicsConfiguration();
                Rectangle actualBounds = (actualConfiguration != null)
                        ? actualConfiguration.getBounds()
                        : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                                .getDefaultConfiguration().getBounds();

                boolean isOffTop = currentLocationOnScreen.y < actualBounds.y + 1;
                boolean isOffLeft = currentLocationOnScreen.x < actualBounds.x;
                boolean isOffRight = currentLocationOnScreen.x + frame.getWidth() > actualBounds.x + actualBounds.width;
                boolean isOffBottom = currentLocationOnScreen.y + frame.getHeight() > actualBounds.y
                        + actualBounds.height;

                boolean configurationChanged = (expectedBounds != null && !expectedBounds.equals(actualBounds));

                if (configurationChanged && expectedConfiguration != null && actualConfiguration != null) {
                    Point2D mappedCenter = mapCenterBetweenConfigs(intendedCenter.x, intendedCenter.y,
                            expectedConfiguration, actualConfiguration);
                    int computedLeft = (int) Math.round(mappedCenter.getX() - packedSize.width / 2.0);
                    int computedTop = (int) Math.round(mappedCenter.getY() - packedSize.height / 2.0);

                    computedLeft = Math.max(actualBounds.x,
                            Math.min(computedLeft, actualBounds.x + actualBounds.width - packedSize.width));
                    computedTop = Math.max(actualBounds.y,
                            Math.min(computedTop, actualBounds.y + actualBounds.height - packedSize.height));

                    if (computedTop + frameInsets.top < actualBounds.y + 1) {
                        computedTop = actualBounds.y + 1 - frameInsets.top;
                    }

                    frame.setLocation(computedLeft, computedTop);

                    return;
                }

                if (isOffTop || isOffLeft || isOffRight || isOffBottom) {
                    int clampedLeft = Math.max(actualBounds.x, Math.min(currentLocationOnScreen.x,
                            actualBounds.x + actualBounds.width - frame.getWidth()));
                    int clampedTop = Math.max(actualBounds.y, Math.min(currentLocationOnScreen.y,
                            actualBounds.y + actualBounds.height - frame.getHeight()));

                    if (clampedTop + frameInsets.top < actualBounds.y + 1) {
                        clampedTop = actualBounds.y + 1 - frameInsets.top;
                    }

                    frame.setLocation(clampedLeft, clampedTop);
                }
            }
        } catch (IllegalComponentStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the GraphicsConfiguration that contains the provided screen-coordinate point, or the default
     * configuration.
     *
     * @param preferredLocation
     *            - A point in screen coordinates used to select the GraphicsConfiguration; may be null
     * @return The GraphicsConfiguration for the device containing preferredLocation, or the default configuration
     */
    public static GraphicsConfiguration getTargetGraphicsConfiguration(Point preferredLocation) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        if (preferredLocation == null) {
            return graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration();
        }

        for (GraphicsDevice device : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration targetConfiguration = device.getDefaultConfiguration();

            if (targetConfiguration.getBounds().contains(preferredLocation)) {
                return targetConfiguration;
            }
        }

        return graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration();
    }

}
