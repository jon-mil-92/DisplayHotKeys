package com.dhk.model;

import java.awt.DisplayMode;

/**
 * Defines the model for a Slot. The display mode, scaling mode, DPI scale percentage, orientation mode, and hot key for
 * a slot are initialized here.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class Slot {

    private DisplayMode displayMode;
    private int scalingMode;
    private int dpiScalePercentage;
    private int orientationMode;
    private HotKey hotKey;
    private boolean clearingSlot;

    /**
     * Constructor for the Slot class.
     * 
     * @param displayMode
     *            - The display mode for the slot
     * @param scalingMode
     *            - The scaling mode for the slot
     * @param dpiScalePercentage
     *            - The DPI scale percentage for the slot
     * @param orientationMode
     *            - The orientation mode for the display
     * @param changingHotKey
     *            - The "changing hot key" state for the slot
     * @param hotKey
     *            - The hot key for the slot
     */
    public Slot(DisplayMode displayMode, int scalingMode, int dpiScalePercentage, int orientationMode,
            boolean changingHotKey, HotKey hotKey) {
        this.displayMode = displayMode;
        this.scalingMode = scalingMode;
        this.dpiScalePercentage = dpiScalePercentage;
        this.orientationMode = orientationMode;
        this.hotKey = hotKey;
        this.clearingSlot = false;
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
     * Gets the orientation mode for the slot.
     * 
     * @return The orientation mode for the slot
     */
    public int getOrientationMode() {
        return orientationMode;
    }

    /**
     * Sets the orientation mode for the slot.
     * 
     * @param orientationMode
     *            - The orientation mode for the slot
     */
    public void setOrientationMode(int orientationMode) {
        this.orientationMode = orientationMode;
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

    /**
     * Gets whether or not the slot is being cleared.
     * 
     * @return Whether or not the slot is being cleared
     */
    public boolean isClearingSlot() {
        return clearingSlot;
    }

    /**
     * Sets whether or not the slot is being cleared.
     * 
     * @param clearingSlot
     *            - Whether or not the slot is being cleared
     */
    public void setClearingSlot(boolean clearingSlot) {
        this.clearingSlot = clearingSlot;
    }

}
