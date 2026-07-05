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
#include "ArrangeDisplay.h"
#include "DisplayConfig.h"

using namespace std;

/**
 * Flags for applying a supplied CCD configuration and persisting it to the Windows display database.
 */
static const UINT32 SDC_SUPPLIED_APPLY_FLAGS = SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE;

/**
 * Pixel tolerance for treating two display edges (or centers) as intentionally aligned. Small residues from manual
 * dragging or differing DPI would otherwise defeat exact-equality alignment detection; near-aligned displays are
 * snapped to exact alignment while genuinely offset (near-diagonal) displays, which sit far from any alignment, are
 * left as-is.
 */
static const LONG ALIGNMENT_TOLERANCE_PX = 50;

/**
 * Working geometry for one display during an arrangement reflow: its stable ID, its captured (old) and computed (new)
 * top-left position and size, and whether the reflow has already placed it.
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
     * The captured source width before the change.
     */
    LONG oldWidth;

    /**
     * The captured source height before the change.
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
     * The new source width (differs from the old width only for the changed display).
     */
    LONG newWidth;

    /**
     * The new source height (differs from the old height only for the changed display).
     */
    LONG newHeight;

    /**
     * Whether the reflow has already positioned this display.
     */
    bool placed;
};

/**
 * Reports whether two coordinates are within the given tolerance of each other.
 *
 * @param first
 *            - The first coordinate
 * @param second
 *            - The second coordinate
 * @param tolerance
 *            - The maximum difference that still counts as equal
 *
 * @return Whether the two coordinates are within tolerance
 */
static bool nearlyEqual(LONG first, LONG second, LONG tolerance) {
    return (first - second <= tolerance) && (second - first <= tolerance);
}

/**
 * Computes a neighbor's aligned coordinate along the axis perpendicular to a shared edge, staying as faithful to the
 * original arrangement as possible. Clean alignments are detected within a pixel tolerance and preserved so they
 * survive a resize: a leading-edge-aligned neighbor (top or left) keeps its leading edges flush, a
 * trailing-edge-aligned neighbor (bottom or right) keeps its trailing edges flush, and a centered neighbor stays
 * centered. The tolerance absorbs small residues from manual dragging or differing DPI. Any other (near-diagonal)
 * neighbor keeps its distance from whichever of the anchor's edges it was biased toward, so its small overlap is
 * preserved rather than snapped. The result is clamped to keep at least a one-pixel shared edge so the neighbor can
 * never be pushed off into an unreachable state.
 *
 * @param anchorIsChanged
 *            - Whether the anchor display is the one that was resized
 * @param anchorOldStart
 *            - The anchor's old start coordinate on this axis (old top or left)
 * @param anchorOldSize
 *            - The anchor's old size on this axis (old height or width)
 * @param anchorNewStart
 *            - The anchor's new start coordinate on this axis
 * @param anchorNewSize
 *            - The anchor's new size on this axis
 * @param neighborOldStart
 *            - The neighbor's old start coordinate on this axis
 * @param neighborSize
 *            - The neighbor's size on this axis (unchanged by the reflow)
 *
 * @return The neighbor's new start coordinate on this axis
 */
