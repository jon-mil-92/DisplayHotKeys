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
import java.awt.FontMetrics;

import javax.swing.JButton;

import com.dhk.localization.Localizer;

/**
 * A Change Hot Key button that is wide enough for every text it shows while a hot key is recorded, so it never resizes
 * partway through a recording. The texts are measured whenever the layout asks, which is once the button is in its
 * window, since text measured any earlier ignores the display scaling and comes out too narrow.
 *
 * @author Jonathan R. Miller
 */
public class ChangeHotKeyButton extends JButton {

    private String[] stateTexts;

    /**
     * Serialization version of the {@link ChangeHotKeyButton} class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Message key of every text the button shows while no hot key is recorded and while one is.
     */
    private static final String[] STATE_TEXT_KEYS = {"hotKey.change", "hotKey.press", "hotKey.release",
            "hotKey.noSubsets", "hotKey.set", "hotKey.notChanged"};

    /**
     * Default constructor for the {@link ChangeHotKeyButton} class.
     */
    public ChangeHotKeyButton() {
        super(Localizer.getText("hotKey.change"));

        // A language change rebuilds every button, so the texts of the language it was built in stay correct
        stateTexts = new String[STATE_TEXT_KEYS.length];

        for (int textIndex = 0; textIndex < STATE_TEXT_KEYS.length; textIndex++) {
            stateTexts[textIndex] = Localizer.getText(STATE_TEXT_KEYS[textIndex]);
        }

        setFocusPainted(false);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fontMetrics = getFontMetrics(getFont());
        int widestTextWidth = 0;

        for (String stateText : stateTexts) {
            widestTextWidth = Math.max(widestTextWidth, fontMetrics.stringWidth(stateText));
        }

        // The border and padding around the shown text stay the same whichever text is shown
        Dimension shownTextSize = super.getPreferredSize();
        int paddingWidth = shownTextSize.width - fontMetrics.stringWidth(getText());

        return new Dimension(paddingWidth + widestTextWidth, shownTextSize.height);
    }

}
