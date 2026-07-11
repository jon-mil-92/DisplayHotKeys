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

import java.awt.GraphicsConfiguration;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dhk.utility.FrameUtil;
import com.dhk.view.DhkView;

import lc.kra.system.mouse.GlobalMouseHook;
import lc.kra.system.mouse.event.GlobalMouseAdapter;
import lc.kra.system.mouse.event.GlobalMouseEvent;

/**
 * Controls frame drag synchronization so a cross-display drag refreshes and re-fits the frame only after the drag has
 * settled on the destination monitor.
 *
 * @author Jonathan R. Miller
 */
public class FrameDragController implements IController {

    private static final int DISPLAY_SYNC_POST_RELEASE_DELAY_MS = 25;

    private DhkView view;
    private JFrame observedFrame;
    private GraphicsConfiguration cachedConfiguration;
    private Timer displaySyncTimer;
    private GlobalMouseHook globalMouseHook;
    private GlobalMouseAdapter globalMouseListener;
    private PropertyChangeListener graphicsConfigurationListener;
    private ComponentAdapter frameMoveListener;
    private WindowAdapter frameCloseListener;
    private boolean displayTransitionPending;
    private boolean leftMouseButtonDown;

    /**
     * Constructor for the {@link FrameDragController} class.
     *
     * @param view
     *            - The view for the application
     * @param globalMouseHook
     *            - The shared global mouse hook, owned by {@link DhkController} and kept alive across app refreshes
     */
    public FrameDragController(DhkView view, GlobalMouseHook globalMouseHook) {
        this.view = view;
        this.globalMouseHook = globalMouseHook;
    }

    @Override
    public void initController() {
        observedFrame = null;
        cachedConfiguration = null;
        displayTransitionPending = false;
        leftMouseButtonDown = false;
        displaySyncTimer = createDisplaySyncTimer();
    }

    @Override
    public void initListeners() {
        JFrame frame = view.getFrame();

        if (frame == null) {
            return;
        }

        observedFrame = frame;
        cachedConfiguration = frame.getGraphicsConfiguration();
        displayTransitionPending = false;
        leftMouseButtonDown = false;

        installGlobalMouseListener();
        installFrameListeners(frame);
    }

    @Override
    public void cleanUp() {
        removeFrameListeners();

        stopTimer(displaySyncTimer);
        displayTransitionPending = false;

        // Detach only this controller's listener; the shared hook persists across app refreshes
        removeGlobalMouseListener();

        leftMouseButtonDown = false;
        observedFrame = null;
        cachedConfiguration = null;
    }

    /**
     * Installs frame-level listeners that detect cross-display moves and frame disposal.
     */
    private void installFrameListeners(JFrame frame) {
        frameCloseListener = new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                cleanUp();
            }
        };

        graphicsConfigurationListener = propertyChangeEvent -> beginDisplayTransition();
        frameMoveListener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent componentEvent) {
                if (frame.getGraphicsConfiguration() != cachedConfiguration) {
                    beginDisplayTransition();
                } else if (displayTransitionPending) {
                    scheduleDisplaySyncAfterMove();
                }
            }
        };

        frame.addWindowListener(frameCloseListener);
        frame.addPropertyChangeListener("graphicsConfiguration", graphicsConfigurationListener);
        frame.addComponentListener(frameMoveListener);
    }

    /**
     * Removes the listeners attached directly to the frame currently being observed.
     */
    private void removeFrameListeners() {
        if (observedFrame == null) {
            frameCloseListener = null;
            graphicsConfigurationListener = null;
            frameMoveListener = null;
            return;
        }

        if (frameCloseListener != null) {
            observedFrame.removeWindowListener(frameCloseListener);
            frameCloseListener = null;
        }

        if (graphicsConfigurationListener != null) {
            observedFrame.removePropertyChangeListener("graphicsConfiguration", graphicsConfigurationListener);
            graphicsConfigurationListener = null;
        }

        if (frameMoveListener != null) {
            observedFrame.removeComponentListener(frameMoveListener);
            frameMoveListener = null;
        }
    }

    /**
     * Marks that a cross-display move is underway. The refresh waits for the real left-button release, then runs once
     * the destination monitor has settled.
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
     * Performs the deferred frame refresh after move/configuration changes have settled.
     */
    private void updateAfterDisplaySync() {
        JFrame frame = view.getFrame();

        if (frame == null || !frame.isDisplayable() || !displayTransitionPending) {
            return;
        }

        if (leftMouseButtonDown) {
            scheduleDisplaySyncAfterMove();
            return;
        }

        displayTransitionPending = false;

        // Cache the destination config before refreshing so the refresh's own moves are not read as a new transition
        cachedConfiguration = frame.getGraphicsConfiguration();
        FrameUtil.refreshFrame(frame);
    }

    /**
     * Attaches this controller's listener to the shared mouse hook so native title-bar drags report the real
     * left-button press/release. Adding to the hook is cheap, so no background thread is needed.
     */
    private void installGlobalMouseListener() {
        if (globalMouseHook == null || globalMouseListener != null) {
            return;
        }

        globalMouseListener = createGlobalMouseListener();
        globalMouseHook.addMouseListener(globalMouseListener);
    }

    /**
     * Creates the global mouse listener used for native title-bar drags that Swing cannot observe.
     */
    private GlobalMouseAdapter createGlobalMouseListener() {
        return new GlobalMouseAdapter() {
            @Override
            public void mousePressed(GlobalMouseEvent mouseEvent) {
                if (mouseEvent.getButton() == GlobalMouseEvent.BUTTON_LEFT) {
                    SwingUtilities.invokeLater(() -> {
                        leftMouseButtonDown = true;
                        stopTimer(displaySyncTimer);
                    });
                }
            }

            @Override
            public void mouseReleased(GlobalMouseEvent mouseEvent) {
                if (mouseEvent.getButton() != GlobalMouseEvent.BUTTON_LEFT) {
                    return;
                }

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
     * Detaches this controller's listener from the shared mouse hook, leaving the hook alive for later refreshes.
     */
    private void removeGlobalMouseListener() {
        if (globalMouseHook == null || globalMouseListener == null) {
            return;
        }

        try {
            globalMouseHook.removeMouseListener(globalMouseListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        globalMouseListener = null;
    }

    /**
     * Creates the one-shot timer used for deferred display synchronization work.
     */
    private Timer createDisplaySyncTimer() {
        Timer timer = new Timer(DISPLAY_SYNC_POST_RELEASE_DELAY_MS, e -> updateAfterDisplaySync());
        timer.setRepeats(false);

        return timer;
    }

    /**
     * Restarts a one-shot timer using the provided delay.
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
     */
    private void stopTimer(Timer timer) {
        if (timer != null) {
            timer.stop();
        }
    }

}
