package com.dhk.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;

/**
 * This class gets the application's path and uses it to create or destroy a batch file that will run the application on
 * Windows login.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class RunOnStartupManager {
    private String startupPath;
    private String runOnStartupFileName;
    private String runOnStartupFilePath;
    private File runOnStartupFile;
    private String jarFilePath;

    /**
     * Constructor for the RunOnStartupManager class.
     */
    public RunOnStartupManager() {
        // Initialize the path to the Windows startup folder for the current user.
        startupPath = System.getProperty("user.home")
                + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";

        // Initialize the name for the run on startup file.
        runOnStartupFileName = "StartDisplayHotKeys.bat";

        // Set the full path to the run on startup file.
        runOnStartupFilePath = startupPath + runOnStartupFileName;

        // Create a file object for the run on startup file.
        runOnStartupFile = new File(runOnStartupFilePath);

        // Create a file with the path of this app.
        File jarFile = null;
        try {
            jarFile = new File(
                    RunOnStartupManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Get the path for this runnable jar file.
        jarFilePath = jarFile.getPath();

        // Change the .jar extension to .exe because this runnable jar will be wrapped in a Windows executable file.
        jarFilePath = jarFilePath.replaceAll(".jar", ".exe");
    }

    /**
     * This method adds a batch file to the user's startup folder that will execute this application upon login.
     */
    public void addToStartup() {
        try {
            // Initialize the writer for the run on startup file.
            PrintWriter startupFileWriter = new PrintWriter(runOnStartupFile);

            // Write the command that will execute upon user login to the run on startup file.
            startupFileWriter.print("start " + "\"\" \"" + jarFilePath + "\"");

            // Close the file writer.
            startupFileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will remove the run on startup file from the user's startup folder.
     */
    public void removeFromStartup() {
        // Delete the run on startup file if it exists.
        try {
            Files.deleteIfExists(runOnStartupFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
