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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JToggleButton;
import javax.swing.UIManager;

import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * Shows an on/off switch drawn in the current theme's colors, since the look and feel offers no switch of its own.
 * While the switch is on, its track fills with the theme's slider color and its thumb sits at the right end.
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

        // The switch paints all of itself, so its parent shows around the track and the button border stays off
        setOpaque(false);
        setBorderPainted(false);
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
            float thumbX = isSelected() ? trackX + TRACK_WIDTH - THUMB_INSET - thumbDiameter : trackX + THUMB_INSET;
            float thumbY = trackY + THUMB_INSET;

            // The theme colors are read on every paint, so a theme change needs no extra wiring
            Color indicator = indicatorColor();

            if (isSelected()) {
                switchGraphics.setColor(indicator);
                switchGraphics.fill(
                        FlatUIUtils.createComponentRectangle(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT));
            } else {
                // While disabled, the outline takes the color of a disabled slider track
                String outlineColorKey = isEnabled() ? "Label.foreground" : "Slider.disabledTrackColor";

                switchGraphics.setColor(UIManager.getColor(outlineColorKey));
                FlatUIUtils.paintOutline(switchGraphics, trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, OUTLINE_WIDTH,
                        TRACK_HEIGHT);
            }

            // Ring the thumb while the switch has keyboard focus, the same way each slider rings its own thumb
            if (FlatUIUtils.isPermanentFocusOwner(this)) {
                int focusWidth = UIManager.getInt("Slider.focusWidth");
                float focusDiameter = thumbDiameter + 2 * focusWidth;

                switchGraphics.setColor(UIManager.getColor("Slider.focusedColor"));
                switchGraphics.fill(
                        new Ellipse2D.Float(thumbX - focusWidth, thumbY - focusWidth, focusDiameter, focusDiameter));
            }

            switchGraphics.setColor(isSelected() ? UIManager.getColor("Panel.background") : indicator);
            switchGraphics.fill(new Ellipse2D.Float(thumbX, thumbY, thumbDiameter, thumbDiameter));
        } finally {
            switchGraphics.dispose();
        }
    }

    /**
     * Returns the color of the part that shows the switch's state, which is the track while on and the thumb while off.
     * It follows the slider thumb's disabled, pressed, and hover colors, so the switches match the sliders beside them.
     *
     * @return The indicator color
     */
    private Color indicatorColor() {
        Color enabledColor = UIManager.getColor(isSelected() ? "Slider.thumbColor" : "Label.foreground");

        return FlatButtonUI.buttonStateColor(this, enabledColor, UIManager.getColor("Slider.disabledThumbColor"), null,
                UIManager.getColor("Slider.hoverThumbColor"), UIManager.getColor("Slider.pressedThumbColor"));
    }

}
