package com.dhk.model;

import java.awt.DisplayMode;

/**
 * This class represents the model for a Slot. The display mode, scaling mode, DPI scale percentage, and hot key for a
 * slot are initialized here.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class Slot {
    private DisplayMode displayMode;
    private int scalingMode;
    private int dpiScalePercentage;
    private HotKey hotKey;

    /**
     * Constructor for the Slot class.
     * 
     * @param displayMode        - The display mode for the slot.
     * @param scalingMode        - The scaling mode for the slot.
     * @param dpiScalePercentage - The DPI scale percentage for the slot.
     * @param changingHotKey     - The "changing hot key" state for the slot.
     * @param hotKey             - The hot key for the slot.
     */
    public Slot(DisplayMode displayMode, int scalingMode, int dpiScalePercentage, boolean changingHotKey,
            HotKey hotKey) {
        // Initialize fields.
        this.displayMode = displayMode;
        this.scalingMode = scalingMode;
        this.dpiScalePercentage = dpiScalePercentage;
        this.hotKey = hotKey;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the display mode of the slot.
     * 
     * @return The display mode of the slot.
     */
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Setter for the display mode of the slot.
     * 
     * @param displayMode - The new display mode for the slot.
     */
    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * Getter for the scaling mode of the slot.
     * 
     * @return The scaling mode of the slot.
     */
    public int getScalingMode() {
        return scalingMode;
    }

    /**
     * Setter for the scaling mode of the slot.
     * 
     * @param scalingMode - The new scaling mode of the slot.
     */
    public void setScalingMode(int scalingMode) {
        this.scalingMode = scalingMode;
    }

    /**
     * Getter for the DPI scale percentage of the slot.
     * 
     * @return The DPI scale percentage of the slot.
     */
    public int getDpiScalePercentage() {
        return dpiScalePercentage;
    }

    /**
     * Setter for the DPI scale percentage of the slot.
     * 
     * @param dpiScalePercentage - The new DPI scale percentage of the slot.
     */
    public void setDpiScalePercentage(int dpiScalePercentage) {
        this.dpiScalePercentage = dpiScalePercentage;
    }

    /**
     * Getter for the hot key for the slot.
     * 
     * @return The hot key for the slot.
     */
    public HotKey getHotKey() {
        return hotKey;
    }

    /**
     * Setter for the hot key for the slot.
     * 
     * @param hotKey - The new hot key for the slot.
     */
    public void setHotKey(HotKey hotKey) {
        this.hotKey = hotKey;
    }
}
