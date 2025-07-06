package com.dhk.controllers;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.ui.DhkView;
import com.dhk.ui.Theme;
import com.dhk.window.FrameUpdater;

/**
 * This class controls the Theme button. Listeners are added to the corresponding view component so that 
 * when the Theme button is pressed, the application's theme will be toggled between "Light" and "Dark."
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class ThemeButtonController implements Controller {
	private DhkView view;
	private SettingsManager settings;
	private Theme themeChooser;
	private FrameUpdater frameUpdater;
	
	/**
	 * Constructor for the ThemeButtonController class.
	 *
	 * @param view - The view for the application.
	 * @param settings - The settings manager for the application.
	 */
	public ThemeButtonController(DhkView view, SettingsManager settings) {
		// Initialize fields.
		this.view = view;
		this.settings = settings;
		
		// Initialize the theme chooser that will control the active theme.
		themeChooser = new Theme();
		
		// Initialize the frame updater that will update the application's view.
		frameUpdater = new FrameUpdater(view);
	}
	
	/**
	 * This method initializes the listeners for the theme button.
	 */
	@Override
	public void initListeners() {
		// Start the action listener for the theme button action.
		view.getThemeButton().addActionListener(e -> themeButtonAction());
						
		// Set the state change listener for the theme button.
		view.getThemeButton().addChangeListener(e -> themeButtonStateChangeAction());
								
		// Set the focus listener for the theme button.
		view.getThemeButton().addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// Switch to the rollover state when the theme button is focused.
				view.getThemeButton().getModel().setRollover(true);
			}

			@Override
			public void focusLost(FocusEvent e) {
				// Leave the rollover state when the theme button is not focused.
				view.getThemeButton().getModel().setRollover(false);
			}
		});
						
		// Set the mouse listener for the theme button.
		view.getThemeButton().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				// Set the focus on the theme button when the mouse hovers over it.
				view.getThemeButton().requestFocusInWindow();
			}
		});
	}
	
	/**
	 * Toggle the "dark mode" state and update the UI, and then save the new settings.
	 */
	private void themeButtonAction() {
		// Toggle the dark mode state.
		view.getThemeButton().toggleDarkMode();
		
		// Change the theme using the theme changer.
		themeChooser.useDarkMode(view.getThemeButton().isDarkMode());
						
		// Update the view's UI.
		frameUpdater.updateUI();
		
		// Save the new UI mode into the settings file.
		settings.saveIniDarkMode(view.getThemeButton().isDarkMode());
	}
	
	/**
	 * This method changes the theme button icon depending on the button's state.
	 */
	private void themeButtonStateChangeAction() {
		// If the user is holding the action button on the theme button...
		if (view.getThemeButton().getModel().isArmed()) {
			// Use the corresponding pressed icon.
			setPressedIcon();
		}
		// If the user is hovering on the theme button...
		else if (view.getThemeButton().getModel().isRollover()) {
			// Use the corresponding hover icon.
			setHoverIcon();
		}
		// Otherwise, if the user is not interacting with the theme button...
		else {
			// Use the corresponding idle icon.
			setIdleIcon();
		}	
	}
	
	/**
	 * This method sets the pressed icon corresponding to the "dark mode" state.
	 */
	private void setPressedIcon() {
		// If the UI is in dark mode...
		if (view.getThemeButton().isDarkMode()) {
			// Use the pressed icon for dark mode.
			view.getThemeButton().setIcon(view.getThemeButton().getDarkModePressedIcon());
		}
		else {
			// Use the pressed icon for light mode.
			view.getThemeButton().setIcon(view.getThemeButton().getLightModePressedIcon());
		}
	}
	
	/**
	 * This method sets the hover icon corresponding to the "dark mode" state.
	 */
	private void setHoverIcon() {
		// If the UI is in dark mode...
		if (view.getThemeButton().isDarkMode()) {
			// Use the hover icon for dark mode.
			view.getThemeButton().setIcon(view.getThemeButton().getDarkModeHoverIcon());
		}
		else {
			// Use the hover icon for light mode.
			view.getThemeButton().setIcon(view.getThemeButton().getLightModeHoverIcon());
		}
	}
	
	/**
	 * This method sets the idle icon corresponding to the "dark mode" state.
	 */
	private void setIdleIcon() {
		// If the UI is in dark mode...
		if (view.getThemeButton().isDarkMode()) {
			// Use the idle icon for dark mode.
			view.getThemeButton().setIcon(view.getThemeButton().getDarkModeIcon());
		}
		else {
			// Use the idle icon for light mode.
			view.getThemeButton().setIcon(view.getThemeButton().getLightModeIcon());
		}
	}
}
