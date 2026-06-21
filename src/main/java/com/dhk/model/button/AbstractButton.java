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
package com.dhk.model.button;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Defines an abstract button that implements the icon change action and defines the update methods for a button. This
 * class also provides a lightweight tooltip implementation that avoids heavyweight Popup windows by creating the
 * tooltip using the frame's layered pane as the popup owner.
 *
 * @author Jonathan R. Miller
 */
public abstract class AbstractButton extends JButton {

    private static final long serialVersionUID = -8040714399831618945L;

    private ButtonProperties buttonProperties;
    private transient JLabel layeredTooltipLabel;
    private transient MouseAdapter tooltipMouseAdapter;
    private volatile String tooltipText;
    private transient Timer tooltipDelayTimer;
    private volatile boolean tooltipCreating;

    private static final int TOOLTIP_DELAY_MS = 300;

    /**
     * Default constructor for the {@link AbstractButton} class.
     */
    public AbstractButton() {
        super();
    }

    /**
     * Gets an SVG icon for the resource at the given path with the given image scale percentage.
     *
     * @param path
     *            - The path to the icon resource
     * @param scale
     *            - The image scale percentage
     *
     * @return The SVG icon for the resource at the given path with the given image scale percentage
     */
    protected FlatSVGIcon getSvgIcon(String path, float scale) {
        return new FlatSVGIcon(getClass().getResource(path)).derive(scale);
    }

    /**
     * Changes the icon based on the state of the button.
     */
    protected void iconChangeAction() {
        if (getModel().isArmed()) {
            updateHeldIcon();
        } else if (getModel().isRollover()) {
            updateHoverIcon();
        } else {
            updateIdleIcon();
        }
    }

    /**
     * Updates the icon when the button is idle.
     */
    public abstract void updateIdleIcon();

    /**
     * Updates the icon when the cursor is hovering over the button.
     */
    public abstract void updateHoverIcon();

    /**
     * Updates the icon when the button is held down.
     */
    public abstract void updateHeldIcon();

    /**
     * Gets the button properties.
     *
     * @return The button properties
     */
    public ButtonProperties getButtonProperties() {
        return buttonProperties;
    }

    /**
     * Sets the button properties.
     *
     * @param buttonProperties
     *            - The button properties to set
     */
    public void setButtonProperties(ButtonProperties buttonProperties) {
        this.buttonProperties = buttonProperties;
    }

