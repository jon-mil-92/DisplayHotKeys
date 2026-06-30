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

    private static final Object MOUSE_HOOK_LIFECYCLE_LOCK = new Object();
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
    private boolean mouseHookInstallStarted;
    private boolean mouseHookDisposed;

    /**
     * Constructor for the {@link FrameDragController} class.
     *
     * @param view
     *            - The view for the application
     */
    public FrameDragController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
        observedFrame = null;
        cachedConfiguration = null;
        displayTransitionPending = false;
        leftMouseButtonDown = false;
        mouseHookInstallStarted = false;
        mouseHookDisposed = false;
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
        mouseHookDisposed = false;

        installGlobalMouseHook();
        installFrameListeners(frame);
    }

    @Override
    public void cleanUp() {
        removeFrameListeners();

        stopTimer(displaySyncTimer);
        displayTransitionPending = false;

        GlobalMouseHook mouseHookToShutdown;
        GlobalMouseAdapter mouseListenerToRemove;

        synchronized (this) {
            mouseHookDisposed = true;
            mouseHookToShutdown = globalMouseHook;
            mouseListenerToRemove = globalMouseListener;
            globalMouseHook = null;
            globalMouseListener = null;
        }

        if (mouseHookToShutdown != null) {
            shutdownMouseHook(mouseHookToShutdown, mouseListenerToRemove);
        }

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
        FrameUtil.refreshFrame(frame);
        cachedConfiguration = frame.getGraphicsConfiguration();
    }

    /**
     * Installs a default-mode global mouse hook on a background thread so native title-bar drags report the real
     * left-button press/release without blocking the EDT.
     */
    private void installGlobalMouseHook() {
        synchronized (this) {
            if (mouseHookInstallStarted || mouseHookDisposed) {
                return;
            }

            mouseHookInstallStarted = true;
        }

        Thread hookInstallThread = new Thread(() -> {
            GlobalMouseHook mouseHook = null;
            GlobalMouseAdapter hookListener = null;

            try {
                synchronized (MOUSE_HOOK_LIFECYCLE_LOCK) {
                    mouseHook = new GlobalMouseHook();
                    hookListener = createGlobalMouseListener();
                    mouseHook.addMouseListener(hookListener);

                    synchronized (FrameDragController.this) {
                        if (mouseHookDisposed) {
                            shutdownMouseHook(mouseHook, hookListener);
                            return;
                        }

                        globalMouseHook = mouseHook;
                        globalMouseListener = hookListener;
                    }
                }
            } catch (UnsatisfiedLinkError | RuntimeException e) {
                shutdownMouseHook(mouseHook, hookListener);
                e.printStackTrace();
            }
        }, "DisplayHotKeys-MouseHook");

        hookInstallThread.setDaemon(true);
        hookInstallThread.start();
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
     * Removes the listener from a mouse hook and shuts the hook down. Safe for partially initialized hook instances.
     */
    private void shutdownMouseHook(GlobalMouseHook mouseHook, GlobalMouseAdapter mouseListener) {
        if (mouseHook == null) {
            return;
        }

        synchronized (MOUSE_HOOK_LIFECYCLE_LOCK) {
            if (mouseListener != null) {
                try {
                    mouseHook.removeMouseListener(mouseListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                mouseHook.shutdownHook();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
