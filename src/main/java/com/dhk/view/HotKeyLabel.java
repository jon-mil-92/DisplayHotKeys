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
