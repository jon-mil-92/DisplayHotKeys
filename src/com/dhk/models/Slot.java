package com.dhk.models;

import java.awt.DisplayMode;

/**
 * This class represents the model for a Slot. The display mode, display scale, and hot key for a slot are initialized
 * here.
 * 
 * @author Jonathan Miller
 * @version 1.2.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class Slot {
    private DisplayMode displayMode;
    private int scalingMode;
    private int displayScale;
    private HotKey hotKey;

    /**
     * Constructor for the Slot class.
     * 
     * @param displayMode    - The display mode for the slot.
     * @param scalingMode    - The scaling mode for the slot.
     * @param displayScale   - The display scale for the slot.
     * @param changingHotKey - The "changing hotkey" state for the slot.
     * @param hotKey         - The hotkey for the slot.
     */
    public Slot(DisplayMode displayMode, int scalingMode, int displayScale, boolean changingHotKey, HotKey hotKey) {
        // Initialize hotkey slot fields.
        this.displayMode = displayMode;
        this.scalingMode = scalingMode;
        this.displayScale = displayScale;
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
     * Getter for the display scale of the slot.
     * 
     * @return The display scale of the slot.
     */
    public int getDisplayScale() {
        return displayScale;
    }

    /**
     * Setter for the display scale of the slot.
     * 
     * @param displayScale - The new display scale of the slot.
     */
    public void setDisplayScale(int displayScale) {
        this.displayScale = displayScale;
    }

    /**
     * Getter for the hotkey for the slot.
     * 
     * @return The hotkey for the slot.
     */
    public HotKey getHotKey() {
        return hotKey;
    }

    /**
     * Setter for the hotkey for the slot.
     * 
     * @param hotKey - The new hotkey for the slot.
     */
    public void setHotKey(HotKey hotKey) {
        this.hotKey = hotKey;
    }
}
