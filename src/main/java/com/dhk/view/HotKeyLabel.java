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

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.formdev.flatlaf.ui.FlatLineBorder;

/**
 * Shows a hot key inside a rounded outline that always fits the hot key's text, drawn in the current theme's border
 * color. While the hot key is being recorded, the outline takes the theme's focus color so it reads as listening.
 *
 * @author Jonathan R. Miller
 */
public class HotKeyLabel extends JLabel {

    private boolean recording;

    /**
     * Serialization version of the {@link HotKeyLabel} class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Height of the outline, which matches the combo boxes and buttons beside a hot key.
     */
    private static final int OUTLINE_HEIGHT = 28;

    /**
     * Space between the hot key text and the left and right edges of the outline.
     */
    private static final int TEXT_INSET = 10;

    /**
     * Constructor for the {@link HotKeyLabel} class.
     *
     * @param text
     *            - The text of the hot key to show
     */
    public HotKeyLabel(String text) {
        super(text, SwingConstants.CENTER);
    }

    /**
     * Sets whether the hot key is being recorded, which outlines it in the theme's focus color instead of its border
     * color.
     *
     * @param recording
     *            - Whether the hot key is being recorded
     */
    public void setRecording(boolean recording) {
        this.recording = recording;

        applyOutline();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        // A theme change replaces the theme colors, so the outline is rebuilt from them instead of keeping stale ones
        applyOutline();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, OUTLINE_HEIGHT);
    }

    /**
     * Outlines the hot key with a rounded border in the theme color matching whether it is being recorded.
     */
    private void applyOutline() {
        String colorKey = recording ? "Component.focusedBorderColor" : "Component.borderColor";
        Insets textInsets = new Insets(0, TEXT_INSET, 0, TEXT_INSET);

        setBorder(new FlatLineBorder(textInsets, UIManager.getColor(colorKey), 1f, UIManager.getInt("Component.arc")));
    }

}
