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
     * The label of each setting line, shared by every panel and drawn in the left column.
     */
    std::vector<std::wstring> labels;

    /**
     * The value of each setting line, panel by panel, with one value per label for each header, drawn bold in the
     * right column.
     */
    std::vector<std::wstring> values;

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
     * Color of the line drawn around each panel and of the separator beneath its header line.
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
     * Space on each side of the separator beneath the header line.
     */
    int headerGap;

    /**
     * Space between consecutive setting lines.
     */
    int lineGap;

    /**
     * Space between the label column and the value column.
     */
    int columnGap;

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
 * A shown display info panel and the positions it was laid out with, in physical pixels, so painting it never has to
 * measure or scale anything again.
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
     * Top-left corner of the header line, whose left edge every label shares.
     */
    POINT headerOrigin;

    /**
     * Bounds of the line separating the header line from the setting lines.
     */
    RECT separatorRect;

    /**
     * Top of the first setting line.
     */
    LONG firstLineTop;

    /**
     * Distance from the top of one setting line to the top of the next.
     */
    LONG lineSpacing;

    /**
     * Left edge of the value column, which sits past the widest label by the column gap.
     */
    LONG valueColumnLeft;

    /**
     * Whether the panel draws its own border, which it does where the system cannot color the window border.
     */
    bool drawFrame;
};

/**
 * Owns the fonts a panel is measured and painted in, scaled for the panel's display, and deletes them on destruction.
 */
struct DisplayInfoPanelFonts {
    /**
     * Bold font of the header line.
     */
    HFONT headerFont;

    /**
     * Regular font of the setting labels.
     */
    HFONT labelFont;

    /**
     * Bold font of the setting values.
     */
    HFONT valueFont;

    /**
     * Deletes the fonts, which must no longer be selected into any device context.
     */
    ~DisplayInfoPanelFonts() {
        DeleteObject(headerFont);
        DeleteObject(labelFont);
        DeleteObject(valueFont);
    }
};
