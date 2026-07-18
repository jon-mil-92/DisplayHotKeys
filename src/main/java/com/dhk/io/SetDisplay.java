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
package com.dhk.io;

/**
 * Utilizes the SetDisplay JNI library to immediately apply the given display mode, scaling mode, DPI scale percentage,
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
     * @param bitDepth
     *            - The new bit depth for the given display
     * @param refreshRate
     *            - The new refresh rate for the given display
     * @param scalingMode
     *            - The new scaling mode for the given display
     * @param dpiScalePercentage
     *            - The new DPI scale percentage for the given display
     */
    private native void setDisplay(String displayId, int resWidth, int resHeight, int bitDepth, int refreshRate,
            int scalingMode, int dpiScalePercentage);

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
     * @param bitDepth
     *            - The new bit depth for the given display
     * @param refreshRate
     *            - The new refresh rate for the given display
     * @param scalingMode
     *            - The new scaling mode for the given display
     * @param dpiScalePercentage
     *            - The new DPI scale percentage for the given display
     */
    public void applyDisplaySettings(String displayId, int resWidth, int resHeight, int bitDepth, int refreshRate,
            int scalingMode, int dpiScalePercentage) {
        setDisplay(displayId, resWidth, resHeight, bitDepth, refreshRate, scalingMode, dpiScalePercentage);
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