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
package com.dhk.model;

import java.util.List;

/**
 * Defines the model for a Display. The id, active number of hot key slots, and array list of hot key slots are
 * initialized here.
 *
 * @author Jonathan R. Miller
 */
public class Display {

    private String id;
    private int numOfActiveSlots;
    private List<Slot> slots;

    /**
     * Constructor for the {@link Display} class.
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
