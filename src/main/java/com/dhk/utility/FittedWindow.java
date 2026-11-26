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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import com.dhk.io.WindowPlacement;
import com.dhk.io.WindowPlacement.DisplayArea;
import com.dhk.io.WindowPlacement.WindowBounds;

/**
 * Keeps one frame or dialog capped to its display's working area and at its recorded placement through any display
 * change. AWT lays out and places the window, but only once AWT agrees with Windows on the window's display, since AWT
 * reaches a display change later than Windows does.
 *
 * @author Jonathan R. Miller
 */
final class FittedWindow {

    private final Window window;
    private final Runnable afterFitAction;
    private RecordedPlacement recordedPlacement;
    private DisplayArea fittedDisplay;
    private Runnable pendingFit;
    private boolean returnPending;
    private boolean placementCheckQueued;
    private boolean surfaceReplaced;
    private double measuredScale;

    /**
     * Whether a native surface reclaim is queued on the EDT, so every window completing its fit before it runs shares
     * its collection. Only read and written on the EDT.
     */
    private static boolean nativeReclaimQueued;

    /**
     * Whether a native surface reclaim was requested since the background collector last took a request. Guarded by
     * {@link #NATIVE_RECLAIM_LOCK}.
     */
    private static boolean nativeReclaimRequested;

    /**
     * Whether the background collector is running, in which case it takes a new request once its current collection
     * ends rather than another collector starting. Guarded by {@link #NATIVE_RECLAIM_LOCK}.
     */
    private static boolean nativeReclaimRunning;

    /**
     * Whether the left mouse button is held anywhere in the system, as of the button events the EDT has handled. A
     * pending fit waits while it is held, so a drag is never interrupted.
     */
    private static boolean leftMouseButtonDown;

    /**
     * Client property key under which a window's root pane holds the fitted window that keeps it fitted.
     */
    private static final String FITTED_WINDOW_PROPERTY = FittedWindow.class.getName();

    /**
     * DPI of a display at a 100% scale. A display's DPI divided by it is the scale AWT sizes windows by there.
     */
    private static final int UNSCALED_DPI = 96;

    /**
     * Lock guarding the reclaim request and the collector's running state, which the EDT and the background collector
     * share.
     */
    private static final Object NATIVE_RECLAIM_LOCK = new Object();

    /**
     * Constructor for the {@link FittedWindow} class.
     *
     * @param window
     *            - The frame or dialog to keep fitted
     * @param afterFitAction
     *            - The action to run after each fit that places the window (may be null)
     */
    private FittedWindow(Window window, Runnable afterFitAction) {
        this.window = window;
        this.afterFitAction = afterFitAction;
    }

    /**
     * Starts keeping a newly built and packed frame or dialog fitted. It is fitted where it first goes at once, and
     * again once it is on screen, then records its placement from then on.
     *
     * @param window
     *            - The packed frame or dialog to keep fitted
     * @param afterFitAction
     *            - The action to run after each fit that places the window (may be null)
     */
    static void fitNewWindow(Window window, Runnable afterFitAction) {
        FittedWindow fittedWindow = new FittedWindow(window, afterFitAction);
        fittedWindow.track();
        fittedWindow.requestFit(fittedWindow::fitInitially);

        // Queued before the window shows, and before a modal dialog's show blocks, so it runs once it is on screen
        SwingUtilities.invokeLater(fittedWindow::settle);
    }

    /**
     * Returns the fitted window that keeps a frame or dialog fitted.
     *
     * @param window
     *            - The window to look up
     *
     * @return The fitted window, or null if the window is not kept fitted
     */
    static FittedWindow of(Window window) {
        if (!(window instanceof RootPaneContainer container)) {
            return null;
        }

        return (FittedWindow) container.getRootPane().getClientProperty(FITTED_WINDOW_PROPERTY);
    }

    /**
     * Re-fits the window after its content or its display changes, keeping its top-left unless that would leave part of
     * it off screen. After any change to its recorded display it returns to the placement it recorded before.
     */
    void requestRefit() {
        requestFit(() -> refit(false));
    }