    /**
     * Overrides the default Swing tooltip behavior and installs a lightweight tooltip that is shown in the frame's
     * layered pane. This prevents Swing from creating a heavyweight Popup window that can persist as a ghost. The
     * method disables the default Swing tooltip for this component only and installs mouse listeners that show and hide
     * a lightweight tooltip label after a short delay.
     *
     * @param text
     *            - The tooltip text to show for this button
     */
    @Override
    public void setToolTipText(String text) {
        this.tooltipText = text;
        super.setToolTipText(null);
        uninstallLightweightTooltip();

        if (text == null || text.isEmpty()) {
            return;
        } else {
            tooltipMouseAdapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    scheduleShowTooltip(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    cancelScheduledShow();
                    hideLightweightTooltip();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    cancelScheduledShow();
                    hideLightweightTooltip();
                }
            };

            try {
                addMouseListener(tooltipMouseAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Schedule showing the tooltip after a short delay
     *
     * @param mouseEvent
     *            - The mouse event that triggered the schedule
     */
    private void scheduleShowTooltip(MouseEvent mouseEvent) {
        cancelScheduledShow();

        tooltipDelayTimer = new Timer(TOOLTIP_DELAY_MS, _ -> showLightweightTooltip(mouseEvent));
        tooltipDelayTimer.setRepeats(false);
        tooltipDelayTimer.start();
    }

    /**
     * Cancel any scheduled tooltip show.
     */
    private void cancelScheduledShow() {
        if (tooltipDelayTimer != null) {
            try {
                tooltipDelayTimer.stop();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                tooltipDelayTimer = null;
            }
        }
    }

    /**
     * Remove listeners and hide any visible tooltip.
     */
    private void uninstallLightweightTooltip() {
        cancelScheduledShow();

        if (tooltipMouseAdapter != null) {
            try {
                removeMouseListener(tooltipMouseAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                tooltipMouseAdapter = null;
            }
        }

        hideLightweightTooltip();
    }

    /**
     * Show a lightweight tooltip by adding a JLabel directly into the frame's layered pane. Measures after adding so
     * the full text is visible.
     *
     * @param mouseEvent
     *            - The mouse event that triggered the show (may be null)
     */
    private void showLightweightTooltip(MouseEvent mouseEvent) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showLightweightTooltip(mouseEvent));
            return;
        }

        synchronized (this) {
            if (tooltipCreating) {
                return;
            } else {
                tooltipCreating = true;
            }
        }

        hideLightweightTooltip();

        try {
            if (tooltipText == null || tooltipText.isEmpty()) {
                tooltipCreating = false;
                return;
            }

            Window window = SwingUtilities.getWindowAncestor(this);

            if (!(window instanceof JFrame)) {
                tooltipCreating = false;
                return;
            }

            JFrame frame = (JFrame) window;
            JLayeredPane layeredPane = frame.getLayeredPane();

            if (!frame.isShowing() || !layeredPane.isShowing()) {
                tooltipCreating = false;
                return;
            }

            JLabel tooltipLabel = new JLabel(tooltipText);
            tooltipLabel.setOpaque(true);

            Color background = UIManager.getColor("ToolTip.background");

            if (background == null) {
                background = new Color(255, 255, 210);
            }

            tooltipLabel.setBackground(background);

            Color foreground = UIManager.getColor("ToolTip.foreground");

            if (foreground != null) {
                tooltipLabel.setForeground(foreground);
            }

            tooltipLabel.setFont(UIManager.getFont("ToolTip.font"));

            tooltipLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));

            // Add with temporary bounds so UI is installed and preferred size is accurate immediately
            tooltipLabel.setBounds(0, 0, 1, 1);
            layeredPane.add(tooltipLabel, JLayeredPane.POPUP_LAYER);

            // Force UI delegate to install and compute preferred size
            layeredPane.revalidate();
            tooltipLabel.invalidate();
            tooltipLabel.validate();

            Dimension pref = tooltipLabel.getPreferredSize();
            int tooltipW = pref.width;
            int tooltipH = pref.height;

            Point compPoint = new Point(getWidth() / 2, getHeight() + 4);
            Point layeredPoint = SwingUtilities.convertPoint(this, compPoint, layeredPane);
            layeredPoint.x -= tooltipW / 2;

            int margin = 4;
            int maxX = Math.max(margin, layeredPane.getWidth() - tooltipW - margin);
            int maxY = Math.max(margin, layeredPane.getHeight() - tooltipH - margin);

            if (layeredPoint.x < margin) {
                layeredPoint.x = margin;
            }

            if (layeredPoint.x > maxX) {
                layeredPoint.x = maxX;
            }

            if (layeredPoint.y > maxY) {
                Point compAbove = new Point(getWidth() / 2, -tooltipH - 4);
                Point layeredAbove = SwingUtilities.convertPoint(this, compAbove, layeredPane);
                int aboveY = Math.max(margin, Math.min(layeredAbove.y, maxY));
                layeredPoint.y = aboveY;
            } else {
                if (layeredPoint.y < margin) {
                    layeredPoint.y = margin;
                }
            }

            tooltipLabel.setBounds(layeredPoint.x, layeredPoint.y, tooltipW, tooltipH);
            layeredPane.revalidate();
            layeredPane.repaint();

            layeredTooltipLabel = tooltipLabel;
            tooltipCreating = false;
        } catch (Throwable t) {
            t.printStackTrace();
            layeredTooltipLabel = null;
            tooltipCreating = false;
        }
    }

    /**
     * Hide and remove the tooltip label from the layered pane if present.
     */
    private void hideLightweightTooltip() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::hideLightweightTooltip);
            return;
        }

        if (layeredTooltipLabel != null) {
            try {
                Container parent = layeredTooltipLabel.getParent();

                if (parent != null) {
                    parent.remove(layeredTooltipLabel);
                    parent.revalidate();
                    parent.repaint();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                layeredTooltipLabel = null;
            }
        }
    }

}
