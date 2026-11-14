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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Enforces a single running instance of the application by holding an exclusive lock on a lock file for the lifetime of
 * the process. The operating system releases the lock automatically if the process ends abnormally.
 *
 * @author Jonathan R. Miller
 */
public class SingleInstanceLock {

    private RandomAccessFile lockFileHandle;
    private FileChannel lockChannel;
    private FileLock lock;

    /**
     * Path of the file locked to ensure only one instance of the application runs.
     */
    private static final String LOCK_FILE_PATH = System.getProperty("user.home")
            + "\\Documents\\DisplayHotKeys\\DisplayHotKeys.lock";

    /**
     * Default constructor for the {@link SingleInstanceLock} class.
     */
    public SingleInstanceLock() {
    }

    /**
     * Attempts to acquire the exclusive single-instance lock and, on success, registers a shutdown hook to release it.
     * The hook also keeps this instance reachable so the held lock is never released early.
     *
     * @return True if this process acquired the lock, or false if another instance already holds it
     */
    public boolean tryLock() {
        try {
            File lockFile = new File(LOCK_FILE_PATH);
            lockFile.getParentFile().mkdirs();

            lockFileHandle = new RandomAccessFile(lockFile, "rw");
            lockChannel = lockFileHandle.getChannel();
            lock = lockChannel.tryLock();

            // A null lock means another instance already holds the exclusive lock
            if (lock == null) {
                lockChannel.close();
                lockFileHandle.close();

                return false;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(this::release));

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            // Fail open so a lock-file I/O glitch cannot block the application from starting at all
            return true;
        }
    }

    /**
     * Releases the exclusive lock, closes the lock file resources, and deletes the lock file so it does not linger in
     * the user's documents folder after the application exits.
     */
    private void release() {
        try {
            if (lock != null) {
                lock.release();
            }

            if (lockChannel != null) {
                lockChannel.close();
            }

            if (lockFileHandle != null) {
                lockFileHandle.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            new File(LOCK_FILE_PATH).delete();
        }
    }

}
