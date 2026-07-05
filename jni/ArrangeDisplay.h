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
#pragma once

#include <Windows.h>
#include <string>
#include <vector>

using namespace std;

/**
 * A display's stable ID paired with its captured desktop rectangle (source-mode position and size), used to preserve
 * the multi-monitor arrangement across a resolution or orientation change that resizes one display.
 */
struct DisplayRect {
    /**
     * The stable display ID the rectangle belongs to.
     */
    string stableId;

    /**
     * The desktop position (top-left virtual-desktop coordinate) of the display's source mode.
     */
    POINTL position;

    /**
     * The source-mode width (desktop pixels) of the display.
     */
    UINT32 width;

    /**
     * The source-mode height (desktop pixels) of the display.
     */
    UINT32 height;
};

/**
 * Captures the desktop rectangle (position and source size) of every active display from the current CCD configuration,
 * keyed by stable ID. Call this before a resolution or orientation change so preserveArrangement can rebuild the
 * arrangement around the change.
 *
 * @return The stable ID, source-mode position, and source-mode size of each active display
 */
vector<DisplayRect> captureDisplayRects();

/**
 * Preserves the multi-monitor arrangement after the changed display was resized (by a resolution or orientation
 * change), using the rectangles captured before the change. The changed display stays anchored and the rest of the
 * layout is rebuilt so every display keeps its relative arrangement, edge/center alignment, and diagonal corner
 * adjacency.
 *
 * @param changedStableId
 *            - The stable display ID of the display whose resolution or orientation changed
 * @param savedRects
 *            - The desktop rectangle captured for each display before the change
 */
void preserveArrangement(const string &changedStableId, const vector<DisplayRect> &savedRects);