    /**
     * Re-fits the window after a re-initialization rebuilt it in place, keeping its share of the working area even when
     * its size changed. After any change to its recorded display it returns to the placement it recorded before.
     */
    void requestRebuiltRefit() {
        requestFit(() -> refit(true));
    }

    /**
     * Re-fits the window after a working area changed, which never changes its content. A window still fitted to its
     * unchanged display is left as it is, while any other keeps or returns to its placement.
     */
    void requestWorkingAreaRefit() {
        ensureFitPending();
        requestFit(pendingFit);
    }

    /**
     * Notes that the left mouse button was pressed, which holds back every pending fit until it is released.
     */
    static void leftMouseButtonPressed() {
        leftMouseButtonDown = true;
    }

    /**
     * Notes that the left mouse button was released and checks every frame and dialog with a pending fit, which a drag
     * held back.
     */
    static void leftMouseButtonReleased() {
        leftMouseButtonDown = false;

        for (Window window : Window.getWindows()) {
            FittedWindow fittedWindow = of(window);

            if (fittedWindow != null && window.isDisplayable() && fittedWindow.pendingFit != null) {
                fittedWindow.queuePlacementCheck();
            }
        }
    }

    /**
     * Marks the window as fitted, then checks its placement whenever it moves or resizes, and re-fits it once for each
     * new configuration AWT gives it.
     */
    private void track() {
        ((RootPaneContainer) window).getRootPane().putClientProperty(FITTED_WINDOW_PROPERTY, this);

        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent componentEvent) {
                queuePlacementCheck();
            }

