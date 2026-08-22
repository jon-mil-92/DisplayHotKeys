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

import java.awt.Rectangle;

import javax.swing.SwingUtilities;

/**
 * Manages the notification area icon through the native library. The native layer owns the icon and reports clicks, and
 * this class forwards them to the registered listener on the EDT so the menu is only ever shown from the EDT.
 *
 * @author Jonathan R. Miller
 */
public class SystemTrayIcon {

    private TrayIconListener trayIconListener;
    private TrayIconRenderer trayIconRenderer;
    private int appliedIconWidth;
    private int appliedIconHeight;

    /**
     * Fallback icon edge (px) used when the native small-icon size is unavailable, matching the Windows default.
     */
    private static final int DEFAULT_ICON_SIZE = 16;

    /**
     * Default constructor for the {@link SystemTrayIcon} class.
     */
    public SystemTrayIcon() {
    }

    static {
        try {
            System.loadLibrary("SystemTrayIcon");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers a listener that will be notified when the native layer reports a tray icon interaction.
     *
     * @param listener
     *            - The listener to notify
     */
    public void registerTrayIconListener(TrayIconListener listener) {
        this.trayIconListener = listener;
    }

    /**
     * Defines a JNI function to start the native notification area icon.
     *
     * @param tooltip
     *            - The tooltip text to show for the icon
     * @param iconPixels
     *            - The icon pixels in packed ARGB order, row by row from the top
     * @param iconWidth
     *            - The icon width in pixels
     * @param iconHeight
     *            - The icon height in pixels
     *
     * @return Whether the icon was registered
     */
    private native boolean nativeStart(String tooltip, int[] iconPixels, int iconWidth, int iconHeight);

    /**
     * Defines a JNI function to stop the native notification area icon.
     */
    private native void nativeStop();

    /**
     * Defines a JNI function to replace the icon shown in the notification area. The replacement is applied on the
     * native thread that owns the icon, so success means the work was handed over rather than finished.
     *
     * @param iconPixels
     *            - The icon pixels in packed ARGB order, row by row from the top
     * @param iconWidth
     *            - The icon width in pixels
     * @param iconHeight
     *            - The icon height in pixels
     *
     * @return Whether the replacement was handed over to the thread that owns the icon
     */
    private native boolean nativeSetIcon(int[] iconPixels, int iconWidth, int iconHeight);

    /**
     * Defines a JNI function to show or hide the icon without releasing the native resources backing it.
     *
     * @param visible
     *            - Whether the icon should be shown
     */
    private native void nativeSetVisible(boolean visible);

    /**
     * Defines a JNI function to get the notification area icon size for the current display scale.
     *
     * @return The icon width and height in pixels, or null when the size is unavailable
     */
    private native int[] nativeGetIconSize();

    /**
     * Starts the notification area icon with the given tooltip and renderer. Must be called after registering a
     * listener, since the native layer resolves the callbacks when it starts.
     *
     * @param tooltip
     *            - The tooltip text to show for the icon
     * @param renderer
     *            - The renderer that draws the icon at the size the notification area asks for
     *
     * @return Whether the icon was registered
     */
    public boolean start(String tooltip, TrayIconRenderer renderer) {
        trayIconRenderer = renderer;

        Rectangle iconSize = resolveIconSize();
        int[] iconPixels = trayIconRenderer.renderIconPixels(iconSize.width, iconSize.height);

        appliedIconWidth = iconSize.width;
        appliedIconHeight = iconSize.height;

        return nativeStart(tooltip, iconPixels, iconSize.width, iconSize.height);
    }

    /**
     * Stops the notification area icon and clears the registered listener.
     */
    public void stop() {
        nativeStop();

        this.trayIconListener = null;
    }

    /**
     * Shows or hides the icon, keeping the native resources alive so it can be shown again cheaply.
     *
     * @param visible
     *            - Whether the icon should be shown
     */
    public void setVisible(boolean visible) {
        nativeSetVisible(visible);
    }

    /**
     * Rescales the icon when the notification area icon size has changed, which happens when the display hosting the
     * task bar changes scale. Rescaling only on a real size change keeps this off the cost of an ordinary refresh, and
     * the new size counts as applied only once the native layer accepts it.
     */
    public void refreshIconSize() {
        if (trayIconRenderer == null) {
            return;
        }

        Rectangle iconSize = resolveIconSize();

        if (iconSize.width == appliedIconWidth && iconSize.height == appliedIconHeight) {
            return;
        }

        int[] iconPixels = trayIconRenderer.renderIconPixels(iconSize.width, iconSize.height);

        // Leaving the applied size untouched on failure lets the next refresh retry instead of skipping the rescale
        if (!nativeSetIcon(iconPixels, iconSize.width, iconSize.height)) {
            return;
        }

        appliedIconWidth = iconSize.width;
        appliedIconHeight = iconSize.height;
    }

    /**
     * Resolves the notification area icon size, falling back to the Windows default when the native size is
     * unavailable.
     *
     * @return The icon size, with the width and height in the bounds' width and height
     */
    private Rectangle resolveIconSize() {
        int[] nativeSize = nativeGetIconSize();

        if (nativeSize == null || nativeSize.length < 2 || nativeSize[0] <= 0 || nativeSize[1] <= 0) {
            return new Rectangle(0, 0, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE);
        }

        return new Rectangle(0, 0, nativeSize[0], nativeSize[1]);
    }

    /**
     * Called from native code when the notification area icon is asked to show its menu.
     *
     * @param anchorX
     *            - The x coordinate to anchor the menu to, in physical screen pixels
     * @param anchorY
     *            - The y coordinate to anchor the menu to, in physical screen pixels
     * @param iconLeft
     *            - The left edge of the icon, in physical screen pixels
     * @param iconTop
     *            - The top edge of the icon, in physical screen pixels
     * @param iconRight
     *            - The right edge of the icon, in physical screen pixels
     * @param iconBottom
     *            - The bottom edge of the icon, in physical screen pixels
     */
    private void onMenuRequested(int anchorX, int anchorY, int iconLeft, int iconTop, int iconRight, int iconBottom) {
        final TrayIconListener listener = this.trayIconListener;

        if (listener != null) {
            Rectangle iconBounds = new Rectangle(iconLeft, iconTop, iconRight - iconLeft, iconBottom - iconTop);

            // Forward to the EDT so the menu is only ever shown from the EDT
            SwingUtilities.invokeLater(() -> listener.menuRequested(anchorX, anchorY, iconBounds));
        }
    }

}
