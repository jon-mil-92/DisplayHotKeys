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
     * Releases the exclusive lock and closes the lock file resources.
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
        }
    }

}
