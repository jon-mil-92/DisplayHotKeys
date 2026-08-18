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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.dhk.utility.FrameUtil;

/**
 * Shows the tray icon's menu as a themed pop-up menu. The menu and the window anchoring it are built for each show and
 * discarded afterward, so neither can hold display geometry or theme state that a later change would invalidate.
 *
 * @author Jonathan R. Miller
 */
public class TrayMenu {

    private JPopupMenu popupMenu;
    private JDialog anchorDialog;
    private Runnable restoreAction;
    private Runnable aboutAction;
    private Runnable exitAction;

    /**
     * Edge (px) of the dialog the menu is anchored to. A dialog with no area resolves poorly to a display, and the
     * menu needs its anchor to resolve to the display the icon was clicked on.
     */
    private static final int ANCHOR_DIALOG_EDGE = 1;

    /**
     * Fully transparent background for the anchor dialog, which must stay showing to hold the focus the menu needs.
     */
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

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
     * Shows the menu for a tray icon at the given physical screen position. Any menu already showing is dismissed
     * first, so a second request can never leave two menus behind.
     *
     * @param anchorX
     *            - The x coordinate to anchor the menu to, in physical screen pixels
     * @param anchorY
     *            - The y coordinate to anchor the menu to, in physical screen pixels
     * @param iconBounds
     *            - The bounds of the tray icon, in physical screen pixels
     */
    public void show(int anchorX, int anchorY, Rectangle iconBounds) {
        dismiss();

        GraphicsConfiguration configuration = configurationAt(anchorX, anchorY);

        if (configuration == null) {
            return;
        }

        Point anchor = toLogicalPoint(anchorX, anchorY, configuration);

        // Built per show so the items pick up the current theme, which a menu outside every window's tree would miss
        popupMenu = buildPopupMenu();

        /*
         * A dialog, not a window: an ownerless window is never focusable, so it can never make this process own the
         * foreground, and the menu would then receive neither the click nor the key press that dismisses it
         */
        anchorDialog = new JDialog((Frame) null, "", false, configuration);
        anchorDialog.setUndecorated(true);
        anchorDialog.setAlwaysOnTop(true);
        anchorDialog.setSize(ANCHOR_DIALOG_EDGE, ANCHOR_DIALOG_EDGE);
        anchorDialog.setLocation(anchor.x, anchor.y);

        // Fully transparent, since the dialog sits under the cursor and would otherwise show as a dot on the task bar
        anchorDialog.setBackground(TRANSPARENT);
        anchorDialog.setOpacity(0f);

        anchorDialog.setVisible(true);
        anchorDialog.toFront();
        anchorDialog.requestFocus();

        retireAnchorDialogOnHide(popupMenu, anchorDialog);

        // Set the invoker before measuring, so the menu is sized against the display it will open on
        popupMenu.setInvoker(anchorDialog);

        Point location = menuLocation(anchor, iconBounds, configuration);

        popupMenu.setLocation(location.x, location.y);
        popupMenu.setVisible(true);
    }

    /**
     * Dismisses the menu when it is showing, so a display change never leaves a menu placed against the old geometry.
     */
    public void dismiss() {
        if (popupMenu != null) {
            popupMenu.setVisible(false);
        }

        // Hiding an already-hidden menu raises no event, so retire the dialog here rather than leave it showing
        if (anchorDialog != null) {
            disposeAnchorDialog(popupMenu, anchorDialog);
        }
    }

    /**
     * Builds the menu with the items for the tray icon. Building it per show keeps it on the current theme without
     * having to update it from every path that changes the theme.
     *
     * @return The built menu
     */
    private JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        // The menu must be a window of its own so it can extend past the anchor dialog and over the task bar
        menu.setLightWeightPopupEnabled(false);

