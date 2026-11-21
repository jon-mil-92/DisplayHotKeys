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

/**
 * Shows a translucent panel at the chosen position on each given display through the native library. The native layer
 * owns the panels, scales them for the display each one sits on, and hides them on its own once their time is up.
 *
 * @author Jonathan R. Miller
 */
public class DisplayInfoOverlay {

    /**
     * Whether the native library loaded, so showing and hiding do nothing instead of throwing when it is missing.
     */
    private static boolean libraryLoaded;

    /**
     * Index of the panel background color in the colors handed to {@link #show}.
     */
    public static final int OVERLAY_COLOR_BACKGROUND = 0;

    /**
     * Index of the text color in the colors handed to {@link #show}.
     */
    public static final int OVERLAY_COLOR_FOREGROUND = 1;

    /**
     * Index of the panel border color in the colors handed to {@link #show}.
     */
    public static final int OVERLAY_COLOR_BORDER = 2;

    /**
     * Number of colors handed to {@link #show}.
     */
    public static final int OVERLAY_COLOR_COUNT = 3;

    /**
     * Index of the setting line font size in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_FONT_SIZE = 0;

    /**
     * Index of the header line font size in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_HEADER_FONT_SIZE = 1;

    /**
     * Index of the space between a panel's edges and its text in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_PADDING = 2;

    /**
     * Index of the space on each side of the separator beneath the header line in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_HEADER_GAP = 3;

    /**
     * Index of the space between consecutive setting lines in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_LINE_GAP = 4;

    /**
     * Index of the space between a panel and the edges of its display's work area in the metrics handed to
     * {@link #show}.
     */
    public static final int OVERLAY_METRIC_MARGIN = 5;

    /**
     * Index of the space between the label column and the value column in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_COLUMN_GAP = 6;

    /**
     * Number of metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_COUNT = 7;

    /**
     * Number of ints per panel in the anchor points handed to {@link #show}, which are the desktop x and y of a point
     * on the panel's display.
     */
    public static final int ANCHOR_FIELD_COUNT = 2;

    /**
     * Position handed to {@link #show} that places the panels in the top-right corner of each display, which is the
     * default position.
     */
    public static final int OVERLAY_POSITION_TOP_RIGHT = 1;

    /**
     * Number of positions handed to {@link #show}, which are top left, top right, center, bottom left, and bottom right
     * in that order.
     */
    public static final int OVERLAY_POSITION_COUNT = 5;

    /**
     * Opacity handed to {@link #show} at which the panels are fully opaque, while an opacity of 0 makes them invisible.
     */
    public static final int MAX_OVERLAY_OPACITY = 255;

    /**
     * Default constructor for the {@link DisplayInfoOverlay} class.
     */
    public DisplayInfoOverlay() {
    }

    static {
        try {
            System.loadLibrary("DisplayInfoOverlay");
            libraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Defines a JNI function to show one panel per header, replacing any panels already showing.
     *
     * @param headers
     *            - The bold first line of each panel
     * @param labels
     *            - The label of each setting line, shared by every panel, which needs at least one label
     * @param values
     *            - The value of each setting line, panel by panel, with one value per label for each header
     * @param anchorPoints
     *            - The desktop x and y coordinates of a point on each panel's display, in physical pixels
     * @param position
     *            - Where the panels sit on each display, below {@link #OVERLAY_POSITION_COUNT}
     * @param overlayColors
     *            - The panel colors in packed ARGB order, indexed by the OVERLAY_COLOR constants
     * @param overlayMetrics
     *            - The panel metrics in unscaled, possibly fractional pixels, indexed by the OVERLAY_METRIC constants
     * @param fontFamily
     *            - The family of the installed font the panels are drawn in
     * @param opacity
     *            - The opacity of the panels, from 0 (invisible) to 255 (opaque)
     * @param durationMs
     *            - How long the panels stay visible, in milliseconds
     *
     * @return Whether the panels were accepted
     */
    private native boolean nativeShow(String[] headers, String[] labels, String[] values, int[] anchorPoints,
            int position, int[] overlayColors, float[] overlayMetrics, String fontFamily, int opacity, int durationMs);

    /**
     * Defines a JNI function to hide every panel that is showing.
     */
    private native void nativeHide();

    /**
     * Shows one panel per header at the given position on its display, replacing any panels already showing and
     * restarting the time they stay visible. The native layer copies everything, so the panels never call back here.
     *
     * @param headers
     *            - The bold first line of each panel
     * @param labels
     *            - The label of each setting line, shared by every panel, which needs at least one label
     * @param values
     *            - The value of each setting line, panel by panel, with one value per label for each header
     * @param anchorPoints
     *            - The desktop x and y coordinates of a point on each panel's display, in physical pixels
     * @param position
     *            - Where the panels sit on each display, below {@link #OVERLAY_POSITION_COUNT}
     * @param overlayColors
     *            - The panel colors in packed ARGB order, indexed by the OVERLAY_COLOR constants
     * @param overlayMetrics
     *            - The panel metrics in unscaled, possibly fractional pixels, indexed by the OVERLAY_METRIC constants
     * @param fontFamily
     *            - The family of the installed font the panels are drawn in
     * @param opacity
     *            - The opacity of the panels, from 0 (invisible) to 255 (opaque)
     * @param durationMs
     *            - How long the panels stay visible, in milliseconds
     *
     * @return Whether the panels were accepted
     */
    public boolean show(String[] headers, String[] labels, String[] values, int[] anchorPoints, int position,
            int[] overlayColors, float[] overlayMetrics, String fontFamily, int opacity, int durationMs) {
        if (!libraryLoaded) {
            return false;
        }

        return nativeShow(headers, labels, values, anchorPoints, position, overlayColors, overlayMetrics, fontFamily,
                opacity, durationMs);
    }

    /**
     * Hides every panel that is showing, without waiting for the panels to finish closing.
     */
    public void hide() {
        if (!libraryLoaded) {
            return;
        }

        nativeHide();
    }

}
