package com.dhk.controllers;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.io.SettingsManager;
import com.dhk.io.RunOnStartupManager;
import com.dhk.ui.DhkView;

/**
 * This class controls the Run On Startup button. Listeners are added to the corresponding view component so that 
 * when the Run On Startup button is pressed, the application will toggle the ability for the application to launch on 
 * user login.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class RunOnStartupButtonController implements Controller {
	private DhkView view;
	private SettingsManager settings;
	private RunOnStartupManager runOnStartupManager;
	
	/**
	 * Constructor for the RunOnStartupButtonController class.
	 *
	 * @param view - The view for the application.
	 * @param settings - The settings manager for the application.
	 */
	public RunOnStartupButtonController(DhkView view, SettingsManager settings) {
		// Get the application's view and settings manager.
		this.view = view;
		this.settings = settings;
		
		// Instantiate the run on startup manager.
		runOnStartupManager = new RunOnStartupManager();
	}
	
	/**
	 * This method initializes the listeners for the run on startup button.
	 */
	public void initListeners() {
		// Start the action listener for the run on startup button action.
		view.getRunOnStartupButton().addActionListener(e -> runOnStartupButtonAction());
				
		// Set the state change listener for the run on startup button.
		view.getRunOnStartupButton().addChangeListener(e -> startupButtonStateChangeAction());
						
		// Set the focus listener for the run on startup button.
		view.getRunOnStartupButton().addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// Switch to the rollover state when the run on startup button is focused.
				view.getRunOnStartupButton().getModel().setRollover(true);
			}

			@Override
			public void focusLost(FocusEvent e) {
				// Leave the rollover state when the run on startup button is not focused.
				view.getRunOnStartupButton().getModel().setRollover(false);
			}
		});
				
		// Set the mouse listener for the run on startup button.
		view.getRunOnStartupButton().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				// Set the focus on the run on startup button when the mouse hovers over it.
				view.getRunOnStartupButton().requestFocusInWindow();
			}
		});
	}
	
	/**
	 * This method toggles the "run on startup" state, adds or removes a batch file to the startup folder, and then 
	 * saves the new "run on startup" state.
	 */
	private void runOnStartupButtonAction() {
		// Toggle the "run on startup" state.
		view.getRunOnStartupButton().toggleRunOnStartup();
		
		// If the application should run on startup...
		if (view.getRunOnStartupButton().isRunOnStartup()) {
			// Add the startup batch file to run this application on Windows login.
			runOnStartupManager.addToStartup();
		}
		else {
			// Remove the startup batch file so this application does not run on Windows login.
			runOnStartupManager.removeFromStartup();
		}
							
		// Save the new "run on startup" state into the settings file.
		settings.saveIniRunOnStartup(view.getRunOnStartupButton().isRunOnStartup());
	}

	/**
	 * This method changes the run on startup button icon depending on the button's state.
	 */
	private void startupButtonStateChangeAction() {
		// If the user is holding the action button on the run on startup button...
		if (view.getRunOnStartupButton().getModel().isArmed()) {
			// Use the corresponding pressed icon.
			setPressedIcon();
		}
		// If the user is hovering on the run on startup button...
		else if (view.getRunOnStartupButton().getModel().isRollover()) {
			// Use the corresponding hover icon.
			setHoverIcon();
		}
		// Otherwise, if the user is not interacting with the run on startup button...
		else {
			// Use the corresponding idle icon.
			setIdleIcon();
		}
	}
	
	/**
	 * This method sets the pressed icon corresponding to the "run on startup" state.
	 */
	private void setPressedIcon() {
		// If the application should run on startup...
		if (view.getRunOnStartupButton().isRunOnStartup()) {
			// Use the run on startup enabled pressed button icon.
			view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledPressedIcon());
		}
		else {
			// Use the run on startup disabled pressed button icon.
			view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledPressedIcon());
		}
	}
	
	/**
	 * This method sets the hover icon corresponding to the "run on startup" state.
	 */
	private void setHoverIcon() {
		// If the application should run on startup...
		if (view.getRunOnStartupButton().isRunOnStartup()) {
			// Use the run on startup enabled hover button icon.
			view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledHoverIcon());
		}
		else {
			// Use the run on startup disabled hover button icon.
			view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledHoverIcon());
		}
	}
	
	/**
	 * This method sets the idle icon corresponding to the "run on startup" state.
	 */
	private void setIdleIcon() {
		// If the application should run on startup...
		if (view.getRunOnStartupButton().isRunOnStartup()) {
			// Use the run on startup enabled idle button icon.
			view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupEnabledIdleIcon());
		}
		else {
			// Use the run on startup disabled idle button icon.
			view.getRunOnStartupButton().setIcon(view.getRunOnStartupButton().getRunOnStartupDisabledIdleIcon());
		}
	}
}
