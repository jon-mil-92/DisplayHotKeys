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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.dhk.model.DisplayMode;
import com.dhk.model.RefreshRate;
import com.dhk.model.Resolution;

/**
 * Decomposes an array of display modes into its resolution and refresh-rate parts so the view can present them as two
 * separate selections. The order of the source array is preserved, so the derived resolutions and refresh rates keep
 * whatever ordering the modes were supplied in.
 *
 * @author Jonathan R. Miller
 */
public class DisplayModeUtil {

    /**
     * Default constructor for the {@link DisplayModeUtil} class.
     */
    public DisplayModeUtil() {
    }

    /**
     * Extracts the distinct resolutions from the given display modes, in first-seen order.
     *
     * @param displayModes
     *            - The display modes to extract distinct resolutions from
     *
     * @return The distinct resolutions in first-seen order
     */
    public static Resolution[] distinctResolutions(DisplayMode[] displayModes) {
        Set<Resolution> resolutions = new LinkedHashSet<Resolution>();

        for (DisplayMode displayMode : displayModes) {
            resolutions.add(displayMode.getResolution());
        }

        return resolutions.toArray(new Resolution[0]);
    }

    /**
     * Collects the refresh rates the given display modes offer for the specified resolution, in the order the modes
     * were supplied.
     *
     * @param displayModes
     *            - The display modes to collect refresh rates from
     * @param resolution
     *            - The resolution to collect refresh rates for
     *
     * @return The refresh rates supported by the given resolution
     */
    public static RefreshRate[] refreshRatesForResolution(DisplayMode[] displayModes, Resolution resolution) {
        List<RefreshRate> refreshRates = new ArrayList<RefreshRate>();

        for (DisplayMode displayMode : displayModes) {
            if (displayMode.getResolution().equals(resolution)) {
                refreshRates.add(displayMode.getRefreshRate());
            }
        }

        return refreshRates.toArray(new RefreshRate[0]);
    }

}
