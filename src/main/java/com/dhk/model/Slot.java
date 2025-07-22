package com.dhk.model;

import java.awt.DisplayMode;

/**
 * Defines the model for a Slot. The display mode, scaling mode, DPI scale percentage, and hot key for a slot are
 * initialized here.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class Slot {

    private DisplayMode displayMode;
    private int scalingMode;
    private int dpiScalePercentage;
    private HotKey hotKey;

    /**
     * Constructor for the Slot class.
     * 
     * @param displayMode
     *            - The display mode for the slot
     * @param scalingMode
     *            - The scaling mode for the slot
     * @param dpiScalePercentage
     *            - The DPI scale percentage for the slot
     * @param changingHotKey
     *            - The "changing hot key" state for the slot
     * @param hotKey
     *            - The hot key for the slot
     */
    public Slot(DisplayMode displayMode, int scalingMode, int dpiScalePercentage, boolean changingHotKey,
            HotKey hotKey) {
        this.displayMode = displayMode;
        this.scalingMode = scalingMode;
        this.dpiScalePercentage = dpiScalePercentage;
        this.hotKey = hotKey;
    }

    /**
     * Gets the display mode of the slot.
     * 
     * @return The display mode of the slot
     */
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Sets the display mode of the slot.
     * 
     * @param displayMode
     *            - The new display mode for the slot
     */
    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * Gets the scaling mode of the slot.
     * 
     * @return The scaling mode of the slot
     */
    public int getScalingMode() {
        return scalingMode;
    }

    /**
     * Sets the scaling mode of the slot.
     * 
     * @param scalingMode
     *            - The new scaling mode of the slot
     */
    public void setScalingMode(int scalingMode) {
        this.scalingMode = scalingMode;
    }

    /**
     * Gets the DPI scale percentage of the slot.
     * 
     * @return The DPI scale percentage of the slot
     */
    public int getDpiScalePercentage() {
        return dpiScalePercentage;
    }

    /**
     * Sets the DPI scale percentage of the slot.
     * 
     * @param dpiScalePercentage
     *            - The new DPI scale percentage of the slot
     */
    public void setDpiScalePercentage(int dpiScalePercentage) {
        this.dpiScalePercentage = dpiScalePercentage;
    }

    /**
     * Gets the hot key for the slot.
     * 
     * @return The hot key for the slot
     */
    public HotKey getHotKey() {
        return hotKey;
    }

    /**
     * Sets the hot key for the slot.
     * 
     * @param hotKey
     *            - The new hot key for the slot
     */
    public void setHotKey(HotKey hotKey) {
        this.hotKey = hotKey;
    }

}
