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
package com.dhk.io;

import java.awt.Rectangle;
import java.awt.Window;

/**
 * Reads where Windows has frames and dialogs and their displays, in physical pixels, through the WindowPlacement JNI
 * library. AWT's view of a display lags behind a change to it, so these tell when AWT has caught up, and they record
 * placements that a change in progress cannot distort.
 *
 * @author Jonathan R. Miller
 */
public class WindowPlacement {

    /**
     * Whether the native library loaded, so every query answers null and every resync does nothing when it is missing.
     */
    private static boolean libraryLoaded;

    /**
     * Index of the display's DPI in the display areas the native library returns.
     */
    private static final int DISPLAY_DPI_INDEX = 0;

    /**
     * Index of the display bounds' x, y, width, and height, in that order, in the display areas the native library
     * returns.
     */
    private static final int DISPLAY_BOUNDS_INDEX = 1;

    /**
     * Index of the working area's x, y, width, and height, in that order, in the display areas the native library
     * returns.
     */
    private static final int DISPLAY_WORKING_AREA_INDEX = 5;

    /**
     * Number of ints in the display areas the native library returns.
     */
    private static final int DISPLAY_AREA_LENGTH = 9;

    /**
     * Index of the window bounds' x, y, width, and height, in that order, in the window bounds the native library
     * returns.
     */
    private static final int WINDOW_BOUNDS_INDEX = 0;

    /**
     * Index of the flag that is 1 when the window is minimized or maximized, and 0 otherwise, in the window bounds the
     * native library returns.
     */
    private static final int WINDOW_MINIMIZED_OR_MAXIMIZED_INDEX = 4;

    /**
     * Index of the DPI Windows last scaled the window for in the window bounds the native library returns.
     */
    private static final int WINDOW_DPI_INDEX = 5;

    /**
     * Number of ints in the window bounds the native library returns.
     */
    private static final int WINDOW_BOUNDS_LENGTH = 6;

    /**
     * Default constructor for the {@link WindowPlacement} class.
     */
    public WindowPlacement() {
    }

