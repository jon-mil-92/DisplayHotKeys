package com.dhk.model;

import java.util.ArrayList;

/**
 * This class defines the model for a Display. The id, active number of hot key slots, orientation mode, and array list
 * of hot key slots are initialized here.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class Display {
    private String id;
    private int numOfActiveSlots;
    private int orientationMode;
    private ArrayList<Slot> slots;

    /**
     * Constructor for the Display class.
     * 
     * @param id               - The ID of the display.
     * @param numOfActiveSlots - The number of active hot key slots for the display.
     * @param orientationMode  - The orientation mode for the display.
     * @param slots            - The array of active hot key slots for the display.
     */
    public Display(String id, int numOfActiveSlots, int orientationMode, ArrayList<Slot> slots) {
        // Initialize fields.
        this.id = id;
        this.numOfActiveSlots = numOfActiveSlots;
        this.orientationMode = orientationMode;
        this.slots = slots;
    }

    /**
     * Getter for the ID of the display.
     * 
     * @return The ID of the display.
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for the number of active hot key slots for the display.
     * 
     * @return The number of active hot key slots for the display.
     */
    public int getNumOfActiveSlots() {
        return numOfActiveSlots;
    }

    /**
     * Setter for the number of active hot key slots for the display.
     * 
     * @param numOfActiveSlots - The number of active hot key slots for the display.
     */
    public void setNumOfActiveSlots(int numOfActiveSlots) {
        this.numOfActiveSlots = numOfActiveSlots;
    }

    /**
     * Getter for the orientation mode for the display.
     * 
     * @return The orientation mode for the display.
     */
    public int getOrientationMode() {
        return orientationMode;
    }

    /**
     * Setter for the orientation mode for the display.
     * 
     * @param orientationMode - The orientation mode for the display.
     */
    public void setOrientationMode(int orientationMode) {
        this.orientationMode = orientationMode;
    }

    /**
     * Getter for a requested active hot key slot for the display.
     * 
     * @param slotIndex - The index of the active hot key slot to get.
     * @return The requested active hot key slot for the display.
     */
    public Slot getSlot(int slotIndex) {
        return slots.get(slotIndex);
    }
}
