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

#include <jni.h>
#include <string>
#include <windows.h>

using namespace std;

/**
 * A display's stable ID paired with its captured desktop rectangle (source-mode position and size), used to preserve
 * the multi-display arrangement across a resolution or orientation change that resizes one display.
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
     * The on-desktop footprint width of the display (its source-mode width, swapped for a 90°/270° rotation).
     */
    UINT32 width;

    /**
     * The on-desktop footprint height of the display (its source-mode height, swapped for a 90°/270° rotation).
     */
    UINT32 height;
};

/**
 * Working geometry for one display during an arrangement reflow: its old and new rectangle, its live source-mode index,
 * and whether the reflow has changed or placed it.
 */
struct ReflowRect {
    /**
     * The stable display ID this geometry belongs to.
     */
    string stableId;

    /**
     * The captured left (x) position before the change.
     */
    LONG oldLeft;

    /**
     * The captured top (y) position before the change.
     */
    LONG oldTop;

    /**
     * The captured on-desktop footprint width before the change.
     */
    LONG oldWidth;

    /**
     * The captured on-desktop footprint height before the change.
     */
    LONG oldHeight;

    /**
     * The reflowed left (x) position to apply.
     */
    LONG newLeft;

    /**
     * The reflowed top (y) position to apply.
     */
    LONG newTop;

    /**
     * The new on-desktop footprint width (differs from the old width only for a changed display).
     */
    LONG newWidth;

    /**
     * The new on-desktop footprint height (differs from the old height only for a changed display).
     */
    LONG newHeight;

    /**
     * Index of this display's source mode in the live mode array, or -1 if it is not currently active.
     */
    int liveModeIdx;

    /**
     * Whether this display's size changed, so its edges use alignment rather than a rigid offset.
     */
    bool changed;

    /**
     * Whether the reflow has already positioned this display.
     */
    bool placed;
};

/**
 * Captures the current multi-display arrangement as an encoded String[] (one rectangle per active display) for the
 * caller to hold and hand back to preserveDisplayArrangement after a batch of display changes. This is the read half of
 * arrangement preservation; GetDisplay's JNI simply forwards to it.
 *
 * @param env
 *            - The JNI environment pointer
 *
 * @return A String[] snapshot of the current arrangement, one entry per active display
 */
jobjectArray captureDisplayArrangement(JNIEnv *env);

/**
 * Reflows the multi-display arrangement from the given snapshot so every display keeps its relative position and
 * alignment after one or more displays were resized. This is the write half; SetDisplay's JNI simply forwards to it.
 *
 * @param env
 *            - The JNI environment pointer
 * @param snapshot
 *            - The arrangement snapshot returned by captureDisplayArrangement before the batch of changes
 */
void preserveDisplayArrangement(JNIEnv *env, jobjectArray snapshot);
