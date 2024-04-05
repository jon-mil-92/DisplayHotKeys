package com.dhk.io;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class starts a Windows CLI process to stream commands to.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class WindowsCommandProcessor {
	private String[] command;
	
	/**
	 * Constructor for the WindowsCommandProcessor class.
	 */
	public WindowsCommandProcessor() {
		// The process will be a Windows CLI process.
		command = new String[]{"cmd"};
	}
	
	/**
	 * This method sends a command to the Windows CLI.
	 * 
	 * @param commands - The array of commands to send to the Windows CLI.
	 */
	public void sendCommands(String[] commands) {
		try {
			// Start a new Windows CLI process.
			Process p = Runtime.getRuntime().exec(command);
					
			// Start threads for the Windows CLI error and input streams.
			new Thread(new StreamSync(p.getErrorStream(), System.err)).start();
			new Thread(new StreamSync(p.getInputStream(), System.out)).start();
	
			// Write to the Windows CLI process.
			PrintWriter stdin = new PrintWriter(p.getOutputStream());
			
			// For each command in the array of commands...
			for (String command: commands) {
				// Send the given command to the Windows CLI.
				stdin.println(command);
			}
			
			// Close the stream after sending all commands.
			stdin.close();
			
			// Wait for the Windows CLI exit code and then print it to the console.
			int returnCode = p.waitFor();
			System.out.println("Return code = " + returnCode + "\n");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
