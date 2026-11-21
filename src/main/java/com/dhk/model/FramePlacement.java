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
package com.dhk.model;

import java.awt.Window;
import java.util.Map;

/**
 * Captured placement intent for a frame or dialog, along with the placements of the dialogs open over a frame so they
 * are placed again the same way the frame is.
 *
 * @author Jonathan R. Miller
 */
public final class FramePlacement {

    private final String screenDeviceId;
    private final double leftFraction;
    private final double topFraction;
    private final Map<Window, FramePlacement> dialogPlacements;

    /**
     * Constructor for the {@link FramePlacement} class.
     *
     * @param screenDeviceId
     *            - The ID of the screen device the window is on. AWT renumbers its devices when a display is added or
     *            removed, so the ID is read in the same re-initialization that reproduces the placement
     * @param leftFraction
     *            - The window's left edge as a fraction of the working area's available horizontal space (0 = flush
     *            left, 1 = flush right, 0.5 = centered)
     * @param topFraction
     *            - The window's top edge as a fraction of the working area's available vertical space (0 = flush top, 1
     *            = flush bottom, 0.5 = centered)
     * @param dialogPlacements
     *            - The placements of the dialogs open over the frame, keyed by dialog
     */
    public FramePlacement(String screenDeviceId, double leftFraction, double topFraction,
            Map<Window, FramePlacement> dialogPlacements) {
        this.screenDeviceId = screenDeviceId;
        this.leftFraction = leftFraction;
        this.topFraction = topFraction;
        this.dialogPlacements = Map.copyOf(dialogPlacements);
    }

    /**
     * Gets the ID of the screen device the window is on.
     *
     * @return The ID of the screen device the window is on
     */
    public String getScreenDeviceId() {
        return screenDeviceId;
    }

    /**
     * Gets the window's left edge as a fraction of the working area's available horizontal space.
     *
     * @return The window's left edge as a fraction of the working area's available horizontal space
     */
    public double getLeftFraction() {
        return leftFraction;
    }

    /**
     * Gets the window's top edge as a fraction of the working area's available vertical space.
     *
     * @return The window's top edge as a fraction of the working area's available vertical space
     */
    public double getTopFraction() {
        return topFraction;
    }

    /**
     * Gets the placements of the dialogs that were open over the frame when its placement was captured.
     *
     * @return The placements of the dialogs open over the frame, keyed by dialog
     */
    public Map<Window, FramePlacement> getDialogPlacements() {
        return dialogPlacements;
    }

}
