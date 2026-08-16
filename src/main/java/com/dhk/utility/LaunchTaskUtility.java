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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * Command line utility that reads and writes tasks.
     */
    private static final String TASK_COMMAND = "schtasks";

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
     * Opening element of the block holding the task's triggers.
     */
    private static final String TRIGGERS_OPEN_ELEMENT = "<Triggers>";

    /**
     * Closing element of the block holding the task's triggers.
     */
    private static final String TRIGGERS_CLOSE_ELEMENT = "</Triggers>";

    /**
     * Opening tag of the logon trigger, matched without its closing bracket so it also finds the self-closing form.
     */
    private static final String LOGON_TRIGGER_ELEMENT = "<LogonTrigger";

    /**
     * Element marking the enclosing trigger as disabled.
     */
    private static final String DISABLED_ELEMENT = "<Enabled>false</Enabled>";

    /**
     * Little endian UTF-16 byte order mark that must precede a task definition.
     */
    private static final byte[] BYTE_ORDER_MARK = {(byte) 0xFF, (byte) 0xFE};

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
        // Everything below builds the definition around this path, so nothing past here has to null check it again
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
     * Determines whether the task is registered, which decides whether an account that cannot write the task still has
     * one that starts the application upon login.
     *
     * @return True if the task is registered, false otherwise
     */
    public static boolean isTaskRegistered() {
        return queryTaskXml() != null;
    }

    /**
     * Reads the logon trigger state the registered task currently carries, which is the state the system will actually
     * act on rather than the one that was last requested.
     *
     * @return True if a task is registered and its logon trigger is enabled, false otherwise
     */
    public static boolean isStartOnLogonEnabled() {
        String taskXml = queryTaskXml();

        return taskXml != null && isTriggerEnabled(taskXml);
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

        return isTriggerEnabled(taskXml) == runOnStartup;
    }

    /**
     * Reads the logon trigger state out of a task definition.
     *
     * @param taskXml
     *            - The task definition XML to read
     *
     * @return True if the logon trigger is enabled, false otherwise
     */
    private static boolean isTriggerEnabled(String taskXml) {
        int triggersStart = taskXml.indexOf(TRIGGERS_OPEN_ELEMENT);
        int triggersEnd = taskXml.indexOf(TRIGGERS_CLOSE_ELEMENT, triggersStart);

        // A definition carrying no trigger block cannot start the application upon login
        if (triggersStart == -1 || triggersEnd == -1) {
            return false;
        }

        String triggers = taskXml.substring(triggersStart + TRIGGERS_OPEN_ELEMENT.length(), triggersEnd);

        // Neither can one whose trigger block is empty, which is what a definition stripped of its trigger leaves
        if (triggers.indexOf(LOGON_TRIGGER_ELEMENT) == -1) {
            return false;
        }

        /*
         * The scheduler rewrites the definition it stores, collapsing an enabled trigger to a self-closing element that
         * omits the default, so detect the disabled state and infer the enabled one rather than matching what was
         * written. The search stays inside the trigger block because the settings block carries its own enabled element
         * that reflects the whole task rather than the trigger
         */
        return !triggers.contains(DISABLED_ELEMENT);
    }

    /**
     * Reads the registered task definition.
     *
     * @return The task definition XML, or null when the task is not registered
     */
    private static String queryTaskXml() {
        ProcessBuilder taskQueryBuilder = new ProcessBuilder(TASK_COMMAND, "/Query", "/TN", TASK_NAME, "/XML");
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
        List<String> taskCommand = new ArrayList<String>();
        taskCommand.add(TASK_COMMAND);
        taskCommand.addAll(Arrays.asList(taskArguments));

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
     * @return The application executable path, or null when the application is not running from its own executable
     */
    private static String resolveAppExePath() {
        /*
         * Only the packaged launcher sets this, and it is the executable both the installed and portable packages run.
         * A development run has no executable of its own, and registering a task that starts a class output directory
         * would only yield one that can never start the application
         */
        return System.getProperty("jpackage.app-path");
    }

}