        JMenuItem restoreMenuItem = new JMenuItem("Restore");
        restoreMenuItem.addActionListener(actionEvent -> runMenuAction(restoreAction));
        menu.add(restoreMenuItem);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(actionEvent -> runMenuAction(aboutAction));
        menu.add(aboutMenuItem);

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(actionEvent -> runMenuAction(exitAction));
        menu.add(exitMenuItem);

        return menu;
    }

    /**
     * Retires the menu's anchor dialog once the menu stops showing, so no dialog outlives the menu it was created for.
     *
     * @param menu
     *            - The menu to retire the anchor dialog for
     * @param menuAnchorDialog
     *            - The anchor dialog the menu is showing against
     */
    private void retireAnchorDialogOnHide(JPopupMenu menu, JDialog menuAnchorDialog) {
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                disposeAnchorDialog(menu, menuAnchorDialog);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                disposeAnchorDialog(menu, menuAnchorDialog);
            }
        });
    }

    /**
     * Runs a menu item's action after the menu has finished hiding, so the action never runs while the menu is still
     * tearing down its window.
     *
     * @param menuAction
     *            - The action to run
     */
    private void runMenuAction(Runnable menuAction) {
        if (menuAction != null) {
            SwingUtilities.invokeLater(menuAction);
        }
    }

    /**
     * Retires the dialog anchoring the menu that just closed. Disposing it on a later event lets the menu finish
     * releasing its mouse grab against a window that still has its peer.
     *
     * @param closedMenu
     *            - The menu that is no longer showing
     * @param closedAnchorDialog
     *            - The anchor dialog the closed menu was showing against
     */
    private void disposeAnchorDialog(JPopupMenu closedMenu, JDialog closedAnchorDialog) {
        // Only clear the fields when they still belong to this menu, since a newer show may already own them
        if (closedMenu == popupMenu) {
            popupMenu = null;
            anchorDialog = null;
        }

        SwingUtilities.invokeLater(() -> closedAnchorDialog.dispose());
    }

    /**
     * Finds the configuration of the display containing the given physical screen position. Each configuration's
     * bounds are scaled to physical pixels before the test, since a physical position does not fall inside the logical
     * bounds of a display that is scaled.
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
            Rectangle bounds = physicalBounds(configuration);

            if (bounds.contains(screenX, screenY)) {
                return configuration;
            }

            /*
             * A position can fall just outside every display, since the reported icon bounds are rounded and the
             * overflow area can sit off the edge, so fall back to the display it is genuinely closest to
             */
            long distance = squaredDistanceTo(bounds, screenX, screenY);

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
     * @param bounds
     *            - The bounds to measure to
     * @param screenX
     *            - The x coordinate in physical screen pixels
     * @param screenY
     *            - The y coordinate in physical screen pixels
     *
     * @return The squared distance to the bounds
     */
    private long squaredDistanceTo(Rectangle bounds, int screenX, int screenY) {
        long clampedX = Math.max(bounds.x, Math.min(screenX, bounds.x + bounds.width - 1));
        long clampedY = Math.max(bounds.y, Math.min(screenY, bounds.y + bounds.height - 1));
        long deltaX = screenX - clampedX;
        long deltaY = screenY - clampedY;

        return (deltaX * deltaX) + (deltaY * deltaY);
    }

    /**
     * Returns a configuration's bounds in physical screen pixels. Only the extent is scaled: a configuration's origin
     * is already reported in the physical pixels of the virtual desktop, so scaling it would displace the display.
     *
     * @param configuration
     *            - The configuration to measure
     *
     * @return The bounds in physical screen pixels
     */
    private Rectangle physicalBounds(GraphicsConfiguration configuration) {
        Rectangle bounds = configuration.getBounds();
        AffineTransform transform = configuration.getDefaultTransform();

        int physicalWidth = (int) Math.round(bounds.width * transform.getScaleX());
        int physicalHeight = (int) Math.round(bounds.height * transform.getScaleY());

        return new Rectangle(bounds.x, bounds.y, physicalWidth, physicalHeight);
    }

    /**
     * Converts a physical screen position into the coordinate space the frame utilities place windows in, which keeps
     * a converted anchor comparable to the working area the menu is fitted against.
     *
     * @param screenX
     *            - The x coordinate in physical screen pixels
     * @param screenY
     *            - The y coordinate in physical screen pixels
     * @param configuration
     *            - The configuration of the display containing the position
     *
     * @return The converted position
     */
    private Point toLogicalPoint(int screenX, int screenY, GraphicsConfiguration configuration) {
        Rectangle bounds = configuration.getBounds();
        AffineTransform transform = configuration.getDefaultTransform();

        int logicalX = bounds.x + (int) Math.round((screenX - bounds.x) / transform.getScaleX());
        int logicalY = bounds.y + (int) Math.round((screenY - bounds.y) / transform.getScaleY());

        return new Point(logicalX, logicalY);
    }

    /**
     * Places the menu against the tray icon so it grows away from the task bar and stays inside the working area. The
     * pop-up's own fitting only pulls a menu inward, so it would leave the menu over the task bar without this.
     *
     * @param anchor
     *            - The anchor position in the display's scaled coordinate space
     * @param iconBounds
     *            - The bounds of the tray icon, in physical screen pixels
     * @param configuration
     *            - The configuration of the display containing the anchor
     *
     * @return The location to show the menu at
     */
    private Point menuLocation(Point anchor, Rectangle iconBounds, GraphicsConfiguration configuration) {
        Rectangle workingArea = FrameUtil.workingAreaBounds(configuration);
        Dimension menuSize = popupMenu.getPreferredSize();
        Rectangle iconArea = logicalIconArea(anchor, iconBounds, configuration);

        if (workingArea == null) {
            return new Point(iconArea.x, iconArea.y);
        }

        int menuX = iconArea.x;
        int menuY = iconArea.y;

        /*
         * Open the menu away from the task bar on whichever edge the icon sits: past the far edge means a bottom or
         * right task bar, so grow back across the icon, and before the near edge means a top or left one, so grow out
         */
        if (menuX + menuSize.width > workingArea.x + workingArea.width) {
            menuX = iconArea.x + iconArea.width - menuSize.width;
        } else if (menuX < workingArea.x) {
            menuX = iconArea.x + iconArea.width;
        }

        if (menuY + menuSize.height > workingArea.y + workingArea.height) {
            menuY = iconArea.y - menuSize.height;
        } else if (menuY < workingArea.y) {
            menuY = iconArea.y + iconArea.height;
        }

        // Clamp last so a menu larger than the working area still starts inside it
        menuX = Math.max(workingArea.x, Math.min(menuX, workingArea.x + workingArea.width - menuSize.width));
        menuY = Math.max(workingArea.y, Math.min(menuY, workingArea.y + workingArea.height - menuSize.height));

        return new Point(menuX, menuY);
    }

    /**
     * Converts the tray icon's bounds into the coordinate space the menu is placed in, so the menu can be anchored to
     * the icon itself. Falls back to an empty area at the cursor when the icon reports no bounds, since the cursor can
     * sit away from the icon when it is reached from the overflow area.
     *
     * @param anchor
     *            - The anchor position in the display's scaled coordinate space
     * @param iconBounds
     *            - The bounds of the tray icon, in physical screen pixels
     * @param configuration
     *            - The configuration of the display containing the anchor
     *
     * @return The icon's area in the coordinate space the menu is placed in
     */
    private Rectangle logicalIconArea(Point anchor, Rectangle iconBounds, GraphicsConfiguration configuration) {
        if (iconBounds == null || iconBounds.width <= 0 || iconBounds.height <= 0) {
            return new Rectangle(anchor.x, anchor.y, 0, 0);
        }

        Point topLeft = toLogicalPoint(iconBounds.x, iconBounds.y, configuration);
        Point bottomRight = toLogicalPoint(iconBounds.x + iconBounds.width, iconBounds.y + iconBounds.height,
                configuration);

        return new Rectangle(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

}
