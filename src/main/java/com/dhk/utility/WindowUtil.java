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

import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import com.dhk.io.NativeInputHook;
import com.dhk.io.NativeMouseListener;

/**
 * Provides placement and fitting utilities for the main frame and the dialogs across displays. Each is capped to its
 * display's working area and keeps its placement through any display change, which a {@link FittedWindow} does for it.
 *
 * @author Jonathan R. Miller
 */
public class WindowUtil {

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
     * Caps a newly built and packed frame to the primary display's working area and centers it there, then once more
     * when it is on screen. The frame records its placement from then on.
     *
     * @param frame
     *            - The packed frame to fit and center
     */
    public static void fitFrame(JFrame frame) {
        FittedWindow.fitNewWindow(frame, null);
    }

    /**
     * Caps a packed dialog to the working area of its owner's display and centers it on its owner, or on that display
     * when the owner is not on screen, then once more when it is on screen. The dialog records its placement from then
     * on.
     *
     * @param dialog
     *            - The packed dialog to fit and center
     * @param fittedAction
     *            - The action to run after each fit that places the dialog (may be null)
     */
    public static void fitDialog(JDialog dialog, Runnable fittedAction) {
        FittedWindow.fitNewWindow(dialog, fittedAction);
    }

    /**
     * Re-packs a fitted frame or dialog after its content or its display changes and re-fits it to the working area of
     * the display it occupies, keeping its location unless that would leave part of it off screen. After any change to
     * its display it returns to the placement it recorded before the change.
     *
     * @param window
     *            - The fitted frame or dialog to re-fit
     */
    public static void refitWindow(Window window) {
        FittedWindow fittedWindow = FittedWindow.of(window);

        if (fittedWindow != null) {
            fittedWindow.requestRefit();
        }
    }

    /**
     * Re-fits every frame and dialog a re-initialization rebuilt in place. Each keeps its placement even when its size
     * changed, and returns to its recorded placement when its display changed.
     */
    public static void refitRebuiltWindows() {
        for (Window window : Window.getWindows()) {
            FittedWindow fittedWindow = FittedWindow.of(window);

            if (fittedWindow != null) {
                fittedWindow.requestRebuiltRefit();
            }
        }
    }

    /**
     * Follows the left mouse button through the native input hook for the rest of the application, so a fit a window
     * owes waits while a drag holds the button and runs once the button is released.
     *
     * @param nativeInputHook
     *            - The shared native input hook that reports the real left-button press and release
     */
    public static void trackLeftMouseButton(NativeInputHook nativeInputHook) {
        nativeInputHook.addMouseListener(new NativeMouseListener() {
            @Override
            public void mousePressed() {
                SwingUtilities.invokeLater(FittedWindow::leftMouseButtonPressed);
            }

            @Override
            public void mouseReleased() {
                SwingUtilities.invokeLater(FittedWindow::leftMouseButtonReleased);
            }
        });
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

        /*
         * Each part consumes the presses that land on it, so the scroll pane, its viewport, and the panel all listen.
         * None of them outlives the content, so no listener piles up on a window whose content is rebuilt in place
         */
        scrollPane.addMouseListener(emptySpacePressListener);
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
     * @return True if the window was fitted by this class, otherwise false
     */
    public static boolean isFittedToWorkingArea(Window window) {
        return FittedWindow.of(window) != null;
    }

}
