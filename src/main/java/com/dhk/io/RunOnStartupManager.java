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

    /**
     * The startup folder batch file that starts the application upon login.
     */
    private File runOnStartupFile;

    /**
     * Path of the startup folder, relative to the user's home folder, that Windows runs the contents of upon login.
     */
    private static final String STARTUP_PATH = "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs"
            + "\\Startup\\";

    /**
     * Name of the batch file that starts the application upon login while the task cannot be registered.
     */
    private static final String RUN_ON_STARTUP_FILE_NAME = "StartDisplayHotKeys.bat";

    /**
     * Constructor for the {@link RunOnStartupManager} class.
     */
    public RunOnStartupManager() {
        runOnStartupFile = new File(System.getProperty("user.home") + STARTUP_PATH + RUN_ON_STARTUP_FILE_NAME);
    }

    /**
     * Applies the saved run on startup state on launch, unless only the startup folder file can carry it.
     *
     * @param runOnStartup
     *            - The saved run on startup state
     */
    public void applySavedRunOnStartup(boolean runOnStartup) {
        /*
         * An account that cannot register the task leaves the startup folder file carrying the state, and retrying
         * costs two processes every launch only to fail again. The file already holds the wanted state, so reconcile
         * only when there is a task to hand that state back to
         */
        if (runOnStartupFile.exists() && !LaunchTaskUtility.isTaskRegistered()) {
            return;
        }

        setRunOnStartup(runOnStartup);
    }

    /**
     * Enables the logon trigger of the task so the application starts upon login.
     *
     * @return True if the application will start upon login, false if neither mechanism could be enabled
     */
    public boolean addToStartup() {
        return setRunOnStartup(true);
    }

    /**
     * Disables the logon trigger of the task so it no longer starts the application on login. The task itself stays
     * registered so the launcher can start the application elevated without a consent prompt after the first launch.
     *
     * @return True if the application will no longer start upon login, false if the task could not be rewritten
     */
    public boolean removeFromStartup() {
        return setRunOnStartup(false);
    }

    /**
     * Applies the wanted run on startup state through the task's logon trigger, falling back to the startup folder only
     * while the task cannot be registered. Exactly one of the two ever survives a call, since both would start the
     * application twice upon login.
     *
     * @param runOnStartup
     *            - Whether the application should start upon login
     *
     * @return True if the wanted state was applied, false if it could not be
     */
    private boolean setRunOnStartup(boolean runOnStartup) {
        // The task carries the state whenever it can, so the fallback it replaces must not survive beside it
        boolean taskCarriesState = LaunchTaskUtility.registerTask(runOnStartup);
        boolean fallbackCarriesState = !taskCarriesState && runOnStartup && addStartupFile();

        if (!fallbackCarriesState) {
            removeStartupFile();
        }

        if (taskCarriesState || fallbackCarriesState) {
            return true;
        }

        /*
         * Nothing here could write the wanted state. Turning it off still succeeds when no task starts the application
         * anyway, but a task an administrator registered earlier keeps doing so until it can be rewritten
         */
        return !runOnStartup && !LaunchTaskUtility.isTaskRegistered();
    }

    /**
     * Adds a batch file to the user's startup folder that will execute this application upon login.
     *
     * @return True if the batch file was written, false otherwise
     */
    private boolean addStartupFile() {
        String appExePath = LaunchTaskUtility.getAppExePath();

        if (appExePath == null) {
            return false;
        }

        try {
            PrintWriter startupFileWriter = new PrintWriter(runOnStartupFile);

            // Write the command that will execute upon user login to the run on startup file
            startupFileWriter.print("start " + "\"\" \"" + appExePath + "\"");

            startupFileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            return false;
        }

        return true;
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
