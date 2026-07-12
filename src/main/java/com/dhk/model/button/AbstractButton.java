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

    /**
     * The delay before the tooltip appears after hovering over the button.
     */
    private static final int TOOLTIP_DELAY_MS = 600;

    /**
     * Pixels between the button's bottom edge and the tooltip when it is shown below the button.
     */
    private static final int TOOLTIP_GAP_BELOW_PX = 18;

    /**
     * Pixels between the button's top edge and the tooltip when there is no room below and it flips above.
     */
    private static final int TOOLTIP_GAP_ABOVE_PX = 18;

    /**
     * Minimum number of pixels kept between the tooltip and the edges of the layered pane.
     */
    private static final int TOOLTIP_EDGE_MARGIN_PX = 8;

    /**
     * Inner vertical padding (top and bottom) around the tooltip text.
     */
    private static final int TOOLTIP_PADDING_VERTICAL_PX = 6;

    /**
     * Inner horizontal padding (left and right) around the tooltip text.
     */
    private static final int TOOLTIP_PADDING_HORIZONTAL_PX = 6;

    /**
     * Fallback tooltip background used when the active look and feel does not define one.
     */
    private static final Color TOOLTIP_DEFAULT_BACKGROUND = new Color(255, 255, 210);

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
     * Schedule showing the tooltip after a short delay
     *
     * @param mouseEvent
     *            - The mouse event that triggered the schedule
     */
    private void scheduleShowTooltip(MouseEvent mouseEvent) {
        cancelScheduledShow();

        tooltipDelayTimer = new Timer(TOOLTIP_DELAY_MS, e -> showLightweightTooltip(mouseEvent));
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
            }

            tooltipCreating = true;
        }

        hideLightweightTooltip();

        try {
            layeredTooltipLabel = createAndPlaceTooltip();
        } catch (Throwable t) {
            t.printStackTrace();
            layeredTooltipLabel = null;
        } finally {
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

    /**
     * Build the styled tooltip label, add it to the frame's layered pane, and position it relative to this button.
     *
     * @return The placed tooltip label, or {@code null} if there is no text to show or no suitable layered pane
     */
    private JLabel createAndPlaceTooltip() {
        if (tooltipText == null || tooltipText.isEmpty()) {
            return null;
        }

        JLayeredPane layeredPane = resolveTooltipLayeredPane();

        if (layeredPane == null) {
            return null;
        }

        JLabel tooltipLabel = createTooltipLabel(tooltipText);

        // Add with temporary bounds so the UI delegate is installed and the preferred size is accurate immediately
        tooltipLabel.setBounds(0, 0, 1, 1);
        layeredPane.add(tooltipLabel, JLayeredPane.POPUP_LAYER);

        // Force the UI delegate to install and compute the preferred size
        layeredPane.revalidate();
        tooltipLabel.invalidate();
        tooltipLabel.validate();

        Dimension pref = tooltipLabel.getPreferredSize();
        Point location = computeTooltipLocation(pref.width, pref.height, layeredPane);

        tooltipLabel.setBounds(location.x, location.y, pref.width, pref.height);
        layeredPane.revalidate();
        layeredPane.repaint();

        return tooltipLabel;
    }

    /**
     * Resolve the layered pane of the frame hosting this button, suitable for displaying the tooltip.
     *
     * @return The frame's layered pane, or {@code null} if this button is not hosted in a showing {@link JFrame}
     */
    private JLayeredPane resolveTooltipLayeredPane() {
        Window window = SwingUtilities.getWindowAncestor(this);

        if (!(window instanceof JFrame)) {
            return null;
        }

        JFrame frame = (JFrame) window;
        JLayeredPane layeredPane = frame.getLayeredPane();

        if (!frame.isShowing() || !layeredPane.isShowing()) {
            return null;
        }

        return layeredPane;
    }

    /**
     * Create a tooltip label styled to match the active look and feel's tooltip colors, font, and border.
     *
     * @param text
     *            - The text to display in the tooltip
     *
     * @return The styled tooltip label
     */
    private JLabel createTooltipLabel(String text) {
        JLabel tooltipLabel = new JLabel(text);
        tooltipLabel.setOpaque(true);

        Color background = UIManager.getColor("ToolTip.background");
        tooltipLabel.setBackground(background != null ? background : TOOLTIP_DEFAULT_BACKGROUND);

        Color foreground = UIManager.getColor("ToolTip.foreground");

        if (foreground != null) {
            tooltipLabel.setForeground(foreground);
        }

        tooltipLabel.setFont(UIManager.getFont("ToolTip.font"));

        tooltipLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(TOOLTIP_PADDING_VERTICAL_PX, TOOLTIP_PADDING_HORIZONTAL_PX,
                        TOOLTIP_PADDING_VERTICAL_PX, TOOLTIP_PADDING_HORIZONTAL_PX)));

        return tooltipLabel;
    }

    /**
     * Compute the tooltip's top-left location within the layered pane. The tooltip is centered below the button by
     * default, kept within the pane edges, and flipped above the button when there is not enough room below.
     *
     * @param tooltipW
     *            - The tooltip width in pixels
     * @param tooltipH
     *            - The tooltip height in pixels
     * @param layeredPane
     *            - The layered pane the tooltip is positioned within
     *
     * @return The top-left point for the tooltip, in layered-pane coordinates
     */
    private Point computeTooltipLocation(int tooltipW, int tooltipH, JLayeredPane layeredPane) {
        // Start centered horizontally and offset below the button so the mouse pointer does not obstruct it
        Point below = new Point(getWidth() / 2, getHeight() + TOOLTIP_GAP_BELOW_PX);
        Point location = SwingUtilities.convertPoint(this, below, layeredPane);
        location.x -= tooltipW / 2;

        int maxX = Math.max(TOOLTIP_EDGE_MARGIN_PX, layeredPane.getWidth() - tooltipW - TOOLTIP_EDGE_MARGIN_PX);
        int maxY = Math.max(TOOLTIP_EDGE_MARGIN_PX, layeredPane.getHeight() - tooltipH - TOOLTIP_EDGE_MARGIN_PX);

        location.x = clamp(location.x, TOOLTIP_EDGE_MARGIN_PX, maxX);

        if (location.y > maxY) {
            // Not enough room below, flip the tooltip above the button
            Point above = new Point(getWidth() / 2, -tooltipH - TOOLTIP_GAP_ABOVE_PX);
            Point layeredAbove = SwingUtilities.convertPoint(this, above, layeredPane);
            location.y = Math.max(TOOLTIP_EDGE_MARGIN_PX, Math.min(layeredAbove.y, maxY));
        } else if (location.y < TOOLTIP_EDGE_MARGIN_PX) {
            location.y = TOOLTIP_EDGE_MARGIN_PX;
        }

        return location;
    }

    /**
     * Clamp a value to the inclusive range {@code [min, max]}.
     *
     * @param value
     *            - The value to clamp
     * @param min
     *            - The lower bound
     * @param max
     *            - The upper bound
     *
     * @return The clamped value
     */
    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

}
