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
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Temporary diagnostic log that appends timestamped phase timings to a file beside the installed app, used to locate
 * where the launch and refresh paths block right after signing in to Windows. Remove once the post-sign-in delay is
 * diagnosed.
 *
 * @author Jonathan R. Miller
 */
public class TimingLog {

    /**
     * Formatter for the wall-clock time prefixed to every line.
     */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Milliseconds since the Unix epoch at which the JVM started, used to relate every line to process start.
     */
    private static final long JVM_START_MS = ManagementFactory.getRuntimeMXBean().getStartTime();

    /**
     * Path of the log file, resolved beside the running code so an installed app writes into its own install root.
     */
    private static final Path LOG_PATH = resolveLogPath();

    // Separate sessions in the appended log and expose how long the JVM booted before any code could log
    static {
        log("==== Session started; the JVM booted " + (System.currentTimeMillis() - JVM_START_MS)
                + " ms before this line ====");
    }

    /**
     * Default constructor for the {@link TimingLog} class.
     */
    public TimingLog() {
    }

    /**
     * Starts timing a phase.
     *
     * @return The starting instant to pass to end, in nanoseconds
     */
    public static long start() {
        return System.nanoTime();
    }

    /**
     * Logs the duration of a completed phase.
     *
     * @param label
     *            - The name of the completed phase
     * @param startNanos
     *            - The starting instant returned by start
     */
    public static void end(String label, long startNanos) {
        log(label + " took " + ((System.nanoTime() - startNanos) / 1_000_000) + " ms");
    }

    /**
     * Appends one timestamped message line to the log file, swallowing failures so diagnostics never break the app.
     *
     * @param message
     *            - The message to append
     */
    public static synchronized void log(String message) {
        String line = LocalTime.now().format(TIME_FORMAT) + " | +" + (System.currentTimeMillis() - JVM_START_MS)
                + " ms | " + message + "\r\n";

        try {
            Files.write(LOG_PATH, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolves the log file path beside the running code, stepping out of a jpackage image's app directory so the
     * file lands in the install root.
     *
     * @return The log file path
     */
    private static Path resolveLogPath() {
        try {
            Path codeSource = Paths.get(TimingLog.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path baseDir = Files.isRegularFile(codeSource) ? codeSource.getParent() : codeSource;

            // A jpackage image keeps the jar in <root>\app, so step up one level to write beside the launcher
            if (baseDir != null && baseDir.getFileName() != null && "app".equals(baseDir.getFileName().toString())) {
                baseDir = baseDir.getParent();
            }

            if (baseDir != null) {
                return baseDir.resolve("dhk-timing.log");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Paths.get(System.getProperty("user.dir"), "dhk-timing.log");
    }

}
