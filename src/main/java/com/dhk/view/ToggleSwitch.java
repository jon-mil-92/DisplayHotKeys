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

    /**
     * Serialization version of the {@link ToggleSwitch} class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Width of the track the thumb slides along.
     */
    private static final int TRACK_WIDTH = 40;

    /**
     * Height of the track, which also sets the roundness of its ends.
     */
    private static final int TRACK_HEIGHT = 20;

    /**
     * Space kept clear around the track, since at fractional display scaling the bounds can fall between device pixels
     * and the whole-pixel clip would cut off a flush track.
     */
    public static final int EDGE_MARGIN = 1;

    /**
     * Space between the thumb and the edges of the track.
     */
    private static final int THUMB_INSET = 4;

    /**
     * Stroke width of the outline drawn around the track while the switch is off.
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
