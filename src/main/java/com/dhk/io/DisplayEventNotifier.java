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

import javax.swing.SwingUtilities;

/**
 * Manages native display change notifications. This class loads the native library and exposes start/stop methods. The
 * native code will call back and forward to the registered {@link DisplayChangeListener} on the EDT.
 */
public class DisplayEventNotifier {

    private DisplayChangeListener displayChangeListener;

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
    public void registerListener(DisplayChangeListener listener) {
        this.displayChangeListener = listener;
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

}
