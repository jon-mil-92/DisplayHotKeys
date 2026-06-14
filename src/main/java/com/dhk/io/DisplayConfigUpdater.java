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

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import com.dhk.controller.DhkController;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Detects display configuration changes and triggers application re-initialization when the number of visible displays
 * or the identity of the visible displays changes. This ensures correct behavior when switching between physical and
 * virtual displays, when displays are added or removed, or when virtual display drivers activate/deactivate without
 * changing the display count.
 *
 * This class listens for native display change notifications via {@link DisplayEventNotifier} and compares the current
 * display configuration against the previously known configuration. A debounce layer ensures that only one refresh
 * occurs per real configuration change, preventing UI flicker and redundant re-initialization during transient states.
 *
 * @author Jonathan R. Miller
 */
public class DisplayConfigUpdater implements DisplayChangeListener {

    private final DhkModel model;
    private final DisplayConfig displayConfig;
    private final AppRefresher appRefresher;

    /**
     * Stores the last known array of visible display IDs. Used to detect display identity changes even when the number
     * of displays remains the same.
     */
    private String[] lastDisplayIds;

    /**
     * Executor for debounce scheduling.
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Pending debounce task, if any.
     */
    private ScheduledFuture<?> pendingTask = null;

    /**
     * Debounce delay (milliseconds). This should be short because native stabilization already handles most of the
     * heavy lifting.
     */
    private static final int DEBOUNCE_MS = 100;

    /**
     * Constructor for the {@link DisplayConfigUpdater} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param controller
     *            - The controller for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public DisplayConfigUpdater(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;

        displayConfig = new DisplayConfig();
        displayConfig.updateDisplayConfig();

        // Store initial display IDs for change detection
        lastDisplayIds = displayConfig.getDisplayIds();

        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Called when the native layer detects a display configuration change. A debounce layer ensures that only one
     * refresh occurs per real change, preventing redundant re-initialization during transient display states (e.g.,
     * virtual display creation/destruction waves).
     */
    @Override
    public void displayConfigurationChanged() {
        // Cancel any pending refresh
        if (pendingTask != null) {
            pendingTask.cancel(false);
        }

        // Schedule a new refresh after the debounce delay
        pendingTask = scheduler.schedule(() -> {
            SwingUtilities.invokeLater(() -> {
                // Refresh the display configuration
                displayConfig.updateConnectedDisplays();

                String[] currentIds = displayConfig.getDisplayIds();
                int currentCount = displayConfig.getNumOfConnectedDisplays();
                int previousCount = model.getNumOfConnectedDisplays();
                boolean countChanged = (currentCount != previousCount);
                boolean idsChanged = !Arrays.equals(currentIds, lastDisplayIds);

                if (countChanged || idsChanged) {
                    // Update stored IDs
                    lastDisplayIds = currentIds;

                    // Re-initialize the application to reflect the new display configuration
                    appRefresher.reInitApp();
                }
            });
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

}
