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

#include <cstdlib>
#include <jni.h>
#include <string>
#include <vector>
#include <windows.h>

using namespace std;

/**
 * Flags for applying a supplied CCD configuration and persisting it to the Windows display database.
 */
static const UINT32 SDC_SUPPLIED_APPLY_FLAGS = SDC_APPLY | SDC_USE_SUPPLIED_DISPLAY_CONFIG | SDC_SAVE_TO_DATABASE;

/**
 * Pixel tolerance for treating two display edges or centers as intentionally aligned, so small manual-drag or DPI
 * residues snap flush while a genuinely offset (near-diagonal) display is left as-is.
 */
static const LONG ALIGNMENT_TOLERANCE_PX = 80;

/**
 * Pixel tolerance for treating two stacked displays as touching, so a small snapping residue between them heals to
 * flush while a genuinely intended gap is left as a separation.
 */
static const LONG GAP_TOLERANCE_PX = 20;

static vector<DisplayRect> captureDisplayRects();
static void preserveArrangement(const vector<DisplayRect> &savedRects);
static bool placeNeighbor(ReflowRect &neighbor, const ReflowRect &anchor, bool allowCorner);
static LONG alignPerpendicular(bool useAlignment, LONG anchorOldStart, LONG anchorOldSize, LONG anchorNewStart,
                               LONG anchorNewSize, LONG neighborOldStart, LONG neighborOldSize, LONG neighborNewSize,
                               bool preferTrailingWhenFlush);
static bool nearlyEqual(LONG first, LONG second, LONG tolerance);
static void compactStackedGroups(vector<ReflowRect> &layout);

/**
 * Captures the current multi-monitor arrangement as an encoded String[] (one rectangle per active display) for the
 * caller to hold and hand back to preserveDisplayArrangement after a batch of display changes. This is the read half of
 * arrangement preservation; GetDisplay's JNI simply forwards to it.
 *
 * @param env
 *            - The JNI environment pointer
 *
 * @return A String[] snapshot of the current arrangement, one entry per active display
 */
jobjectArray captureDisplayArrangement(JNIEnv *env) {
    vector<DisplayRect> rects = captureDisplayRects();

    jclass strClass = env->FindClass("java/lang/String");

    if (strClass == nullptr) {
        return nullptr;
    }

    jobjectArray result = env->NewObjectArray((jsize) rects.size(), strClass, nullptr);

    if (result == nullptr) {
        return nullptr;
    }

    for (size_t i = 0; i < rects.size(); i++) {
        // Encode as id|x|y|width|height; the stable ID never contains the pipe delimiter
        string encoded = rects[i].stableId + "|" + to_string(rects[i].position.x) + "|" +
                         to_string(rects[i].position.y) + "|" + to_string(rects[i].width) + "|" +
                         to_string(rects[i].height);

        jstring entry = env->NewStringUTF(encoded.c_str());
        env->SetObjectArrayElement(result, (jsize) i, entry);
        env->DeleteLocalRef(entry);
    }

    return result;
}

/**
 * Reflows the multi-monitor arrangement from the given snapshot so every display keeps its relative position and
 * alignment after one or more displays were resized. This is the write half; SetDisplay's JNI simply forwards to it.
 *
 * @param env
 *            - The JNI environment pointer
 * @param snapshot
 *            - The arrangement snapshot returned by captureDisplayArrangement before the batch of changes
 */
void preserveDisplayArrangement(JNIEnv *env, jobjectArray snapshot) {
    if (snapshot == nullptr) {
        return;
    }

    jsize count = env->GetArrayLength(snapshot);
    vector<DisplayRect> savedRects;

    for (jsize i = 0; i < count; i++) {
        jstring entry = (jstring) env->GetObjectArrayElement(snapshot, i);

        if (entry == nullptr) {
            continue;
        }

        const char *chars = env->GetStringUTFChars(entry, nullptr);

        if (chars != nullptr) {
            string encoded = chars;
            env->ReleaseStringUTFChars(entry, chars);

            // Locate the four delimiters of id|x|y|width|height from the right so the ID may hold anything
            size_t d4 = encoded.rfind('|');
            size_t d3 = (d4 == string::npos) ? string::npos : encoded.rfind('|', d4 - 1);
            size_t d2 = (d3 == string::npos) ? string::npos : encoded.rfind('|', d3 - 1);
            size_t d1 = (d2 == string::npos) ? string::npos : encoded.rfind('|', d2 - 1);

            if (d1 != string::npos) {
                DisplayRect rect = {};
                rect.stableId = encoded.substr(0, d1);
                rect.position.x = (LONG) strtol(encoded.substr(d1 + 1, d2 - d1 - 1).c_str(), nullptr, 10);
                rect.position.y = (LONG) strtol(encoded.substr(d2 + 1, d3 - d2 - 1).c_str(), nullptr, 10);
                rect.width = (UINT32) strtoul(encoded.substr(d3 + 1, d4 - d3 - 1).c_str(), nullptr, 10);
                rect.height = (UINT32) strtoul(encoded.substr(d4 + 1).c_str(), nullptr, 10);
                savedRects.push_back(rect);
            }
        }

        env->DeleteLocalRef(entry);
    }

    preserveArrangement(savedRects);
}

