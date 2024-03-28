package com.dhk.io;

import java.io.File;
import java.net.URISyntaxException;

/**
 * This class utilizes the Windows CLI to communicate with the SetDisplay program to set the given display 
 * mode and DPI scale percentage.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public class DisplaySettings {
	WindowsCommandProcessor cmd;
	String appPath;
	
	/**
	 * Constructor for the DisplaySettings class.
	 */
	public DisplaySettings() {
		// Start a new Windows command processor.
		cmd = new WindowsCommandProcessor();
		
		// Create a file with the path of this app.
		File jarFile = null;
		try {
			jarFile = new File(DisplaySettings.class.getProtectionDomain().getCodeSource().getLocation().toURI()
					.getPath());
		} catch (URISyntaxException e) {
					e.printStackTrace();
		}
							
		// Get the path for the folder containing this runnable jar file.
		appPath = jarFile.getParent() + "\\";
	}
	
	/**
	 * This method sets the primary monitor's display settings.
	 * 
	 * @param width - The horizontal resolution of the primary monitor.
	 * @param height - The vertical resolution of the primary monitor.
	 * @param bitDepth - The bit depth of the primary monitor.
	 * @param refreshRate - The refresh rate of the primary monitor.
	 * @param displayScale - The display scale percentage of the primary monitor.
	 */
	public void setDisplay(String width, String height, String bitDepth,
			String refreshRate, String displayScale) {
		// Create an object to hold the command strings to send to the Windows CLI.
		String[] commands = new String[2];
		
		// Create the command string that will cd into the app's folder.
		commands[0] = "cd " + "\"" + appPath + "\"";
		
		// Create the command string that will set the display settings by using the SetDisplay program.
		commands[1] = ("SetDisplay.exe " + width + " " + height + " " + bitDepth 
				+ " " + refreshRate + " " + displayScale);
		
		// Send both commands to the Windows CLI.
		cmd.sendCommands(commands);
	}
}
