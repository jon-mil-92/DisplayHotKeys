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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dhk.utility.FrameUtil;
import com.dhk.view.DhkView;

/**
 * Re-fits the application's windows when the native layer reports a Windows shell restart. A restart rebroadcasts theme
 * and working-area changes that leave already-shown windows with stray as-needed scroll bars or clipped content, so
 * this re-fits in place rather than triggering a full re-initialization.
 *
 * @author Jonathan R. Miller
 */
public class ShellRestartHandler implements ShellRestartListener {

    private final DhkView view;
    private final Timer refitTimer;

    /**
     * Constructor for the {@link ShellRestartHandler} class.
     *
     * @param view
     *            - The view for the application
     */
    public ShellRestartHandler(DhkView view) {
        this.view = view;
        refitTimer = new Timer(FrameUtil.REFRESH_DELAY_MS, e -> refitAllWindows());
        refitTimer.setRepeats(false);
    }

    @Override
    public void shellRestarted() {
        refitTimer.restart();
    }

    /**
     * Stops any pending deferred re-fit. Called when the owning controller is torn down (on app re-init or shutdown) so
     * the Timer cannot fire against a disposed view and is released for garbage collection.
     */
    public void cleanUp() {
        refitTimer.stop();
    }

    /**
     * Re-fits every currently showing window. The main frame fits through the scroll-pane-aware refresh so its
     * as-needed scroll bars settle; other windows are re-laid-out and fixed-size ones re-packed.
     */
    private void refitAllWindows() {
        for (Window window : Window.getWindows()) {
            if (!window.isShowing()) {
                continue;
            }

            if (window == view.getFrame()) {
                FrameUtil.refreshFrame(view.getFrame());
            } else {
                refitWindow(window);
            }
        }
    }

    /**
     * Re-lays-out a single window, re-packing it only when it is fixed-size and re-centering an owned dialog so it
     * stays over its owner after the pack.
     *
     * @param window
     *            - The window to re-fit
     */
    private void refitWindow(Window window) {
        SwingUtilities.updateComponentTreeUI(window);
        window.revalidate();

        // Re-pack only fixed-size windows, since a user may have deliberately resized a resizable one
        if (!isResizable(window)) {
            window.pack();
        }

        if (window instanceof Dialog && window.getOwner() != null) {
            window.setLocationRelativeTo(window.getOwner());
        }

        window.validate();
        window.repaint();
    }

    /**
     * Reports whether a window can be resized by the user, treating non-frame, non-dialog windows as resizable so they
     * are not re-packed.
     *
     * @param window
     *            - The window to test
     *
     * @return Whether the window is resizable
     */
    private boolean isResizable(Window window) {
        if (window instanceof Frame) {
            return ((Frame) window).isResizable();
        }

        if (window instanceof Dialog) {
            return ((Dialog) window).isResizable();
        }

        return true;
    }

}
