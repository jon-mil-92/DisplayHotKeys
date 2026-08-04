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
