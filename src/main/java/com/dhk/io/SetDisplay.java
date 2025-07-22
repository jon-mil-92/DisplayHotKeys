package com.dhk.io;

/**
 * Utilizes the SetDisplay JNI library to immediately apply the given display mode, scaling mode, and DPI scale
 * percentage for the given display.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class SetDisplay {

    /**
     * Default constructor for the SetDisplay class.
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

    /*
     * Defines a JNI function to immediately apply the given display settings for the given display.
     */
    private native void setDisplay(String displayId, int resWidth, int resHeight, int bitDepth, int refreshRate,
            int scalingMode, int dpiScalePercentageScale);

    /*
     * Defines a JNI function to immediately apply the given orientation for the given display.
     */
    private native void setOrientation(String dispalayId, int orientationMode);

    /**
     * Immediately applies the given display settings for the given display.
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
     * Immediately applies the given orientation for the given display.
     * 
     * @param displayId
     *            - The ID of the display to apply the orientation for
     * @param orientationMode
     *            - The new orientation for the given display. 0 for landscape, 1 for portrait, 2 for inverted
     *            landscape, and 3 for for inverted portrait
     */
    public void applyDisplayOrientation(String displayId, int orientationMode) {
        setOrientation(displayId, orientationMode);
    }

}