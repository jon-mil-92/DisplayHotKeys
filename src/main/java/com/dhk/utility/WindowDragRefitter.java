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
package com.dhk.utility;

import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dhk.io.NativeInputHook;
import com.dhk.io.NativeMouseListener;

/**
 * Refreshes a frame or dialog after it is dragged onto another display, but only once the drag has settled on the
 * destination display. One instance observes one window and detaches all of its listeners when that window closes.
 *
 * @author Jonathan R. Miller
 */
public class WindowDragRefitter {

    private Window observedWindow;
    private NativeInputHook nativeInputHook;
    private Runnable refreshAction;
    private GraphicsConfiguration cachedConfiguration;
    private Timer displaySyncTimer;
    private NativeMouseListener nativeMouseListener;
    private PropertyChangeListener graphicsConfigurationListener;
    private ComponentAdapter windowMoveListener;
    private WindowAdapter windowCloseListener;
    private boolean displayTransitionPending;
    private boolean leftMouseButtonDown;

    /**
     * Delay after the left mouse button is released before the deferred display synchronization runs, so it never
     * interrupts a drag.
     */
    private static final int DISPLAY_SYNC_POST_RELEASE_DELAY_MS = 25;

    /**
     * Constructor for the {@link WindowDragRefitter} class.
     *
     * @param observedWindow
     *            - The frame or dialog to refresh after a cross-display drag
     * @param nativeInputHook
     *            - The shared native input hook that reports the real left-button press and release (may be null)
     * @param refreshAction
     *            - The action that refreshes the window for the display it now occupies
     */
    public WindowDragRefitter(Window observedWindow, NativeInputHook nativeInputHook, Runnable refreshAction) {
        this.observedWindow = observedWindow;
        this.nativeInputHook = nativeInputHook;
        this.refreshAction = refreshAction;
    }

    /**
     * Starts observing the window. Without the native hook the move listeners still run, so a refresh follows once the
     * window stops moving on the new display.
     */
    public void install() {
        if (observedWindow == null) {
            return;
        }

        cachedConfiguration = observedWindow.getGraphicsConfiguration();
        displayTransitionPending = false;
        leftMouseButtonDown = false;
        displaySyncTimer = createDisplaySyncTimer();

        installNativeMouseListener();
        installWindowListeners();
    }

    /**
     * Stops observing the window, detaching every listener and stopping the timer. The shared input hook stays alive
     * for the rest of the application, and calling this again does nothing.
     */
    public void uninstall() {
        removeWindowListeners();

        stopTimer(displaySyncTimer);
        displayTransitionPending = false;

        // Detach only this refitter's listener, since the shared hook persists across app refreshes
        removeNativeMouseListener();

        leftMouseButtonDown = false;
        cachedConfiguration = null;
    }

