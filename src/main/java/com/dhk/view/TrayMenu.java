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
package com.dhk.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.ui.FlatNativeWindowsLibrary;
import com.formdev.flatlaf.util.SystemInfo;

/**
 * Shows the tray icon's menu in an undecorated window of its own. The window and menu are built for each show and
 * discarded afterward, so neither can hold display geometry or theme state that a later change would invalidate.
 *
 * @author Jonathan R. Miller
 */
public class TrayMenu {

    private JPopupMenu popupMenu;
    private JDialog menuDialog;
    private Runnable restoreAction;
    private Runnable aboutAction;
    private Runnable exitAction;
    private boolean showInFlight;

    /**
     * Client property FlatLaf checks before painting a pop-up menu's line border, set when the window's native border
     * draws the line instead so the two never double up.
     */
    private static final String NATIVE_BORDER_KEY = "FlatLaf.internal.FlatPopupFactory.popupUsesNativeBorder";

    /**
     * Constructor for the {@link TrayMenu} class.
     *
     * @param restoreAction
     *            - The action to run when the restore item is selected
     * @param aboutAction
     *            - The action to run when the about item is selected
     * @param exitAction
     *            - The action to run when the exit item is selected
     */
    public TrayMenu(Runnable restoreAction, Runnable aboutAction, Runnable exitAction) {
        this.restoreAction = restoreAction;
        this.aboutAction = aboutAction;
        this.exitAction = exitAction;
    }

    /**
     * Shows the menu for a tray icon at the given physical screen position. A request arriving while a show has not
     * finished is dropped rather than queued, since each queued request would leave a window of its own.
     *
     * @param anchorX
     *            - The x coordinate to anchor the menu to, in physical screen pixels
     * @param anchorY
     *            - The y coordinate to anchor the menu to, in physical screen pixels
     */
    public void show(int anchorX, int anchorY) {
        if (showInFlight) {
            return;
        }

        showInFlight = true;

        try {
            showMenu(anchorX, anchorY);
        } finally {
            showInFlight = false;
        }
    }

    /**
     * Builds the menu and its window and shows them at the given physical screen position. Any menu already showing is
     * dismissed first, so a second request can never leave two menus behind.
     *
     * @param anchorX
     *            - The x coordinate to anchor the menu to, in physical screen pixels
     * @param anchorY
     *            - The y coordinate to anchor the menu to, in physical screen pixels
     */
    private void showMenu(int anchorX, int anchorY) {
        dismiss();

        GraphicsConfiguration configuration = configurationAt(anchorX, anchorY);

        if (configuration == null) {
            return;
        }

        // Built per show so the items pick up the current theme, which a menu outside every window's tree would miss
        popupMenu = buildPopupMenu();
        menuDialog = buildMenuDialog(popupMenu, configuration);

        retireOnDismissal(popupMenu, menuDialog);

        /*
         * The size comes from the packed window rather than the menu's preferred size, which a pop-up reports
         * differently before it has a parent, so the menu would be placed against a stale width
         */
        Point location = menuLocation(anchorX, anchorY, menuDialog.getSize(), configuration);

        menuDialog.setLocation(location);
        applyNativeBorder(popupMenu, menuDialog);

        mapMenuDialog(menuDialog, location);

        /*
         * Selecting the menu is what a pop-up's own show would have done, and it is what arms and highlights an item
         * under the pointer. Unlike showing, it needs no on-screen location, so it can wait until the window is mapped
         */
        MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{popupMenu});

