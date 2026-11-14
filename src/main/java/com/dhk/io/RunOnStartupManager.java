/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
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

    private File runOnStartupFile;

    /**
     * Path of the startup folder relative to the user's home folder.
     */
    private static final String STARTUP_PATH = "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs"
            + "\\Startup\\";

    /**
     * Batch file the startup folder runs upon login, used as a fallback when the scheduled task cannot be registered.
     */
    private static final String RUN_ON_STARTUP_FILE_NAME = "StartDisplayHotKeys.bat";

    /**
     * Constructor for the {@link RunOnStartupManager} class.
     */
    public RunOnStartupManager() {
        runOnStartupFile = new File(System.getProperty("user.home") + STARTUP_PATH + RUN_ON_STARTUP_FILE_NAME);
    }

    /**
     * Applies the saved run on startup state on launch, unless the startup folder file of this copy is already the only
     * thing carrying it, and reports what will really start the application.
     *
     * @param runOnStartup
     *            - The saved run on startup state
     *
     * @return True if the application will start upon login, whether or not that is the saved state
     */
    public boolean applySavedRunOnStartup(boolean runOnStartup) {
        /*
         * The startup folder file only survives on an account that could not write the task, where retrying costs two
         * processes every launch to fail the same way. Skipping is only safe while that file is the sole thing starting
         * the application, since a task that also starts it must be reconciled to stop the two stacking
         */
        if (startupFileStartsThisCopy() && runOnStartup && !LaunchTaskUtility.isStartOnLogonEnabled()) {
            return true;
        }

        if (setRunOnStartup(runOnStartup)) {
            return runOnStartup;
        }

        /*
         * The wanted state could not be written, so what starts the application is whatever the account could not
         * change. A task left enabled still starts it, which the button has to report rather than the saved state
         */
        return LaunchTaskUtility.isStartOnLogonEnabled() || startupFileStartsThisCopy();
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
         * Nothing here could write the wanted state, so report what the system will actually do rather than what was
         * asked for. A task registered against stricter rights than this account holds keeps its own trigger state,
         * which already matches whenever the wanted state is off and no task starts the application
         */
        return LaunchTaskUtility.isStartOnLogonEnabled() == runOnStartup;
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
     * Determines whether the startup folder file starts this copy of the application. A portable copy that moved leaves
     * one naming a path that no longer holds the application, which must be rewritten rather than trusted.
     *
     * @return True if the file exists and names this copy's executable, false otherwise
     */
    private boolean startupFileStartsThisCopy() {
        String appExePath = LaunchTaskUtility.getAppExePath();

        if (appExePath == null || !runOnStartupFile.exists()) {
            return false;
        }

        try {
            return Files.readString(runOnStartupFile.toPath()).contains(appExePath);
        } catch (IOException e) {
            e.printStackTrace();

            return false;
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
