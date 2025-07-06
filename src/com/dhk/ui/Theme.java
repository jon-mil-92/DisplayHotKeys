package com.dhk.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

/**
 * This class sets the theme for the application. It allows the theme of the application to be switched between Light 
 * and Dark themes.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class Theme {
	
	/**
	 * Constructor for the ThemeChooser class.
	 */
	public Theme () {
		// Install the Roboto font family.
		FlatRobotoFont.install();
		
		// Enable the Roboto font family.
		FlatLaf.setPreferredFontFamily(FlatRobotoFont.FAMILY);
		
		// Load the FlatLaf properties files.
		FlatLaf.registerCustomDefaultsSource("com.dhk.ui");
	}
	
	/**
	 * This method updates the "look and feel" of the app.
	 * 
	 * @param darkMode - Whether or not the dark mode "look and feel" should be applied or not.
	 */
	public void useDarkMode(boolean darkMode) {
		// If the user selects dark mode...
		if (darkMode) {
			// Set the snapshot that will fade to the new UI.
			FlatAnimatedLafChange.showSnapshot();
			
			// Apply the dark "look and feel" for the GUI.
			FlatDarculaLaf.setup();
			
			// Update the UI after changing the theme.
			FlatLaf.updateUI();
			
			// Fade the snapshot of the old UI to the new UI.
			FlatAnimatedLafChange.hideSnapshotWithAnimation();
		}
		else {
			// Set the snapshot that will fade to the new UI.
			FlatAnimatedLafChange.showSnapshot();
			
			// Apply the light "look and feel" for the GUI.
			FlatIntelliJLaf.setup();
			
			// Update the UI after changing the theme.
			FlatLaf.updateUI();
			
			// Fade the snapshot of the old UI to the new UI.
			FlatAnimatedLafChange.hideSnapshotWithAnimation();
		}
	}
}
