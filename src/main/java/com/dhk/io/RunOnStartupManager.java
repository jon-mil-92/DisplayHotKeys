package com.dhk.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;

/**
 * Gets the application's path and uses it to create or destroy a batch file that will run the application on Windows
 * login.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
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
        startupPath = System.getProperty("user.home")
                + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";

        runOnStartupFileName = "StartDisplayHotKeys.bat";
        runOnStartupFilePath = startupPath + runOnStartupFileName;
        runOnStartupFile = new File(runOnStartupFilePath);

        File jarFile = null;

        try {
            jarFile = new File(
                    RunOnStartupManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        jarFilePath = jarFile.getPath();
        jarFilePath = jarFilePath.replaceAll(".jar", ".exe");
    }

    /**
     * Adds a batch file to the user's startup folder that will execute this application upon login.
     */
    public void addToStartup() {
        try {
            PrintWriter startupFileWriter = new PrintWriter(runOnStartupFile);

            // Write the command that will execute upon user login to the run on startup file
            startupFileWriter.print("start " + "\"\" \"" + jarFilePath + "\"");

            startupFileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the run on startup file from the user's startup folder.
     */
    public void removeFromStartup() {
        try {
            Files.deleteIfExists(runOnStartupFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
