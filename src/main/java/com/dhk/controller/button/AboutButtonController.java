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
package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.io.NativeInputHook;
import com.dhk.view.AboutDialog;
import com.dhk.view.DhkView;

/**
 * Controls the About button. Listeners are added to the corresponding view component so that when the About button is
 * pressed, an "About Display Hot Keys" dialog is shown with application information.
 *
 * @author Jonathan R. Miller
 */
public class AboutButtonController extends AbstractButtonController implements IController {

    private DhkView view;
    private NativeInputHook nativeInputHook;
    private AboutDialog aboutDialog;

    /**
     * Constructor for the {@link AboutButtonController} class.
     *
     * @param view
     *            - The view for the application
     * @param nativeInputHook
     *            - The shared native input hook the dialog uses to re-fit after a cross-display drag (may be null)
     */
    public AboutButtonController(DhkView view, NativeInputHook nativeInputHook) {
        this.view = view;
        this.nativeInputHook = nativeInputHook;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        view.getAboutButton().addActionListener(e -> showAboutDialog(null));

        initStateChangeListeners(view.getAboutButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Shows an "About Display Hot Keys" dialog with application information, built per show so it binds to the current
     * frame, since every re-initialization replaces it.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    public void showAboutDialog(Runnable onCloseAction) {
        aboutDialog = new AboutDialog(view, nativeInputHook);
        aboutDialog.showAboutDialog(onCloseAction);
    }

}