static LONG alignPerpendicular(bool anchorIsChanged, LONG anchorOldStart, LONG anchorOldSize, LONG anchorNewStart,
                               LONG anchorNewSize, LONG neighborOldStart, LONG neighborSize) {
    LONG result;

    if (!anchorIsChanged) {
        // The anchor kept its size, so the neighbor's exact offset is preserved
        result = anchorNewStart + (neighborOldStart - anchorOldStart);
    } else {
        // Doubled centers (avoids halving) and trailing edges, used to detect clean alignments and any diagonal bias
        LONG neighborCenter = 2 * neighborOldStart + neighborSize;
        LONG anchorCenter = 2 * anchorOldStart + anchorOldSize;
        LONG neighborEnd = neighborOldStart + neighborSize;
        LONG anchorEnd = anchorOldStart + anchorOldSize;

        if (nearlyEqual(neighborOldStart, anchorOldStart, ALIGNMENT_TOLERANCE_PX)) {
            // Leading-edge-aligned (top or left): keep the leading edges flush regardless of the sizes
            result = anchorNewStart;
        } else if (nearlyEqual(neighborEnd, anchorEnd, ALIGNMENT_TOLERANCE_PX)) {
            // Trailing-edge-aligned (bottom or right): keep the trailing edges flush regardless of the sizes
            result = (anchorNewStart + anchorNewSize) - neighborSize;
        } else if (nearlyEqual(neighborCenter, anchorCenter, 2 * ALIGNMENT_TOLERANCE_PX)) {
            // Centered on the anchor (doubled tolerance matches the doubled centers): keep it centered in the new span
            result = anchorNewStart + (anchorNewSize - neighborSize) / 2;
        } else if (neighborCenter < anchorCenter) {
            // Not aligned but biased toward the anchor's low edge: preserve the distance from that edge
            result = anchorNewStart + (neighborOldStart - anchorOldStart);
        } else {
            // Not aligned but biased toward the anchor's high edge: preserve the distance from that edge
            result = (anchorNewStart + anchorNewSize) - neighborSize + (neighborEnd - anchorEnd);
        }
    }

    // Keep at least a one-pixel shared edge with the anchor's new span so the neighbor stays connected and reachable
    LONG minStart = anchorNewStart - neighborSize + 1;
    LONG maxStart = anchorNewStart + anchorNewSize - 1;

    if (result < minStart) {
        result = minStart;
    } else if (result > maxStart) {
        result = maxStart;
    }

    return result;
}

/**
 * Attempts to position an unplaced neighbor relative to an already-placed anchor, preserving how they were arranged.
 * An edge-adjacent neighbor is placed flush against the anchor's changed edge with its perpendicular alignment
 * preserved; a corner-adjacent (diagonally touching) neighbor is pinned on both axes so the shared corner stays flush,
 * which keeps diagonal mouse transit working. Corner adjacency is considered only when allowCorner is set, so an edge
 * relationship is always preferred.
 *
 * @param neighbor
 *            - The unplaced neighbor to position; its new position is written on a match
 * @param anchor
 *            - The already-placed anchor to position against
 * @param anchorIsChanged
 *            - Whether the anchor is the resized display, requiring proportional edge alignment
 * @param allowCorner
 *            - Whether to also match a corner (diagonal) adjacency
 *
 * @return Whether the neighbor was adjacent to the anchor and has been positioned
 */
static bool placeNeighbor(ReflowRect &neighbor, const ReflowRect &anchor, bool anchorIsChanged, bool allowCorner) {
    // Displays share an edge only when their spans overlap on the axis perpendicular to that edge
    bool verticalOverlap =
        neighbor.oldTop < anchor.oldTop + anchor.oldHeight && anchor.oldTop < neighbor.oldTop + neighbor.oldHeight;
    bool horizontalOverlap =
        neighbor.oldLeft < anchor.oldLeft + anchor.oldWidth && anchor.oldLeft < neighbor.oldLeft + neighbor.oldWidth;

    if (verticalOverlap && neighbor.oldLeft == anchor.oldLeft + anchor.oldWidth) {
        // Neighbor sits to the right; keep it flush and preserve its vertical alignment
        neighbor.newLeft = anchor.newLeft + anchor.newWidth;
        neighbor.newTop = alignPerpendicular(anchorIsChanged, anchor.oldTop, anchor.oldHeight, anchor.newTop,
                                             anchor.newHeight, neighbor.oldTop, neighbor.newHeight);
        return true;
    }

    if (verticalOverlap && neighbor.oldLeft + neighbor.oldWidth == anchor.oldLeft) {
        // Neighbor sits to the left
        neighbor.newLeft = anchor.newLeft - neighbor.newWidth;
        neighbor.newTop = alignPerpendicular(anchorIsChanged, anchor.oldTop, anchor.oldHeight, anchor.newTop,
                                             anchor.newHeight, neighbor.oldTop, neighbor.newHeight);
        return true;
    }

    if (horizontalOverlap && neighbor.oldTop == anchor.oldTop + anchor.oldHeight) {
        // Neighbor sits below; keep it flush and preserve its horizontal alignment
        neighbor.newTop = anchor.newTop + anchor.newHeight;
        neighbor.newLeft = alignPerpendicular(anchorIsChanged, anchor.oldLeft, anchor.oldWidth, anchor.newLeft,
                                              anchor.newWidth, neighbor.oldLeft, neighbor.newWidth);
        return true;
    }

    if (horizontalOverlap && neighbor.oldTop + neighbor.oldHeight == anchor.oldTop) {
        // Neighbor sits above
        neighbor.newTop = anchor.newTop - neighbor.newHeight;
        neighbor.newLeft = alignPerpendicular(anchorIsChanged, anchor.oldLeft, anchor.oldWidth, anchor.newLeft,
                                              anchor.newWidth, neighbor.oldLeft, neighbor.newWidth);
        return true;
    }

    if (!allowCorner) {
        return false;
    }

    // Corner (diagonal) adjacency: the spans meet at a single point, so pin both axes to keep that corner touching
    bool touchesRight = (neighbor.oldLeft == anchor.oldLeft + anchor.oldWidth);
    bool touchesLeft = (neighbor.oldLeft + neighbor.oldWidth == anchor.oldLeft);
    bool touchesBelow = (neighbor.oldTop == anchor.oldTop + anchor.oldHeight);
    bool touchesAbove = (neighbor.oldTop + neighbor.oldHeight == anchor.oldTop);

    if ((touchesRight || touchesLeft) && (touchesBelow || touchesAbove)) {
        neighbor.newLeft = touchesRight ? anchor.newLeft + anchor.newWidth : anchor.newLeft - neighbor.newWidth;
        neighbor.newTop = touchesBelow ? anchor.newTop + anchor.newHeight : anchor.newTop - neighbor.newHeight;
        return true;
    }

    return false;
}

