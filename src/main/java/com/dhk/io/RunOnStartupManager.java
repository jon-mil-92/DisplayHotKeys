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
import java.nio.file.Files;

import com.dhk.utility.LaunchTaskUtility;

/**
 * Enables or disables running the application upon login, preferring the logon trigger of the task that starts it
 * elevated and falling back to a startup folder batch file when that task cannot be registered.
 *
 * @author Jonathan R. Miller
 */
public class RunOnStartupManager {

    private String startupPath;
    private String runOnStartupFileName;
    private String runOnStartupFilePath;
    private File runOnStartupFile;

    /**
     * Constructor for the {@link RunOnStartupManager} class.
     */
    public RunOnStartupManager() {
        startupPath = System.getProperty("user.home")
                + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";

        runOnStartupFileName = "StartDisplayHotKeys.bat";
        runOnStartupFilePath = startupPath + runOnStartupFileName;
        runOnStartupFile = new File(runOnStartupFilePath);
    }

    /**
     * Enables the logon trigger of the task so the application starts upon login.
     */
    public void addToStartup() {
        setRunOnStartup(true);
    }

    /**
     * Disables the logon trigger of the task so it no longer starts the application on login. The task itself stays
     * registered so the launcher can start the application elevated without a consent prompt after the first launch.
     */
    public void removeFromStartup() {
        setRunOnStartup(false);
    }

    /**
     * Applies the wanted run on startup state through the task's logon trigger, falling back to the startup folder when
     * the task cannot be registered.
     *
     * @param runOnStartup
     *            - Whether the application should start upon login
     */
    private void setRunOnStartup(boolean runOnStartup) {
        if (LaunchTaskUtility.registerTask(runOnStartup)) {
            removeStartupFile();

            return;
        }

        /*
         * Registering the task needs administrator rights, which a standard account never has. Fall back to the startup
         * folder batch file so the setting still works there, at the cost of the login console flash
         */
        if (runOnStartup) {
            addStartupFile();

            return;
        }

        removeStartupFile();
    }

    /**
     * Adds a batch file to the user's startup folder that will execute this application upon login.
     */
    private void addStartupFile() {
        String appExePath = LaunchTaskUtility.getAppExePath();

        if (appExePath == null) {
            return;
        }

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
    private void removeStartupFile() {
        try {
            Files.deleteIfExists(runOnStartupFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
