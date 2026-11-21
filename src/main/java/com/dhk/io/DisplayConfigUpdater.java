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

import com.dhk.controller.DhkController;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.view.DhkView;

/**
 * Re-initializes the application when the native layer reports a display configuration change. The native
 * {@link DisplayEventNotifier} signals displays added or removed, and resolution, refresh rate, DPI scale, scaling
 * mode, or orientation changes.
 *
 * @author Jonathan R. Miller
 */
public class DisplayConfigUpdater implements DisplayChangeListener {

    private final AppRefresher appRefresher;
    private final DhkController controller;
    private boolean cleanedUp;

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
        this.controller = controller;

        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    @Override
    public void displayConfigurationChanged() {
        /*
         * A callback queued on the EDT before the notifier stopped can arrive after teardown. Re-initializing then
         * would duplicate the re-initialization that already absorbed the change, so it is swallowed
         */
        if (cleanedUp) {
            return;
        }

        // Covers changes made outside the app, which reach the tray and the display info panels through no other path
        controller.displayConfigurationChanged();

        // The native layer reports a change only once it has settled, so the app re-initializes without further delay
        appRefresher.reInitApp();
    }

    /**
     * Permanently retires this updater. Called when the owning controller is torn down (on app re-init or shutdown) so
     * a late notification callback cannot re-initialize a disposed view.
     */
    public void cleanUp() {
        cleanedUp = true;
    }

}
