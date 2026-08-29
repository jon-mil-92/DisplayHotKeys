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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reads keyboard and mouse input through the native library and delivers it to registered listeners. Keys are reported
 * as virtual key codes, which a listener pairs with the hot keys it is watching for.
 *
 * @author Jonathan R. Miller
 */
public class NativeInputHook {

    private final List<NativeKeyListener> keyListeners;
    private final List<NativeMouseListener> mouseListeners;
    private volatile boolean running;

    static {
        try {
            System.loadLibrary("NativeInputHook");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Default constructor for the {@link NativeInputHook} class.
     */
    public NativeInputHook() {
        keyListeners = new CopyOnWriteArrayList<NativeKeyListener>();
        mouseListeners = new CopyOnWriteArrayList<NativeMouseListener>();
    }

    /**
     * Defines a JNI function to install the keyboard and mouse hooks.
     *
     * @return Whether both hooks were installed
     */
    private native boolean nativeStart();

    /**
     * Defines a JNI function to remove the keyboard and mouse hooks.
     */
    private native void nativeStop();

    /**
     * Installs the hooks. The native layer reads input without placing anything in the path the rest of the system
     * receives its own input through, so a slow listener delays only later events reported here.
     *
     * @return Whether the hooks were installed
     */
    public synchronized boolean start() {
        if (running) {
            return true;
        }

        if (!nativeStart()) {
            return false;
        }

        running = true;

        return true;
    }

    /**
     * Removes the hooks. Only called when the application is exiting, so they are released deterministically rather
     * than at process teardown.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;

        nativeStop();
    }

    /**
     * Adds a listener notified of every native key event.
     *
     * @param listener
     *            - The listener to add
     */
    public void addKeyListener(NativeKeyListener listener) {
        keyListeners.add(listener);
    }

    /**
     * Removes a listener from the native key event notifications.
     *
     * @param listener
     *            - The listener to remove
     */
    public void removeKeyListener(NativeKeyListener listener) {
        keyListeners.remove(listener);
    }

    /**
     * Adds a listener notified of every native mouse button event.
     *
     * @param listener
     *            - The listener to add
     */
    public void addMouseListener(NativeMouseListener listener) {
        mouseListeners.add(listener);
    }

    /**
     * Removes a listener from the native mouse button event notifications.
     *
     * @param listener
     *            - The listener to remove
     */
    public void removeMouseListener(NativeMouseListener listener) {
        mouseListeners.remove(listener);
    }

    /**
     * Called from native code when a key changes state. Runs on the native reporting thread, so a slow listener delays
     * later events reported here but never input for other applications.
     *
     * @param virtualKeyCode
     *            - The virtual key code of the key that changed state
     * @param isPressed
     *            - Whether the key was pressed rather than released
     */
    private void onKeyEvent(int virtualKeyCode, boolean isPressed) {
        // Checked here and not for the mouse, since key events arrive at typing rate while clicks are occasional
        if (keyListeners.isEmpty()) {
            return;
        }

        NativeKeyEvent keyEvent = new NativeKeyEvent(virtualKeyCode);

        for (NativeKeyListener listener : keyListeners) {
            // Contained per listener, including an Error, so one failing listener never stops the rest from being told
            try {
                if (isPressed) {
                    listener.keyPressed(keyEvent);
                } else {
                    listener.keyReleased(keyEvent);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Called from native code when the left mouse button changes state. Runs on the native reporting thread, so a slow
     * listener delays later events reported here but never input for other applications.
     *
     * @param isPressed
     *            - Whether the button was pressed rather than released
     */
    private void onMouseEvent(boolean isPressed) {
        for (NativeMouseListener listener : mouseListeners) {
            // Contained per listener, including an Error, so one failing listener never stops the rest from being told
            try {
                if (isPressed) {
                    listener.mousePressed();
                } else {
                    listener.mouseReleased();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
