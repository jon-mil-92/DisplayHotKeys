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
package com.dhk.controller;

import javax.swing.JFrame;

import com.dhk.io.NativeInputHook;
import com.dhk.utility.WindowDragRefitter;
import com.dhk.utility.WindowUtil;
import com.dhk.view.DhkView;

/**
 * Controls frame drag synchronization so a cross-display drag refreshes and re-fits the frame only after the drag has
 * settled on the destination display.
 *
 * @author Jonathan R. Miller
 */
public class FrameDragController implements IController {

    private DhkView view;
    private NativeInputHook nativeInputHook;
    private WindowDragRefitter frameDragRefitter;

    /**
     * Constructor for the {@link FrameDragController} class.
     *
     * @param view
     *            - The view for the application
     * @param nativeInputHook
     *            - The shared native input hook, owned by {@link DhkController} and kept alive across app refreshes
     */
    public FrameDragController(DhkView view, NativeInputHook nativeInputHook) {
        this.view = view;
        this.nativeInputHook = nativeInputHook;
    }

    @Override
    public void initController() {
        frameDragRefitter = null;
    }

    @Override
    public void initListeners() {
        JFrame frame = view.getFrame();

        if (frame == null) {
            return;
        }

        frameDragRefitter = new WindowDragRefitter(frame, nativeInputHook, () -> WindowUtil.refreshWindow(frame));
        frameDragRefitter.install();
    }

    @Override
    public void cleanUp() {
        if (frameDragRefitter != null) {
            frameDragRefitter.uninstall();
            frameDragRefitter = null;
        }
    }

}
