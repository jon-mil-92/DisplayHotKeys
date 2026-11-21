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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

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
     * Number of working area changes reported so far. A display measurement cached before the latest change is stale,
     * because a moved or resized task bar changes the working area but not the display's bounds.
     */
    private static int workingAreaChangeCount;

    /**
     * Client property key that caches the measurements of the display a frame or dialog was last fitted to.
     */
    private static final String CACHED_DISPLAY_METRICS_PROPERTY = WindowUtil.class.getName() + ".cachedDisplayMetrics";

    /**
     * Client property key that holds the placement a re-initialization retained for a dialog it destroyed.
     */
    private static final String RETAINED_PLACEMENT_PROPERTY = WindowUtil.class.getName() + ".retainedPlacement";

    /**
     * Client property key that holds the placement a frame or dialog last recorded while its display was unchanged.
     */
    private static final String RECORDED_PLACEMENT_PROPERTY = WindowUtil.class.getName() + ".recordedPlacement";

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
     * Captures the placement of a frame and of every dialog open over it, so each can be reproduced after the view is
     * re-initialized, even after the display is resized or rescaled.
     *
     * @param frame
     *            - The frame to capture the placement of
     *
     * @return The captured placement
     */
    public static FramePlacement capturePlacement(JFrame frame) {
        Map<Window, FramePlacement> dialogPlacements = new HashMap<>();
        captureDialogPlacements(frame, dialogPlacements);

        return capturePlacement(frame, dialogPlacements);
    }

    /**
     * Captures the placement of every showing dialog the owner owns, and of the dialogs those dialogs own in turn.
     *
     * @param owner
     *            - The window whose owned dialogs to capture
     * @param dialogPlacements
     *            - The map to add each captured dialog placement to, keyed by dialog
     */
    private static void captureDialogPlacements(Window owner, Map<Window, FramePlacement> dialogPlacements) {
        for (Window ownedWindow : owner.getOwnedWindows()) {
            if (ownedWindow instanceof JDialog && ownedWindow.isShowing()) {
                dialogPlacements.put(ownedWindow, capturePlacement(ownedWindow, Map.of()));
                captureDialogPlacements(ownedWindow, dialogPlacements);
            }
        }
    }

    /**
     * Captures the placement of a frame or dialog on the display it is on. Once a display change has moved the window,
     * its position no longer reflects its placement, so the placement it recorded before the change is used.
     *
     * @param window
     *            - The frame or dialog to capture the placement of
     * @param dialogPlacements
     *            - The placements of the dialogs open over the window, keyed by dialog
     *
     * @return The captured placement
     */
    private static FramePlacement capturePlacement(Window window, Map<Window, FramePlacement> dialogPlacements) {
        RecordedPlacement placement = isDisplayChanging(window) ? recordedPlacement(window) : measurePlacement(window);

        return new FramePlacement(placement.screenDeviceId(), placement.leftFraction(), placement.topFraction(),
                dialogPlacements);
    }

    /**
     * Measures the placement of a frame or dialog. Its top-left is recorded as a fraction of its working area's
     * available space (the working area minus its size), so a window against an edge or corner stays there.
     *
     * @param window
     *            - The frame or dialog to measure the placement of
     *
     * @return The measured placement
     */
    private static RecordedPlacement measurePlacement(Window window) {
        GraphicsConfiguration configuration = window.getGraphicsConfiguration();
        Rectangle bounds = window.getBounds();

        // The cached measurement is reused, so measuring on every move makes no native insets query
        DisplayMetrics metrics = displayMetrics(window, configuration);
        Rectangle workingArea = metrics.workingArea();
        double leftFraction = placeableFraction(bounds.x - workingArea.x, workingArea.width - bounds.width);
        double topFraction = placeableFraction(bounds.y - workingArea.y, workingArea.height - bounds.height);

        return new RecordedPlacement(configuration.getDevice().getIDstring(), metrics.displayBounds(), leftFraction,
                topFraction);
    }

    /**
     * Returns an offset as a fraction of the available space, kept within 0 to 1 so a window captured partly off screen
     * is placed fully on it.
     *
     * @param offset
     *            - The window's offset from the working area's leading edge along one axis
     * @param availableSpace
     *            - The working area's size minus the window's size along the same axis
     *
     * @return The fraction of the available space, or 0 when the window has no space to move in
     */
    private static double placeableFraction(int offset, int availableSpace) {
        if (availableSpace <= 0) {
            return 0.0;
        }

        return Math.max(0.0, Math.min(1.0, (double) offset / availableSpace));
    }

    /**
     * Hands each dialog in the placement its captured placement just before a re-initialization destroys it, so the
     * dialog shown in its place is placed the same way the frame is.
     *
     * @param placement
     *            - The frame placement the re-initialization reproduces (may be null)
     */
    public static void retainDialogPlacements(FramePlacement placement) {
        if (placement == null) {
            return;
        }

        for (Map.Entry<Window, FramePlacement> dialogEntry : placement.getDialogPlacements().entrySet()) {
            rootPane(dialogEntry.getKey()).putClientProperty(RETAINED_PLACEMENT_PROPERTY, dialogEntry.getValue());
        }
    }

    /**
     * Gets the placement a re-initialization retained for a dialog it destroyed.
     *
     * @param dialog
     *            - The destroyed dialog
     *
     * @return The retained placement, or null if no re-initialization destroyed the dialog
     */
    public static FramePlacement getRetainedPlacement(JDialog dialog) {
        return (FramePlacement) dialog.getRootPane().getClientProperty(RETAINED_PLACEMENT_PROPERTY);
    }

    /**
     * Caps a packed frame to the working area of the placement's display, then places it there to reproduce the
     * placement. The display's measurement is cached on the frame for its later re-fits.
     *
     * @param frame
     *            - The packed frame to fit and place
     * @param placement
     *            - The captured placement to reproduce, or null to center the frame on the default display
     */
    public static void fitFrameToPlacement(JFrame frame, FramePlacement placement) {
        Rectangle workingArea = fitToDisplay(frame, resolveConfiguration(placement));
        frame.setLocation(locationForPlacement(placement, workingArea, frame.getSize()));
        trackPlacement(frame);
    }

    /**
     * Settles the scroll bars of a frame that was just shown, then re-asserts its placement in case the OS positioned
     * it somewhere other than intended, even on another display.
     *
     * @param frame
     *            - The frame that was just shown
     * @param placement
     *            - The placement the frame was fitted to (may be null)
     */
    public static void settleShownFrame(JFrame frame, FramePlacement placement) {
        settleScrollBars(frame);

        // A minimized or maximized frame keeps the place the OS gives it
        if (!frame.isDisplayable() || frame.getExtendedState() != JFrame.NORMAL) {
            return;
        }

        // The fit cached the target display's measurement on the frame, so this makes no native insets query
        Rectangle workingArea = displayMetrics(frame, resolveConfiguration(placement)).workingArea();
        frame.setLocation(locationForPlacement(placement, workingArea, frame.getSize()));
    }

    /**
     * Caps a packed dialog to the working area of its display the same way the main frame is fitted, then reproduces
     * the given placement as the main frame does, or centers it on its owner, while keeping it fully on screen. Its
     * scroll bars settle once it is shown, as the main frame's do.
     *
     * @param dialog
     *            - The packed dialog to fit and place
     * @param placement
     *            - The placement to reproduce, or null to center the dialog on its owner
     */
    public static void fitDialogToPlacement(JDialog dialog, FramePlacement placement) {
        if (placement != null) {
            Rectangle workingArea = fitToDisplay(dialog, resolveConfiguration(placement));
            dialog.setLocation(locationForPlacement(placement, workingArea, dialog.getSize()));
        } else {
            Rectangle workingArea = fitToDisplay(dialog, dialog.getGraphicsConfiguration());
            dialog.setLocationRelativeTo(dialog.getOwner());

            // Centering on an owner near a display edge can push the dialog past it, so pull it inside
            dialog.setLocation(clampToWorkingArea(workingArea, dialog.getSize(), dialog.getLocation()));
        }

        trackPlacement(dialog);

        // Queued before a modal show blocks, so the scroll bars settle once the dialog is on screen
        SwingUtilities.invokeLater(() -> settleScrollBars(dialog));
    }

    /**
     * Records the window's placement every time it moves or resizes, so a later re-initialization can reproduce where
     * the window was before a display change moved it.
     *
     * @param window
     *            - The fitted frame or dialog to track
     */
    private static void trackPlacement(Window window) {
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent componentEvent) {
                recordPlacement(window);
            }

            @Override
            public void componentResized(ComponentEvent componentEvent) {
                recordPlacement(window);
            }
        });
    }

    /**
     * Records the window's current placement, unless a display change is under way, since the window is then moved for
     * the changed display and the placement from before the change must be kept.
     *
     * @param window
     *            - The frame or dialog whose placement to record
     */
    private static void recordPlacement(Window window) {
        if (!isDisplayChanging(window)) {
            rootPane(window).putClientProperty(RECORDED_PLACEMENT_PROPERTY, measurePlacement(window));
        }
    }

    /**
     * Determines whether a display change is under way for the window, which shows as the display it recorded its
     * placement on now having different bounds. A move to another display is not a display change.
     *
     * @param window
     *            - The frame or dialog to check
     *
     * @return True if the window's display changed since it recorded its placement, otherwise false
     */
    private static boolean isDisplayChanging(Window window) {
        RecordedPlacement recordedPlacement = recordedPlacement(window);
        GraphicsConfiguration configuration = window.getGraphicsConfiguration();

        return recordedPlacement != null
                && recordedPlacement.screenDeviceId().equals(configuration.getDevice().getIDstring())
                && !recordedPlacement.displayBounds().equals(configuration.getBounds());
    }

    /**
     * Returns the placement the window last recorded.
     *
     * @param window
     *            - The frame or dialog whose recorded placement to return
     *
     * @return The recorded placement, or null if the window has not recorded one
     */
    private static RecordedPlacement recordedPlacement(Window window) {
        return (RecordedPlacement) rootPane(window).getClientProperty(RECORDED_PLACEMENT_PROPERTY);
    }

    /**
     * Refreshes a frame's or dialog's UI, then re-fits it to the working area of the display it currently occupies.
     *
     * @param window
     *            - The frame or dialog to refresh
     */
    public static void refreshWindow(Window window) {
        if (!window.isDisplayable()) {
            return;
        }

        SwingUtilities.updateComponentTreeUI(window);
        revalidateContent(window);
        refitWindow(window);

        window.repaint();
        scheduleNativeSurfaceReclaim();
    }

    /**
     * Re-packs a showing frame or dialog after its content changes and re-fits it to the working area of the display it
     * occupies, keeping its location unless that would leave part of it off screen. After a display change it returns
     * to the placement it recorded before the change. Unlike a refresh, the UI is kept.
     *
     * @param window
     *            - The frame or dialog to re-fit
     */
    public static void refitWindow(Window window) {
        if (!window.isDisplayable()) {
            return;
        }

        // The display is measured again only after it changes, so repeated re-fits make no native insets query
        Rectangle workingArea = displayMetrics(window, window.getGraphicsConfiguration()).workingArea();
        Point location = window.getLocation();

        window.pack();
        sizeToContent(window, workingArea);

        // A display change moved the window, so it returns to the placement it recorded before the change
        if (isDisplayChanging(window)) {
            location = locationForPlacement(capturePlacement(window, Map.of()), workingArea, window.getSize());
        }

        window.setLocation(clampToWorkingArea(workingArea, window.getSize(), location));
        window.validate();

        // The window is already realized here, so its client area is laid out and the shortfall can settle in place
        settleScrollBars(window);
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
     */
    public static void setScrollingContent(Window window, JPanel content, Runnable onEmptySpacePress) {
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
    }

    /**
     * Determines whether a window has been fitted to a working area by this class, which the main frame and every
     * dialog placed through it are.
     *
     * @param window
     *            - The window to check
     *
     * @return True if the window holds cached display metrics from a fit, otherwise false
     */
    public static boolean isFittedToWorkingArea(Window window) {
        return window instanceof RootPaneContainer && cachedDisplayMetrics(window) != null;
    }

    /**
     * Marks every cached display measurement as stale after a working area change, so each frame and dialog measures
     * its display again the next time it is fitted, placed, or captured.
     */
    public static void invalidateDisplayMetrics() {
        workingAreaChangeCount++;
    }

    /**
     * Caches the display's measurement on the window, then sizes the window to its content within the working area.
     *
     * @param window
     *            - The packed frame or dialog to cap
     * @param configuration
     *            - The graphics configuration of the display to fit the window to
     *
     * @return The display's working area
     */
    private static Rectangle fitToDisplay(Window window, GraphicsConfiguration configuration) {
        Rectangle workingArea = displayMetrics(window, configuration).workingArea();
        sizeToContent(window, workingArea);

        return workingArea;
    }

    /**
     * Returns the window's cached measurement of the display when it is still current, and otherwise measures the
     * display and caches the result. A rescaled or resized display, or a changed working area, is measured again.
     *
     * @param window
     *            - The frame or dialog to cache the measurement on
     * @param configuration
     *            - The graphics configuration of the display to measure
     *
     * @return The measurement of the display
     */
    private static DisplayMetrics displayMetrics(Window window, GraphicsConfiguration configuration) {
        DisplayMetrics cachedMetrics = cachedDisplayMetrics(window);
        Rectangle displayBounds = configuration.getBounds();

        if (cachedMetrics != null && cachedMetrics.configuration() == configuration
                && cachedMetrics.displayBounds().equals(displayBounds)
                && cachedMetrics.workingAreaChangeCount() == workingAreaChangeCount) {
            return cachedMetrics;
        }

        // The working area is the display minus its screen insets (task bar, menu bar), which are queried natively
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        Rectangle workingArea = new Rectangle(displayBounds.x + insets.left, displayBounds.y + insets.top,
                displayBounds.width - insets.left - insets.right, displayBounds.height - insets.top - insets.bottom);

        DisplayMetrics measuredMetrics = new DisplayMetrics(configuration, displayBounds, workingArea,
                workingAreaChangeCount);
        rootPane(window).putClientProperty(CACHED_DISPLAY_METRICS_PROPERTY, measuredMetrics);

        return measuredMetrics;
    }

    /**
     * Sizes the window to its content within the working area. An axis that still overflows reserves room for the other
     * axis's scroll bar, so a scroll bar shows only where the content truly does not fit.
     *
     * @param window
     *            - The frame or dialog to size
     * @param workingArea
     *            - The working area of the display the window is fitted to
     */
    private static void sizeToContent(Window window, Rectangle workingArea) {
        JScrollPane scrollPane = contentScrollPane(window);
        int desiredWidth = window.getWidth();
        int desiredHeight = window.getHeight();

        if (scrollPane != null) {
            // The scroll pane's size counts any scroll bar it shows, so the content alone decides the size it needs
            Dimension viewPreferred = scrollPane.getViewport().getView().getPreferredSize();
            desiredWidth += viewPreferred.width - scrollPane.getWidth();
            desiredHeight += viewPreferred.height - scrollPane.getHeight();

            int verticalScrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
            int horizontalScrollBarHeight = scrollPane.getHorizontalScrollBar().getPreferredSize().height;

            boolean needsHorizontal = desiredWidth > workingArea.width;
            boolean needsVertical = desiredHeight > workingArea.height;

            if (needsHorizontal) {
                desiredHeight += horizontalScrollBarHeight;
            }

            if (needsVertical) {
                desiredWidth += verticalScrollBarWidth;
            }

            // Reserving space for one scroll bar can push the perpendicular axis over, requiring the other as well
            if (!needsVertical && desiredHeight > workingArea.height) {
                desiredWidth += verticalScrollBarWidth;
            }

            if (!needsHorizontal && desiredWidth > workingArea.width) {
                desiredHeight += horizontalScrollBarHeight;
            }
        }

        // A pack made before the real frame insets were known can be off either way, so the window may shrink as well
        window.setSize(Math.min(desiredWidth, workingArea.width), Math.min(desiredHeight, workingArea.height));
    }

    /**
     * Sizes a realized window to its content again, since the window's real frame can leave its client area larger or
     * smaller than the estimate it was packed with. A resized window is pulled back inside the working area.
     *
     * @param window
     *            - The fitted frame or dialog to settle
     */
    private static void settleScrollBars(Window window) {
        if (contentScrollPane(window) == null || !window.isDisplayable()) {
            return;
        }

        Rectangle workingArea = cachedDisplayMetrics(window).workingArea();
        sizeToContent(window, workingArea);
        window.validate();

        // Growing a window placed against the working area's edge would push it past that edge, so pull it back inside
        window.setLocation(clampToWorkingArea(workingArea, window.getSize(), window.getLocation()));
    }

    /**
     * Clamps the preferred top-left location for a window into the working area, so the window stays fully on screen.
     *
     * @param workingArea
     *            - The working-area bounds that should contain the window
     * @param windowSize
     *            - The window size to keep within the working area
     * @param preferredLocation
     *            - The desired top-left location before clamping
     *
     * @return The clamped top-left location
     */
    private static Point clampToWorkingArea(Rectangle workingArea, Dimension windowSize, Point preferredLocation) {
        int maxLeft = workingArea.x + Math.max(0, workingArea.width - windowSize.width);
        int maxTop = workingArea.y + Math.max(0, workingArea.height - windowSize.height);
        int clampedLeft = Math.max(workingArea.x, Math.min(preferredLocation.x, maxLeft));
        int clampedTop = Math.max(workingArea.y, Math.min(preferredLocation.y, maxTop));

        return new Point(clampedLeft, clampedTop);
    }

    /**
     * Computes the top-left location that reproduces the placement's fractions of the working area's available space,
     * or centers the window when there is no placement. The window is capped to the working area, so it stays inside.
     *
     * @param placement
     *            - The captured placement (may be null)
     * @param workingArea
     *            - The current working area of the target display
     * @param windowSize
     *            - The size of the window to place
     *
     * @return The top-left location in screen coordinates
     */
    private static Point locationForPlacement(FramePlacement placement, Rectangle workingArea, Dimension windowSize) {
        double leftFraction = (placement != null) ? placement.getLeftFraction() : 0.5;
        double topFraction = (placement != null) ? placement.getTopFraction() : 0.5;
        int left = (int) Math.round(workingArea.x + leftFraction * (workingArea.width - windowSize.width));
        int top = (int) Math.round(workingArea.y + topFraction * (workingArea.height - windowSize.height));

        return new Point(left, top);
    }

    /**
     * Resolves the graphics configuration of the display the placement targets. AWT may replace a display's
     * configuration after a display change, so the display is matched by its device ID, and the default display is used
     * when there is no placement or its display is gone.
     *
     * @param placement
     *            - The captured placement to resolve a configuration for (may be null to use the default display)
     *
     * @return The graphics configuration of the target display
     */
    private static GraphicsConfiguration resolveConfiguration(FramePlacement placement) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        if (placement != null) {
            for (GraphicsDevice device : graphicsEnvironment.getScreenDevices()) {
                if (device.getIDstring().equals(placement.getScreenDeviceId())) {
                    return device.getDefaultConfiguration();
                }
            }
        }

        return graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Revalidates the window's content. Revalidating the scrolled view stops at its scroll pane, so the content pane is
     * revalidated as well.
     *
     * @param window
     *            - The frame or dialog whose content to revalidate
     */
    private static void revalidateContent(Window window) {
        JScrollPane scrollPane = contentScrollPane(window);

        if (scrollPane != null) {
            ((JComponent) scrollPane.getViewport().getView()).revalidate();
        }

        ((JComponent) ((RootPaneContainer) window).getContentPane()).revalidate();
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
        return ((RootPaneContainer) window).getContentPane() instanceof JScrollPane scrollPane ? scrollPane : null;
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
     * Returns the display measurement cached on the window.
     *
     * @param window
     *            - The frame or dialog whose cached display measurement to return
     *
     * @return The cached display measurement, or null if none has been cached
     */
    private static DisplayMetrics cachedDisplayMetrics(Window window) {
        return (DisplayMetrics) rootPane(window).getClientProperty(CACHED_DISPLAY_METRICS_PROPERTY);
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
     * Measurement of the display a frame or dialog was fitted to, cached on the window so its re-fits make no native
     * insets query until the display or its working area changes.
     *
     * @param configuration
     *            - The graphics configuration of the measured display
     * @param displayBounds
     *            - The display's bounds when measured, which change when the display is rescaled or resized
     * @param workingArea
     *            - The display's working area when measured
     * @param workingAreaChangeCount
     *            - The number of working area changes reported when measured, which tells a stale working area apart
     */
    private record DisplayMetrics(GraphicsConfiguration configuration, Rectangle displayBounds, Rectangle workingArea,
            int workingAreaChangeCount) {
    }

    /**
     * Placement a frame or dialog recorded, together with the display it was recorded on, so a later display change can
     * be told apart from a move to another display.
     *
     * @param screenDeviceId
     *            - The ID of the screen device the window was on
     * @param displayBounds
     *            - The bounds of that display when the placement was recorded
     * @param leftFraction
     *            - The window's left edge as a fraction of the working area's available horizontal space
     * @param topFraction
     *            - The window's top edge as a fraction of the working area's available vertical space
     */
    private record RecordedPlacement(String screenDeviceId, Rectangle displayBounds, double leftFraction,
            double topFraction) {
    }

}
