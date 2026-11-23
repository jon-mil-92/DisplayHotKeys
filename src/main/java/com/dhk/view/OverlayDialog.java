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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.dhk.utility.WindowUtil;

/**
 * Base for the modal dialogs that darken the main frame while they are open. It builds a scrolling dialog that is
 * fitted and re-fitted to its display's working area like the main frame, closes it exactly once, and rebuilds it
 * inside its window when a re-initialization replaces the controllers it is wired to.
 *
 * @author Jonathan R. Miller
 */
public abstract class OverlayDialog implements IView {

    private DhkView view;
    private String title;
    private Component originalGlassPane;
    private Component darkeningGlassPane;
    private JDialog dialog;
    private WindowAdapter windowListener;
    private Runnable reopenAction;
    private boolean dialogClosed;

    /**
     * Overlay dialog on screen, so a re-initialization can rebuild it inside its window. At most one is open, because
     * the main frame is blocked and the tray hides while a dialog opened from it shows.
     */
    private static OverlayDialog shownDialog;

    /**
     * Overlay dialog a re-initialization is replacing, set only while its reopen action runs, so the dialog that action
     * shows takes over its window instead of opening a new one.
     */
    private static OverlayDialog replacedDialog;

    /**
     * Translucent black painted over the main frame while a dialog is open, which dims the frame without hiding it.
     */
    private static final Color DARKENING_COLOR = new Color(0, 0, 0, 110);

    /**
     * Constructor for the {@link OverlayDialog} class.
     *
     * @param view
     *            - The view for the application
     * @param title
     *            - The title shown in the dialog's title bar
     */
    protected OverlayDialog(DhkView view, String title) {
        this.view = view;
        this.title = title;

        darkeningGlassPane = createDarkeningGlassPane();
    }

    /**
     * Shows the dialog on the AWT event dispatching thread, darkening the main frame until the dialog closes. A
     * re-initialization rebuilds it by showing it again, which suits a dialog that builds its whole content when shown.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    protected void showDialog(Runnable onCloseAction) {
        showDialog(onCloseAction, () -> showDialog(onCloseAction));
    }

    /**
     * Shows the dialog on the AWT event dispatching thread, darkening the main frame until the dialog closes.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     * @param reopenAction
     *            - The action that shows the dialog again, which a re-initialization runs to rebuild it in its window
     */
    protected void showDialog(Runnable onCloseAction, Runnable reopenAction) {
        this.reopenAction = reopenAction;

        // A re-initialization hands over the window of the dialog it replaces, which stays on screen throughout
        if (replacedDialog != null) {
            takeOverWindow(replacedDialog, onCloseAction);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                originalGlassPane = view.getFrame().getGlassPane();

                dialog = new JDialog(view.getFrame(), title, true);
                dialog.setResizable(false);
                buildWindowContent(onCloseAction);
                view.getDefaultFocusComponent().requestFocusInWindow();

                // Pack to the content, then cap to the display's working area and center, as the main frame is fitted
                dialog.pack();
                WindowUtil.fitDialog(dialog, null);
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
     * Rebuilds the overlay dialog on screen inside its window through its reopen action, so its content wires to the
     * controllers a re-initialization just replaced. The window stays on screen, so nothing closes or reopens.
     */
    public static void rebuildShownDialog() {
        if (shownDialog == null) {
            return;
        }

        replacedDialog = shownDialog;
        replacedDialog.releaseWindow();

        // Every reopen action calls showDialog before returning, so its dialog takes over the window handed over here
        replacedDialog.reopenAction.run();
        replacedDialog = null;
    }

    /**
     * Takes over the window of the dialog a re-initialization is replacing and rebuilds this dialog's content in it.
     * The window keeps showing, and the re-initialization re-fits it once every window is rebuilt.
     *
     * @param previousDialog
     *            - The dialog whose window to take over
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void takeOverWindow(OverlayDialog previousDialog, Runnable onCloseAction) {
        dialog = previousDialog.dialog;
        originalGlassPane = previousDialog.originalGlassPane;

        // The title is set again, since a language change rebuilds the dialog in its new language
        dialog.setTitle(title);
        buildWindowContent(onCloseAction);

        getDefaultFocusComponent().requestFocusInWindow();
    }

    /**
     * Detaches this dialog's window listener from its window, which stays on screen for the dialog that takes it over.
     */
    private void releaseWindow() {
        dialog.removeWindowListener(windowListener);
    }

    /**
     * Builds the dialog's scrolling content and window listener in its window, then darkens the main frame behind it.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void buildWindowContent(Runnable onCloseAction) {
        dialogClosed = false;
        shownDialog = this;

        // Let the subclass lay out its components in the scrolling content panel
        JPanel contentPanel = new JPanel();
        WindowUtil.setScrollingContent(dialog, contentPanel, () -> getDefaultFocusComponent().requestFocusInWindow());
        installWindowListener(onCloseAction);
        buildContent(dialog, contentPanel);

        view.getFrame().setGlassPane(darkeningGlassPane);
        darkeningGlassPane.setVisible(true);
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
     * Installs the listener that brings the dialog forward when it opens and closes it exactly once.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void installWindowListener(Runnable onCloseAction) {
        // Prevent default close and handle the title-bar close ourselves
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        windowListener = new WindowAdapter() {
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
        };

        dialog.addWindowListener(windowListener);
    }

    /**
     * Closes the dialog, runs the close action, and removes the darkening from the main frame.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void closeDialog(Runnable onCloseAction) {
        // The title-bar close can land after the dialog has already closed
        if (dialogClosed) {
            return;
        }

        dialogClosed = true;
        shownDialog = null;

        getDefaultFocusComponent().requestFocusInWindow();

        if (onCloseAction != null) {
            onCloseAction.run();
        }

        darkeningGlassPane.setVisible(false);
        view.getFrame().setGlassPane(originalGlassPane);

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
