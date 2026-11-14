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
package com.dhk.utility;

import com.dhk.model.DisplayMode;

/**
 * Inverts a given display mode by swapping the width and height of the resolution.
 *
 * @author Jonathan R. Miller
 */
public class DisplayModeInverter {

    /**
     * Default constructor for the {@link DisplayModeInverter} class.
     */
    public DisplayModeInverter() {
    }

    /**
     * Swap the width and height of the given display mode.
     *
     * @param displayMode
     *            - The display mode to invert
     *
     * @return The inverted display mode
     */
    public static DisplayMode invertDisplayMode(DisplayMode displayMode) {
        int width = displayMode.getWidth();
        int height = displayMode.getHeight();
        int refreshNumerator = displayMode.getRefreshNumerator();
        int refreshDenominator = displayMode.getRefreshDenominator();

        // Swap the width and height for the inverted display mode, preserving the exact refresh rate
        DisplayMode invertedDisplayMode = new DisplayMode(height, width, refreshNumerator, refreshDenominator);

        return invertedDisplayMode;
    }

    /**
     * Swap the width and height of the given display modes.
     *
     * @param displayModes
     *            - The display modes to invert
     *
     * @return The inverted display modes
     */
    public static DisplayMode[] invertDisplayModes(DisplayMode[] displayModes) {
        DisplayMode[] invertedDisplayModes = new DisplayMode[displayModes.length];

        for (int i = 0; i < displayModes.length; i++) {
            invertedDisplayModes[i] = invertDisplayMode(displayModes[i]);
        }

        return invertedDisplayModes;
    }

}
