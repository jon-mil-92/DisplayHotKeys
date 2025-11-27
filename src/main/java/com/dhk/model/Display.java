package com.dhk.model;

import java.util.List;

/**
 * Defines the model for a Display. The id, active number of hot key slots, and array list of hot key slots are
 * initialized here.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class Display {

    private String id;
    private int numOfActiveSlots;
    private List<Slot> slots;

    /**
     * Constructor for the Display class.
     * 
     * @param id
     *            - The ID of the display
     * @param numOfActiveSlots
     *            - The number of active hot key slots for the display
     * @param slots
     *            - The list of active hot key slots for the display
     */
    public Display(String id, int numOfActiveSlots, List<Slot> slots) {
        this.id = id;
        this.numOfActiveSlots = numOfActiveSlots;
        this.slots = slots;
    }

    /**
     * Gets the ID of the display.
     * 
     * @return The ID of the display
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the number of active hot key slots for the display.
     * 
     * @return The number of active hot key slots for the display
     */
    public int getNumOfActiveSlots() {
        return numOfActiveSlots;
    }

    /**
     * Sets the number of active hot key slots for the display.
     * 
     * @param numOfActiveSlots
     *            - The number of active hot key slots for the display
     */
    public void setNumOfActiveSlots(int numOfActiveSlots) {
        this.numOfActiveSlots = numOfActiveSlots;
    }

    /**
     * Gets a requested active hot key slot for the display.
     * 
     * @param slotIndex
     *            - The index of the active hot key slot to get
     *
     * @return The requested active hot key slot for the display
     */
    public Slot getSlot(int slotIndex) {
        return slots.get(slotIndex);
    }

}
