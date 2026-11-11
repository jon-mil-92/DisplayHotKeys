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
package com.dhk.utility;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Shows a tooltip for a component in its window's layered pane rather than in a heavyweight popup window, which can
 * persist as a ghost. The tooltip appears after a short hover, takes the theme's tooltip look, and works in any window.
 *
 * @author Jonathan R. Miller
 */
public class LightweightTooltip {

    private JComponent owner;
    private String tooltipText;
    private MouseAdapter tooltipMouseAdapter;
    private Timer tooltipDelayTimer;
    private JLabel layeredTooltipLabel;

    /**
     * The delay before the tooltip appears after hovering over the component.
     */
    private static final int TOOLTIP_DELAY_MS = 600;

    /**
     * Pixels between the component's bottom edge and the tooltip when it is shown below the component.
     */
    private static final int TOOLTIP_GAP_BELOW_PX = 18;

    /**
     * Pixels between the component's top edge and the tooltip when there is no room below and it flips above.
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
     * Constructor for the {@link LightweightTooltip} class.
     *
     * @param owner
     *            - The component the tooltip is shown for
     */
    public LightweightTooltip(JComponent owner) {
        this.owner = owner;
    }

    /**
     * Sets the text of the tooltip, showing it after a short hover over the component, or removes the tooltip when the
     * text is empty.
     *
     * @param text
     *            - The tooltip text, or null to remove the tooltip
     */
    public void setText(String text) {
        tooltipText = text;
        uninstall();

        if (text == null || text.isEmpty()) {
            return;
        }

        tooltipMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                scheduleShow();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelScheduledShow();
                hide();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                cancelScheduledShow();
                hide();
            }
        };

        owner.addMouseListener(tooltipMouseAdapter);
    }

    /**
     * Removes the hover listener and hides any visible tooltip.
     */
    private void uninstall() {
        cancelScheduledShow();

        if (tooltipMouseAdapter != null) {
            owner.removeMouseListener(tooltipMouseAdapter);
            tooltipMouseAdapter = null;
        }

        hide();
    }

    /**
     * Schedules showing the tooltip after a short delay.
     */
    private void scheduleShow() {
        cancelScheduledShow();

        tooltipDelayTimer = new Timer(TOOLTIP_DELAY_MS, e -> show());
        tooltipDelayTimer.setRepeats(false);
        tooltipDelayTimer.start();
    }

    /**
     * Cancels any scheduled tooltip show.
     */
    private void cancelScheduledShow() {
        if (tooltipDelayTimer != null) {
            tooltipDelayTimer.stop();
            tooltipDelayTimer = null;
        }
    }

    /**
     * Shows the tooltip by adding a label directly into the window's layered pane, replacing any tooltip still shown.
     */
    private void show() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::show);
            return;
        }

        hide();

        try {
            layeredTooltipLabel = createAndPlaceTooltip();
        } catch (RuntimeException e) {
            e.printStackTrace();
            layeredTooltipLabel = null;
        }
    }

    /**
     * Hides and removes the tooltip label from the layered pane if present.
     */
    private void hide() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::hide);
            return;
        }

        if (layeredTooltipLabel == null) {
            return;
        }

        Container parent = layeredTooltipLabel.getParent();

        if (parent != null) {
            parent.remove(layeredTooltipLabel);
            parent.revalidate();
            parent.repaint();
        }

        layeredTooltipLabel = null;
    }

    /**
     * Builds the styled tooltip label, adds it to the window's layered pane, and positions it relative to the
     * component.
     *
     * @return The placed tooltip label, or null if there is no text to show or no suitable layered pane
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
     * Resolves the layered pane of the showing window hosting the component, which may be a frame or a dialog.
     *
     * @return The window's layered pane, or null if the component is not hosted in a showing window
     */
    private JLayeredPane resolveTooltipLayeredPane() {
        Window window = SwingUtilities.getWindowAncestor(owner);

        if (!(window instanceof RootPaneContainer)) {
            return null;
        }

        JLayeredPane layeredPane = ((RootPaneContainer) window).getLayeredPane();

        if (!window.isShowing() || !layeredPane.isShowing()) {
            return null;
        }

        return layeredPane;
    }

    /**
     * Creates a tooltip label styled to match the active look and feel's tooltip colors, font, and border.
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
     * Computes the tooltip's top-left location within the layered pane. The tooltip is centered below the component by
     * default, kept within the pane edges, and flipped above the component when there is not enough room below.
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
        // Start centered horizontally and offset below the component so the mouse pointer does not obstruct it
        Point below = new Point(owner.getWidth() / 2, owner.getHeight() + TOOLTIP_GAP_BELOW_PX);
        Point location = SwingUtilities.convertPoint(owner, below, layeredPane);
        location.x -= tooltipW / 2;

        int maxX = Math.max(TOOLTIP_EDGE_MARGIN_PX, layeredPane.getWidth() - tooltipW - TOOLTIP_EDGE_MARGIN_PX);
        int maxY = Math.max(TOOLTIP_EDGE_MARGIN_PX, layeredPane.getHeight() - tooltipH - TOOLTIP_EDGE_MARGIN_PX);

        location.x = clamp(location.x, TOOLTIP_EDGE_MARGIN_PX, maxX);

        if (location.y > maxY) {
            // Not enough room below, flip the tooltip above the component
            Point above = new Point(owner.getWidth() / 2, -tooltipH - TOOLTIP_GAP_ABOVE_PX);
            Point layeredAbove = SwingUtilities.convertPoint(owner, above, layeredPane);
            location.y = Math.max(TOOLTIP_EDGE_MARGIN_PX, Math.min(layeredAbove.y, maxY));
        } else if (location.y < TOOLTIP_EDGE_MARGIN_PX) {
            location.y = TOOLTIP_EDGE_MARGIN_PX;
        }

        return location;
    }

    /**
     * Clamps a value to the inclusive range [min, max].
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
