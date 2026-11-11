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
package com.dhk.io;

/**
 * Shows a translucent panel in the top-right corner of each given display through the native library. The native layer
 * owns the panels, scales them for the display each one sits on, and hides them on its own once their time is up.
 *
 * @author Jonathan R. Miller
 */
public class DisplayInfoOverlay {

    /**
     * Whether the native library loaded, so showing and hiding quietly do nothing instead of throwing when it is
     * missing.
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
     * Index of the font size of the setting lines in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_FONT_SIZE = 0;

    /**
     * Index of the font size of the header line in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_HEADER_FONT_SIZE = 1;

    /**
     * Index of the space between a panel's edges and its text in the metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_PADDING = 2;

    /**
     * Index of the space between the header line and the first setting line in the metrics handed to {@link #show}.
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
     * Number of metrics handed to {@link #show}.
     */
    public static final int OVERLAY_METRIC_COUNT = 6;

    /**
     * Number of ints per panel in the anchor points handed to {@link #show}, which are the desktop x and y coordinates
     * of a point on the display the panel belongs to.
     */
    public static final int ANCHOR_FIELD_COUNT = 2;

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
     * @param lines
     *            - The setting lines of every panel, panel by panel, with linesPerPanel lines for each header
     * @param linesPerPanel
     *            - The number of setting lines in each panel
     * @param anchorPoints
     *            - The desktop x and y coordinates of a point on each panel's display, in physical pixels
     * @param overlayColors
     *            - The panel colors in packed ARGB order, indexed by the OVERLAY_COLOR constants
     * @param overlayMetrics
     *            - The panel metrics in unscaled pixels, indexed by the OVERLAY_METRIC constants
     * @param fontFamily
     *            - The family of the font the panels are drawn in
     * @param fontFiles
     *            - The font files to register privately for the family, or null when the family is installed or the
     *            fonts were already registered, which lasts for the life of the process
     * @param opacity
     *            - The opacity of the panels, from 0 (invisible) to 255 (opaque)
     * @param durationMs
     *            - How long the panels stay visible, in milliseconds
     *
     * @return Whether the panels were accepted and any supplied fonts are registered
     */
    private native boolean nativeShow(String[] headers, String[] lines, int linesPerPanel, int[] anchorPoints,
            int[] overlayColors, int[] overlayMetrics, String fontFamily, byte[][] fontFiles, int opacity,
            int durationMs);

    /**
     * Defines a JNI function to hide every panel that is showing.
     */
    private native void nativeHide();

    /**
     * Shows one panel per header in the top-right corner of its display, replacing any panels already showing and
     * restarting the time they stay visible. The native layer copies everything, so the panels never call back here.
     *
     * @param headers
     *            - The bold first line of each panel
     * @param lines
     *            - The setting lines of every panel, panel by panel, with linesPerPanel lines for each header
     * @param linesPerPanel
     *            - The number of setting lines in each panel
     * @param anchorPoints
     *            - The desktop x and y coordinates of a point on each panel's display, in physical pixels
     * @param overlayColors
     *            - The panel colors in packed ARGB order, indexed by the OVERLAY_COLOR constants
     * @param overlayMetrics
     *            - The panel metrics in unscaled pixels, indexed by the OVERLAY_METRIC constants
     * @param fontFamily
     *            - The family of the font the panels are drawn in
     * @param fontFiles
     *            - The font files to register privately for the family, or null when the family is installed or the
     *            fonts were already registered, which lasts for the life of the process
     * @param opacity
     *            - The opacity of the panels, from 0 (invisible) to 255 (opaque)
     * @param durationMs
     *            - How long the panels stay visible, in milliseconds
     *
     * @return Whether the panels were accepted and any supplied fonts are registered
     */
    public boolean show(String[] headers, String[] lines, int linesPerPanel, int[] anchorPoints, int[] overlayColors,
            int[] overlayMetrics, String fontFamily, byte[][] fontFiles, int opacity, int durationMs) {
        if (!libraryLoaded) {
            return false;
        }

        return nativeShow(headers, lines, linesPerPanel, anchorPoints, overlayColors, overlayMetrics, fontFamily,
                fontFiles, opacity, durationMs);
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
