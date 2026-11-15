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
package com.dhk.theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatLabelUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * Pads every label's text on both sides, since the first and last glyphs draw past the measured text width at many
 * display scales, and a label sized exactly to its text would cut them off. Both theme files register it.
 *
 * @author Jonathan R. Miller
 */
public class PaddedLabelUI extends FlatLabelUI {

    /**
     * Two pixels on each side of the text, which covers the widest glyph overhang at every display scale. Borders hold
     * no state, so every label shares this one.
     */
    private static final Border TEXT_PADDING_BORDER = BorderFactory.createEmptyBorder(0, 2, 0, 2);

    /**
     * Constructor for the {@link PaddedLabelUI} class.
     *
     * @param shared
     *            - Whether this instance is shared by every label without a style of its own
     */
    protected PaddedLabelUI(boolean shared) {
        super(shared);
    }

    /**
     * Creates the UI for a label, sharing one instance among the labels without a style of their own as FlatLaf does.
     *
     * @param component
     *            - The label to create the UI for
     *
     * @return The UI for the label
     */
    public static ComponentUI createUI(JComponent component) {
        if (FlatUIUtils.canUseSharedUI(component)) {
            return FlatUIUtils.createSharedUI(PaddedLabelUI.class, () -> new PaddedLabelUI(true));
        }

        return new PaddedLabelUI(false);
    }

    @Override
    protected void installDefaults(JLabel label) {
        super.installDefaults(label);

        // A label that sets its own border, such as a list cell renderer or the hot key outline, keeps that border
        if (label.getBorder() == null) {
            label.setBorder(TEXT_PADDING_BORDER);
        }
    }

}
