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

import com.dhk.utility.TimingLog;

import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.mouse.GlobalMouseHook;

/**
 * Installs the JVM-lifetime global keyboard and mouse hooks on a background thread. Installing a hook can take many
 * seconds right after signing in to Windows, so the install overlaps the rest of the launch work instead of
 * serializing with it.
 *
 * @author Jonathan R. Miller
 */
public class GlobalHookInstaller {

    /**
     * Global keyboard hook that lives for the whole application lifetime, or null until installed or on failure.
     */
    private GlobalKeyboardHook keyboardHook;

    /**
     * Permanent held-key tracker attached to the keyboard hook for the whole application lifetime.
     */
    private HeldKeyTracker heldKeyTracker;

    /**
     * Global mouse hook shared by the frame drag controller, or null until installed or on failure.
     */
    private GlobalMouseHook mouseHook;

    /**
     * Background thread performing the install, or null until the install starts.
     */
    private Thread installThread;

    /**
     * Default constructor for the {@link GlobalHookInstaller} class.
     */
    public GlobalHookInstaller() {
    }

    /**
     * Starts installing the hooks on a background thread.
     */
    public void startInstall() {
        installThread = new Thread(this::installHooks, "Global Hook Installer");

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
     * Gets the installed global keyboard hook. Only valid to call after awaitInstall returns.
     *
     * @return The global keyboard hook, or null if its installation failed
     */
    public GlobalKeyboardHook getKeyboardHook() {
        return keyboardHook;
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
     * Gets the installed global mouse hook. Only valid to call after awaitInstall returns.
     *
     * @return The global mouse hook, or null if its installation failed
     */
    public GlobalMouseHook getMouseHook() {
        return mouseHook;
    }

    /**
     * Installs the keyboard hook with its permanent held-key tracker and then the mouse hook. A hook that fails to
     * install is left null so the owning controller's recovery path can retry it.
     */
    private void installHooks() {
        // Create the tracker even if the hook install fails so the hot keys controller always receives one
        heldKeyTracker = new HeldKeyTracker();

        long keyboardHookStart = TimingLog.start();

        try {
            keyboardHook = new GlobalKeyboardHook(true);
            keyboardHook.addKeyListener(heldKeyTracker);
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            e.printStackTrace();
        }

        TimingLog.end("background keyboard hook install", keyboardHookStart);

        long mouseHookStart = TimingLog.start();
        mouseHook = createMouseHook();
        TimingLog.end("background mouse hook install", mouseHookStart);
    }

    /**
     * Creates the global mouse hook, returning null if the native hook cannot be established so a failure never
     * prevents the rest of the application from starting.
     *
     * @return The global mouse hook, or null if it could not be created
     */
    public static GlobalMouseHook createMouseHook() {
        try {
            return new GlobalMouseHook();
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            e.printStackTrace();

            return null;
        }
    }

}
