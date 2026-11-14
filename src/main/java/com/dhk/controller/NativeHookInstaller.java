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

    private NativeInputHook inputHook;
    private HeldKeyTracker heldKeyTracker;
    private Thread installThread;

    /**
     * Hook the shutdown hook stops, replaced whenever a recovery install creates a new one.
     */
    private static volatile NativeInputHook activeHook;

    /**
     * Whether the shutdown hook has been registered, so a recovery install never registers a second one.
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
        // Always create the tracker so the hot keys controller receives one even when the hook fails to install
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
            // Release any earlier hook so a recovery install never leaks it into the input path of every application
            stopActiveHook();

            NativeInputHook createdHook = new NativeInputHook();

            if (!createdHook.start()) {
                return null;
            }

            // Record the hook before registering the shutdown hook so it is still stopped if registering throws
            activeHook = createdHook;

            /*
             * A shutdown hook stops the input hook on every exit path, including a sign-out. It is registered once
             * because it always stops the active hook, and registering it fails harmlessly once shutdown has begun
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