    static {
        try {
            // The library reaches native windows through the JDK's AWT native interface, which must be loaded first
            System.loadLibrary("jawt");
            System.loadLibrary("WindowPlacement");
            libraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Defines a JNI function to get the name of the display a frame or dialog is mostly on.
     *
     * @param window
     *            - The frame or dialog whose display to name
     *
     * @return The GDI device name of the display, or null if the window has no native window
     */
    private static native String getWindowDisplayName(Window window);

    /**
     * Defines a JNI function to get the name of the primary display.
     *
     * @return The GDI device name of the primary display, or null if it could not be read
     */
    private static native String getPrimaryDisplayName();

    /**
     * Defines a JNI function to get the area of the display with the given name.
     *
     * @param displayName
     *            - The GDI device name of the display
     *
     * @return The display's DPI, bounds, and working area, or null if no display with that name is attached
     */
    private static native int[] getDisplayArea(String displayName);

    /**
     * Defines a JNI function to get the bounds of a frame or dialog.
     *
     * @param window
     *            - The frame or dialog whose bounds to get
     *
     * @return The window's bounds, whether it is minimized or maximized, and its DPI, or null without a native window
     */
    private static native int[] getWindowBounds(Window window);

    /**
     * Defines a JNI function to send a frame or dialog the move and size notifications Windows sends after it moves or
     * resizes, for the bounds it already has.
     *
     * @param window
     *            - The frame or dialog to notify
     */
    private static native void resyncWindowBounds(Window window);

    /**
     * Returns the name of the display a frame or dialog is mostly on.
     *
     * @param window
     *            - The frame or dialog whose display to name
     *
     * @return The GDI device name of the display, or null if it could not be read
     */
    public static String displayNameOf(Window window) {
        return libraryLoaded ? getWindowDisplayName(window) : null;
    }

    /**
     * Returns the area of the display a frame or dialog is mostly on.
     *
     * @param window
     *            - The frame or dialog whose display to return
     *
     * @return The display's area, or null if it could not be read
     */
    public static DisplayArea displayOf(Window window) {
        return displayArea(displayNameOf(window));
    }

    /**
     * Returns the area of the primary display.
     *
     * @return The primary display's area, or null if it could not be read
     */
    public static DisplayArea primaryDisplay() {
        return displayArea(libraryLoaded ? getPrimaryDisplayName() : null);
    }

    /**
     * Returns the current area of the display with the given name.
     *
     * @param displayName
     *            - The GDI device name of the display (may be null)
     *
     * @return The display's area, or null if no display with that name is attached
     */
    public static DisplayArea displayArea(String displayName) {
        int[] packedDisplayArea = (libraryLoaded && displayName != null) ? getDisplayArea(displayName) : null;

        if (packedDisplayArea == null || packedDisplayArea.length < DISPLAY_AREA_LENGTH) {
            return null;
        }

        return new DisplayArea(displayName, packedDisplayArea[DISPLAY_DPI_INDEX],
                unpackRectangle(packedDisplayArea, DISPLAY_BOUNDS_INDEX),
                unpackRectangle(packedDisplayArea, DISPLAY_WORKING_AREA_INDEX));
    }

    /**
     * Returns the bounds of a frame or dialog.
     *
     * @param window
     *            - The frame or dialog whose bounds to return
     *
     * @return The window's bounds, or null if they could not be read
     */
    public static WindowBounds windowBounds(Window window) {
        int[] packedWindowBounds = libraryLoaded ? getWindowBounds(window) : null;

        if (packedWindowBounds == null || packedWindowBounds.length < WINDOW_BOUNDS_LENGTH) {
            return null;
        }

        boolean minimizedOrMaximized = packedWindowBounds[WINDOW_MINIMIZED_OR_MAXIMIZED_INDEX] != 0;

        return new WindowBounds(unpackRectangle(packedWindowBounds, WINDOW_BOUNDS_INDEX), minimizedOrMaximized,
                packedWindowBounds[WINDOW_DPI_INDEX]);
    }

    /**
     * Has AWT read the bounds of a frame or dialog again. AWT keeps the bounds it read at the scale it had then, even
     * after the scale changes, and re-sends them with every resize, so stale bounds would move or missize the window.
     *
     * @param window
     *            - The frame or dialog whose bounds AWT reads again
     */
    public static void resyncAwtBounds(Window window) {
        if (libraryLoaded) {
            resyncWindowBounds(window);
        }
    }

    /**
     * Unpacks a rectangle that the native library packed as its x, y, width, and height, in that order.
     *
     * @param packedValues
     *            - The packed ints holding the rectangle
     * @param xIndex
     *            - The index of the rectangle's x among the packed ints
     *
     * @return The rectangle
     */
    private static Rectangle unpackRectangle(int[] packedValues, int xIndex) {
        return new Rectangle(packedValues[xIndex], packedValues[xIndex + 1], packedValues[xIndex + 2],
                packedValues[xIndex + 3]);
    }

    /**
     * Area of a display in physical pixels, together with the name that identifies the display and its DPI.
     *
     * @param name
     *            - The GDI device name of the display, which stays the same when its DPI scale or resolution changes
     * @param dpi
     *            - The display's effective DPI, which is 96 at a 100% scale
     * @param bounds
     *            - The display's bounds on the desktop
     * @param workingArea
     *            - The display's bounds minus the task bar and any other bar docked to it
     */
    public record DisplayArea(String name, int dpi, Rectangle bounds, Rectangle workingArea) {
    }

    /**
     * Bounds of a frame or dialog in physical pixels.
     *
     * @param bounds
     *            - The window's bounds on the desktop, which Windows parks far off screen while it is minimized
     * @param minimizedOrMaximized
     *            - Whether the window is minimized or maximized, so its bounds do not show where it is placed
     * @param dpi
     *            - The DPI Windows last scaled the window for, which reaches a display's new DPI on its own schedule
     */
    public record WindowBounds(Rectangle bounds, boolean minimizedOrMaximized, int dpi) {
    }

}
