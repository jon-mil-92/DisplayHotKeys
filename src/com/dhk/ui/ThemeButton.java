package com.dhk.ui;

import java.awt.Dimension;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;

/**
 * This class defines the Theme button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.1.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ThemeButton extends JButton {
	private static final long serialVersionUID = 1L;
	private Icon darkModeIcon;
	private Icon lightModeIcon;
	private Icon darkModeHoverIcon;
	private Icon lightModeHoverIcon;
	private Icon darkModePressedIcon;
	private Icon lightModePressedIcon;
	private boolean darkMode;
	
	// Set a fixed size for the button icon.
	private final Dimension BUTTON_ICON_SIZE = new Dimension(48, 48);
	
	/**
	 * Constructor for the ThemeButton class.
	 * 
	 * @param darkMode - The initial dark mode state of the button.
	 * @param darkModeIconPath - The resource path for the dark mode idle icon.
	 * @param darkModeHoverIconPath - The resource path for the dark mode hover icon.
	 * @param lightModeIconPath - The resource path for the light mode idle icon.
	 * @param lightModeHoverIconPath - The resource path for the light mode hover icon.
	 */
	public ThemeButton (boolean darkMode, String darkModeIconPath, String darkModeHoverIconPath, 
			String lightModeIconPath, String lightModeHoverIconPath) {
		// Start the application in dark mode.
		this.darkMode = darkMode;
		
		// Initialize dark theme button icons.
		darkModeIcon = new FlatSVGIcon(getClass().getResource(darkModeIconPath)).derive(0.75f);
		darkModeHoverIcon = new FlatSVGIcon(getClass().getResource(darkModeHoverIconPath)).derive(0.75f);
		darkModePressedIcon = new FlatSVGIcon(getClass().getResource(darkModeHoverIconPath)).derive(0.60f);
		
		// Initialize light theme button icons.
		lightModeIcon = new FlatSVGIcon(getClass().getResource(lightModeIconPath)).derive(0.75f);
		lightModeHoverIcon = new FlatSVGIcon(getClass().getResource(lightModeHoverIconPath)).derive(0.75f);
		lightModePressedIcon = new FlatSVGIcon(getClass().getResource(lightModeHoverIconPath)).derive(0.60f);
		
		// If the UI is in dark mode...
		if (darkMode) {
			// Initialize the theme button icon to the dark mode icon.
			this.setIcon(darkModeIcon);
		}
		// Otherwise, if the UI is in light mode...
		else {
			// Initialize the theme button icon to the light mode icon.
			this.setIcon(lightModeIcon);
		}
		
		// Set the tooltip for the button.
		this.setToolTipText("Change Theme");
		
		// Set the initial button size.
		this.setPreferredSize(BUTTON_ICON_SIZE);
		
		// Remove all input mapping from the button.
		this.getInputMap().clear();
				
		// Only show the icon for the theme button.
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
		this.setFocusPainted(false);
	}
	
	/**
	 * Toggle the "dark mode" state of the UI.
	 */
	public void toggleDarkMode() {
		darkMode = !darkMode;
	}
	
	/**
	 * Getter for the dark mode state of the icon.
	 * 
	 * @return The dark mode state of the icon.
	 */
	public boolean isDarkMode() {
		return darkMode;
	}
	
	/**
	 * Setter for the dark mode state of the icon.
	 * 
	 * @param darkMode - Whether or not to enable the dark mode state of the icon.
	 */
	public void setDarkMode (boolean darkMode) {
		this.darkMode = darkMode;
	}
	
	/**
	 * Getter for the dark mode theme button icon when it is idle.
	 * 
	 * @return The idle dark mode theme button icon.
	 */
	public Icon getDarkModeIcon() {
		return darkModeIcon;
	}

	/**
	 * Getter for the light mode theme button icon when it is idle.
	 * 
	 * @return The idle light mode theme button icon.
	 */
	public Icon getLightModeIcon() {
		return lightModeIcon;
	}
	
	/**
	 * Getter for the dark mode theme button icon when the cursor is over the button or the button is in focus.
	 * 
	 * @return The dark mode theme button hover icon.
	 */
	public Icon getDarkModeHoverIcon() {
		return darkModeHoverIcon;
	}

	/**
	 * Getter for the light mode theme button icon when the cursor is over the button or the button is in focus.
	 * 
	 * @return The light mode theme button hover icon.
	 */
	public Icon getLightModeHoverIcon() {
		return lightModeHoverIcon;
	}
	
	/**
	 * Getter for the dark mode theme button icon when the button is held down.
	 * 
	 * @return The pressed dark mode theme button icon.
	 */
	public Icon getDarkModePressedIcon() {
		return darkModePressedIcon;
	}

	/**
	 * Getter for the light mode theme button icon when the button is held down.
	 * 
	 * @return The pressed light mode theme button icon.
	 */
	public Icon getLightModePressedIcon() {
		return lightModePressedIcon;
	}
}
