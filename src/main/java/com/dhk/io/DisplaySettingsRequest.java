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
package com.dhk.io;

/**
 * The settings to apply to one display through {@link SetDisplay}, as one slot holds them.
 *
 * @param displayId
 *            - The stable ID of the display to apply the settings to
 * @param width
 *            - The horizontal resolution to apply
 * @param height
 *            - The vertical resolution to apply
 * @param refreshNumerator
 *            - The numerator of the exact refresh rate to apply (Hz = numerator / denominator)
 * @param refreshDenominator
 *            - The denominator of the exact refresh rate to apply (Hz = numerator / denominator)
 * @param dpiScalePercentage
 *            - The DPI scale percentage to apply
 * @param scalingMode
 *            - The scaling mode to apply. 0 for Aspect Ratio, 1 for Stretched, and 2 for Centered
 * @param orientationMode
 *            - The orientation mode to apply. 0 for Landscape, 1 for Portrait, 2 for Inverted Landscape, and 3 for
 *            Inverted Portrait
 *
 * @author Jonathan R. Miller
 */
public record DisplaySettingsRequest(String displayId, int width, int height, int refreshNumerator,
        int refreshDenominator, int dpiScalePercentage, int scalingMode, int orientationMode) {
}
