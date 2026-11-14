/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
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
