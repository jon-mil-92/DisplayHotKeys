/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
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
 * @author Jonathan R. Miller
 */
public class RunOnStartupManager {

    private String startupPath;
    private String runOnStartupFileName;
    private String runOnStartupFilePath;
    private File runOnStartupFile;
    private String appExePath;

    /**
     * Constructor for the {@link RunOnStartupManager} class.
     */
    public RunOnStartupManager() {
        startupPath = System.getProperty("user.home")
                + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";

        runOnStartupFileName = "StartDisplayHotKeys.bat";
        runOnStartupFilePath = startupPath + runOnStartupFileName;
        runOnStartupFile = new File(runOnStartupFilePath);

        // The jpackage launcher exposes its own executable path here; fall back to the code source when unpackaged
        appExePath = System.getProperty("jpackage.app-path");

        if (appExePath == null) {
            appExePath = deriveExePathFromCodeSource();
        }
    }

    /**
     * Adds a batch file to the user's startup folder that will execute this application upon login.
     */
    public void addToStartup() {
        try {
            PrintWriter startupFileWriter = new PrintWriter(runOnStartupFile);

            // Write the command that will execute upon user login to the run on startup file
            startupFileWriter.print("start " + "\"\" \"" + appExePath + "\"");

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

    /**
     * Derives the launcher executable path from the running code source for unpackaged development runs.
     *
     * @return The derived executable path, or null if the code source location cannot be resolved
     */
    private String deriveExePathFromCodeSource() {
        File jarFile = null;

        try {
            jarFile = new File(
                    RunOnStartupManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (jarFile == null) {
            return null;
        }

        return jarFile.getPath().replaceAll(".jar", ".exe");
    }

}
