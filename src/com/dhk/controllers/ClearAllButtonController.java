package com.dhk.controllers;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Clear All button. Listeners are added to the corresponding view component so that when the 
 * Clear All button is pressed, the display mode, display scale, and hot key for each slot is set to default.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class ClearAllButtonController implements Controller {
	private DhkModel model;
	private DhkView view;
	private SettingsManager settings;
	private FrameUpdater frameUpdater;
	
	/**
	 * Constructor for the ClearAllButtonController class.
	 * 
	 * @param model - The model for the application.
	 * @param view - The view for the application.
	 * @param settings - The settings manager of the application.
	 */
	public ClearAllButtonController (DhkModel model, DhkView view, SettingsManager settings) {
		// Get the application's model, view, and settings manager.
		this.model = model;
		this.view = view;
		this.settings = settings;
		
		// Create the frame updater object that will be used to refresh the frame once all slots are cleared.
		frameUpdater = new FrameUpdater(view);
	}
	
	/**
	 * This method initializes the listeners for the clear all button.
	 */
	@Override
	public void initListeners() {
		// Start the action listener for the clear all button action.
		view.getClearAllButton().addActionListener(e -> clearAllButtonAction());
		
		// Set the state change listener for the clear all button.
		view.getClearAllButton().addChangeListener(e -> clearAllButtonStateChangeAction());
		
		// Set the focus listener for the clear all button from the view.
		view.getClearAllButton().addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// Switch to the rollover state when the clear all button is focused.
				view.getClearAllButton().getModel().setRollover(true);
			}

			@Override
			public void focusLost(FocusEvent e) {
				// Leave the rollover state when the clear all button is not focused.
				view.getClearAllButton().getModel().setRollover(false);
			}
		});
		
		// Set the mouse listener for the clear all hot keys button.
		view.getClearAllButton().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				// Set the focus on the clear all hot keys button when the mouse hovers over it.
				view.getClearAllButton().requestFocusInWindow();
			}
		});
	}
	
	/**
	 * Clear all slots and then update the UI.
	 */
	private void clearAllButtonAction() {
		// Set the default values for all slots.
		clearAllDisplayModes();
		clearAllDisplayScales();
		clearAllHotKeys();
		
		// Update the frame.
		frameUpdater.updateUI();
	}
	
	/**
	 * Clear all display modes and save the changes.
	 */
	private void clearAllDisplayModes() {
		// Get the array of supported display modes for the main display.
		DisplayMode[] displayModes = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDisplayModes();
					
		// Get the default highest display mode.
		DisplayMode defaultDisplayMode = displayModes[displayModes.length - 1];
					
		// For all slots...
		for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
			// Set the slot's display mode to the default highest value.
			model.getSlot(i).setDisplayMode(defaultDisplayMode);
			
			// Update the display mode in the view for the slot.
			view.getSlot(i).getDisplayModes().setSelectedIndex(0);
			
			// Save the display mode in the settings.
			settings.saveIniSlotDisplayMode(i + 1, defaultDisplayMode.getWidth(), defaultDisplayMode.getWidth(), 
					defaultDisplayMode.getBitDepth(), defaultDisplayMode.getRefreshRate());
		}
	}
	
	/**
	 * Clear all display scales and save the changes.
	 */
	private void clearAllDisplayScales() {
		// For all slots...
		for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
			// Reset the display scale from the slot's model.
			model.getSlot(i).setDisplayScale(100);
			
			// Update the display scale in the view for the slot.
			view.getSlot(i).getDisplayScales().setSelectedIndex(0);
			
			// Save the display scale in the settings.
			settings.saveIniSlotDisplayScale(i + 1, 100);
		}
	}
	
	/**
	 * Clear all hot keys and save the changes.
	 */
	private void clearAllHotKeys() {
		// For all slots...
		for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
			// Clear the hot key's keys from the slot's model.
			model.getSlot(i).getHotKey().getKeys().clear();
			
			// Update the hot key in the view for the slot.
			view.getSlot(i).getHotKey().setText("Not Set!");
			
			// Save the hot key in the settings.
			settings.saveIniSlotHotKey(i + 1, model.getSlot(i).getHotKey());
		}
	}
	
	/**
	 * This method changes the clear all button icon depending on the button's state.
	 */
	private void clearAllButtonStateChangeAction() {
		// If the user is holding the action button on the clear all button...
		if (view.getClearAllButton().getModel().isArmed()) {
			// Use the pressed icon for the clear all button.
			view.getClearAllButton().setIcon(view.getClearAllButton().getClearAllPressedIcon());
		}
		// If the user is hovering on the clear all button...
		else if (view.getClearAllButton().getModel().isRollover()) {
			// Use the hover icon for the clear all button.
			view.getClearAllButton().setIcon(view.getClearAllButton().getClearAllHoverIcon());
		}
		// Otherwise, if the user is not interacting with the clear all button...
		else {
			// Use the idle icon for the clear all button.
			view.getClearAllButton().setIcon(view.getClearAllButton().getClearAllIdleIcon());
		}
	}
}
