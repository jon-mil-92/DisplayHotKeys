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
package com.dhk.utility;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import com.dhk.model.FramePlacement;

/**
 * Provides placement, fitting, and refresh utilities for the main frame and the dialogs across displays. A new frame
 * keeps the previous frame's position even after its display changes resolution, DPI scale, or virtual-desktop origin,
 * and every frame or dialog is capped to its display's working area with scroll bars only where its content overflows.
 *
 * @author Jonathan R. Miller
 */
public class WindowUtil {

    /**
     * Whether a native surface reclaim collection is running, so back to back refreshes never stack another one.
     */
    private static volatile boolean nativeReclaimInProgress;

    /**
     * Client property key that caches a frame's most recent graphics configuration.
     */
    private static final String CACHED_DISPLAY_CONFIGURATION_PROPERTY = WindowUtil.class.getName()
            + ".cachedDisplayConfiguration";

    /**
     * Client property key that caches a frame's most recent working area size.
     */
    private static final String CACHED_WORKING_AREA_SIZE_PROPERTY = WindowUtil.class.getName()
            + ".cachedWorkingAreaSize";

    /**
     * Delay before refreshing the view, which gives AWT time to update its desktop metrics so the refresh never reads a
     * transient working area.
     */
    public static final int REFRESH_DELAY_MS = 400;

    /**
     * Pixels the scrolling content moves per mouse-wheel notch or scroll-bar arrow click, for frames and dialogs alike.
     */
    private static final int SCROLL_UNIT_INCREMENT = 16;

    /**
     * Default constructor for the {@link WindowUtil} class.
     */
    public WindowUtil() {
    }

    /**
     * Grows the window by any pixels its client area falls short of the content, so the as-needed scroll bars do not
     * appear when the content actually fits on screen.
     *
     * @param window
     *            - The frame or dialog to correct (may be null)
     * @param scrollPane
     *            - The window's content scroll pane whose content to fit (may be null)
     * @param workingArea
     *            - The display's working area size, reused from the fit so no second native insets query is made on the
     *            re-init path (may be null to skip the cap)
     */
    public static void settleScrollBars(Window window, JScrollPane scrollPane, Dimension workingArea) {
        if (window == null || scrollPane == null || !window.isDisplayable()) {
            return;
        }

        Component view = scrollPane.getViewport().getView();

        if (view == null) {
            return;
        }

        // The scroll pane is the content pane, so its gap below the content's preferred size is the client shortfall
        Dimension viewPreferred = view.getPreferredSize();
        Dimension paneSize = scrollPane.getSize();
        int shortfallWidth = Math.max(0, viewPreferred.width - paneSize.width);
        int shortfallHeight = Math.max(0, viewPreferred.height - paneSize.height);

        if (shortfallWidth == 0 && shortfallHeight == 0) {
            return;
        }

        // Cap growth to the working area so a small display keeps its scroll bars instead of growing off screen
        int newWidth = window.getWidth() + shortfallWidth;
        int newHeight = window.getHeight() + shortfallHeight;

        if (workingArea != null) {
            newWidth = Math.min(newWidth, workingArea.width);
            newHeight = Math.min(newHeight, workingArea.height);
        }

        if (newWidth != window.getWidth() || newHeight != window.getHeight()) {
            window.setSize(newWidth, newHeight);
            window.validate();
        }
    }

    /**
     * Refreshes a frame's or dialog's UI, then re-fits it to the working area of the display it currently occupies.
     * Absorbs any pack shortfall afterward so every refresh path settles the as-needed scroll bars.
     *
     * @param window
     *            - The frame or dialog to refresh (may be null)
     */
    public static void refreshWindow(Window window) {
        if (!(window instanceof RootPaneContainer) || !window.isDisplayable()) {
            return;
        }

        JScrollPane scrollPane = contentScrollPane(window);

        updateCachedDisplayMetrics(window);

        SwingUtilities.updateComponentTreeUI(window);
        revalidateContent(window, scrollPane);
        refitWindow(window);

        window.repaint();
        scheduleNativeSurfaceReclaim();
    }

    /**
     * Re-packs a showing frame or dialog after its content changes and re-fits it to the working area of the display it
     * occupies, keeping its location unless that would leave part of it off screen. Unlike a refresh, the UI is kept.
     *
     * @param window
     *            - The frame or dialog to re-fit (may be null)
     */
    public static void refitWindow(Window window) {
        if (!(window instanceof RootPaneContainer) || !window.isDisplayable()) {
            return;
        }

        JScrollPane scrollPane = contentScrollPane(window);

        // The display is measured again only after a move to another one, so repeated re-fits make no native query
        updateCachedDisplayMetrics(window);

        repackAndFitToScreen(window, scrollPane);
        window.validate();

        // The window is already realized here, so its client area is laid out and the shortfall can settle in place
        settleScrollBars(window, scrollPane, cachedWorkingAreaSize(window));
    }

