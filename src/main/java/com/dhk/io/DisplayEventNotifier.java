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

import javax.swing.SwingUtilities;

/**
 * Manages native display change notifications. This class loads the native library and exposes start/stop methods. The
 * native code will call back and forward to the registered {@link DisplayChangeListener} on the EDT.
 */
public class DisplayEventNotifier {

    private DisplayChangeListener displayChangeListener;
    private ShellRestartListener shellRestartListener;

    /**
     * Default constructor for the {@link DisplayEventNotifier} class.
     */
    public DisplayEventNotifier() {
    }

    static {
        try {
            System.loadLibrary("DisplayEventNotifier");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Register a listener that will be notified when the native layer detects a display change.
     *
     * @param listener
     *            - the listener to notify
     */
    public void registerDisplayChangeListener(DisplayChangeListener listener) {
        this.displayChangeListener = listener;
    }

    /**
     * Register a listener that will be notified when the native layer detects a Windows shell restart.
     *
     * @param listener
     *            - the listener to notify
     */
    public void registerShellRestartListener(ShellRestartListener listener) {
        this.shellRestartListener = listener;
    }

    /**
     * Defines a JNI function to start the native display event notifier.
     */
    private native void nativeStart();

    /**
     * Defines a JNI function to stop the native display event notifier.
     */
    private native void nativeStop();

    /**
     * Start native display event notifications. Must be called after registering a listener.
     */
    public void start() {
        nativeStart();
    }

    /**
     * Stop native display event notifications.
     */
    public void stop() {
        nativeStop();
        this.displayChangeListener = null;
        this.shellRestartListener = null;
    }

    /**
     * Called from native code when a display change is detected.
     */
    private void onNativeNotify() {
        final DisplayChangeListener listener = this.displayChangeListener;

        if (listener != null) {
            // Forward to the EDT to run UI/model updates safely
            SwingUtilities.invokeLater(() -> listener.displayConfigurationChanged());
        }
    }

    /**
     * Called from native code when a Windows shell restart is detected.
     */
    private void onShellRestart() {
        final ShellRestartListener listener = this.shellRestartListener;

        if (listener != null) {
            // Forward to the EDT to run UI updates safely
            SwingUtilities.invokeLater(() -> listener.shellRestarted());
        }
    }

}
