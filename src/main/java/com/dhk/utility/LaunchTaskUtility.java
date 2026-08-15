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
package com.dhk.utility;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Registers the task that runs this application with the highest available privileges, so the launcher can start the
 * application elevated without a consent prompt after the first launch. The task's logon trigger carries the run on
 * startup state.
 *
 * @author Jonathan R. Miller
 */
public class LaunchTaskUtility {

    /**
     * Name of the task that starts the application.
     */
    private static final String TASK_NAME = "Display Hot Keys";

    /**
     * Exit code returned by the task command line utility when a command succeeds.
     */
    private static final int TASK_COMMAND_SUCCESS = 0;

    /**
     * Instance policy of the task, which must start a second instance so it reaches the single-instance check and
     * reports that the application is already running. Suppressing it here would drop the launch silently instead.
     */
    private static final String MULTIPLE_INSTANCES_POLICY = "Parallel";

    /**
     * Little endian UTF-16 byte order mark that must precede a task definition.
     */
    private static final byte[] BYTE_ORDER_MARK = {(byte) 0xFF, (byte) 0xFE};

    /**
     * File extension that a derived path must carry to be a runnable task command.
     */
    private static final String EXE_EXTENSION = ".exe";

    /**
     * Path of the executable the task runs, resolved once because it cannot change while the application runs.
     */
    private static final String APP_EXE_PATH = resolveAppExePath();

    /**
     * Default constructor for the {@link LaunchTaskUtility} class.
     */
    public LaunchTaskUtility() {
    }

    /**
     * Registers the task when it is missing or no longer matches this copy of the application, so the launcher can
     * start the application elevated without a consent prompt after the first launch.
     *
     * @param runOnStartup
     *            - Whether the task's logon trigger should be enabled
     *
     * @return True if the task is registered and current, false otherwise
     */
    public static boolean registerTask(boolean runOnStartup) {
        if (APP_EXE_PATH == null) {
            return false;
        }

        // Re-registering on every launch would spawn a process each time, so only do so when the task is out of date
        if (isTaskCurrent(runOnStartup)) {
            return true;
        }

        return writeTask(runOnStartup);
    }

    /**
     * Gets the path of the executable the task runs, which is also what starts the application upon login.
     *
     * @return The application executable path, or null when it does not resolve to an executable
     */
    public static String getAppExePath() {
        return APP_EXE_PATH;
    }

    /**
     * Determines whether the task is registered and already matches the running executable, the wanted logon trigger
     * state, and the wanted instance policy. A portable copy can move between launches, which leaves a stale task that
     * would silently fail to start.
     *
     * @param runOnStartup
     *            - The wanted logon trigger state
     *
     * @return True if the registered task matches the current executable, trigger state, and policy, false otherwise
     */
    private static boolean isTaskCurrent(boolean runOnStartup) {
        String taskXml = queryTaskXml();

        if (taskXml == null) {
            return false;
        }

        if (!taskXml.contains("<Command>" + escapeXml(APP_EXE_PATH) + "</Command>")) {
            return false;
        }

        // A task registered by an earlier version suppresses a second instance, so rewrite it when the policy is stale
        if (!taskXml.contains("<MultipleInstancesPolicy>" + MULTIPLE_INSTANCES_POLICY + "</MultipleInstancesPolicy>")) {
            return false;
        }

        /*
         * The scheduler rewrites the definition it stores, collapsing an enabled trigger to a self-closing element that
         * omits the default, so detect the disabled state and infer the enabled one rather than matching what was
         * written
         */
        boolean triggerDisabled = taskXml.contains("<Enabled>false</Enabled>");

        return triggerDisabled != runOnStartup;
    }

