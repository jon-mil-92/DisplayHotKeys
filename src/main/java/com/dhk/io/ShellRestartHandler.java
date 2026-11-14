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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dhk.utility.WindowUtil;
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
        refitTimer = new Timer(WindowUtil.REFRESH_DELAY_MS, e -> refitAllWindows());
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
     * Re-fits every currently showing window. The main frame and the dialogs fitted like it refresh through the
     * working-area-aware refresh, so they stay capped to their display and their scroll bars settle. Other windows are
     * re-laid-out and fixed-size ones re-packed.
     */
    private void refitAllWindows() {
        for (Window window : Window.getWindows()) {
            if (!window.isShowing()) {
                continue;
            }

            if (window == view.getFrame()) {
                WindowUtil.refreshWindow(view.getFrame());
            } else if (WindowUtil.isFittedToWorkingArea(window)) {
                // A fitted dialog keeps its working-area cap, then re-centers over its owner as other dialogs do
                WindowUtil.refreshWindow(window);
                window.setLocationRelativeTo(window.getOwner());
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
