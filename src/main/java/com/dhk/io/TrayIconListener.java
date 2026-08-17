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

/**
 * Listener for notification area icon interactions delivered from native code. Every method is called on the EDT.
 *
 * @author Jonathan R. Miller
 */
public interface TrayIconListener {

    /**
     * Called when the tray icon is asked to show its menu.
     *
     * @param anchorX
     *            - The x coordinate to anchor the menu to, in physical screen pixels
     * @param anchorY
     *            - The y coordinate to anchor the menu to, in physical screen pixels
     * @param iconBounds
     *            - The bounds of the tray icon, in physical screen pixels
     */
    void menuRequested(int anchorX, int anchorY, Rectangle iconBounds);

    /**
     * Called when the tray icon is activated with a left click.
     */
    void iconActivated();

    /**
     * Called when a showing menu should be dismissed, such as when the icon's window loses the foreground.
     */
    void menuDismissRequested();

}
