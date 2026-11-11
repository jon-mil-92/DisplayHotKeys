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

/**
 * The text, placement, colors, font, and timing of a set of display info panels, captured up front so showing them
 * never has to call back for any of it. Every metric is in unscaled pixels and is scaled for the display each panel
 * sits on.
 */
struct DisplayInfoOverlayDefinition {
    /**
     * The bold first line of each panel, one per panel.
     */
    std::vector<std::wstring> headers;

    /**
     * The setting lines of every panel, panel by panel, with linesPerPanel lines for each header.
     */
    std::vector<std::wstring> lines;

    /**
     * Number of setting lines in each panel.
     */
    int linesPerPanel;

    /**
     * A point on each panel's display in physical desktop coordinates, which selects the display the panel sits on.
     */
    std::vector<POINT> anchorPoints;

    /**
     * Color filling each panel.
     */
    COLORREF backgroundColor;

    /**
     * Color of every line of text.
     */
    COLORREF foregroundColor;

    /**
     * Color of the line drawn around each panel.
     */
    COLORREF borderColor;

    /**
     * Family of the font the panels are drawn in.
     */
    std::wstring fontFamily;

    /**
     * Height of the setting lines' font em square.
     */
    int fontSize;

    /**
     * Height of the header line's font em square.
     */
    int headerFontSize;

    /**
     * Space between a panel's edges and its text.
     */
    int padding;

    /**
     * Space between the header line and the first setting line.
     */
    int headerGap;

    /**
     * Space between consecutive setting lines.
     */
    int lineGap;

    /**
     * Space between a panel and the edges of its display's work area, and between panels stacked on one display.
     */
    int margin;

    /**
     * Opacity of the panels, from 0 (invisible) to 255 (opaque).
     */
    BYTE opacity;

    /**
     * How long the panels stay visible, in milliseconds.
     */
    UINT durationMs;
};

/**
 * A shown display info panel and the layout it was measured with, so painting it never has to measure again.
 */
struct DisplayInfoPanel {
    /**
     * The panel's window, or null when the panel could not be shown.
     */
    HWND window;

    /**
     * DPI of the display the panel sits on, which every metric is scaled by.
     */
    UINT dpi;

    /**
     * Height of the header line in the panel's scaled font.
     */
    int headerHeight;

    /**
     * Height of each setting line in the panel's scaled font.
     */
    int lineHeight;

    /**
     * Whether the panel draws its own border, which it does where the system cannot color the window border.
     */
    bool drawFrame;
};