            @Override
            public void componentResized(ComponentEvent componentEvent) {
                queuePlacementCheck();
            }
        });

        // AWT changes the window's scale and replaces its drawing surface before it reports a new configuration
        window.addPropertyChangeListener("graphicsConfiguration", this::configurationChanged);
    }

    /**
     * Makes a re-fit pending for a new configuration AWT gave the window, such as after a display change or a move onto
     * another display. Every configuration one change reports shares the one pending re-fit.
     *
     * @param configurationChangeEvent
     *            - The event that reports the window's new configuration
     */
    private void configurationChanged(PropertyChangeEvent configurationChangeEvent) {
        // AWT never repaints the window on its new surface
        window.repaint();

        // One change reports several configurations, so the surfaces they orphan are reclaimed once its fit completes
        if (configurationChangeEvent.getOldValue() != null) {
            surfaceReplaced = true;
        }

        // The display can change back before the pending re-fit runs, so a change to it is noted as it is reported
        noteRecordedDisplayChange();
        ensureFitPending();
        queuePlacementCheck();
    }

    /**
     * Runs a fit for a window that is still displayable. The fit stays pending while AWT disagrees with Windows on its
     * display and runs again once AWT may have caught up. A newer fit replaces a pending one.
     *
     * @param fit
     *            - The fit to run, which re-reads everything it needs each time it runs
     */
    private void requestFit(Runnable fit) {
        if (!window.isDisplayable()) {
            return;
        }

        pendingFit = fit;
        fit.run();
    }

    /**
     * Makes sure the window has a pending fit, adding a re-fit for its display when it has none. A fit already pending
     * is kept, since it re-reads the display as well and may carry a content change that must be laid out.
     */
    private void ensureFitPending() {
        if (pendingFit == null) {
            pendingFit = this::refitForDisplayChange;
        }
    }

    /**
     * Queues one check that records the window's placement and runs any fit it is waiting on, unless a drag holds the
     * left mouse button. The moves of a drag share one queued check, leaving the event queue free to repaint.
     */
    private void queuePlacementCheck() {
        if (placementCheckQueued) {
            return;
        }

        placementCheckQueued = true;

        // The check runs after the current event, since a new configuration is reported while AWT holds its tree lock
        SwingUtilities.invokeLater(() -> {
            placementCheckQueued = false;

            if (!window.isDisplayable()) {
                return;
            }

            recordUserPlacement();

            // A fit during a drag would pull the window away from the cursor, so the button's release runs it instead
            if (pendingFit != null && !leftMouseButtonDown) {
                pendingFit.run();
            }
        });
    }

    /**
     * Records the window's current placement as the user left it, unless a fit is pending, which records where the
     * window ends up, or the display it recorded its placement on was rescaled or resized since. Windows may then have
     * moved the window anywhere, so the recorded placement is kept.
     */
    private void recordUserPlacement() {
        // A display can change and change back before the window's fit runs, which only the pending fit reveals
        if (pendingFit == null && !isRecordedDisplayChanging()) {
            recordPlacement(WindowPlacement.windowBounds(window), WindowPlacement.displayOf(window));
        }
    }

    /**
     * Determines whether the display the window recorded its placement on is still attached but now has a different DPI
     * or bounds. A move to another display, or a recorded display that is gone, is not a display change.
     *
     * @return True if the window's recorded display was rescaled or resized since the window recorded its placement
     */
    private boolean isRecordedDisplayChanging() {
        DisplayArea recordedDisplay = currentRecordedDisplay();

        return recordedDisplay != null && (recordedDisplay.dpi() != recordedPlacement.display().dpi()
                || !recordedDisplay.bounds().equals(recordedPlacement.display().bounds()));
    }

    /**
     * Notes that a return to the window's recorded placement is pending once the display it was recorded on changed in
     * any way, its working area included. The return stays pending until a fit completes, since a display that changes
     * back before then has still moved the window.
     *
     * @return The recorded display as it is now, or null if there is no recorded placement or its display is gone
     */
    private DisplayArea noteRecordedDisplayChange() {
        DisplayArea recordedDisplay = currentRecordedDisplay();

        if (recordedDisplay != null && !recordedDisplay.equals(recordedPlacement.display())) {
            returnPending = true;
        }

        return recordedDisplay;
    }

    /**
     * Returns the current area of the display the window recorded its placement on.
     *
     * @return The display's current area, or null if there is no recorded placement or its display is gone
     */
    private DisplayArea currentRecordedDisplay() {
        return (recordedPlacement != null) ? WindowPlacement.displayArea(recordedPlacement.display().name()) : null;
    }

    /**
     * Stores the window's measured placement as the placement it recorded. A window whose placement cannot be measured
     * keeps the placement it recorded before.
     *
     * @param windowBounds
     *            - The window's current bounds (may be null)
     * @param display
     *            - The display the window is mostly on (may be null)
     */
    private void recordPlacement(WindowBounds windowBounds, DisplayArea display) {
        RecordedPlacement measuredPlacement = measurePlacement(windowBounds, display);

        if (measuredPlacement != null) {
            recordedPlacement = measuredPlacement;
        }
    }

    /**
     * Measures the window's placement on the display it is mostly on. Its top-left is measured as a share of the
     * working area's available space (the working area minus its size), so a window at an edge stays there.
     *
     * @param windowBounds
     *            - The window's current bounds (may be null)
     * @param display
     *            - The display the window is mostly on (may be null)
     *
     * @return The measured placement, or null if the window is minimized or maximized or could not be measured
     */
    private RecordedPlacement measurePlacement(WindowBounds windowBounds, DisplayArea display) {
        if (windowBounds == null || windowBounds.minimizedOrMaximized() || display == null) {
            return null;
        }

        // A window still where its record puts it keeps the record, so only the user's moves and resizes change it
        if (isAtRecordedPlacement(display)) {
            return new RecordedPlacement(display, recordedPlacement.leftFraction(), recordedPlacement.topFraction());
        }

        Rectangle bounds = windowBounds.bounds();
        Rectangle workingArea = display.workingArea();
        double leftFraction = placeableFraction(bounds.x - workingArea.x, workingArea.width - bounds.width);
        double topFraction = placeableFraction(bounds.y - workingArea.y, workingArea.height - bounds.height);

        return new RecordedPlacement(display, leftFraction, topFraction);
    }

    /**
     * Determines whether the window is exactly where its recorded placement puts it on the display it occupies, in
     * which case the record still describes it. AWT converts between its pixels and physical ones without loss, so the
     * check is exact.
     *
     * @param display
     *            - The display the window occupies
     *
     * @return True if placing the window by its recorded placement would leave it where it is
     */
    private boolean isAtRecordedPlacement(DisplayArea display) {
        if (recordedPlacement == null || !recordedPlacement.display().name().equals(display.name())
                || !configurationMatches(window.getGraphicsConfiguration(), display)) {
            return false;
        }

        Point location = window.getLocation();
        Point placedLocation = targetLocation(logicalWorkingAreaOf(display), location, window.getSize(),
                atPlacement(recordedPlacement));

        return placedLocation.equals(location);
    }

    /**
     * Returns an offset as a fraction of the available space, kept within 0 to 1 so a window measured partly off screen
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
     * Re-fits the window for a new configuration or working area, unless a re-fit would leave it as it is. That is the
     * case while its last fit is in effect on an unchanged display and the window lies within the working area.
     */
    private void refitForDisplayChange() {
        noteRecordedDisplayChange();
        WindowBounds windowBounds = WindowPlacement.windowBounds(window);

        // A re-fit would leave the window where it is, so it completes as one would, recording where the user left it
        if (!returnPending && fittedDisplay != null && isFitInEffect(windowBounds, fittedDisplay)
                && logicalWorkingAreaOf(fittedDisplay).contains(window.getBounds())) {
            pendingFit = null;
            reclaimReplacedSurfaces();
            recordPlacement(windowBounds, fittedDisplay);
            return;
        }

        refit(false);
    }

    /**
     * Re-fits the window to its content, returning it to its recorded placement while a return is pending. Otherwise it
     * keeps its top-left, or its share of the working area when it keeps its proportions.
     *
     * @param keepProportions
     *            - Whether the window keeps its share of the working area rather than its top-left
     */
    private void refit(boolean keepProportions) {
        DisplayArea recordedDisplay = noteRecordedDisplayChange();

        if (returnPending && recordedDisplay != null) {
            fit(recordedDisplay, atPlacement(recordedPlacement));
            return;
        }

        // The window's share is measured before the fit resizes it, so it describes where the window was
        DisplayArea display = WindowPlacement.displayOf(window);
        RecordedPlacement currentPlacement = keepProportions
                ? measurePlacement(WindowPlacement.windowBounds(window), display)
                : null;

        fit(display, (currentPlacement != null) ? atPlacement(currentPlacement) : atStartLocation());
    }

    /**
     * Fits the window where it first goes. A dialog is centered on its owner, or on its owner's display when the owner
     * is not on screen, and a frame is centered on the primary display.
     */
    private void fitInitially() {
        // No placement centers the window on the display
        if (!(window instanceof JDialog)) {
            fit(WindowPlacement.primaryDisplay(), atPlacement(null));
            return;
        }

        Window owner = window.getOwner();
        WindowBounds ownerBounds = (owner != null && owner.isShowing()) ? WindowPlacement.windowBounds(owner) : null;
        DisplayArea ownerDisplay = WindowPlacement.displayOf((owner != null) ? owner : window);

        if (ownerBounds == null || ownerBounds.minimizedOrMaximized() || ownerDisplay == null) {
            fit(ownerDisplay, atPlacement(null));
            return;
        }

        fit(ownerDisplay, centeredOn(logicalBoundsOf(ownerBounds.bounds(), ownerDisplay)));
    }

    /**
     * Fits the window where it first goes once more now that it is on screen, since its scroll bars only settle once
     * its frame is real. A window waiting on a fit settles when that fit runs, and one not on screen when it is shown.
     */
    private void settle() {
        if (window.isShowing() && pendingFit == null) {
            requestFit(this::fitInitially);
        }
    }

    /**
     * Packs the window and sizes it to its content within the display's working area, then places it where the rule
     * puts it and records that placement. A window AWT and Windows disagree on waits with its fit pending, and one that
     * is minimized or maximized, or whose display cannot be read, is only laid out.
     *
     * @param display
     *            - The display to fit the window to (may be null)
     * @param locationRule
     *            - The rule that decides where the window goes on the display
     */
    private void fit(DisplayArea display, LocationRule locationRule) {
        WindowBounds startBounds = WindowPlacement.windowBounds(window);
        boolean placeable = display != null && startBounds != null;

        if (placeable && !isAgreedWithWindows(startBounds, WindowPlacement.displayNameOf(window), display)) {
            window.validate();
            window.repaint();
            return;
        }

        // AWT keeps the bounds it read at an old scale, so it reads them again before the pack re-sends them
        WindowPlacement.resyncAwtBounds(window);
        Point startLocation = window.getLocation();
        measureAtCurrentScale();
        invalidateContent();
        window.pack();

        if (!placeable || startBounds.minimizedOrMaximized()) {
            pendingFit = null;
            fittedDisplay = null;
            reclaimReplacedSurfaces();
            window.validate();
            return;
        }

        Rectangle workingArea = logicalWorkingAreaOf(display);
        Dimension fittedSize = contentSize(workingArea.getSize());
        Point fittedLocation = targetLocation(workingArea, startLocation, fittedSize, locationRule);
        window.setBounds(new Rectangle(fittedLocation, fittedSize));
        window.validate();

        // Windows keeps what it can of a moved or resized window's old pixels, so the whole window is painted anew
        window.repaint();

        WindowBounds placedBounds = WindowPlacement.windowBounds(window);

        // A window placed onto another display, or placed while AWT took a display change, fits again once AWT agrees
        if (!isFitInEffect(placedBounds, display)) {
            return;
        }

        pendingFit = null;
        returnPending = false;
        fittedDisplay = display;
        reclaimReplacedSurfaces();
        recordPlacement(placedBounds, display);

        if (afterFitAction != null) {
            afterFitAction.run();
        }
    }

    /**
     * Determines whether a fit is in effect as computed. AWT can take a display change in the middle of a fit and place
     * the window at a stale scale, so the display must be unchanged, and the window must be on it at bounds that AWT
     * reads back as the bounds it holds.
     *
     * @param placedBounds
     *            - The window's bounds after it was placed (may be null)
     * @param display
     *            - The display the window was fitted to
     *
     * @return True if the window is placed as fitted, or false if it has to be fitted again
     */
    private boolean isFitInEffect(WindowBounds placedBounds, DisplayArea display) {
        String windowDisplayName = WindowPlacement.displayNameOf(window);

        // AWT cannot reach every physical pixel at a fractional scale, so the bounds are compared in AWT's pixels
        return placedBounds != null && display.equals(WindowPlacement.displayArea(display.name()))
                && display.name().equals(windowDisplayName)
                && isAgreedWithWindows(placedBounds, windowDisplayName, display)
                && logicalBoundsOf(placedBounds.bounds(), display).equals(window.getBounds());
    }

    /**
     * Recreates the UI of every component in the window when its scale changed since its content was last measured,
     * since Swing keeps sizes measured at the old scale that a DPI change on the window's own display never refreshes.
     */
    private void measureAtCurrentScale() {
        double scale = window.getGraphicsConfiguration().getDefaultTransform().getScaleX();

        // A window no fit has measured yet has a measured scale of 0, and its content is new
        boolean rescaled = measuredScale != 0 && measuredScale != scale;
        measuredScale = scale;

        if (rescaled) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    /**
     * Determines whether AWT agrees with Windows on the display the window is fitted to, which AWT reaches later than
     * Windows after a change. On that display, AWT's configuration for the window and its DPI must agree as well.
     *
     * @param windowBounds
     *            - The window's current bounds
     * @param windowDisplayName
     *            - The name of the display the window is mostly on (may be null)
     * @param display
     *            - The display the window is fitted to
     *
     * @return True if the window can be fitted to the display through AWT now
     */
    private boolean isAgreedWithWindows(WindowBounds windowBounds, String windowDisplayName, DisplayArea display) {
        GraphicsConfiguration targetConfiguration = configurationOf(display);

        if (targetConfiguration == null || !configurationMatches(targetConfiguration, display)) {
            return false;
        }

        // A window on another display is moved onto this one by the fit, which runs again once the window lands
        if (!display.name().equals(windowDisplayName)) {
            return true;
        }

        // Windows rescales a window on screen on its own schedule, while a hidden or minimized one waits to be shown
        boolean scaledByWindows = !window.isShowing() || windowBounds.minimizedOrMaximized()
                || windowBounds.dpi() == display.dpi();

        return scaledByWindows && configurationMatches(window.getGraphicsConfiguration(), display);
    }

    /**
     * Determines whether an AWT configuration describes a display as Windows has it now. Its scale is read live from
     * the display, while its bounds change only once AWT has caught up with the display, so both must match.
     *
     * @param configuration
     *            - The AWT configuration to check
     * @param display
     *            - The display as Windows has it
     *
     * @return True if the configuration has the display's DPI, origin, and size at that DPI
     */
    private static boolean configurationMatches(GraphicsConfiguration configuration, DisplayArea display) {
        Rectangle configurationBounds = configuration.getBounds();
        Rectangle displayBounds = display.bounds();
        double displayScale = (double) display.dpi() / UNSCALED_DPI;

        return Math.round(configuration.getDefaultTransform().getScaleX() * UNSCALED_DPI) == display.dpi()
                && configurationBounds.getLocation().equals(displayBounds.getLocation())
                && Math.abs(configurationBounds.width - displayBounds.width / displayScale) <= 1
                && Math.abs(configurationBounds.height - displayBounds.height / displayScale) <= 1;
    }

    /**
     * Returns AWT's configuration for a display. AWT keeps each display at its physical origin and only scales its
     * size, so the display is found by its origin.
     *
     * @param display
     *            - The display to find the configuration of
     *
     * @return AWT's configuration for the display, or null if AWT does not have the display yet
     */
    private static GraphicsConfiguration configurationOf(DisplayArea display) {
        for (GraphicsDevice screenDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration configuration = screenDevice.getDefaultConfiguration();

            if (configuration.getBounds().getLocation().equals(display.bounds().getLocation())) {
                return configuration;
            }
        }

        return null;
    }

    /**
     * Returns a display's working area in the logical pixels AWT places windows in, scaled exactly as AWT scales it. It
     * is read from Windows, since AWT refreshes its own copy of the working area only after a change is reported.
     *
     * @param display
     *            - The display whose working area to return
     *
     * @return The display's working area in logical pixels
     */
    private static Rectangle logicalWorkingAreaOf(DisplayArea display) {
        // AWT scales in single precision, so the result rounds exactly as AWT's does even at a custom scale
        float displayScale = (float) display.dpi() / UNSCALED_DPI;
        Rectangle bounds = display.bounds();
        Rectangle workingArea = display.workingArea();

        // AWT keeps the display at its physical origin, and scales its size and the inset at each edge on their own
        int leftInset = toLogicalPixels(workingArea.x - bounds.x, displayScale);
        int topInset = toLogicalPixels(workingArea.y - bounds.y, displayScale);
        int rightInset = toLogicalPixels(bounds.x + bounds.width - workingArea.x - workingArea.width, displayScale);
        int bottomInset = toLogicalPixels(bounds.y + bounds.height - workingArea.y - workingArea.height, displayScale);
        int logicalWidth = toLogicalPixels(bounds.width, displayScale);
        int logicalHeight = toLogicalPixels(bounds.height, displayScale);

        return new Rectangle(bounds.x + leftInset, bounds.y + topInset, logicalWidth - leftInset - rightInset,
                logicalHeight - topInset - bottomInset);
    }

    /**
     * Returns the logical bounds AWT gives a window with the given physical bounds on a display. AWT keeps the display
     * at its physical origin, and scales the window's offset from it and its size.
     *
     * @param physicalBounds
     *            - The window's bounds in physical pixels
     * @param display
     *            - The display the window is on
     *
     * @return The window's bounds in the logical pixels of AWT
     */
    private static Rectangle logicalBoundsOf(Rectangle physicalBounds, DisplayArea display) {
        float displayScale = (float) display.dpi() / UNSCALED_DPI;
        Point displayOrigin = display.bounds().getLocation();

        return new Rectangle(displayOrigin.x + toLogicalPixels(physicalBounds.x - displayOrigin.x, displayScale),
                displayOrigin.y + toLogicalPixels(physicalBounds.y - displayOrigin.y, displayScale),
                toLogicalPixels(physicalBounds.width, displayScale),
                toLogicalPixels(physicalBounds.height, displayScale));
    }

    /**
     * Converts a length in physical pixels to the logical pixels of AWT at a scale, rounding half down as AWT does.
     *
     * @param physicalPixels
     *            - The length in physical pixels
     * @param scale
     *            - The display's scale, in the single precision AWT keeps it in
     *
     * @return The length in logical pixels
     */
    private static int toLogicalPixels(int physicalPixels, float scale) {
        return (int) Math.ceil(physicalPixels / scale - 0.5);
    }

    /**
     * Returns the size that fits the packed window to its content within the maximum size. An axis that still overflows
     * reserves room for the other axis's scroll bar, so a scroll bar shows only where the content truly does not fit.
     *
     * @param maximumSize
     *            - The largest size the window can take on its display
     *
     * @return The window's fitted size
     */
    private Dimension contentSize(Dimension maximumSize) {
        JScrollPane scrollPane = contentScrollPane();
        int desiredWidth = window.getWidth();
        int desiredHeight = window.getHeight();

        if (scrollPane != null) {
            // The scroll pane's size counts any scroll bar it shows, so the content alone decides the size it needs
            Dimension viewPreferred = scrollPane.getViewport().getView().getPreferredSize();
            desiredWidth += viewPreferred.width - scrollPane.getWidth();
            desiredHeight += viewPreferred.height - scrollPane.getHeight();

            int verticalScrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
            int horizontalScrollBarHeight = scrollPane.getHorizontalScrollBar().getPreferredSize().height;

            boolean needsHorizontal = desiredWidth > maximumSize.width;
            boolean needsVertical = desiredHeight > maximumSize.height;

            if (needsHorizontal) {
                desiredHeight += horizontalScrollBarHeight;
            }

            if (needsVertical) {
                desiredWidth += verticalScrollBarWidth;
            }

            // Reserving space for one scroll bar can push the perpendicular axis over, requiring the other as well
            if (!needsVertical && desiredHeight > maximumSize.height) {
                desiredWidth += verticalScrollBarWidth;
            }

            if (!needsHorizontal && desiredWidth > maximumSize.width) {
                desiredHeight += horizontalScrollBarHeight;
            }
        }

        return new Dimension(Math.min(desiredWidth, maximumSize.width), Math.min(desiredHeight, maximumSize.height));
    }

    /**
     * Returns where the rule puts a window of the given size, clamped so it stays fully inside the working area.
     *
     * @param workingArea
     *            - The working area that should contain the window
     * @param startLocation
     *            - The window's top-left before it was fitted
     * @param windowSize
     *            - The window's size
     * @param locationRule
     *            - The rule that decides where the window goes on the display
     *
     * @return The clamped top-left location
     */
    private static Point targetLocation(Rectangle workingArea, Point startLocation, Dimension windowSize,
            LocationRule locationRule) {
        Point preferredLocation = locationRule.locate(workingArea, startLocation, windowSize);
        int maxLeft = workingArea.x + Math.max(0, workingArea.width - windowSize.width);
        int maxTop = workingArea.y + Math.max(0, workingArea.height - windowSize.height);
        int clampedLeft = Math.max(workingArea.x, Math.min(preferredLocation.x, maxLeft));
        int clampedTop = Math.max(workingArea.y, Math.min(preferredLocation.y, maxTop));

        return new Point(clampedLeft, clampedTop);
    }

    /**
     * Returns a rule that reproduces the placement's fractions of the working area's available space, or centers the
     * window when there is no placement.
     *
     * @param placement
     *            - The placement to reproduce, or null to center the window
     *
     * @return The location rule
     */
    private static LocationRule atPlacement(RecordedPlacement placement) {
        double leftFraction = (placement != null) ? placement.leftFraction() : 0.5;
        double topFraction = (placement != null) ? placement.topFraction() : 0.5;

        return (workingArea, startLocation, windowSize) -> new Point(
                (int) Math.round(workingArea.x + leftFraction * (workingArea.width - windowSize.width)),
                (int) Math.round(workingArea.y + topFraction * (workingArea.height - windowSize.height)));
    }

    /**
     * Returns a rule that keeps the window's top-left where it was before it was fitted.
     *
     * @return The location rule
     */
    private static LocationRule atStartLocation() {
        return (workingArea, startLocation, windowSize) -> startLocation;
    }

    /**
     * Returns a rule that centers the window on the given bounds, such as the bounds of the window's owner.
     *
     * @param centerBounds
     *            - The bounds to center the window on
     *
     * @return The location rule
     */
    private static LocationRule centeredOn(Rectangle centerBounds) {
        return (workingArea, startLocation, windowSize) -> new Point(
                (int) Math.round(centerBounds.getCenterX() - windowSize.width / 2.0),
                (int) Math.round(centerBounds.getCenterY() - windowSize.height / 2.0));
    }

    /**
     * Invalidates the window's content up to the window, so the next pack measures whatever content changed. The
     * scrolled view is invalidated when the content scrolls, since its scroll pane caches the size it last measured.
     */
    private void invalidateContent() {
        JScrollPane scrollPane = contentScrollPane();
        Container contentPane = ((RootPaneContainer) window).getContentPane();

        ((scrollPane != null) ? scrollPane.getViewport().getView() : contentPane).invalidate();
    }

    /**
     * Returns the window's content pane when it is a scroll pane, which is how the main frame and the overlay dialogs
     * scroll their content.
     *
     * @return The content scroll pane, or null if the content pane does not scroll
     */
    private JScrollPane contentScrollPane() {
        return ((RootPaneContainer) window).getContentPane() instanceof JScrollPane scrollPane ? scrollPane : null;
    }

    /**
     * Requests a background collection once the window's fit completes after AWT replaced its drawing surface, to
     * reclaim the native back-buffer surfaces AWT orphans with every configuration it gave the window meanwhile.
     */
    private void reclaimReplacedSurfaces() {
        if (!surfaceReplaced) {
            return;
        }

        surfaceReplaced = false;

        // Queued behind the fits already waiting, so the windows of one display change share a single collection
        if (!nativeReclaimQueued) {
            nativeReclaimQueued = true;
            SwingUtilities.invokeLater(FittedWindow::startNativeReclaim);
        }
    }

    /**
     * Starts the background collector that reclaims orphaned native surfaces. A collector already running takes the
     * request instead and collects once more after its current collection, covering surfaces orphaned meanwhile.
     */
    private static void startNativeReclaim() {
        nativeReclaimQueued = false;

        synchronized (NATIVE_RECLAIM_LOCK) {
            nativeReclaimRequested = true;

            if (nativeReclaimRunning) {
                return;
            }

            nativeReclaimRunning = true;
        }

        // Collect off the EDT so a display change never stalls the EDT on the GC
        Thread reclaimThread = new Thread(FittedWindow::collectNativeSurfaces, "DisplayHotKeys-NativeSurfaceReclaim");
        reclaimThread.setDaemon(true);
        reclaimThread.start();
    }

    /**
     * Collects once for each pending reclaim request until none is left. Every request made during a collection shares
     * the one collection that follows it.
     */
    private static void collectNativeSurfaces() {
        while (takeNativeReclaimRequest()) {
            System.gc();
        }
    }

    /**
     * Takes the pending reclaim request. When there is none, the collector is marked stopped in the same step, so the
     * next request starts a new collector rather than being left to one that is ending.
     *
     * @return True if a reclaim request was pending
     */
    private static boolean takeNativeReclaimRequest() {
        synchronized (NATIVE_RECLAIM_LOCK) {
            nativeReclaimRunning = nativeReclaimRequested;
            nativeReclaimRequested = false;

            return nativeReclaimRunning;
        }
    }

    /**
     * Rule that decides where a fitted frame or dialog goes on its display, in the logical pixels of AWT.
     */
    @FunctionalInterface
    private interface LocationRule {

        /**
         * Returns the top-left location for a window of the given size, before it is clamped into the working area.
         *
         * @param workingArea
         *            - The working area of the display the window is fitted to
         * @param startLocation
         *            - The window's top-left before it was fitted
         * @param windowSize
         *            - The window's size
         *
         * @return The top-left location
         */
        Point locate(Rectangle workingArea, Point startLocation, Dimension windowSize);

    }

    /**
     * Placement a frame or dialog recorded, together with the display it was recorded on as that display was then, so a
     * later change to the display can be told apart from a move to another display.
     *
     * @param display
     *            - The display the window was on, with the DPI, bounds, and working area it had at the time
     * @param leftFraction
     *            - The window's left edge as a fraction of the working area's available horizontal space
     * @param topFraction
     *            - The window's top edge as a fraction of the working area's available vertical space
     */
    private record RecordedPlacement(DisplayArea display, double leftFraction, double topFraction) {
    }

}