    /**
     * Wraps the content in a borderless scroll pane that becomes the window's content pane, so every component stays
     * reachable when the window is capped to a small display. The as-needed scroll bars stay hidden wherever it fits.
     *
     * @param window
     *            - The frame or dialog whose content pane to set
     * @param content
     *            - The panel holding the window's content
     * @param onEmptySpacePress
     *            - The action to run when the mouse is pressed on empty space in the window
     *
     * @return The scroll pane set as the window's content pane
     */
    public static JScrollPane setScrollingContent(Window window, JPanel content, Runnable onEmptySpacePress) {
        JScrollPane scrollPane = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        ((RootPaneContainer) window).setContentPane(scrollPane);

        MouseAdapter emptySpacePressListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                onEmptySpacePress.run();
            }
        };

        // The scroll pane consumes a press before it reaches the window, so the panel and viewport listen as well
        window.addMouseListener(emptySpacePressListener);
        content.addMouseListener(emptySpacePressListener);
        scrollPane.getViewport().addMouseListener(emptySpacePressListener);

        return scrollPane;
    }

    /**
     * Determines whether a window has been fitted to a working area by this class, which a dialog is once it is shown
     * and the main frame is once it has been refreshed.
     *
     * @param window
     *            - The window to check (may be null)
     *
     * @return True if the window holds cached display metrics from a fit, otherwise false
     */
    public static boolean isFittedToWorkingArea(Window window) {
        return window instanceof RootPaneContainer && cachedWorkingAreaSize(window) != null;
    }

    /**
     * Schedules a background collection after an in-place refresh to reclaim the native back-buffer surfaces AWT
     * orphans while re-rendering across displays.
     */
    private static void scheduleNativeSurfaceReclaim() {
        if (nativeReclaimInProgress) {
            return;
        }

        nativeReclaimInProgress = true;

        // Collect off the EDT so a refresh never stalls on the GC
        Thread reclaimThread = new Thread(() -> {
            try {
                System.gc();
            } finally {
                nativeReclaimInProgress = false;
            }
        }, "DisplayHotKeys-NativeSurfaceReclaim");

        reclaimThread.setDaemon(true);
        reclaimThread.start();
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
     * Caps a window's current size to the working area, reserving room for any scroll bars its content needs. A size
     * that already fits is left unchanged.
     *
     * @param window
     *            - The frame or dialog to cap
     * @param scrollPane
     *            - The window's content scroll pane, whose scroll bars to reserve room for (may be null to skip it)
     * @param workingArea
     *            - The display's working area size (may be null to leave the size unchanged)
     */
    public static void capToWorkingArea(Window window, JScrollPane scrollPane, Dimension workingArea) {
        Dimension fittedSize = fitToWorkingArea(window.getSize(), scrollPane, workingArea);

        if (!fittedSize.equals(window.getSize())) {
            window.setSize(fittedSize);
        }
    }

    /**
     * Caps a packed dialog to the working area of its display the same way the main frame is fitted, then centers it on
     * its owner while keeping it fully on screen. Its scroll bars settle once it is shown, as the main frame's do.
     *
     * @param dialog
     *            - The packed dialog to fit and place
     */
    public static void fitDialogToWorkingArea(JDialog dialog) {
        JScrollPane scrollPane = contentScrollPane(dialog);
        GraphicsConfiguration configuration = dialog.getGraphicsConfiguration();
        Rectangle workingArea = workingAreaBounds(configuration);

        if (workingArea == null) {
            dialog.setLocationRelativeTo(dialog.getOwner());
            return;
        }

        // Cache the measured display, so later re-fits reuse it until the dialog moves to another display
        Dimension workingAreaSize = workingArea.getSize();
        cacheDisplayMetrics(dialog, configuration, workingAreaSize);
        capToWorkingArea(dialog, scrollPane, workingAreaSize);

        // Centering on an owner near a display edge can push the dialog past it, so pull it back inside
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setLocation(clampToWorkingArea(workingArea, dialog.getSize(), dialog.getLocation()));

        // Queued before a modal show blocks, so the scroll bars settle once the dialog is on screen
        SwingUtilities.invokeLater(() -> settleScrollBars(dialog, scrollPane, workingAreaSize));
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

        return clampToWorkingArea(workingAreaBounds(configuration), frameSize, preferredLocation);
    }

    /**
     * Clamps the preferred top-left location for a window into already measured working-area bounds, so a caller that
     * has the bounds avoids another native screen insets query.
     *
     * @param workingArea
     *            - The working-area bounds that should contain the window (may be null)
     * @param windowSize
     *            - The window size to keep within the working area
     * @param preferredLocation
     *            - The desired top-left location before clamping
     *
     * @return The clamped top-left location, or the preferred location unchanged if no working area is available
     */
    public static Point clampToWorkingArea(Rectangle workingArea, Dimension windowSize, Point preferredLocation) {
        if (workingArea == null || windowSize == null || preferredLocation == null) {
            return preferredLocation;
        }

        int maxLeft = workingArea.x + Math.max(0, workingArea.width - windowSize.width);
        int maxTop = workingArea.y + Math.max(0, workingArea.height - windowSize.height);
        int clampedLeft = Math.max(workingArea.x, Math.min(preferredLocation.x, maxLeft));
        int clampedTop = Math.max(workingArea.y, Math.min(preferredLocation.y, maxTop));

        return new Point(clampedLeft, clampedTop);
    }

    /**
     * Refreshes the cached graphics configuration and working area when the window moves to another display, or when
     * those metrics have not been cached yet.
     *
     * @param window
     *            - The frame or dialog whose cached display metrics to refresh
     */
    private static void updateCachedDisplayMetrics(Window window) {
        GraphicsConfiguration configuration = window.getGraphicsConfiguration();
        GraphicsConfiguration cachedConfiguration = cachedDisplayConfiguration(window);
        Dimension cachedWorkingArea = cachedWorkingAreaSize(window);

        if (configuration != cachedConfiguration || cachedWorkingArea == null) {
            cacheDisplayMetrics(window, configuration, workingAreaSize(configuration));
        }
    }

    /**
     * Stores a window's display configuration and working area size on its root pane for later re-fits.
     *
     * @param window
     *            - The frame or dialog to cache the display metrics on
     * @param configuration
     *            - The graphics configuration of the display the window occupies
     * @param workingArea
     *            - The working area size of that display
     */
    private static void cacheDisplayMetrics(Window window, GraphicsConfiguration configuration, Dimension workingArea) {
        rootPane(window).putClientProperty(CACHED_DISPLAY_CONFIGURATION_PROPERTY, configuration);
        rootPane(window).putClientProperty(CACHED_WORKING_AREA_SIZE_PROPERTY, workingArea);
    }

    /**
     * Re-packs the window for the current display, then caps its size to the display's working area and keeps the title
     * bar reachable.
     *
     * @param window
     *            - The frame or dialog to re-pack and fit to the current display
     * @param scrollPane
     *            - The scroll pane used to reserve room for scroll bars when fitting (may be null)
     */
    private static void repackAndFitToScreen(Window window, JScrollPane scrollPane) {
        Point preferredLocation = window.getLocation();

        window.pack();
        capToWorkingArea(window, scrollPane, cachedWorkingAreaSize(window));

        Point clampedLocation = clampToWorkingArea(window.getGraphicsConfiguration(), window.getSize(),
                preferredLocation);

        if (clampedLocation != null && !clampedLocation.equals(window.getLocation())) {
            window.setLocation(clampedLocation);
        }
    }

    /**
     * Revalidates the window's content, the scrolled view first and then its scroll pane when the content scrolls.
     *
     * @param window
     *            - The frame or dialog whose content to revalidate
     * @param scrollPane
     *            - The window's content scroll pane (may be null when the content pane does not scroll)
     */
    private static void revalidateContent(Window window, JScrollPane scrollPane) {
        if (scrollPane == null) {
            Container contentPane = ((RootPaneContainer) window).getContentPane();

            if (contentPane instanceof JComponent) {
                ((JComponent) contentPane).revalidate();
            }

            return;
        }

        Component view = scrollPane.getViewport().getView();

        if (view instanceof JComponent) {
            ((JComponent) view).revalidate();
        }

        scrollPane.revalidate();
    }

    /**
     * Returns the window's content pane when it is a scroll pane, which is how the main frame and the overlay dialogs
     * scroll their content.
     *
     * @param window
     *            - The frame or dialog whose content pane to inspect
     *
     * @return The content scroll pane, or null if the content pane does not scroll
     */
    private static JScrollPane contentScrollPane(Window window) {
        Container contentPane = ((RootPaneContainer) window).getContentPane();

        return contentPane instanceof JScrollPane ? (JScrollPane) contentPane : null;
    }

    /**
     * Returns the root pane of a frame or dialog, which holds its cached display metrics.
     *
     * @param window
     *            - The frame or dialog whose root pane to return
     *
     * @return The window's root pane
     */
    private static JRootPane rootPane(Window window) {
        return ((RootPaneContainer) window).getRootPane();
    }

    /**
     * Returns the cached graphics configuration stored on the window.
     *
     * @param window
     *            - The frame or dialog whose cached graphics configuration to return
     *
     * @return The cached graphics configuration, or null if none has been cached
     */
    private static GraphicsConfiguration cachedDisplayConfiguration(Window window) {
        return (GraphicsConfiguration) rootPane(window).getClientProperty(CACHED_DISPLAY_CONFIGURATION_PROPERTY);
    }

    /**
     * Returns the cached working-area size stored on the window.
     *
     * @param window
     *            - The frame or dialog whose cached working-area size to return
     *
     * @return The cached working-area size, or null if none has been cached
     */
    private static Dimension cachedWorkingAreaSize(Window window) {
        return (Dimension) rootPane(window).getClientProperty(CACHED_WORKING_AREA_SIZE_PROPERTY);
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
