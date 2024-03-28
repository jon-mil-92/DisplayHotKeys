package com.dhk.controllers;

import com.dhk.io.SettingsManager;
import com.dhk.io.DisplaySettings;
import com.dhk.io.KeyText;
import com.dhk.models.DhkModel;
import com.dhk.models.HotKey;
import com.dhk.models.Key;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;
import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;
import java.util.ArrayList;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This class controls the hot keys for the application. Listeners are added to the Change Hot Key buttons to enable the 
 * functionality of changing hot keys. This class is also responsible for triggering hot key events once a hot key press
 * is detected.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class HotKeysController implements Controller, GlobalKeyListener {
	private SettingsManager settings;
	private DhkView view;
	private DhkModel model;
	private DisplaySettings displaySettings;
	private HotKey hotKeyBackup;
	private KeyText keyText;
	private Timer inputTimer;
	private FrameUpdater frameUpdater;
	private int currentKeyCount;
	private int maxNumOfSlots;
	private boolean idleUser;
	
	// Allow three seconds for idle user before the change hotkey state reverts.
	private final static int IDLE_INPUT_TIMEOUT = 3000;
	
	// Allow one second between releasing input key presses before leaving the changing hotkey state.
	private final static int INPUT_RELEASE_TIMEOUT = 1000;
	
	// Only get the first three unique keys while changing the hotkey keys.
	private final int MAX_KEY_COUNT = 3;

	/**
	 * Constructor for the HotKeysController class.
	 *
	 * @param model - The model for the application.
	 * @param view - The view for the application.
	 * @param settings - The settings manager for the application.
	 */
	public HotKeysController(DhkModel model, DhkView view, SettingsManager settings) {
		// Initialize the global keyboard input hook.
		GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook(true);
		
		// Register the global keyboard listener.
		keyboardHook.addKeyListener(this);
		
		// Initialize fields.
		this.model = model;
		this.view = view;
		this.settings = settings;
		currentKeyCount = 0;
		idleUser = true;
		
		// Initialize a new frame updater object that will be used to refresh the frame once a hotkey is changed.
		frameUpdater = new FrameUpdater(view);
		
		// Get the maximum number of slots that can be displayed in the application from the settings manager.
		maxNumOfSlots = settings.getMaxNumOfSlots();
		
		// Initialize the display settings object that will update the display settings.
		displaySettings = new DisplaySettings();
		
		// Initialize the key text object to get the correct text representation of a key given a key code.
		keyText = new KeyText();
	}
	
	//------------------------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------------------------
	
	/**
	 * This method initializes the listeners for hotkey input.
	 */
	public void initListeners() {
		// Set the action listener for each change hotkey button in the view.
		for (int i = 0; i < maxNumOfSlots; i++) {
			// The index for the slot view to add an action listener to.
			int slotIndex = i;
			
			// Set action listeners for the change hotkey button presses from the view.
			view.getSlot(slotIndex).getChangeHotKeyButton().addActionListener(e -> slotHotKeyChangeEvent(slotIndex));
			
			// Set mouse listeners for the change hotkey buttons from the view.
			view.getSlot(slotIndex).getChangeHotKeyButton().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					// Set the focus on the change hotkey buttons when the mouse hovers over it.
					view.getSlot(slotIndex).getChangeHotKeyButton().requestFocusInWindow();
				}
			});
		}
	}
	
	@Override
	public void keyPressed(GlobalKeyEvent keyEvent) {
		// Use the AWT event dispatch thread for key press events.
    	SwingUtilities.invokeLater(new Runnable() {
    		@Override
    		public void run() {
				// For each slot...
				for (int i = 0; i < maxNumOfSlots; i++) {
					// Set the slot's pressed hotkey keys.
					setPressedKeys(keyEvent, model.getSlot(i).getHotKey().getKeys());
							
					// Update each hotkey state every time a hotkey is pressed.
					updateHotKeyState(model.getSlot(i).getHotKey());
							
					// Only check the active slots for building a new key or settings a display mode.
					if (i < model.getNumOfSlots()) {
						// If the user is changing a slot's hotkey...
						if (model.getSlot(i).getHotKey().isChangingHotKey()) {
							// Build the slot's hotkey from the pressed keys during the "changing hotkey" state.
							buildHotKey(keyEvent, model.getSlot(i).getHotKey(), i);
											
							// If any hotkey is a subset of another...
							if (anyHotKeySubset()) {
								// Notify the user that hotkeys cannot be a subset of another hotkey.
								view.getSlot(i).getChangeHotKeyButton().setText("No Subsets!");
							}
							//Otherwise, keep notifying the user to release the hotkey to set it.
							else {
								// Update the change hotkey button text to notify user to input keys.
								view.getSlot(i).getChangeHotKeyButton().setText("Release To Set");
							}
						}
										
						// If no hotkey is being changed and a hotkey is pressed but not held down...
						if (!changingHotKey() && model.getSlot(i).getHotKey().isHotKeyPressed() 
								&& !model.getSlot(i).getHotKey().isHotKeyHeldDown()) {
							// Set the display settings.
							displaySettings.setDisplay(
									Integer.toString(model.getSlot(i).getDisplayMode().getWidth()),
									Integer.toString(model.getSlot(i).getDisplayMode().getHeight()),
									Integer.toString(model.getSlot(i).getDisplayMode().getBitDepth()), 
									Integer.toString(model.getSlot(i).getDisplayMode().getRefreshRate()), 
									Integer.toString(model.getSlot(i).getDisplayScale()));
						}
					}
				}
    		}
    	});
	}

	@Override
	public void keyReleased(GlobalKeyEvent keyEvent) {
		// For each slot...
		for (int i = 0; i < maxNumOfSlots; i++) {
			// Set the hotkey's keys to not pressed.
			setReleasedKeys(keyEvent, model.getSlot(i).getHotKey().getKeys());
					
			// Update each hotkey state every time a key is released.
			updateHotKeyState(model.getSlot(i).getHotKey());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
    // Private Methods
    //------------------------------------------------------------------------------------------------------------------
	
	/**
	 * This method changes the specified slot's change hotkey button text and updates the model's corresponding boolean.
	 */
	private void slotHotKeyChangeEvent(int slotIndex) {
		// Do not allow changing multiple hotkeys at the same time.
		if (!changingHotKey()) {
			// Create a new hotkey for the backup of the old hotkey.
			hotKeyBackup = new HotKey(new ArrayList<Key>());
			
			// Add all keys from the old hotkey into the backup hotkey.
			hotKeyBackup.getKeys().addAll(model.getSlot(slotIndex).getHotKey().getKeys());
			
			// Start a new key array list for the slot's hotkey.
			model.getSlot(slotIndex).getHotKey().setKeys(new ArrayList<Key>());
			
			// Update the change hotkey button text to notify user to input keys.
			view.getSlot(slotIndex).getChangeHotKeyButton().setText("Press Hot Key");
			
			// Disable interactive view components during changing the hotkey.
			disableComponents();
			
			// Change the slot into the "changing hotkey" state.
			model.getSlot(slotIndex).getHotKey().setChangingHotKey(true);
		
			// Set the idle input timer to stop getting keys after a certain period of inactivity.
			stopSettingHotKeyAfter(IDLE_INPUT_TIMEOUT, slotIndex);
		}
	}
	
	/**
	 * This method checks to see if any slots are in the "changing hotkey" state.
	 * 
	 * @return Whether or not any active slots are in the "changing hotkey" state.
	 */
	private boolean changingHotKey() {
		// Boolean to check the current slot's "changing hotkey" state against.
		boolean changingHotKeys = false;
		
		// For each hotkey slot...
		for (int i = 0; i < model.getNumOfSlots(); i++) {
			// If any hotkey is being changed the boolean will return true...
			changingHotKeys |= (model.getSlot(i).getHotKey().isChangingHotKey());
		}
		
		return changingHotKeys;
	}

	/**
	 * This method disables all interactive view components except the exit button component to avoid unintended 
	 * selection during changing the hotkey.
	 */
	private void disableComponents() {
		// Disable the number of slots combo box while getting user input.
		view.getNumberOfSlots().setEnabled(false);
		
		// Disable the clear all button while getting user input.
		view.getClearAllButton().setEnabled(false);
		
		// Disable the run on startup button while getting user input.
		view.getRunOnStartupButton().setEnabled(false);
		
		// Disable the theme button while getting user input.
		view.getThemeButton().setEnabled(false);
		
		// Disable the minimize button while getting user input.
		view.getMinimizeButton().setEnabled(false);
		
		// Disable the exit button while getting user input.
		view.getExitButton().setEnabled(false);
		
		// Disable all interactive components for each slot in the view.
		for (int i = 0; i < model.getNumOfSlots(); i++) {
			// Disable all display mode combo boxes while getting input from the user.
			view.getSlot(i).getDisplayModes().setEnabled(false);
			
			// Disable all display scale combo boxes while getting input from the user.
			view.getSlot(i).getDisplayScales().setEnabled(false);
			
			// Disable all clear hotkey buttons while getting user input.
			view.getSlot(i).getClearHotKeyButton().setEnabled(false);
			
			// Disable all change hotkey buttons while getting user input.
			view.getSlot(i).getChangeHotKeyButton().setEnabled(false);
		}
	}
	
	/**
	 * This method re-enables all interactive view components except the exit button component because it is already 
	 * enabled.
	 */
	private void enableComponents() {
		// Enable the number of slots combo box after getting user input.
		view.getNumberOfSlots().setEnabled(true);
		
		// Enable the clear all button after getting user input.
		view.getClearAllButton().setEnabled(true);
		
		// Enable the run on startup button after getting user input.
		view.getRunOnStartupButton().setEnabled(true);
		
		// Enable the theme button after getting user input.
		view.getThemeButton().setEnabled(true);
		
		// Enable the minimize button after getting user input.
		view.getMinimizeButton().setEnabled(true);
		
		// Enable the exit button after getting user input.
		view.getExitButton().setEnabled(true);
				
		// Enable all interactive components for each slot in the view.
		for (int i = 0; i < model.getNumOfSlots(); i++) {
			// Enable all display mode combo boxes after getting input from the user.
			view.getSlot(i).getDisplayModes().setEnabled(true);
					
			// Enable all display scale combo boxes after getting input from the user.
			view.getSlot(i).getDisplayScales().setEnabled(true);
			
			// Enable all clear hotkey buttons after getting user input.
			view.getSlot(i).getClearHotKeyButton().setEnabled(true);
			
			// Enable all change hotkey buttons after getting user input.
			view.getSlot(i).getChangeHotKeyButton().setEnabled(true);
		}
	}
	
	/**
	 * Start a timer and stop allowing user input after it has completed if the specified hotkey is released. Otherwise,
	 * the input release timer is started again. If the user did not type any keys before the idle timeout, then the 
	 * original hotkey is restored.
	 * 
	 * @param milliseconds - The timeout value.
	 */
	private void stopSettingHotKeyAfter(int milliseconds, int slotIndex) {
		// Create a new timer to stop user input after the specified number of milliseconds.
		inputTimer = new Timer(milliseconds, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// If the specified hotkey is still pressed while getting user input...
				if (model.getSlot(slotIndex).getHotKey().isHotKeyPressed()) {
					// Create a new timer to set the new hotkey after the hotkey is released.
					stopSettingHotKeyAfter(INPUT_RELEASE_TIMEOUT, slotIndex);
				}
				else {
					// Leave the hotkey's "changing hotkey" state for the specified slot.
					leaveChangingHotKeyState(slotIndex);
				}
			}
		});
		
		// Only execute timeout event once.
		inputTimer.setRepeats(false);
			
		// Start counting down the timer.
		inputTimer.start();
	}
	
	/**
	 * Leave the "changing hotkey" state for the specified slot's hotkey.
	 * 
	 * @param slotIndex - The slot index for the hotkey that is leaving the "changing hotkey" state.
	 */
	private void leaveChangingHotKeyState(int slotIndex) {
		// Reset the key counter.
		currentKeyCount = 0;
		
		// Reset the idle user boolean so that the user will be idle upon enter any "changing hotkey" state.
		idleUser = true;
		
		// If the user did not type any keys before the idle timeout...
		if (model.getSlot(slotIndex).getHotKey().getKeys().size() == 0 || anyHotKeySubset()) {
			// Restore the hotkey to what it was before trying to change it.
			model.getSlot(slotIndex).setHotKey(hotKeyBackup);
			
			// Update the view for the restored hotkey.
			view.getSlot(slotIndex).getHotKey().setText(model.getSlot(slotIndex).getHotKey().getHotKeyString());
			
			// Update the view's frame.
	    	frameUpdater.updateUI();
		}
		
		// Update the hotkey button to notify the user that they can change the hotkey again.
		view.getSlot(slotIndex).getChangeHotKeyButton().setText("Change Hot Key");
			
		// No longer getting input from the user, so update the "changing hotkey" state for the specified slot's hotkey.
		model.getSlot(slotIndex).getHotKey().setChangingHotKey(false);
		
		// Save the new hotkey for the specified slot in the settings file.
		settings.saveIniSlotHotKey(slotIndex + 1, model.getSlot(slotIndex).getHotKey());
		
		// Re-enable disabled components.
		enableComponents();
	}
	
	/**
	 * This method sets hotkey keys as pressed if the given key event matches a key in the hotkey.
	 * 
	 * @param e - The key event that occurred.
	 * @param keys - The array list of keys for a hotkey.
	 */
	private void setPressedKeys(GlobalKeyEvent keyEvent, ArrayList<Key> keys) {
		// For each key in the array list...
		for (int i = 0; i < keys.size(); i++) {
			// If the key event's key code matches the current key in the array list...
			if (keyEvent.getVirtualKeyCode() == keys.get(i).getKey()) {
				// Update the boolean that determines whether the key is being pressed or not.
				keys.get(i).setKeyPressed(true);
			}
		}
	}
	
	/**
	 * This method sets hotkey keys as not pressed if the given key event matches a key in the hotkey.
	 * 
	 * @param e - The key event that occurred.
	 * @param keys - The array list of keys for a hotkey.
	 */
	private void setReleasedKeys(GlobalKeyEvent keyEvent, ArrayList<Key> keys) {
		// For each key in the array list...
		for (int i = 0; i < keys.size(); i++) {
			// If the key event's key code matches the current key in the array list...
			if (keyEvent.getVirtualKeyCode() == keys.get(i).getKey()) {
				// Update the boolean that determines whether the key is being pressed or not.
				keys.get(i).setKeyPressed(false);
			}
		}
	}
	
	/**
	 * This method updates the state of the given hotkey.
	 * 
	 * @param hotKey - The hotkey for the keys to check.
	 * @return Whether or not all keys in a hotkey are pressed.
	 */
	private boolean updateHotKeyState(HotKey hotKey) {
		boolean allKeysPressed = true;
		
		// If the hotkey is not set...
		if (hotKey.getKeys().size() == 0) {
			// Then no keys are pressed.
			allKeysPressed = false;
		}
		
		// For each key in the hotkey array list...
		for (int i = 0; i < hotKey.getKeys().size(); i++) {
			// Check each key state and store the AND conditional of them all.
			allKeysPressed &= hotKey.getKeys().get(i).isKeyPressed();
		}
		
		// If the hotkey is pressed currently, and it was pressed the last time a key event was fired...
		if (allKeysPressed && hotKey.isHotKeyPressed()) {
			// Set the slot's hotkey state to "held down."
			hotKey.setHotKeyHeldDown(true);
		}
		// Else, if the hotkey is pressed now but not the last time a key event was fired...
		else if (allKeysPressed && !hotKey.isHotKeyPressed()){
			// Set the slot's hotkey state to "pressed."
			hotKey.setHotKeyPressed(true);
		}
		// Otherwise, if the hotkey is not currently pressed or held down...
		else {
			// Set the slot's hotkey states to "not pressed" and "not held down."
			hotKey.setHotKeyPressed(false);
			hotKey.setHotKeyHeldDown(false);
		}
		
		return allKeysPressed;
	}
	
	/**
	 * This method builds the new hotkey that will be used to change display settings.
	 * 
	 * @param keyEvent - The native key event that was fired.
	 * @param hotKey - The hotkey to build.
	 * @param slotIndex - The slot index for the hotkey to build.
	 */
	private void buildHotKey(GlobalKeyEvent keyEvent, HotKey hotKey, int slotIndex) {
		// Define a variable to hold the pressed hotkey.
		Key pressedKey;
		
		// Get the key code and text representation for the pressed key.
		int keyCode = keyEvent.getVirtualKeyCode();
		String keyCodeText = keyText.getKeyCodeText(keyCode);
		
		// If the user has not enter the max number of keys an the input timeout has not been reached...
		if (currentKeyCount < MAX_KEY_COUNT) {
			// Create a temporary hotkey for comparison in the following conditional.
			pressedKey = new Key(keyCode, keyCodeText, true);
			
			// Only allow unique keys to form the hotkey, and ignore key code 255 due to a bug in System Hook.
			if (!hotKey.getKeys().contains(pressedKey) && !(pressedKey.getKey() == 255)) {
				// Add the new key into the hotkey array list.
				hotKey.getKeys().add(new Key(keyCode, keyCodeText, false));
				
				// Update the hotkey text in the view for the slot.
				view.getSlot(slotIndex).getHotKey().setText(model.getSlot(slotIndex).getHotKey().getHotKeyString());
				
		    	// Update the view's frame.
		    	frameUpdater.updateUI();
				
				// Increment the key counter.
				currentKeyCount += 1;
			}
		}
		
		// If the hotkey is in the "changing hotkey" state and awaiting user input for the new hotkey keys...
		if (idleUser) {
			// The user is no longer idle since they pressed a key during the hotkey's "changing hotkey" state.
			idleUser = false;
			
			// Stop the current idle timer since the user pressed a key.
			inputTimer.stop();
			
			// Start a new timer that will set the hotkey after the user releases the hotkey.
			stopSettingHotKeyAfter(INPUT_RELEASE_TIMEOUT, slotIndex);
		}
		else {
			// Restart the input release timer if the user presses a key during the changing hot key state.
			inputTimer.restart();
		}
	}
	
	/**
	 * This method checks if a specified slot's hotkey is a subset of another hotkey.
	 * 
	 * @param slotIndex - The index for the slot to check for.
	 * @return Whether or not the specified slot's hotkey is a subset of another.
	 */
	private boolean isHotKeySubset(int slotIndex) {
		// The boolean to determine if all keys from the given slot's hotkey are contained in the current hotkey.
		boolean allKeysContained = false;
		
		// Get the hotkey key array list from the specified slot.
		ArrayList<Key> keys = model.getSlot(slotIndex).getHotKey().getKeys();
		
		// Only compare hotkeys that have been set.
		if (keys.size() > 0) {
			// For each slot...
			for (int i = 0; i < maxNumOfSlots; i++) {
				// Skip comparing the given hotkey to check.
				if (i != slotIndex) {
					// If all of the keys from the given slot's hotkey are in the current hotkey.
					allKeysContained |= model.getSlot(i).getHotKey().getKeys().containsAll(keys);
				}
			}
		}
		
		return allKeysContained;
	}
	
	/**
	 * This method checks if any hotkey is a subset of another hotkey.
	 * 
	 * @return Whether or not there are any hotkey subsets.
	 */
	private boolean anyHotKeySubset() {
		// The boolean to determine if there are any hotkey subsets.
		boolean anyHotKeySubset = false;
		
		// For each slot...
		for (int i = 0; i < maxNumOfSlots; i++) {
			// If any hotkey is a subset of another.
			anyHotKeySubset |= isHotKeySubset(i);
		}
		
		return anyHotKeySubset;
	}
}