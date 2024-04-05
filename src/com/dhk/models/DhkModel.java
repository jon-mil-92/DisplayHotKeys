package com.dhk.models;

import java.util.ArrayList;
import com.dhk.io.SettingsManager;

/**
 * This class is the primary model of Display Hot Keys. Each slot in the application is initialized here.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DhkModel {
	private ArrayList<Slot> slots;
	private int numOfSlots;
	private int maxNumOfSlots;
	
	/**
	 * This method initializes the number of active slots and each slot in the application from the settings file.
	 * 
	 * @param settings - The manager of the application's settings file.
	 */
	public void initSlots (SettingsManager settings) {
		// Initialize the maximum number of slots that can be displayed in the application.
		maxNumOfSlots = settings.getMaxNumOfSlots();
		
		// Initialize the number of hotkey slots that will appear in the view.
		numOfSlots = settings.getIniNumOfSlots();
				
		// Initialize the array list of hotkey slots.
		slots = new ArrayList<Slot>(maxNumOfSlots);
				
		// Add new hotkey slots to the array list of hotkey slots.
		for (int i = 1; i <= maxNumOfSlots; i++) {
			slots.add(new Slot(settings.getIniSlotDisplayMode(i), settings.getIniSlotScalingMode(i), 
					settings.getIniSlotDisplayScale(i), false, settings.getIniSlotHotKey(i)));
		}
	}
	
	//------------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Getter for the active number of slots in the view.
	 * 
	 * @return The active number of slots in the view.
	 */
	public int getNumOfSlots() {
		return numOfSlots;
	}

	/**
	 * Setter for the active number of slots in the view.
	 * 
	 * @param numberOfSlots - The new active number of slots in the view.
	 */
	public void setNumOfSlots(int numberOfSlots) {
		this.numOfSlots = numberOfSlots;
	}
	
	/**
	 * Getter for the max number of slots in the view.
	 * 
	 * @return The max number of slots in the view.
	 */
	public int getMaxNumOfSlots() {
		return maxNumOfSlots;
	}

	/**
	 * Getter for the array list of slots.
	 * 
	 * @return The array list of slots.
	 */
	public ArrayList<Slot> getSlots() {
		return slots;
	}
	
	/**
	 * Getter for the specified slot.
	 * 
	 * @param slotIndex - The index of the slot to get.
	 * @return The the specified slot.
	 */
	public Slot getSlot(int slotIndex) {
		return slots.get(slotIndex);
	}
}
