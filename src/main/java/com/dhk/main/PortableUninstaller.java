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
package com.dhk.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;

import com.dhk.io.RunOnStartupManager;
import com.dhk.io.SettingsManager;
import com.dhk.io.SingleInstanceLock;
import com.dhk.localization.Localizer;
import com.dhk.utility.LaunchTaskUtility;
import com.dhk.view.ConfirmationDialog;
import com.dhk.view.NoticeDialog;

/**
 * Cleans up what a portable copy of the application leaves outside its own folder, which is the scheduled task, the
 * startup folder fallback, and optionally the saved settings. Deleting the folder alone would leave the task to fail at
 * every login. Every prompt and result is shown in the application's language.
 *
 * @author Jonathan R. Miller
 */
public class PortableUninstaller {

    /**
     * Product name shown as the title of every dialog, which is never translated.
     */
    private static final String DIALOG_TITLE = "Display Hot Keys";

    /**
     * Name of the application executable, used to find copies of the application that are still running.
     */
    private static final String APP_EXE_NAME = "DisplayHotKeys.exe";

    /**
     * Default constructor for the {@link PortableUninstaller} class.
     */
    public PortableUninstaller() {
    }

    /**
     * Closes any running copy of the application, removes the task and the startup folder fallback, asks whether to
     * delete the saved settings, and then reports what was done.
     */
    public void uninstall() {
        List<String> results = new ArrayList<String>();

        // A running copy keeps the lock file and its own files in use, which blocks deleting them, so close it first
        if (closeRunningApp()) {
            results.add(Localizer.getText("uninstall.appClosed"));
        }

        results.add(Localizer.getText(removeTask()));

        if (new RunOnStartupManager().removeStartupFile()) {
            results.add(Localizer.getText("uninstall.startupRemoved"));
        }

        results.add(Localizer.getText(removeSettings()));
        results.add(Localizer.getText("uninstall.complete"));

        new NoticeDialog(null, DIALOG_TITLE, String.join("\n", results)).showNoticeDialog();
    }

    /**
     * Forcibly closes every other running copy of the application and waits for each to exit.
     *
     * @return True if a running copy was closed, otherwise false
     */
    private boolean closeRunningApp() {
        ProcessHandle uninstaller = ProcessHandle.current();
        ProcessHandle launcher = uninstaller.parent().orElse(null);
        boolean appClosed = false;

        for (ProcessHandle process : ProcessHandle.allProcesses().toList()) {
            Optional<String> command = process.info().command();

            /*
             * This cleanup runs from the same executable, which relaunches itself as a child process that ends whenever
             * the launching process ends, so neither of the two may be closed
             */
            boolean runsThisCleanup = process.equals(uninstaller) || process.equals(launcher);

            if (runsThisCleanup || command.isEmpty() || !command.get().endsWith(APP_EXE_NAME)) {
                continue;
            }

            if (process.destroyForcibly()) {
                process.onExit().join();
                appClosed = true;
            }
        }

        return appClosed;
    }

    /**
     * Deletes the task that starts the application through its launcher.
     *
     * @return The message key of the result to report
     */
    private String removeTask() {
        if (!LaunchTaskUtility.isTaskRegistered()) {
            return "uninstall.taskMissing";
        }

        return LaunchTaskUtility.deleteTask() ? "uninstall.taskRemoved" : "uninstall.taskFailed";
    }

    /**
     * Deletes the saved settings and the lock file left beside them when the user agrees, since keeping the settings
     * preserves the saved slots for a later copy of the application.
     *
     * @return The message key of the result to report
     */
    private String removeSettings() {
        if (!SettingsManager.savedSettingsExist()) {
            return "uninstall.settingsMissing";
        }

        int answer = new ConfirmationDialog(null, DIALOG_TITLE, Localizer.getText("uninstall.settingsPrompt"))
                .showConfirmationDialog();

        if (answer != JOptionPane.YES_OPTION) {
            return "uninstall.settingsKept";
        }

        // The lock file goes first, since the settings folder is only removed once nothing else is left in it
        SingleInstanceLock.deleteLockFile();

        return SettingsManager.deleteSavedSettings() ? "uninstall.settingsDeleted" : "uninstall.settingsKept";
    }

}
