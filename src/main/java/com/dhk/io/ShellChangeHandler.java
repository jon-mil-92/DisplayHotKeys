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

import com.dhk.utility.WindowUtil;

/**
 * Re-fits the application's windows as soon as the native layer reports a Windows shell restart or a working area
 * change. Both can leave already-shown windows with stray as-needed scroll bars, clipped content, or a part under the
 * task bar, so this re-fits in place rather than triggering a full re-initialization.
 *
 * @author Jonathan R. Miller
 */
public class ShellChangeHandler implements ShellRestartListener, WorkingAreaChangeListener {

    /**
     * Default constructor for the {@link ShellChangeHandler} class.
     */
    public ShellChangeHandler() {
    }

    @Override
    public void shellRestarted() {
        refitAllWindows(false);
    }

    @Override
    public void workingAreaChanged() {
        refitAllWindows(true);
    }

    /**
     * Re-fits every currently showing window. The main frame and the dialogs fitted like it re-fit through the
     * working-area-aware re-fit, so they keep their placement on the changed working area and their scroll bars settle.
     * Other windows are re-laid-out and fixed-size ones re-packed.
     *
     * @param workingAreaChange
     *            - Whether only a working area changed, which leaves a fitted window on an unchanged display as it is
     */
    private void refitAllWindows(boolean workingAreaChange) {
        for (Window window : Window.getWindows()) {
            if (!window.isShowing()) {
                continue;
            }

            if (!WindowUtil.isFitted(window)) {
                refitWindow(window);
            } else if (workingAreaChange) {
                WindowUtil.refitWindowForWorkingArea(window);
            } else {
                // A shell restart can leave stray scroll bars, so every fitted window is laid out again
                WindowUtil.refitWindow(window);
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