    /**
     * Reads the registered task definition.
     *
     * @return The task definition XML, or null when the task is not registered
     */
    private static String queryTaskXml() {
        ProcessBuilder taskQueryBuilder = new ProcessBuilder("schtasks", "/Query", "/TN", TASK_NAME, "/XML");
        taskQueryBuilder.redirectErrorStream(true);

        try {
            Process taskQuery = taskQueryBuilder.start();
            String taskXml = new String(taskQuery.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (taskQuery.waitFor() != TASK_COMMAND_SUCCESS) {
                return null;
            }

            return taskXml;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return null;
        }
    }

    /**
     * Writes the task from a task definition file, replacing any existing registration.
     *
     * @param runOnStartup
     *            - Whether the task's logon trigger should be enabled
     *
     * @return True if the task was registered, false when registration failed
     */
    private static boolean writeTask(boolean runOnStartup) {
        Path taskDefinitionFile = null;

        try {
            taskDefinitionFile = Files.createTempFile("DisplayHotKeys", ".xml");

            /*
             * The task command line utility only reads task definitions encoded as little endian UTF-16, and it rejects
             * one lacking a byte order mark as malformed, so write the mark ahead of the definition
             */
            byte[] definitionBytes = buildTaskDefinition(runOnStartup).getBytes(StandardCharsets.UTF_16LE);
            byte[] taskDefinitionBytes = new byte[BYTE_ORDER_MARK.length + definitionBytes.length];

            System.arraycopy(BYTE_ORDER_MARK, 0, taskDefinitionBytes, 0, BYTE_ORDER_MARK.length);
            System.arraycopy(definitionBytes, 0, taskDefinitionBytes, BYTE_ORDER_MARK.length, definitionBytes.length);

            Files.write(taskDefinitionFile, taskDefinitionBytes);

            return runTaskCommand("/Create", "/TN", TASK_NAME, "/XML", taskDefinitionFile.toString(), "/F");
        } catch (IOException e) {
            return false;
        } finally {
            if (taskDefinitionFile != null) {
                try {
                    Files.deleteIfExists(taskDefinitionFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Runs the task command line utility with the given arguments.
     *
     * @param taskArguments
     *            - The arguments to pass to the task command line utility
     *
     * @return True if the command succeeded, false otherwise
     */
    private static boolean runTaskCommand(String... taskArguments) {
        String[] taskCommand = new String[taskArguments.length + 1];
        taskCommand[0] = "schtasks";

        for (int argumentIndex = 0; argumentIndex < taskArguments.length; argumentIndex++) {
            taskCommand[argumentIndex + 1] = taskArguments[argumentIndex];
        }

        ProcessBuilder taskCommandBuilder = new ProcessBuilder(taskCommand);
        taskCommandBuilder.redirectErrorStream(true);

        try {
            Process taskProcess = taskCommandBuilder.start();

            // Drain the output so the utility never blocks on a full pipe
            taskProcess.getInputStream().readAllBytes();

            return taskProcess.waitFor() == TASK_COMMAND_SUCCESS;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return false;
        }
    }

    /**
     * Builds the task definition. The command line utility's own flags force settings that would stop the application
     * from starting on battery power and would terminate it when a laptop is unplugged, so the definition is supplied
     * as XML instead to turn those off.
     *
     * @param runOnStartup
     *            - Whether the logon trigger should be enabled
     *
     * @return The task definition XML
     */
    private static String buildTaskDefinition(boolean runOnStartup) {
        StringBuilder taskDefinition = new StringBuilder();

        taskDefinition.append("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n");
        taskDefinition.append("<Task version=\"1.2\" ");
        taskDefinition.append("xmlns=\"http://schemas.microsoft.com/windows/2004/02/mit/task\">\n");
        taskDefinition.append("  <RegistrationInfo>\n");
        taskDefinition.append("    <Author>Jonathan R. Miller</Author>\n");
        taskDefinition.append("    <Description>Launches Display Hot Keys</Description>\n");
        taskDefinition.append("  </RegistrationInfo>\n");
        taskDefinition.append("  <Principals>\n");
        taskDefinition.append("    <Principal id=\"Author\">\n");
        taskDefinition.append("      <LogonType>InteractiveToken</LogonType>\n");
        taskDefinition.append("      <RunLevel>HighestAvailable</RunLevel>\n");
        taskDefinition.append("    </Principal>\n");
        taskDefinition.append("  </Principals>\n");
        taskDefinition.append("  <Settings>\n");
        taskDefinition
                .append("    <MultipleInstancesPolicy>" + MULTIPLE_INSTANCES_POLICY + "</MultipleInstancesPolicy>\n");
        taskDefinition.append("    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>\n");
        taskDefinition.append("    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>\n");
        taskDefinition.append("    <AllowHardTerminate>false</AllowHardTerminate>\n");
        taskDefinition.append("    <StartWhenAvailable>false</StartWhenAvailable>\n");
        taskDefinition.append("    <RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>\n");
        taskDefinition.append("    <IdleSettings>\n");
        taskDefinition.append("      <StopOnIdleEnd>false</StopOnIdleEnd>\n");
        taskDefinition.append("      <RestartOnIdle>false</RestartOnIdle>\n");
        taskDefinition.append("    </IdleSettings>\n");
        taskDefinition.append("    <AllowStartOnDemand>true</AllowStartOnDemand>\n");
        taskDefinition.append("    <Enabled>true</Enabled>\n");
        taskDefinition.append("    <Hidden>false</Hidden>\n");
        taskDefinition.append("    <RunOnlyIfIdle>false</RunOnlyIfIdle>\n");
        taskDefinition.append("    <ExecutionTimeLimit>PT0S</ExecutionTimeLimit>\n");
        taskDefinition.append("    <Priority>7</Priority>\n");
        taskDefinition.append("  </Settings>\n");
        taskDefinition.append("  <Triggers>\n");
        taskDefinition.append(buildLogonTrigger(runOnStartup));
        taskDefinition.append("  </Triggers>\n");
        taskDefinition.append("  <Actions Context=\"Author\">\n");
        taskDefinition.append("    <Exec>\n");
        taskDefinition.append("      <Command>" + escapeXml(APP_EXE_PATH) + "</Command>\n");
        taskDefinition.append("    </Exec>\n");
        taskDefinition.append("  </Actions>\n");
        taskDefinition.append("</Task>\n");

        return taskDefinition.toString();
    }

    /**
     * Builds the logon trigger element. The trigger stays in the definition when the application should not start on
     * login and is disabled instead, so the task itself survives to keep starting the application elevated.
     *
     * @param runOnStartup
     *            - Whether the logon trigger should be enabled
     *
     * @return The logon trigger XML
     */
    private static String buildLogonTrigger(boolean runOnStartup) {
        StringBuilder logonTrigger = new StringBuilder();

        logonTrigger.append("    <LogonTrigger>\n");
        logonTrigger.append("      <Enabled>" + runOnStartup + "</Enabled>\n");
        logonTrigger.append("    </LogonTrigger>\n");

        return logonTrigger.toString();
    }

    /**
     * Escapes the characters that would otherwise terminate or corrupt the enclosing XML element, since an installation
     * path may legitimately contain an ampersand.
     *
     * @param text
     *            - The text to escape
     *
     * @return The escaped text
     */
    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Resolves the path of the executable that the task runs.
     *
     * @return The application executable path, or null if it does not resolve to an executable
     */
    private static String resolveAppExePath() {
        // The jpackage launcher exposes its own executable path here; fall back to the code source when unpackaged
        String appExePath = System.getProperty("jpackage.app-path");

        if (appExePath != null) {
            return appExePath;
        }

        return deriveExePathFromCodeSource();
    }

    /**
     * Derives the executable path from the running code source for unpackaged runs.
     *
     * @return The derived executable path, or null if the code source does not resolve to an executable
     */
    private static String deriveExePathFromCodeSource() {
        File jarFile = null;

        try {
            jarFile = new File(
                    LaunchTaskUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (jarFile == null) {
            return null;
        }

        String derivedPath = jarFile.getPath().replaceAll(".jar", ".exe");

        /*
         * Running from a development environment resolves the code source to a class output directory, which leaves
         * nothing to replace. The task command line utility accepts a directory without complaint, so registering that
         * path would yield a task that can never start the application
         */
        if (!derivedPath.toLowerCase().endsWith(EXE_EXTENSION)) {
            return null;
        }

        return derivedPath;
    }

}
