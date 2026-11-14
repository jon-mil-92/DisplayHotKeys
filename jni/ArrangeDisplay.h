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
#pragma once

#include <jni.h>
#include <string>
#include <windows.h>

/**
 * A display's stable ID paired with its captured desktop rectangle (source-mode position and size), used to preserve
 * the multi-display arrangement across a resolution or orientation change that resizes one display.
 */
struct DisplayRect {
    /**
     * The stable display ID the rectangle belongs to.
     */
    std::string stableId;

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
    std::string stableId;

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
