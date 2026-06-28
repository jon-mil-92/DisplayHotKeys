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

import javax.swing.Timer;

import com.dhk.controller.DhkController;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Re-initializes the application when the native layer reports a display configuration change. The native
 * {@link DisplayEventNotifier} signals displays added or removed, and resolution, DPI, or orientation changes.
 *
 * @author Jonathan R. Miller
 */
public class DisplayConfigUpdater implements DisplayChangeListener {

    private final AppRefresher appRefresher;
    private final Timer reInitTimer;

    private static final int REINIT_DELAY_MS = 400;

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
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
        reInitTimer = new Timer(REINIT_DELAY_MS, e -> appRefresher.reInitApp());
        reInitTimer.setRepeats(false);
    }

    @Override
    public void displayConfigurationChanged() {
        reInitTimer.restart();
    }

    /**
     * Stops any pending deferred re-initialization. Called when the owning controller is torn down (on app re-init or
     * shutdown) so the Timer cannot fire against a disposed view and is released for garbage collection.
     */
    public void cleanUp() {
        reInitTimer.stop();
    }

}