/**
 * Captures the desktop rectangle (position and source size) of every active display from the current CCD configuration,
 * keyed by stable ID, so the multi-monitor arrangement can be preserved across a resolution or orientation change that
 * resizes one display.
 *
 * @return The stable ID, source-mode position, and source-mode size of each active display
 */
vector<DisplayRect> captureDisplayRects() {
    vector<DisplayRect> rects;
    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> modes;

    if (!queryActiveCcdConfig(paths, modes)) {
        return rects;
    }

    for (const DISPLAYCONFIG_PATH_INFO &path : paths) {
        if ((path.flags & DISPLAYCONFIG_PATH_ACTIVE) == 0) {
            continue;
        }

        UINT32 sourceModeIdx = path.sourceInfo.modeInfoIdx;

        if (sourceModeIdx == DISPLAYCONFIG_PATH_MODE_IDX_INVALID || sourceModeIdx >= modes.size() ||
            modes[sourceModeIdx].infoType != DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE) {
            continue;
        }

        string stableId = stableIdForTarget(path.targetInfo);

        if (stableId.empty()) {
            continue;
        }

        const DISPLAYCONFIG_SOURCE_MODE &source = modes[sourceModeIdx].sourceMode;

        rects.push_back({stableId, source.position, source.width, source.height});
    }

    return rects;
}

/**
 * Preserves the multi-monitor arrangement after the changed display was resized (by a resolution or orientation
 * change). Restoring absolute positions fails when the changed display grew, since neighbors overlap and Windows
 * re-packs them (stacking above/below). Instead the changed display stays anchored and the layout is rebuilt by a
 * breadth-first walk of the original adjacency graph: each neighbor is placed flush against its anchor's changed edge
 * while its perpendicular alignment (edge-aligned or centered) is preserved, and displays not touching the changed one
 * keep their exact relative offsets. The layout is normalized so the original primary returns to the desktop origin and
 * applied strictly, falling back to SDC_ALLOW_CHANGES only if a driver rejects it.
 *
 * @param changedStableId
 *            - The stable display ID of the display whose resolution or orientation changed
 * @param savedRects
 *            - The desktop rectangle captured for each display before the change
 */