        confirmFocus(popupMenu, menuDialog, true);
    }

    /**
     * Builds the undecorated window that carries the menu, already packed around it and ready to be placed. A dialog,
     * not a window: an ownerless window is never focusable, so this process could never own the foreground, and the
     * menu would then receive neither the click nor the key press that dismisses it.
     *
     * @param menu
     *            - The menu the window is built around
     * @param configuration
     *            - The configuration of the display the window will be shown on
     *
     * @return The packed window holding the menu
     */
    private JDialog buildMenuDialog(JPopupMenu menu, GraphicsConfiguration configuration) {
        JDialog dialog = new JDialog((Frame) null, "", false, configuration);
        dialog.setUndecorated(true);
        dialog.setType(Window.Type.UTILITY);
        dialog.setAlwaysOnTop(true);
        dialog.setBackground(menu.getBackground());
        dialog.getContentPane().setBackground(menu.getBackground());

        /*
         * Held as an ordinary child of the window, so the menu is painted without a pop-up window of its own. Mapping
         * an empty window first would leave its bare outline on screen for as long as the window manager takes to
         * answer, which under load is long enough to see
         */
        menu.setVisible(true);
        dialog.getContentPane().add(menu);

        // Sized and laid out while still off screen, so the window's first frame on screen already carries the menu
        dialog.pack();

        return dialog;
    }

    /**
     * Maps the window on screen at the placed location, correcting the location afterward because showing on a display
     * of another scale can move the window off the position it was given.
     *
     * @param dialog
     *            - The window to map
     * @param location
     *            - The location the window was placed at
     */
    private void mapMenuDialog(JDialog dialog, Point location) {
        dialog.setVisible(true);
        dialog.toFront();

        if (!dialog.getLocation().equals(location)) {
            dialog.setLocation(location);
        }
    }

    /**
     * Dismisses the menu when it is showing, so a display change never leaves a menu placed against the old geometry.
     */
    public void dismiss() {
        if (popupMenu != null && menuDialog != null) {
            retire(popupMenu, menuDialog);
        }
    }

    /**
     * Builds the menu with the items for the tray icon. Building it per show keeps it on the current theme without
     * having to update it from every path that changes the theme.
     *
     * @return The built menu
     */
    private JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new EmbeddedPopupMenu();

        addMenuItem(menu, "Restore", restoreAction);
        addMenuItem(menu, "About", aboutAction);
        addMenuItem(menu, "Exit", exitAction);

        return menu;
    }

    /**
     * Adds an item that runs the given action to the menu.
     *
     * @param menu
     *            - The menu to add the item to
     * @param itemText
     *            - The text the item shows
     * @param itemAction
     *            - The action to run when the item is selected
     */
    private void addMenuItem(JPopupMenu menu, String itemText, Runnable itemAction) {
        JMenuItem menuItem = new JMenuItem(itemText);

        menuItem.addActionListener(actionEvent -> runMenuAction(itemAction));
        menu.add(menuItem);
    }

    /**
     * Retires the menu and its window on every way out: the window losing the focus that an outside click takes, or the
     * escape key. Neither relies on a mouse grab, so a stalled grab can never strand the menu.
     *
     * @param menu
     *            - The menu to retire the window for
     * @param dialog
     *            - The window the menu is showing in
     */
    private void retireOnDismissal(JPopupMenu menu, JDialog dialog) {
        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent windowEvent) {
                retire(menu, dialog);
            }
        });

        dialog.getRootPane().registerKeyboardAction(actionEvent -> retire(menu, dialog),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Confirms on a later event that the window took the focus, retrying once. A menu whose process was denied the
     * foreground can never be dismissed from outside, so it is retired rather than left up.
     *
     * @param menu
     *            - The menu that was shown
     * @param dialog
     *            - The window the menu is showing in
     * @param retry
     *            - Whether to bring the window to the front once more before giving up
     */
    private void confirmFocus(JPopupMenu menu, JDialog dialog, boolean retry) {
        SwingUtilities.invokeLater(() -> {
            if (menu != popupMenu || dialog.isFocused()) {
                return;
            }

            if (retry) {
                dialog.toFront();
                confirmFocus(menu, dialog, false);
            } else {
                retire(menu, dialog);
            }
        });
    }

    /**
     * Retires the menu and disposes its window on a later event, so any event still being delivered to the menu lands
     * on a window that has not yet lost its peer. Clearing the selection path releases any item armed by the pointer.
     *
     * @param menu
     *            - The menu to retire
     * @param dialog
     *            - The window the menu is showing in
     */
    private void retire(JPopupMenu menu, JDialog dialog) {
        // Only clear the fields when they still belong to this menu, since a newer show may already own them
        if (menu == popupMenu) {
            popupMenu = null;
            menuDialog = null;
        }

        SwingUtilities.invokeLater(() -> dialog.dispose());

        MenuSelectionManager.defaultManager().clearSelectedPath();
    }

    /**
     * Dismisses the menu, then runs the item's action on a later event so it never runs while the window is still
     * tearing down.
     *
     * @param menuAction
     *            - The action to run
     */
    private void runMenuAction(Runnable menuAction) {
        dismiss();

        if (menuAction != null) {
            SwingUtilities.invokeLater(menuAction);
        }
    }

    /**
     * Gives the window the rounded corners and line border Windows 11 draws for pop-up menus, matching the look FlatLaf
     * gives a menu in a window of its own. The menu's own line is suppressed so the border is drawn only once.
     *
     * @param menu
     *            - The menu whose border color the window border takes
     * @param dialog
     *            - The window showing the menu
     */
    private void applyNativeBorder(JPopupMenu menu, JDialog dialog) {
        if (!SystemInfo.isWindows_11_orLater || !FlatNativeWindowsLibrary.isLoaded()) {
            return;
        }

        long windowHandle = FlatNativeWindowsLibrary.getHWND(dialog);

        if (windowHandle == 0) {
            return;
        }

        Border border = menu.getBorder();
        Color borderColor = border instanceof FlatLineBorder ? ((FlatLineBorder) border).getLineColor() : null;

        menu.putClientProperty(NATIVE_BORDER_KEY, Boolean.TRUE);
        FlatNativeWindowsLibrary.setWindowCornerPreference(windowHandle, FlatNativeWindowsLibrary.DWMWCP_ROUNDSMALL);
        FlatNativeWindowsLibrary.dwmSetWindowAttributeCOLORREF(windowHandle,
                FlatNativeWindowsLibrary.DWMWA_BORDER_COLOR, borderColor);
    }

    /**
     * Finds the configuration of the display containing the given physical screen position. Each configuration's bounds
     * are scaled to physical pixels before the test, since a physical position does not fall inside the logical bounds
     * of a display that is scaled.
     *
     * @param screenX
     *            - The x coordinate in physical screen pixels
     * @param screenY
     *            - The y coordinate in physical screen pixels
     *
     * @return The configuration of the display containing the position, or null if there are no displays
     */
    private GraphicsConfiguration configurationAt(int screenX, int screenY) {
        GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        GraphicsConfiguration nearestConfiguration = null;
        long nearestDistance = Long.MAX_VALUE;

        for (GraphicsDevice screenDevice : screenDevices) {
            GraphicsConfiguration configuration = screenDevice.getDefaultConfiguration();
            Rectangle bounds = configuration.getBounds();
            AffineTransform transform = configuration.getDefaultTransform();

            /*
             * The extent is scaled to physical pixels for the test, since a physical position does not fall inside the
             * logical bounds of a scaled display, while the origin is already physical
             */
            int physicalWidth = (int) Math.round(bounds.width * transform.getScaleX());
            int physicalHeight = (int) Math.round(bounds.height * transform.getScaleY());

            if (screenX >= bounds.x && screenX < bounds.x + physicalWidth && screenY >= bounds.y
                    && screenY < bounds.y + physicalHeight) {
                return configuration;
            }

            // A position can miss every display by a pixel of scaling rounding, so fall back to the closest one
            long distance = squaredDistanceTo(bounds.x, bounds.y, physicalWidth, physicalHeight, screenX, screenY);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestConfiguration = configuration;
            }
        }

        return nearestConfiguration;
    }

    /**
     * Returns the squared distance from a position to the nearest point of the given bounds, which is zero when the
     * bounds contain it. Squared distance is compared directly, since only the ordering matters.
     *
     * @param boundsX
     *            - The x origin of the bounds to measure to
     * @param boundsY
     *            - The y origin of the bounds to measure to
     * @param boundsWidth
     *            - The width of the bounds to measure to
     * @param boundsHeight
     *            - The height of the bounds to measure to
     * @param screenX
     *            - The x coordinate in physical screen pixels
     * @param screenY
     *            - The y coordinate in physical screen pixels
     *
     * @return The squared distance to the bounds
     */
    private long squaredDistanceTo(int boundsX, int boundsY, int boundsWidth, int boundsHeight, int screenX,
            int screenY) {
        long clampedX = Math.max(boundsX, Math.min(screenX, boundsX + boundsWidth - 1));
        long clampedY = Math.max(boundsY, Math.min(screenY, boundsY + boundsHeight - 1));
        long deltaX = screenX - clampedX;
        long deltaY = screenY - clampedY;

        return (deltaX * deltaX) + (deltaY * deltaY);
    }

    /**
     * Places the menu for a physical screen position so it grows away from the task bar, the way the system aligns its
     * own tray menus. The display is measured whole, so the menu may cover the task bar it was opened from.
     *
     * @param screenX
     *            - The x coordinate to anchor the menu to, in physical screen pixels
     * @param screenY
     *            - The y coordinate to anchor the menu to, in physical screen pixels
     * @param menuSize
     *            - The size the menu will be shown at
     * @param configuration
     *            - The configuration of the display containing the anchor
     *
     * @return The location to show the menu at
     */
    private Point menuLocation(int screenX, int screenY, Dimension menuSize, GraphicsConfiguration configuration) {
        Rectangle displayArea = configuration.getBounds();
        AffineTransform transform = configuration.getDefaultTransform();
        Insets taskBarInsets = screenInsets(configuration);

        // The anchor is converted into the scaled space windows are placed in, which the display bounds are already in
        int anchorX = displayArea.x + (int) Math.round((screenX - displayArea.x) / transform.getScaleX());
        int anchorY = displayArea.y + (int) Math.round((screenY - displayArea.y) / transform.getScaleY());

        /*
         * The task bar's edge decides which way the menu grows, rather than whether it happens to fit: a tray icon sits
         * against that edge, so anchoring the menu's near side to it is what keeps the menu off the pointer
         */
        int menuX = placeAxis(anchorX, menuSize.width, displayArea.x, displayArea.width, taskBarInsets.right > 0);
        int menuY = placeAxis(anchorY, menuSize.height, displayArea.y, displayArea.height, taskBarInsets.bottom > 0);

        return new Point(menuX, menuY);
    }

    /**
     * Returns the screen insets the task bar reserves on a display, or empty insets when they cannot be read.
     *
     * @param configuration
     *            - The configuration to measure
     *
     * @return The insets reserved on the display
     */
    private Insets screenInsets(GraphicsConfiguration configuration) {
        try {
            return Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        } catch (Exception exception) {
            return new Insets(0, 0, 0, 0);
        }
    }

    /**
     * Places one axis of the menu against the anchor on the requested side, turning it back across the anchor when that
     * side would run off the display, and pulling it inside when it fits on neither side.
     *
     * @param anchor
     *            - The anchor coordinate on this axis
     * @param menuExtent
     *            - The menu's extent along this axis
     * @param displayOrigin
     *            - The display's origin on this axis
     * @param displayExtent
     *            - The display's extent along this axis
     * @param growBack
     *            - Whether the menu grows back toward the origin rather than away from it
     *
     * @return The menu's coordinate on this axis
     */
    private int placeAxis(int anchor, int menuExtent, int displayOrigin, int displayExtent, boolean growBack) {
        int farEdge = displayOrigin + displayExtent;
        int menuCoordinate = growBack ? anchor - menuExtent : anchor;

        // Only the side that would leave the display turns the menu back the other way
        if (menuCoordinate < displayOrigin) {
            menuCoordinate = anchor;
        } else if (menuCoordinate + menuExtent > farEdge) {
            menuCoordinate = anchor - menuExtent;
        }

        // Pull inside last, for a menu too large to fit on either side of the anchor
        return Math.max(displayOrigin, Math.min(menuCoordinate, farEdge - menuExtent));
    }

    /**
     * A menu held as an ordinary child of a window rather than shown as a pop-up. Showing a pop-up realizes a window of
     * its own, which is left behind at the corner of the screen when the menu is only ever a child.
     *
     * @author Jonathan R. Miller
     */
    private static class EmbeddedPopupMenu extends JPopupMenu {

        private boolean embeddedVisible;

        /**
         * Serialization id, declared because the Swing component this extends is serializable.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor for the {@link EmbeddedPopupMenu} class.
         */
        private EmbeddedPopupMenu() {
        }

        /**
         * Records the visibility the menu paints at, without the pop-up realization the inherited method performs.
         *
         * @param visible
         *            - Whether the menu should be visible
         */
        @Override
        public void setVisible(boolean visible) {
            if (embeddedVisible == visible) {
                return;
            }

            embeddedVisible = visible;

            firePropertyChange("visible", !visible, visible);
            invalidate();
        }

        /**
         * Returns whether the menu is visible, which the inherited method would answer from a pop-up that never exists.
         *
         * @return Whether the menu is visible
         */
        @Override
        public boolean isVisible() {
            return embeddedVisible;
        }

    }

}
