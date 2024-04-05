package com.dhk.controllers;

import java.awt.DisplayMode;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the Display Mode combo boxes. Listeners are added to the corresponding view components so that 
 * when a new display mode is selected from a Display Mode combo box, the model is updated.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DisplayModeController implements Controller {
	private DhkView view;
	private DhkModel model;
	private SettingsManager settings;
	
	/**
	 * Constructor for the DisplayModeController class.
	 *
	 * @param model - The model for the application.
	 * @param view - The view for the application.
	 * @param settings - The settings manager for the application.
	 */
	public DisplayModeController(DhkModel model, DhkView view, SettingsManager settings) {
		// Get the application's model, view, and settings manager.
		this.model = model;
		this.view = view;
		this.settings = settings;
	}
	
	/**
	 *  This method initializes the listeners for the display mode combo boxes.
	 */
	public void initListeners() {
		// Set the action listener for each slot in the view.
		for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
			// The index for the slot view to add an action listener to.
			int slotIndex = i;
			
			// Set action listeners for display mode changes from the view.
			view.getSlot(slotIndex).getDisplayModes().addActionListener(e -> saveSlotDisplayMode(slotIndex));
									
			// Set mouse listeners for the display mode combo boxes from the view.
			view.getSlot(slotIndex).getDisplayModes().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					// Set the focus on the display mode combo boxes when the mouse hovers over it.
					view.getSlot(slotIndex).getDisplayModes().requestFocusInWindow();
				}
			});
		}
	}
	
	/**
	 * This method updates the model's display mode for the specified slot with the selected display mode from the view.
	 */
	private void saveSlotDisplayMode(int slotIndex) {
		// Get the selected display mode for the specified slot from the combo box.
		DisplayMode selectedDisplayMode = (DisplayMode)view.getSlot(slotIndex).getDisplayModes().getSelectedItem();
		
		// Update the specified slot's display mode in the model.
		model.getSlot(slotIndex).setDisplayMode(selectedDisplayMode);
		
		// Save the specified slot's display mode in the settings ini file.
		settings.saveIniSlotDisplayMode(slotIndex + 1, selectedDisplayMode.getWidth(), selectedDisplayMode.getHeight(), 
				selectedDisplayMode.getBitDepth(), selectedDisplayMode.getRefreshRate());
	}
}