/**
 * Captures the desktop rectangle (position and source size) of every active display, keyed by stable ID. Call before a
 * resolution or orientation change so preserveArrangement can rebuild the arrangement around it.
 *
 * @return The stable ID, source-mode position, and source-mode size of each active display
 */
static vector<DisplayRect> captureDisplayRects() {
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
 * Preserves the multi-monitor arrangement after a resolution or orientation change resized one or more displays. Every
 * display's applied size and live source-mode index are read from the live config in a single pass, the primary is held
 * at the desktop origin, and the rest of the layout is rebuilt from the captured rectangles so each display keeps its
 * relative position and alignment.
 *
 * @param savedRects
 *            - The desktop rectangle captured for each display before the change
 */
static void preserveArrangement(const vector<DisplayRect> &savedRects) {
    // Fewer than two displays have no arrangement to preserve
    if (savedRects.size() < 2) {
        return;
    }

    vector<DISPLAYCONFIG_PATH_INFO> paths;
    vector<DISPLAYCONFIG_MODE_INFO> modes;

    if (!queryActiveCcdConfig(paths, modes)) {
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
        layout[i].liveModeIdx = -1;
        layout[i].changed = false;
        layout[i].placed = false;
    }

    // One pass over active paths: resolve each stable ID once, recording the matching display's applied size and mode
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
        const DISPLAYCONFIG_SOURCE_MODE &source = modes[sourceModeIdx].sourceMode;

        for (ReflowRect &rect : layout) {
            if (rect.stableId != stableId) {
                continue;
            }

            rect.newWidth = (LONG) source.width;
            rect.newHeight = (LONG) source.height;
            rect.changed = (rect.newWidth != rect.oldWidth || rect.newHeight != rect.oldHeight);
            rect.liveModeIdx = (int) sourceModeIdx;
            break;
        }
    }

    // Nothing resized, so the arrangement still matches the desired layout
    bool anyChanged = false;

    for (const ReflowRect &rect : layout) {
        if (rect.changed) {
            anyChanged = true;
            break;
        }
    }

    if (!anyChanged) {
        return;
    }

    // Anchor the primary (old desktop origin) as the fixed reference, falling back to the first display
    int anchorIndex = 0;

    for (size_t i = 0; i < count; i++) {
        if (layout[i].oldLeft == 0 && layout[i].oldTop == 0) {
            anchorIndex = (int) i;
            break;
        }
    }

    layout[anchorIndex].placed = true;

    // Place edge-adjacent neighbors first so a shared edge wins, then corner-adjacent, until the layout stops growing
    for (int phase = 0; phase < 2; phase++) {
        bool allowCorner = (phase == 1);
        bool progress = true;

        while (progress) {
            progress = false;

            for (size_t p = 0; p < count; p++) {
                if (!layout[p].placed) {
                    continue;
                }

                for (size_t q = 0; q < count; q++) {
                    if (layout[q].placed) {
                        continue;
                    }

                    if (placeNeighbor(layout[q], layout[p], allowCorner)) {
                        layout[q].placed = true;
                        progress = true;
                    }
                }
            }
        }
    }

    // Re-flush displays stacked on the same side of a grown display so no gap opens between them
    compactStackedGroups(layout);

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

    // Write the reflowed positions straight into the live modes via each display's recorded source-mode index
    bool changed = false;

    for (const ReflowRect &rect : layout) {
        if (rect.liveModeIdx < 0) {
            continue;
        }

        POINTL target = {rect.newLeft - originLeft, rect.newTop - originTop};
        POINTL &current = modes[rect.liveModeIdx].sourceMode.position;

        if (current.x != target.x || current.y != target.y) {
            current = target;
            changed = true;
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

/**
 * Positions an unplaced neighbor flush against an already-placed anchor, preserving their edge adjacency (or, when
 * allowCorner is set, corner adjacency) so the arrangement survives a resize of either display.
 *
 * @param neighbor
 *            - The unplaced neighbor to position; its new position is written on a match
 * @param anchor
 *            - The already-placed anchor to position against
 * @param allowCorner
 *            - Whether to also match a corner (diagonal) adjacency
 *
 * @return Whether the neighbor was adjacent to the anchor and has been positioned
 */
static bool placeNeighbor(ReflowRect &neighbor, const ReflowRect &anchor, bool allowCorner) {
    // Displays share an edge only when their spans overlap on the axis perpendicular to that edge
    bool verticalOverlap =
        neighbor.oldTop < anchor.oldTop + anchor.oldHeight && anchor.oldTop < neighbor.oldTop + neighbor.oldHeight;
    bool horizontalOverlap =
        neighbor.oldLeft < anchor.oldLeft + anchor.oldWidth && anchor.oldLeft < neighbor.oldLeft + neighbor.oldWidth;

    // Reproduce the alignment when either display resized; otherwise keep the exact offset
    bool useAlignment = anchor.changed || neighbor.changed;

    if (verticalOverlap && neighbor.oldLeft == anchor.oldLeft + anchor.oldWidth) {
        // Neighbor sits to the right; keep it flush and preserve its vertical alignment
        neighbor.newLeft = anchor.newLeft + anchor.newWidth;
        neighbor.newTop =
            alignPerpendicular(useAlignment, anchor.oldTop, anchor.oldHeight, anchor.newTop, anchor.newHeight,
                               neighbor.oldTop, neighbor.oldHeight, neighbor.newHeight, true);
        return true;
    }

    if (verticalOverlap && neighbor.oldLeft + neighbor.oldWidth == anchor.oldLeft) {
        // Neighbor sits to the left
        neighbor.newLeft = anchor.newLeft - neighbor.newWidth;
        neighbor.newTop =
            alignPerpendicular(useAlignment, anchor.oldTop, anchor.oldHeight, anchor.newTop, anchor.newHeight,
                               neighbor.oldTop, neighbor.oldHeight, neighbor.newHeight, true);
        return true;
    }

    if (horizontalOverlap && neighbor.oldTop == anchor.oldTop + anchor.oldHeight) {
        // Neighbor sits below; keep it flush and preserve its horizontal alignment
        neighbor.newTop = anchor.newTop + anchor.newHeight;
        neighbor.newLeft =
            alignPerpendicular(useAlignment, anchor.oldLeft, anchor.oldWidth, anchor.newLeft, anchor.newWidth,
                               neighbor.oldLeft, neighbor.oldWidth, neighbor.newWidth, false);
        return true;
    }

    if (horizontalOverlap && neighbor.oldTop + neighbor.oldHeight == anchor.oldTop) {
        // Neighbor sits above
        neighbor.newTop = anchor.newTop - neighbor.newHeight;
        neighbor.newLeft =
            alignPerpendicular(useAlignment, anchor.oldLeft, anchor.oldWidth, anchor.newLeft, anchor.newWidth,
                               neighbor.oldLeft, neighbor.oldWidth, neighbor.newWidth, false);
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
 * Computes a neighbor's aligned coordinate on the axis perpendicular to a shared edge, preserving the alignment it
 * had with the anchor through the resize and clamping it to keep at least a one-pixel shared edge.
 *
 * @param useAlignment
 *            - Whether either display resized, so the original alignment is reproduced rather than the exact offset
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
 * @param neighborOldSize
 *            - The neighbor's old size on this axis, used to detect the original alignment
 * @param neighborNewSize
 *            - The neighbor's new size on this axis, used to place it
 * @param preferTrailingWhenFlush
 *            - Whether same-size, edge-flush displays keep their trailing edges aligned (bottom/right) not leading
 *
 * @return The neighbor's new start coordinate on this axis
 */
static LONG alignPerpendicular(bool useAlignment, LONG anchorOldStart, LONG anchorOldSize, LONG anchorNewStart,
                               LONG anchorNewSize, LONG neighborOldStart, LONG neighborOldSize, LONG neighborNewSize,
                               bool preferTrailingWhenFlush) {
    LONG result;

    if (!useAlignment) {
        // Neither display resized, so the neighbor keeps its exact offset
        result = anchorNewStart + (neighborOldStart - anchorOldStart);
    } else {
        // Detect the original alignment from the old geometry, then place with the new sizes
        LONG neighborOldEnd = neighborOldStart + neighborOldSize;
        LONG anchorOldEnd = anchorOldStart + anchorOldSize;
        LONG neighborOldCenter = 2 * neighborOldStart + neighborOldSize;
        LONG anchorOldCenter = 2 * anchorOldStart + anchorOldSize;

        bool leadingAligned = nearlyEqual(neighborOldStart, anchorOldStart, ALIGNMENT_TOLERANCE_PX);
        bool trailingAligned = nearlyEqual(neighborOldEnd, anchorOldEnd, ALIGNMENT_TOLERANCE_PX);

        if (preferTrailingWhenFlush && leadingAligned && trailingAligned) {
            // Both edges were flush (same size): keep trailing edges aligned so a grown display stays bottom aligned
            result = (anchorNewStart + anchorNewSize) - neighborNewSize;
        } else if (leadingAligned) {
            // Leading-edge-aligned (top or left): keep the leading edges flush regardless of the sizes
            result = anchorNewStart;
        } else if (trailingAligned) {
            // Trailing-edge-aligned (bottom or right): keep the trailing edges flush regardless of the sizes
            result = (anchorNewStart + anchorNewSize) - neighborNewSize;
        } else if (nearlyEqual(neighborOldCenter, anchorOldCenter, 2 * ALIGNMENT_TOLERANCE_PX)) {
            // Centered on the anchor (doubled tolerance matches the doubled centers): keep it centered in the new span
            result = anchorNewStart + (anchorNewSize - neighborNewSize) / 2;
        } else if (neighborOldCenter < anchorOldCenter) {
            // Not aligned but biased toward the anchor's low edge: preserve the distance from that edge
            result = anchorNewStart + (neighborOldStart - anchorOldStart);
        } else {
            // Not aligned but biased toward the anchor's high edge: preserve the distance from that edge
            result = (anchorNewStart + anchorNewSize) - neighborNewSize + (neighborOldEnd - anchorOldEnd);
        }
    }

    // Keep at least a one-pixel shared edge with the anchor's new span so the neighbor stays connected and reachable
    LONG minStart = anchorNewStart - neighborNewSize + 1;
    LONG maxStart = anchorNewStart + anchorNewSize - 1;

    if (result < minStart) {
        result = minStart;
    } else if (result > maxStart) {
        result = maxStart;
    }

    return result;
}

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
 * Re-flushes displays stacked on the same side of a resized display and keeps the stack's alignment with that display.
 * The per-member reflow aligns each side-neighbor to the resized display independently, which both splits an originally
 * flush stack and loses the group's overall alignment (e.g. a centered stack drifts) when the display resizes. This
 * pass makes each such stack rigid again, then repositions it as one unit so a centered/leading/trailing/near-diagonal
 * stack keeps that relationship. Pure integer work over the reflow set.
 *
 * @param layout
 *            - The reflow working set, updated in place for any stacked side-neighbors that need re-flushing
 */
static void compactStackedGroups(vector<ReflowRect> &layout) {
    size_t count = layout.size();
    vector<int> group;

    for (size_t a = 0; a < count; a++) {
        const ReflowRect &anchor = layout[a];

        // Only a display that resized can split its side-neighbors
        if (!anchor.changed) {
            continue;
        }

        // Sides 0 = right of anchor, 1 = left, 2 = below, 3 = above; sides 0/1 stack along Y, sides 2/3 along X
        for (int side = 0; side < 4; side++) {
            bool vertical = (side <= 1);
            group.clear();

            for (size_t m = 0; m < count; m++) {
                if (m == a) {
                    continue;
                }

                const ReflowRect &member = layout[m];
                bool adjacent = false;
                bool flushToNewEdge = false;

                if (side == 0) {
                    bool overlap = member.oldTop < anchor.oldTop + anchor.oldHeight &&
                                   anchor.oldTop < member.oldTop + member.oldHeight;
                    adjacent = overlap && member.oldLeft == anchor.oldLeft + anchor.oldWidth;
                    flushToNewEdge = member.newLeft == anchor.newLeft + anchor.newWidth;
                } else if (side == 1) {
                    bool overlap = member.oldTop < anchor.oldTop + anchor.oldHeight &&
                                   anchor.oldTop < member.oldTop + member.oldHeight;
                    adjacent = overlap && member.oldLeft + member.oldWidth == anchor.oldLeft;
                    flushToNewEdge = member.newLeft + member.newWidth == anchor.newLeft;
                } else if (side == 2) {
                    bool overlap = member.oldLeft < anchor.oldLeft + anchor.oldWidth &&
                                   anchor.oldLeft < member.oldLeft + member.oldWidth;
                    adjacent = overlap && member.oldTop == anchor.oldTop + anchor.oldHeight;
                    flushToNewEdge = member.newTop == anchor.newTop + anchor.newHeight;
                } else {
                    bool overlap = member.oldLeft < anchor.oldLeft + anchor.oldWidth &&
                                   anchor.oldLeft < member.oldLeft + member.oldWidth;
                    adjacent = overlap && member.oldTop + member.oldHeight == anchor.oldTop;
                    flushToNewEdge = member.newTop + member.newHeight == anchor.newTop;
                }

                // Same side of the SAME anchor and flush to its new edge is what marks a genuine stacked group
                if (adjacent && flushToNewEdge) {
                    group.push_back((int) m);
                }
            }

            // A lone side-neighbor (single-row, single-column, or grid) has nothing to stay flush with
            if (group.size() < 2) {
                continue;
            }

            // Insertion-sort the small group into geometric order along the stacking axis
            for (size_t i = 1; i < group.size(); i++) {
                int key = group[i];
                LONG keyStart = vertical ? layout[key].oldTop : layout[key].oldLeft;
                size_t j = i;

                while (j > 0 && (vertical ? layout[group[j - 1]].oldTop : layout[group[j - 1]].oldLeft) > keyStart) {
                    group[j] = group[j - 1];
                    j--;
                }

                group[j] = key;
            }

            // Lay each near-flush successor rigidly against its predecessor so the stack stays gap-free
            bool singleFlushRun = true;

            for (size_t i = 1; i < group.size(); i++) {
                ReflowRect &prev = layout[group[i - 1]];
                ReflowRect &cur = layout[group[i]];

                /*
                 * Treat members within the alignment tolerance of touching as one rigid stack and heal the small gap.
                 * Windows snapping rarely leaves a stack pixel-perfect flush (a few px of residue is common), so an
                 * exact test would miss real stacks and skip the realign; a gap wider than the tolerance is a genuine
                 * separation and is left alone
                 */
                LONG prevEnd = vertical ? prev.oldTop + prev.oldHeight : prev.oldLeft + prev.oldWidth;
                LONG curStart = vertical ? cur.oldTop : cur.oldLeft;
                bool flush = nearlyEqual(curStart, prevEnd, GAP_TOLERANCE_PX);

                if (!flush) {
                    singleFlushRun = false;
                } else if (vertical) {
                    cur.newTop = prev.newTop + prev.newHeight;
                } else {
                    cur.newLeft = prev.newLeft + prev.newWidth;
                }
            }

            if (!singleFlushRun) {
                continue;
            }

            /*
             * Reposition the flush stack as one unit against the anchor so a stack that was centered (or leading,
             * trailing, or near-diagonal) keeps that relationship after the anchor resizes. The per-member reflow only
             * sees each member's own alignment and loses the group's, so drive the whole run through alignPerpendicular
             */
            ReflowRect &first = layout[group.front()];
            ReflowRect &last = layout[group.back()];

            LONG groupOldStart = vertical ? first.oldTop : first.oldLeft;
            LONG groupOldEnd = vertical ? last.oldTop + last.oldHeight : last.oldLeft + last.oldWidth;
            LONG groupOldSize = groupOldEnd - groupOldStart;
            LONG groupNewSize = vertical ? (last.newTop + last.newHeight) - first.newTop
                                         : (last.newLeft + last.newWidth) - first.newLeft;

            LONG anchorOldStart = vertical ? anchor.oldTop : anchor.oldLeft;
            LONG anchorOldSize = vertical ? anchor.oldHeight : anchor.oldWidth;
            LONG anchorNewStart = vertical ? anchor.newTop : anchor.newLeft;
            LONG anchorNewSize = vertical ? anchor.newHeight : anchor.newWidth;

            // Right/left stacks prefer trailing edges when both are flush; above/below do not (as in placeNeighbor)
            bool preferTrailingWhenFlush = vertical;

            LONG groupNewStart = alignPerpendicular(true, anchorOldStart, anchorOldSize, anchorNewStart, anchorNewSize,
                                                    groupOldStart, groupOldSize, groupNewSize, preferTrailingWhenFlush);

            LONG shift = groupNewStart - (vertical ? first.newTop : first.newLeft);

            for (int index : group) {
                if (vertical) {
                    layout[index].newTop += shift;
                } else {
                    layout[index].newLeft += shift;
                }
            }
        }
    }
}
