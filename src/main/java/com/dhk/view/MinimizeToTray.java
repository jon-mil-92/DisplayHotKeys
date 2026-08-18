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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.dhk.io.SystemTrayIcon;
import com.dhk.model.DhkModel;
import com.dhk.utility.FrameUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

/**
 * Enables the application to be minimized to the system tray and restored from the system tray. A single instance lives
 * for the whole application lifetime so re-initializations reuse the live tray instead of rebuilding it.
 *
 * @author Jonathan R. Miller
 */
public class MinimizeToTray {

    private DhkModel model;
    private DhkView view;
    private SystemTrayIcon systemTrayIcon;
    private TrayMenu trayMenu;
    private FlatSVGIcon trayIcon;

    /**
     * Tooltip text shown for the tray icon.
     */
    private static final String TRAY_NAME = "Display Hot Keys";

    /**
     * Constructor for the {@link MinimizeToTray} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param iconResourcePath
     *            - The icon resource path for the tray icon
     */
    public MinimizeToTray(DhkModel model, DhkView view, String iconResourcePath) {
        this.model = model;
        this.view = view;

        // Kept as a vector, so every icon size is rasterized from it rather than resampled from one fixed size
        trayIcon = new FlatSVGIcon(getClass().getResource(iconResourcePath));
    }

    /**
     * Minimizes the application to the system tray.
     */
    public void execute() {
        // Hide the taskbar icon; the frame is resolved lazily since every re-initialization replaces it
        view.getFrame().setVisible(false);

        if (systemTrayIcon == null) {
            startSystemTray();
        } else {
            systemTrayIcon.setVisible(true);
        }
    }

    /**
     * Starts the system tray icon and wires its menu.
     */
    private void startSystemTray() {
        trayMenu = new TrayMenu(this::restoreAction, this::aboutAction, this::exitAction);
        systemTrayIcon = new SystemTrayIcon();

        systemTrayIcon.registerTrayIconListener(
                (anchorX, anchorY, iconBounds) -> trayMenu.show(anchorX, anchorY, iconBounds));

        systemTrayIcon.start(TRAY_NAME, this::renderTrayIconPixels);
    }

    /**
     * Renders the tray icon at the given size. Rendering from the vector at the exact size keeps the icon sharp at
     * every scale, since nothing is resampled from another size.
     *
     * @param iconWidth
     *            - The icon width in pixels
     * @param iconHeight
     *            - The icon height in pixels
     *
     * @return The rendered icon pixels in packed ARGB order
     */
    private int[] renderTrayIconPixels(int iconWidth, int iconHeight) {
        // Pre-multiplied, which is the form the notification area composites an icon in
        BufferedImage iconImage = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = iconImage.createGraphics();

        /*
         * The icon sizes and paints itself in scaled user interface units, which overshoots the notification area size
         * asked for here and clips the artwork. Undo that factor so the icon renders at exactly the requested pixels
         */
        float userScale = UIScale.getUserScaleFactor();

        graphics.scale(1 / (double) userScale, 1 / (double) userScale);

        trayIcon.derive(iconWidth, iconHeight).paintIcon(null, graphics, 0, 0);
        graphics.dispose();

        return ((DataBufferInt) iconImage.getRaster().getDataBuffer()).getData();
    }

    /**
     * Dismisses a showing tray menu when the display configuration has changed, since the menu was placed against the
     * old geometry. Only the dismissal happens here, as it is the sole part that cannot wait.
     */
    public void displayConfigurationChanged() {
        if (systemTrayIcon == null) {
            return;
        }

        trayMenu.dismiss();
    }

    /**
     * Rescales the tray icon after a re-initialization absorbs a display configuration change, since its size follows
     * the scale of the display hosting the task bar.
     */
    public void displayConfigurationSettled() {
        if (systemTrayIcon == null) {
            return;
        }

        systemTrayIcon.refreshIconSize();
    }

    /**
     * Restores the application from the system tray.
     */
    private void restoreAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = view.getFrame();
                frame.setExtendedState(JFrame.NORMAL);
                frame.setVisible(true);
                view.getDefaultFocusComponent().requestFocusInWindow();
            }
        });

        // Re-fit after the frame is shown so layout staleness accrued while hidden does not surface as scroll bars
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FrameUtil.refreshFrame(view.getFrame());
            }
        });

        shutDownSystemTray();
    }

    /**
     * Shows an "About App" dialog.
     */
    private void aboutAction() {
        hideSystemTray();

        // Build the dialog per show so it binds to the current frame, since every re-initialization replaces it
        new AboutDialog(model, view).showAboutDialog(this::showSystemTray);
    }

    /**
     * Exits the application.
     */
    private void exitAction() {
        shutDownSystemTray();
        System.exit(0);
    }

    /**
     * Hides the tray icon, keeping it alive so dialogs can show the same instance again on close.
     */
    private void hideSystemTray() {
        if (systemTrayIcon != null) {
            systemTrayIcon.setVisible(false);
        }
    }

    /**
     * Shows the tray icon again after a dialog opened from its menu closes.
     */
    private void showSystemTray() {
        if (systemTrayIcon != null) {
            systemTrayIcon.setVisible(true);
        }
    }

    /**
     * Shuts down the tray icon and clears it so the next minimize starts it again.
     */
    public void shutDownSystemTray() {
        if (systemTrayIcon != null) {
            trayMenu.dismiss();

            systemTrayIcon.setVisible(false);
            systemTrayIcon.stop();

            // Cleared together, so the tray and its menu never disagree about whether the tray is live
            systemTrayIcon = null;
            trayMenu = null;
        }
    }

}