    /**
     * Installs window-level listeners that detect cross-display moves and the window closing.
     */
    private void installWindowListeners() {
        windowCloseListener = new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                uninstall();
            }
        };

        graphicsConfigurationListener = propertyChangeEvent -> beginDisplayTransition();
        windowMoveListener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent componentEvent) {
                if (observedWindow.getGraphicsConfiguration() != cachedConfiguration) {
                    beginDisplayTransition();
                } else if (displayTransitionPending) {
                    scheduleDisplaySyncAfterMove();
                }
            }
        };

        observedWindow.addWindowListener(windowCloseListener);
        observedWindow.addPropertyChangeListener("graphicsConfiguration", graphicsConfigurationListener);
        observedWindow.addComponentListener(windowMoveListener);
    }

    /**
     * Removes the listeners attached directly to the observed window.
     */
    private void removeWindowListeners() {
        if (windowCloseListener != null) {
            observedWindow.removeWindowListener(windowCloseListener);
            windowCloseListener = null;
        }

        if (graphicsConfigurationListener != null) {
            observedWindow.removePropertyChangeListener("graphicsConfiguration", graphicsConfigurationListener);
            graphicsConfigurationListener = null;
        }

        if (windowMoveListener != null) {
            observedWindow.removeComponentListener(windowMoveListener);
            windowMoveListener = null;
        }
    }

    /**
     * Marks that a cross-display move is underway. The refresh waits for the real left-button release, then runs once
     * the destination display has settled.
     */
    private void beginDisplayTransition() {
        displayTransitionPending = true;
        stopTimer(displaySyncTimer);
        scheduleDisplaySyncAfterMove();
    }

    /**
     * Schedules a deferred refresh after the drag is released.
     */
    private void scheduleDisplaySyncAfterMove() {
        if (!displayTransitionPending || displaySyncTimer == null) {
            return;
        }

        if (leftMouseButtonDown) {
            stopTimer(displaySyncTimer);
            return;
        }

        restartTimer(displaySyncTimer, DISPLAY_SYNC_POST_RELEASE_DELAY_MS);
    }

    /**
     * Performs the deferred window refresh after move or configuration changes have settled.
     */
    private void updateAfterDisplaySync() {
        if (!observedWindow.isDisplayable() || !displayTransitionPending) {
            return;
        }

        if (leftMouseButtonDown) {
            scheduleDisplaySyncAfterMove();
            return;
        }

        displayTransitionPending = false;

        // Cache the destination config before refreshing so the refresh's own moves are not read as a new transition
        cachedConfiguration = observedWindow.getGraphicsConfiguration();
        refreshAction.run();
    }

    /**
     * Attaches this refitter's listener to the shared input hook so native title-bar drags report the real left-button
     * press and release. Adding the listener is cheap, so no background thread is needed.
     */
    private void installNativeMouseListener() {
        if (nativeInputHook == null || nativeMouseListener != null) {
            return;
        }

        nativeMouseListener = createNativeMouseListener();
        nativeInputHook.addMouseListener(nativeMouseListener);
    }

    /**
     * Creates the global mouse listener used for native title-bar drags that Swing cannot observe.
     *
     * @return The global mouse listener that tracks the left mouse button
     */
    private NativeMouseListener createNativeMouseListener() {
        return new NativeMouseListener() {
            @Override
            public void mousePressed() {
                SwingUtilities.invokeLater(() -> {
                    leftMouseButtonDown = true;
                    stopTimer(displaySyncTimer);
                });
            }

            @Override
            public void mouseReleased() {
                SwingUtilities.invokeLater(() -> {
                    leftMouseButtonDown = false;

                    if (displayTransitionPending) {
                        scheduleDisplaySyncAfterMove();
                    }
                });
            }
        };
    }

    /**
     * Detaches this refitter's listener from the shared input hook, leaving the hook alive for later refreshes.
     */
    private void removeNativeMouseListener() {
        if (nativeInputHook == null || nativeMouseListener == null) {
            return;
        }

        try {
            nativeInputHook.removeMouseListener(nativeMouseListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        nativeMouseListener = null;
    }

    /**
     * Creates the one-shot timer used for deferred display synchronization work.
     *
     * @return The one-shot display synchronization timer
     */
    private Timer createDisplaySyncTimer() {
        Timer timer = new Timer(DISPLAY_SYNC_POST_RELEASE_DELAY_MS, e -> updateAfterDisplaySync());
        timer.setRepeats(false);

        return timer;
    }

    /**
     * Restarts a one-shot timer using the provided delay.
     *
     * @param timer
     *            - The timer to restart (may be null)
     * @param delayMs
     *            - The delay in milliseconds before the timer fires
     */
    private void restartTimer(Timer timer, int delayMs) {
        if (timer == null) {
            return;
        }

        timer.setInitialDelay(delayMs);
        timer.setDelay(delayMs);
        timer.restart();
    }

    /**
     * Stops a timer when present.
     *
     * @param timer
     *            - The timer to stop (may be null)
     */
    private void stopTimer(Timer timer) {
        if (timer != null) {
            timer.stop();
        }
    }

}
