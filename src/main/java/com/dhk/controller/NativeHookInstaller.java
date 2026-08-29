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
package com.dhk.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import com.dhk.io.NativeInputHook;

/**
 * Installs the JVM-lifetime native keyboard and mouse hooks on a background thread. Installing a hook can take many
 * seconds right after signing in to Windows, so the install overlaps the rest of the launch work instead of serializing
 * with it.
 *
 * @author Jonathan R. Miller
 */
public class NativeHookInstaller {

    /**
     * Native input hook that lives for the whole application lifetime, or null until installed or on failure.
     */
    private NativeInputHook inputHook;

    /**
     * Permanent held-key tracker attached to the input hook for the whole application lifetime.
     */
    private HeldKeyTracker heldKeyTracker;

    /**
     * Background thread performing the install, or null until the install starts.
     */
    private Thread installThread;

    /**
     * The hook the shutdown hook stops, replaced whenever a recovery install creates a new one.
     */
    private static volatile NativeInputHook activeHook;

    /**
     * Whether the shutdown hook was registered, so only the first created hook registers one.
     */
    private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

    /**
     * Default constructor for the {@link NativeHookInstaller} class.
     */
    public NativeHookInstaller() {
    }

    /**
     * Starts installing the hooks on a background thread.
     */
    public void startInstall() {
        installThread = new Thread(this::installHooks, "Native Hook Installer");

        installThread.start();
    }

    /**
     * Waits until the hooks are installed, starting the install first if it never began in the background. The
     * installed hooks are only safe to read after this method returns.
     */
    public void awaitInstall() {
        if (installThread == null) {
            startInstall();
        }

        try {
            installThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the installed native input hook. Only valid to call after awaitInstall returns.
     *
     * @return The native input hook, or null if its installation failed
     */
    public NativeInputHook getInputHook() {
        return inputHook;
    }

    /**
     * Gets the permanent held-key tracker created alongside the keyboard hook. Only valid to call after awaitInstall
     * returns.
     *
     * @return The held-key tracker
     */
    public HeldKeyTracker getHeldKeyTracker() {
        return heldKeyTracker;
    }

    /**
     * Installs the input hook with its permanent held-key tracker attached. A hook that fails to install is left null
     * so the owning controller's recovery path can retry it.
     */
    private void installHooks() {
        // Create the tracker even if the hook install fails so the hot keys controller always receives one
        heldKeyTracker = new HeldKeyTracker();

        inputHook = createInputHook();

        if (inputHook != null) {
            inputHook.addKeyListener(heldKeyTracker);
        }
    }

    /**
     * Creates and starts the native input hook, returning null if the hooks cannot be established so a failure never
     * prevents the rest of the application from starting.
     *
     * @return The native input hook, or null if it could not be created
     */
    public static NativeInputHook createInputHook() {
        try {
            /*
             * Release an earlier hook before installing another. A recovery install would otherwise leave the previous
             * one registered for the life of the process, and a low-level hook sits in the input path of every
             * application
             */
            stopActiveHook();

            NativeInputHook createdHook = new NativeInputHook();

            if (!createdHook.start()) {
                return null;
            }

            // Published before anything that can throw below, so a started hook is always the one shutdown stops
            activeHook = createdHook;

            /*
             * Remove the hooks on the way out through a shutdown hook rather than a single exit path, since the app
             * exits from the window, the notification area menu, and a sign-out alike. Registered only for the first
             * hook, so a recovery that follows a failed install never stacks another one. Registering throws once
             * shutdown is under way, which is harmless here because shutdown is already stopping the hook
             */
            if (shutdownHookRegistered.compareAndSet(false, true)) {
                try {
                    Runtime.getRuntime()
                            .addShutdownHook(new Thread(() -> stopActiveHook(), "Native Input Hook Shutdown"));
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }

            return createdHook;
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Stops whichever hook is currently installed, so the shutdown hook always releases the live one rather than the
     * one that existed when it was registered.
     */
    private static void stopActiveHook() {
        NativeInputHook hookToStop = activeHook;

        if (hookToStop != null) {
            activeHook = null;

            hookToStop.stop();
        }
    }

}
