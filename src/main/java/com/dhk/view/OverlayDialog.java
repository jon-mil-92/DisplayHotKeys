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
package com.dhk.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.dhk.io.NativeInputHook;
import com.dhk.utility.WindowDragRefitter;
import com.dhk.utility.WindowUtil;

/**
 * Base for the modal dialogs that darken the main frame while they are open. It builds a scrolling dialog fitted to its
 * display's working area like the main frame, re-fits it after a cross-display drag, and closes it exactly once.
 *
 * @author Jonathan R. Miller
 */
public abstract class OverlayDialog implements IView {

    private DhkView view;
    private NativeInputHook nativeInputHook;
    private String title;
    private JFrame parentFrame;
    private Component originalGlassPane;
    private Component darkeningGlassPane;
    private JDialog dialog;
    private boolean dialogClosed;

    /**
     * Translucent black painted over the main frame while a dialog is open, which dims the frame without hiding it.
     */
    private static final Color DARKENING_COLOR = new Color(0, 0, 0, 110);

    /**
     * Constructor for the {@link OverlayDialog} class.
     *
     * @param view
     *            - The view for the application
     * @param nativeInputHook
     *            - The shared native input hook used to re-fit the dialog after a cross-display drag (may be null)
     * @param title
     *            - The title shown in the dialog's title bar
     */
    protected OverlayDialog(DhkView view, NativeInputHook nativeInputHook, String title) {
        this.view = view;
        this.nativeInputHook = nativeInputHook;
        this.title = title;
        this.parentFrame = view.getFrame();

        originalGlassPane = parentFrame.getGlassPane();
        darkeningGlassPane = createDarkeningGlassPane();
    }

    /**
     * Shows the dialog on the AWT event dispatching thread, darkening the main frame until the dialog closes.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    protected void showDialog(Runnable onCloseAction) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the dialog with scrolling content, then let the subclass lay out its components in it
                dialog = new JDialog(parentFrame, title, true);
                dialog.setResizable(false);

                JPanel contentPanel = new JPanel();
                WindowUtil.setScrollingContent(dialog, contentPanel,
                        () -> getDefaultFocusComponent().requestFocusInWindow());
                installWindowListeners(onCloseAction);
                buildContent(dialog, contentPanel);

                view.getDefaultFocusComponent().requestFocusInWindow();
                parentFrame.setGlassPane(darkeningGlassPane);
                darkeningGlassPane.setVisible(true);

                beforeShow();

                // Pack to the content, then cap to the display's working area and center, as the main frame is fitted
                dialog.pack();
                WindowUtil.fitDialogToWorkingArea(dialog);

                // A drag onto another display refreshes the dialog there once the drag ends, matching the main frame
                new WindowDragRefitter(dialog, nativeInputHook, () -> WindowUtil.refreshWindow(dialog)).install();
                dialog.setVisible(true);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });
    }

    /**
     * Re-fits a showing dialog to its changed content within its display's working area. The dialog keeps its location,
     * so a dialog the user moved is not pulled back.
     */
    protected void refitShowingDialog() {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        WindowUtil.refitWindow(dialog);
    }

    /**
     * Gets the shared native input hook, so a dialog this one opens can re-fit after a cross-display drag as well.
     *
     * @return The shared native input hook, or null if it could not be installed
     */
    protected NativeInputHook getNativeInputHook() {
        return nativeInputHook;
    }

    /**
     * Lays out the dialog's components in its scrolling content panel and attaches any listeners that need the dialog.
     *
     * @param dialog
     *            - The dialog being built
     * @param contentPanel
     *            - The scrolling panel to lay the components out in
     */
    protected abstract void buildContent(JDialog dialog, JPanel contentPanel);

    /**
     * Determines whether a title-bar close may close the dialog right now. Every close is allowed unless a subclass
     * overrides this.
     *
     * @return True if the dialog may close, otherwise false
     */
    protected boolean canClose() {
        return true;
    }

    /**
     * Runs just before the dialog is packed and shown. Does nothing unless a subclass overrides it.
     */
    protected void beforeShow() {
    }

    /**
     * Installs the listeners that bring the dialog forward when it opens and close it exactly once.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void installWindowListeners(Runnable onCloseAction) {
        // Prevent default close and handle the title-bar close ourselves
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            // Take the foreground when opened from the tray, since the hidden owner cannot bring the dialog forward
            @Override
            public void windowOpened(WindowEvent e) {
                dialog.toFront();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                // A subclass can refuse the title-bar close, such as while a hot key is being recorded
                if (!canClose()) {
                    return;
                }

                closeDialog(onCloseAction);
            }

            // A re-initialization disposes the parent frame, destroying this dialog without any close request
            @Override
            public void windowClosed(WindowEvent e) {
                closeDialog(onCloseAction);
            }
        });
    }

    /**
     * Closes the dialog, runs the close action, and removes the darkening from the main frame.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void closeDialog(Runnable onCloseAction) {
        // Disposing fires the close path again, and the title-bar close can land after it has already run
        if (dialogClosed) {
            return;
        }

        dialogClosed = true;

        getDefaultFocusComponent().requestFocusInWindow();

        if (onCloseAction != null) {
            onCloseAction.run();
        }

        darkeningGlassPane.setVisible(false);
        parentFrame.setGlassPane(originalGlassPane);

        dialog.dispose();
    }

    /**
     * Creates a semi-transparent darkening panel to overlay the parent frame.
     *
     * @return A semi-transparent darkening panel to overlay the parent frame
     */
    private Component createDarkeningGlassPane() {
        JPanel darkeningPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(DARKENING_COLOR);
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        darkeningPanel.setOpaque(false);
        darkeningPanel.addMouseListener(new MouseAdapter() {
        });

        return darkeningPanel;
    }

}
