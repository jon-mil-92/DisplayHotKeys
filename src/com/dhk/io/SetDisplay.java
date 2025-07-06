package com.dhk.io;

/**
 * This class utilizes the SetDisplay JNI library to immediately apply the given display mode, scaling mode, and DPI
 * scale percentage for the given display.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class SetDisplay {

    /**
     * Default constructor for the SetDisplay class.
     */
    public SetDisplay() {
    }

    // Load the SetDisplay.dll file.
    static {
        try {
            System.loadLibrary("SetDisplay");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    // Define a JNI function to immediately apply the given display settings for the given display.
    private native void setDisplay(String displayId, int resWidth, int resHeight, int bitDepth, int refreshRate,
            int scalingMode, int dpiScalePercentageScale);

    // Define a JNI function to immediately apply the given orientation for the given display.
    private native void setOrientation(String dispalayId, int orientationMode);

    /**
     * This method immediately applies the given display settings for the given display.
     * 
     * @param displayId          - The ID of the display to apply the display settings for.
     * @param resWidth           - The new horizontal resolution for the given display.
     * @param resHeight          - The new vertical resolution for the given display.
     * @param bitDepth           - The new bit depth for the given display.
     * @param refreshRate        - The new refresh rate for the given display.
     * @param scalingMode        - The new scaling mode for the given display.
     * @param dpiScalePercentage - The new DPI scale percentage for the given display.
     */
    public void applyDisplaySettings(String displayId, int resWidth, int resHeight, int bitDepth, int refreshRate,
            int scalingMode, int dpiScalePercentage) {
        // Call the JNI function to immediately apply the given display settings for the given display.
        setDisplay(displayId, resWidth, resHeight, bitDepth, refreshRate, scalingMode, dpiScalePercentage);
    }

    /**
     * This method immediately applies the given orientation for the given display.
     * 
     * @param displayId       - The ID of the display to apply the orientation for.
     * @param orientationMode - The new orientation for the given display. 0 for landscape, 1 for portrait, 2 for
     *                        inverted landscape, and 3 for for inverted portrait.
     */
    public void applyDisplayOrientation(String displayId, int orientationMode) {
        // Call the JNI function to immediately apply the given orientation for the given display.
        setOrientation(displayId, orientationMode);
    }
}