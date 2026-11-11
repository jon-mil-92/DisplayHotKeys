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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JToggleButton;
import javax.swing.UIManager;

/**
 * Shows an on/off switch drawn in the current theme's colors, since the look and feel offers no switch of its own.
 * While the switch is on, its track fills with the theme's accent color and its thumb sits at the right end.
 *
 * @author Jonathan R. Miller
 */
public class ToggleSwitch extends JToggleButton {

    private static final long serialVersionUID = 1L;

    /**
     * Width of the switch's track.
     */
    private static final int TRACK_WIDTH = 40;

    /**
     * Height of the switch's track, which also sets the roundness of its ends.
     */
    private static final int TRACK_HEIGHT = 20;

    /**
     * Space kept clear around the track on every side. At fractional display scaling the switch's bounds can fall
     * between device pixels, and the clip rounded to whole pixels would cut off a track drawn flush against them.
     */
    public static final int EDGE_MARGIN = 1;

    /**
     * Space between the thumb and the edges of the track.
     */
    private static final int THUMB_INSET = 4;

    /**
     * Width of the outline drawn around the track while the switch is off.
     */
    private static final float OUTLINE_WIDTH = 1f;

    /**
     * Constructor for the {@link ToggleSwitch} class.
     *
     * @param selected
     *            - Whether the switch starts on
     */
    public ToggleSwitch(boolean selected) {
        super(null, null, selected);

        // The switch paints all of itself, so the look and feel's button background, border, and focus ring stay off
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(TRACK_WIDTH + 2 * EDGE_MARGIN, TRACK_HEIGHT + 2 * EDGE_MARGIN);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D switchGraphics = (Graphics2D) graphics.create();

        try {
            switchGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switchGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Center the track, which keeps the edge margin clear even when a layout gives the switch more room
            float trackX = (getWidth() - TRACK_WIDTH) / 2f;
            float trackY = (getHeight() - TRACK_HEIGHT) / 2f;
            float thumbDiameter = TRACK_HEIGHT - 2 * THUMB_INSET;
            float thumbY = trackY + THUMB_INSET;

            // The theme colors are read on every paint, so a theme change needs no extra wiring
            Color foreground = isEnabled()
                    ? themeColor("Label.foreground", Color.BLACK)
                    : themeColor("Label.disabledForeground", Color.GRAY);

            if (isSelected()) {
                Color accent = themeColor("Component.accentColor", themeColor("Component.focusColor", foreground));
                float thumbX = trackX + TRACK_WIDTH - THUMB_INSET - thumbDiameter;

                switchGraphics.setColor(isEnabled() ? accent : foreground);
                switchGraphics.fill(new RoundRectangle2D.Float(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT,
                        TRACK_HEIGHT));

                switchGraphics.setColor(themeColor("Panel.background", Color.WHITE));
                switchGraphics.fill(new Ellipse2D.Float(thumbX, thumbY, thumbDiameter, thumbDiameter));

                return;
            }

            // Inset the outline by half its width so the stroke stays inside the track's bounds
            float outlineInset = OUTLINE_WIDTH / 2f;
            float outlineWidth = TRACK_WIDTH - OUTLINE_WIDTH;
            float outlineHeight = TRACK_HEIGHT - OUTLINE_WIDTH;

            switchGraphics.setColor(foreground);
            switchGraphics.setStroke(new BasicStroke(OUTLINE_WIDTH));
            switchGraphics.draw(new RoundRectangle2D.Float(trackX + outlineInset, trackY + outlineInset, outlineWidth,
                    outlineHeight, outlineHeight, outlineHeight));
            switchGraphics.fill(new Ellipse2D.Float(trackX + THUMB_INSET, thumbY, thumbDiameter, thumbDiameter));
        } finally {
            switchGraphics.dispose();
        }
    }

    /**
     * Returns a color of the current theme, or the fallback color when the theme lacks it.
     *
     * @param colorKey
     *            - The key of the theme color
     * @param fallbackColor
     *            - The color to use when the theme lacks the key
     *
     * @return The theme color
     */
    private Color themeColor(String colorKey, Color fallbackColor) {
        Color color = UIManager.getColor(colorKey);

        return color != null ? color : fallbackColor;
    }

}
