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
 * Utilizes the SetDisplay JNI library to immediately apply the given display mode, DPI scale percentage, scaling mode,
 * and orientation mode for the given display.
 *
 * @author Jonathan R. Miller
 */
public class SetDisplay {

    /**
     * Default constructor for the {@link SetDisplay} class.
     */
    public SetDisplay() {
    }

    static {
        try {
            System.loadLibrary("SetDisplay");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Defines a JNI function to immediately apply the given settings for the given display.
     *
     * @param displayId
     *            - The ID of the display to apply the display settings for
     * @param resWidth
     *            - The new horizontal resolution for the given display
     * @param resHeight
     *            - The new vertical resolution for the given display
     * @param refreshNumerator
     *            - The numerator of the new exact refresh rate (Hz = numerator / denominator)
     * @param refreshDenominator
     *            - The denominator of the new exact refresh rate (Hz = numerator / denominator)
     * @param dpiScalePercentage
     *            - The new DPI scale percentage for the given display
     * @param scalingMode
     *            - The new scaling mode for the given display
     */
    private native void setDisplay(String displayId, int resWidth, int resHeight, int refreshNumerator,
            int refreshDenominator, int dpiScalePercentage, int scalingMode);

    /**
     * Defines a JNI function to immediately apply the given orientation mode for the given display.
     *
     * @param displayId
     *            - The ID of the display to apply the orientation mode for
     * @param orientationMode
     *            - The new orientation mode for the given display. 0 for Landscape, 1 for Portrait, 2 for Inverted
     *            Landscape, and 3 for Inverted Portrait
     */
    private native void setOrientation(String displayId, int orientationMode);

    /**
     * Defines a JNI function to reflow the multi-display arrangement after a batch of display changes.
     *
     * @param arrangementSnapshot
     *            - The arrangement captured before the batch, one encoded rectangle per display
     */
    private native void preserveDisplayArrangement(String[] arrangementSnapshot);

    /**
     * Immediately applies the given settings for the given display.
     *
     * @param displayId
     *            - The ID of the display to apply the display settings for
     * @param resWidth
     *            - The new horizontal resolution for the given display
     * @param resHeight
     *            - The new vertical resolution for the given display
     * @param refreshNumerator
     *            - The numerator of the new exact refresh rate (Hz = numerator / denominator)
     * @param refreshDenominator
     *            - The denominator of the new exact refresh rate (Hz = numerator / denominator)
     * @param dpiScalePercentage
     *            - The new DPI scale percentage for the given display
     * @param scalingMode
     *            - The new scaling mode for the given display
     */
    public void applyDisplaySettings(String displayId, int resWidth, int resHeight, int refreshNumerator,
            int refreshDenominator, int dpiScalePercentage, int scalingMode) {
        setDisplay(displayId, resWidth, resHeight, refreshNumerator, refreshDenominator, dpiScalePercentage,
                scalingMode);
    }

    /**
     * Immediately applies the given orientation mode for the given display.
     *
     * @param displayId
     *            - The ID of the display to apply the orientation mode for
     * @param orientationMode
     *            - The new orientation mode for the given display. 0 for Landscape, 1 for Portrait, 2 for Inverted
     *            Landscape, and 3 for Inverted Portrait
     */
    public void applyDisplayOrientation(String displayId, int orientationMode) {
        setOrientation(displayId, orientationMode);
    }

    /**
     * Reflows the multi-display arrangement against the given snapshot (from GetDisplay's captureArrangement), so every
     * display keeps its relative position and alignment after one or more displays were resized.
     *
     * @param arrangementSnapshot
     *            - The arrangement captured before applying the batch, one encoded rectangle per display
     */
    public void preserveArrangement(String[] arrangementSnapshot) {
        preserveDisplayArrangement(arrangementSnapshot);
    }

}