void preserveArrangement(const string &changedStableId, const vector<DisplayRect> &savedRects) {
    // A single display has no arrangement to preserve
    if (savedRects.size() < 2) {
        return;
    }

    int changedIndex = -1;

    for (size_t i = 0; i < savedRects.size(); i++) {
        if (savedRects[i].stableId == changedStableId) {
            changedIndex = (int) i;
            break;
        }
    }

    if (changedIndex < 0) {
        return;
    }

    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> modes;

    if (!queryActiveCcdConfig(paths, modes)) {
        return;
    }

    // Read the changed display's new (applied) source size from the live config to anchor the reflow around it
    int changedPathIndex = findActivePathForDisplay(paths, changedStableId);

    if (changedPathIndex < 0) {
        return;
    }

    UINT32 changedModeIdx = paths[changedPathIndex].sourceInfo.modeInfoIdx;

    if (changedModeIdx == DISPLAYCONFIG_PATH_MODE_IDX_INVALID || changedModeIdx >= modes.size() ||
        modes[changedModeIdx].infoType != DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE) {
        return;
    }

    size_t count = savedRects.size();

    // Seed the working layout from the captured geometry; new fields start equal to old and are recomputed by the walk
    vector<ReflowRect> layout(count);

    for (size_t i = 0; i < count; i++) {
        layout[i].stableId = savedRects[i].stableId;
        layout[i].oldLeft = savedRects[i].position.x;
        layout[i].oldTop = savedRects[i].position.y;
        layout[i].oldWidth = (LONG) savedRects[i].width;
        layout[i].oldHeight = (LONG) savedRects[i].height;
        layout[i].newLeft = layout[i].oldLeft;
        layout[i].newTop = layout[i].oldTop;
        layout[i].newWidth = layout[i].oldWidth;
        layout[i].newHeight = layout[i].oldHeight;
        layout[i].placed = false;
    }

    // Anchor the changed display at its original top-left with its new size
    layout[changedIndex].newWidth = (LONG) modes[changedModeIdx].sourceMode.width;
    layout[changedIndex].newHeight = (LONG) modes[changedModeIdx].sourceMode.height;
    layout[changedIndex].placed = true;

    /*
     * Grow the placed set outward from the changed display over the original adjacency graph. The first phase places
     * only edge-adjacent neighbors so a shared edge always wins; the second phase additionally pins corner-adjacent
     * (diagonally touching) neighbors so their shared corner stays flush and diagonal mouse transit keeps working. Each
     * phase repeats until it makes no further progress, so placements propagate across the whole layout.
     */
    for (int phase = 0; phase < 2; phase++) {
        bool allowCorner = (phase == 1);
        bool progress = true;

        while (progress) {
            progress = false;

            for (size_t p = 0; p < count; p++) {
                if (!layout[p].placed) {
                    continue;
                }

                bool anchorIsChanged = ((int) p == changedIndex);

                for (size_t q = 0; q < count; q++) {
                    if (layout[q].placed) {
                        continue;
                    }

                    if (placeNeighbor(layout[q], layout[p], anchorIsChanged, allowCorner)) {
                        layout[q].placed = true;
                        progress = true;
                    }
                }
            }
        }
    }

    // Normalize so the display that was primary returns to the desktop origin, keeping the supplied config valid
    LONG originLeft = 0;
    LONG originTop = 0;

    for (const ReflowRect &rect : layout) {
        if (rect.oldLeft == 0 && rect.oldTop == 0) {
            originLeft = rect.newLeft;
            originTop = rect.newTop;
            break;
        }
    }

    // Write the reflowed positions into the live config
    bool changed = false;

    for (const DISPLAYCONFIG_PATH_INFO &path : paths) {
        if ((path.flags & DISPLAYCONFIG_PATH_ACTIVE) == 0) {
            continue;
        }

        UINT32 sourceModeIdx = path.sourceInfo.modeInfoIdx;

        if (sourceModeIdx == DISPLAYCONFIG_PATH_MODE_IDX_INVALID || sourceModeIdx >= modes.size() ||
            modes[sourceModeIdx].infoType != DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE) {
            continue;
        }

        string stableId = stableIdForTarget(path.targetInfo);

        for (const ReflowRect &rect : layout) {
            if (rect.stableId != stableId) {
                continue;
            }

            POINTL target = {rect.newLeft - originLeft, rect.newTop - originTop};
            POINTL &current = modes[sourceModeIdx].sourceMode.position;

            if (current.x != target.x || current.y != target.y) {
                current = target;
                changed = true;
            }

            break;
        }
    }

    // Nothing moved, so the arrangement already matches the desired layout
    if (!changed) {
        return;
    }

    UINT32 pathCount = (UINT32) paths.size();
    UINT32 modeCount = (UINT32) modes.size();

    // Prefer the exact reflowed layout; only let Windows adjust the remainder when a driver rejects the strict apply
    if (SetDisplayConfig(pathCount, paths.data(), modeCount, modes.data(), SDC_SUPPLIED_APPLY_FLAGS) == ERROR_SUCCESS) {
        return;
    }

    SetDisplayConfig(pathCount, paths.data(), modeCount, modes.data(), SDC_SUPPLIED_APPLY_FLAGS | SDC_ALLOW_CHANGES);
}
