package com.dhk.io;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.dhk.models.HotKey;
import com.dhk.models.Key;

/**
 * This class saves the application settings in an ini file. It enables the saving of the active number of slots, the 
 * theme state, the run on startup state, the display modes, the display scales, and the hot keys.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class SettingsManager {
	private JIniFile ini;
	private DisplayMode[] displayModes;
	private KeyText keyText;
	
	// The max number of visible slots in the application frame.
	private final int MAX_NUM_OF_SLOTS = 8;
	
	/**
	 * Constructor for the AppSettingsManager class.
	 */
	public SettingsManager() {
		// Get the array of supported display modes for the main display.
		displayModes = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayModes();
		
		// Get the path for the settings folder that will hold the settings file.
		String settingsPath = System.getProperty("user.home") + "\\Documents\\DisplayHotKeys\\";
		
		// Create the application's settings folder in the user's Documents folder.
		new File(settingsPath).mkdirs();
		
		// Set the full path to the settings file.
		settingsPath = settingsPath + "settings.ini";
		
		// Try to create an ini object from the settings file path.
		try {
			// Create the new settings file object.
			ini = new JIniFile(settingsPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Instantiate the settings validator object.
		SettingsValidator validator = new SettingsValidator(this);
			
		// Validate each property value in the settings file.
		validator.validateAllProperties();
		
		// Initialize the key text object to get the correct text representation of a key given a key code.
		keyText = new KeyText();
	}
	
	//------------------------------------------------------------------------------------------------------------------
    // Settings File Getters and Setters
    //------------------------------------------------------------------------------------------------------------------

	/**
	 * Getter for the number of slots property value from the settings file object.
	 * 
	 * @return The value for the number of slots property.
	 */
	public int getIniNumOfSlots() {
		// Get the number of slots property value from the settings file object.
		return ini.readInteger("Application", "numOfSlots", 1);
	}

	
	/**
	 * Setter for the number of slots property value in the settings file object.
	 * 
	 * @param numOfSlots - The new value for the number of slots property.
	 */
	public void saveIniNumOfSlots(int numOfSlots) {
		// Write the new number of slots property value to the settings file object.
		ini.writeInteger("Application", "numOfSlots", numOfSlots);
		
		updateSettingsFile();
	}
	
	/**
	 * Getter for the dark mode property value from the settings file object.
	 * 
	 * @return The value for the dark mode property.
	 */
	public boolean getIniDarkMode() {
		// Get the dark mode property value from the settings file object.
		return ini.readBool("Application", "darkMode", false);
	}
	
	/**
	 * Setter for the dark mode property value in the settings file object.
	 * 
	 * @param darkMode - The new value for the dark mode property.
	 */
	public void saveIniDarkMode(boolean darkMode) {
		// Write the new dark mode property value to the settings file object.
		ini.writeBool("Application", "darkMode", darkMode);
		
		updateSettingsFile();
	}
	
	/**
	 * Getter for the run on startup property value from the settings file object.
	 * 
	 * @return The value for the run on startup property.
	 */
	public boolean getIniRunOnStartup() {
		// Get the run on startup property value from the settings file object.
		return ini.readBool("Application", "runOnStartup", false);
	}
	
	/**
	 * Setter for the run on startup property value in the settings file object.
	 * 
	 * @param runOnStartup - The new value for the run on startup property.
	 */
	public void saveIniRunOnStartup(boolean runOnStartup) {
		// Write the new run on startup property value to the settings file object.
		ini.writeBool("Application", "runOnStartup", runOnStartup);
		
		updateSettingsFile();
	}

	/**
	 * Getter for the specified slot's display mode built from the display mode properties in the settings file object.
	 * 
	 * @param slotNumber - The slot number to get the display mode for.
	 * @return The display mode from the display property values for the specified slot number.
	 */
	public DisplayMode getIniSlotDisplayMode(int slotNumber) {
		// Get the default highest display mode.
		DisplayMode defaultDisplayMode = displayModes[displayModes.length - 1];
				
		// Build the display mode from the display mode properties for the specified slot in the settings file object.
		DisplayMode slotDisplayMode = new DisplayMode(
				ini.readInteger("Slot" + Integer.toString(slotNumber), "displayModeWidth", 
						defaultDisplayMode.getWidth()), 
				ini.readInteger("Slot" + Integer.toString(slotNumber), "displayModeHeight", 
						defaultDisplayMode.getHeight()), 
				ini.readInteger("Slot" + Integer.toString(slotNumber), "displayModeBitDepth", 
						defaultDisplayMode.getBitDepth()), 
				ini.readInteger("Slot" + Integer.toString(slotNumber), "displayModeRefreshRate", 
						defaultDisplayMode.getRefreshRate()));
		
		return slotDisplayMode;
	}

	/**
	 * Setter for the specified slot's display mode properties in the settings file object.
	 * 
	 * @param slotNumber - The slot number to set the display mode for.
	 * @param width - The new display mode width for the specified slot.
	 * @param height - The new display mode height for the specified slot.
	 * @param bitDepth - The new display mode bit depth for the specified slot.
	 * @param refreshRate - The new display mode refresh rate for the specified slot.
	 */
	public void saveIniSlotDisplayMode(int slotNumber, int width, int height, int bitDepth, int refreshRate) {
		// Write the new display mode property values for the specified slot to the settings file object.
		ini.writeInteger("Slot" + Integer.toString(slotNumber), "displayModeWidth", width);
		ini.writeInteger("Slot" + Integer.toString(slotNumber), "displayModeHeight", height);
		ini.writeInteger("Slot" + Integer.toString(slotNumber), "displayModeBitDepth", bitDepth);
		ini.writeInteger("Slot" + Integer.toString(slotNumber), "displayModeRefreshRate", refreshRate);
		
		updateSettingsFile();
	}

	/**
	 * Getter for the specified slot's display scale property value from the settings file  object.
	 * 
	 * @param slotNumber - The slot number to get the display scale for.
	 * @return The specified slot's display scale property value.
	 */
	public int getIniSlotDisplayScale(int slotNumber) {
		// Get the display scale property value from the settings file object.
		return ini.readInteger("Slot" + Integer.toString(slotNumber), "displayScale", 100);
	}

	/**
	 * Setter for the specified slot's display scale property value in the settings file  object.
	 * 
	 * @param slotNumber - The slot number to set the display scale for.
	 * @param displayScale - The specified slot's new value for the display scale property.
	 */
	public void saveIniSlotDisplayScale(int slotNumber, int displayScale) {
		// Write the new display scale property value for the specified slot to the settings file.
		ini.writeInteger("Slot" + Integer.toString(slotNumber), "displayScale", displayScale);
		
		updateSettingsFile();
	}

	/**
	 * Getter for the specified slot's hotkey built from the hotkey properties in the settings file object.
	 * 
	 * @param slotNumber - The slot number to get the hotkey for.
	 * @return The specified slot's hotkey.
	 */
	public HotKey getIniSlotHotKey(int slotNumber) {
		// Create a new hotkey to store the retrieved key property values.
		HotKey hotKey = new HotKey(new ArrayList<Key>());
		
		// Get the hotkey size property value from the settings file object.
		int hotKeySize = ini.readInteger("Slot" + Integer.toString(slotNumber), "hotKeySize", 1);
		
		// For each stored key that makes up the hotkey...
		for (int i = 1; i <= hotKeySize; i++) {
			// Get the current key property value from the settings file object.
			int keyCode = ini.readInteger("Slot" + Integer.toString(slotNumber), "key" + Integer.toString(i), 1);
			
			// Build the hotkey array list of keys by retrieving each key property value from the settings file.
			hotKey.getKeys().add(new Key(keyCode, keyText.getKeyCodeText(keyCode), false));
		}
		
		return hotKey;
	}

	/**
	 * Setter for the specified slot's hotkey properties in the settings file object.
	 * 
	 * @param slotNumber - The slot number to set the hotkey for.
	 * @param hotKey - The specified slot's hotkey.
	 */
	public void saveIniSlotHotKey(int slotNumber, HotKey hotKey) {
		// Get the new hotkey size.
		int hotKeySize = hotKey.getKeys().size();
		
		// Store the updated hotkey size for the specified slot.
		ini.writeInteger("Slot" + Integer.toString(slotNumber), "hotKeySize", hotKeySize);
		
		// For each key in the hotkey...
		for (int i = 0; i < 3; i++) {
			// Only update key codes for the active keys in the hotkey.
			if (i < hotKeySize) {
				// Get the key code for the current key in the hotkey.
				int keyCode = hotKey.getKeys().get(i).getKey();
				
				// Write the retrieved key code value into the settings file object for the current key.
				ini.writeInteger("Slot" + Integer.toString(slotNumber), "key" + Integer.toString(i + 1), keyCode);
			}
			// Otherwise, reset the key code for the unused keys.
			else {
				ini.writeInteger("Slot" + Integer.toString(slotNumber), "key" + Integer.toString(i + 1), 0);
			}
		}
		
		updateSettingsFile();
	}
	
	//------------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Getter for the ini file that holds the application settings.
	 * 
	 * @return The settings ini file.
	 */
	public JIniFile getIni() {
		return ini;
	}
	
	/**
	 * Getter for the max number of slots.
	 * 
	 * @return The max number of slots.
	 */
	public int getMaxNumOfSlots() {
		return MAX_NUM_OF_SLOTS;
	}
	
	/**
	 * Getter for the array of supported display modes.
	 * 
	 * @return The array of supported display modes.
	 */
	public DisplayMode[] getDisplayModes() {
		return displayModes;
	}
	
	//------------------------------------------------------------------------------------------------------------------
    // Private Methods
    //------------------------------------------------------------------------------------------------------------------
	
	/**
	 * This method simply wraps the JIniFile update file method in a try/catch block.
	 */
	private void updateSettingsFile() {
		// Try to write the new property values to settings file.
		try {
			ini.updateFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
