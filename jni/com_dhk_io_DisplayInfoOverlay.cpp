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
#include "com_dhk_io_DisplayInfoOverlay.h"
#include "DisplayInfoOverlay.h"

#include <atomic>
#include <chrono>
#include <cmath>
#include <condition_variable>
#include <jni.h>
#include <memory>
#include <mutex>
#include <shellscalingapi.h>
#include <string>
#include <thread>
#include <vector>
#include <windows.h>

using namespace std;

static vector<wstring> readWideStrings(JNIEnv *env, jobjectArray javaStrings);
static wstring readWideString(JNIEnv *env, jstring javaString);
static COLORREF colorFromArgb(jint argb);
static bool startOverlayThread();
static void runOverlayThread();
static LRESULT CALLBACK handleControlEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void showPanels();
static void layoutPanel(const DisplayInfoOverlayDefinition &definition, size_t panelIndex, DisplayInfoPanel &panel);
static DisplayInfoPanelFonts createPanelFonts(const DisplayInfoOverlayDefinition &definition, UINT dpi);
static SIZE measureTexts(HDC measureDC, const vector<wstring> &texts, size_t firstIndex, size_t count);
static int scaleMetric(float metric, UINT dpi);
static void updatePanelWindow(const DisplayInfoOverlayDefinition &definition, size_t panelIndex,
                              const DisplayInfoPanel &panel, POINT panelOrigin);
static void paintPanel(HDC paintDC, const DisplayInfoOverlayDefinition &definition, size_t panelIndex,
                       const DisplayInfoPanel &panel);
static void shapePanel(BYTE *pixels, const DisplayInfoPanel &panel, COLORREF borderColor);
static float outlineCoverage(float distance);
static void hidePanels();
static LRESULT CALLBACK handlePanelEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void stopOverlayThread();

/**
 * Thread that runs the Windows message loop owning every panel.
 */
static thread overlayThread;

/**
 * Hidden message-only window that receives the show, hide, and timer messages. Read from the calling thread while the
 * thread owning it may be tearing it down, so it is atomic and cleared before the window is destroyed.
 */
static atomic<HWND> controlWindow{NULL};

/**
 * Guards the finished state of the thread owning the panels.
 */
static mutex startupMutex;

/**
 * Signals a waiting start or stop that the thread owning the panels published its window or finished.
 */
static condition_variable startupSignal;

/**
 * Whether the thread owning the panels has finished, so a start can join it and a stop can tell it ended.
 */
static bool overlayThreadFinished = false;

/**
 * Panels most recently handed over by the caller, waiting for the thread owning the panels to show them.
 */
static shared_ptr<const DisplayInfoOverlayDefinition> pendingDefinition;

/**
 * Guards the panels waiting to be shown.
 */
static mutex definitionMutex;

/**
 * Panels being shown, indexed the same as the headers they show and read only by the thread owning them.
 */
static vector<DisplayInfoPanel> panels;

/**
 * Registered window class name for the hidden message-only window.
 */
static const wchar_t CONTROL_CLASS_NAME[] = L"DHK_DisplayInfoOverlay_ControlWindow";

/**
 * Registered window class name for the panel windows.
 */
static const wchar_t PANEL_CLASS_NAME[] = L"DHK_DisplayInfoOverlay_PanelWindow";

/**
 * Extended styles of a panel window, which make it translucent, let clicks pass through it, keep it from taking the
 * focus, keep it off the task bar and the window switcher, and keep it above ordinary windows.
 */
static constexpr DWORD PANEL_EXTENDED_STYLE =
    WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW | WS_EX_TOPMOST;

/**
 * Private message asking the thread owning the panels to show the panels waiting to be shown.
 */
static constexpr UINT WM_OVERLAY_SHOW = WM_APP + 1;

/**
 * Private message asking the thread owning the panels to hide them.
 */
static constexpr UINT WM_OVERLAY_HIDE = WM_APP + 2;

/**
 * Identifier of the timer that hides the panels once their time is up.
 */
static constexpr UINT_PTR HIDE_TIMER_ID = 1;

/**
 * How long (ms) a start or stop waits on the thread owning the panels before answering without it.
 */
static constexpr int THREAD_TIMEOUT_MS = 2000;

/**
 * Index of the panel background color in the colors the caller supplies, taken from the generated header so the two
 * sides of the boundary can never disagree.
 */
static constexpr int OVERLAY_COLOR_BACKGROUND = com_dhk_io_DisplayInfoOverlay_OVERLAY_COLOR_BACKGROUND;

/**
 * Index of the text color in the colors the caller supplies.
 */
static constexpr int OVERLAY_COLOR_FOREGROUND = com_dhk_io_DisplayInfoOverlay_OVERLAY_COLOR_FOREGROUND;

/**
 * Index of the panel border color in the colors the caller supplies.
 */
static constexpr int OVERLAY_COLOR_BORDER = com_dhk_io_DisplayInfoOverlay_OVERLAY_COLOR_BORDER;

/**
 * Number of colors the caller supplies.
 */
static constexpr int OVERLAY_COLOR_COUNT = com_dhk_io_DisplayInfoOverlay_OVERLAY_COLOR_COUNT;

/**
 * Index of the setting lines' font size in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_FONT_SIZE = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_FONT_SIZE;

/**
 * Index of the header line's font size in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_HEADER_FONT_SIZE = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_HEADER_FONT_SIZE;

/**
 * Index of the padding in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_PADDING = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_PADDING;

/**
 * Index of the gap on each side of the separator beneath the header line in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_HEADER_GAP = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_HEADER_GAP;

/**
 * Index of the gap between setting lines in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_LINE_GAP = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_LINE_GAP;

/**
 * Index of the work area margin in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_MARGIN = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_MARGIN;

/**
 * Index of the gap between the label column and the value column in the metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_COLUMN_GAP = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_COLUMN_GAP;

/**
 * Number of metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_COUNT = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_COUNT;

/**
 * Number of ints per panel in the anchor points the caller supplies.
 */
static constexpr int ANCHOR_FIELD_COUNT = com_dhk_io_DisplayInfoOverlay_ANCHOR_FIELD_COUNT;

/**
 * Number of positions the caller can place the panels at on each display.
 */
static constexpr int OVERLAY_POSITION_COUNT = com_dhk_io_DisplayInfoOverlay_OVERLAY_POSITION_COUNT;

/**
 * Halves of the free space left of the panels at each position, so 0 aligns them left, 1 centers them, and 2 aligns
 * them right. The positions are in the order top left, top right, center, bottom left, and bottom right.
 */
static constexpr int HORIZONTAL_ALIGNMENTS[OVERLAY_POSITION_COUNT] = {0, 2, 1, 0, 2};

/**
 * Halves of the free space above the panels at each position, so 0 aligns them to the top, 1 centers them, and 2
 * aligns them to the bottom. The positions are in the same order as the horizontal alignments.
 */
static constexpr int VERTICAL_ALIGNMENTS[OVERLAY_POSITION_COUNT] = {0, 0, 1, 2, 2};

/**
 * Highest opacity a panel can have, at which it is fully opaque.
 */
static constexpr jint MAX_OVERLAY_OPACITY = com_dhk_io_DisplayInfoOverlay_MAX_OVERLAY_OPACITY;

/**
 * Unscaled thickness of a panel's border and of the line separating its header line from its setting lines.
 */
static constexpr int LINE_THICKNESS = 1;

/**
 * Unscaled radius of a panel's rounded corners, which matches the corners Windows 11 gives its windows.
 */
static constexpr int CORNER_RADIUS = 8;

/**
 * Shows one panel per header at the given position on its display, replacing any panels already showing and
 * restarting the time they stay visible. Everything is copied here, so the panels never call back for any of it.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param headers
 *            - The bold first line of each panel
 * @param labels
 *            - The label of each setting line, shared by every panel, which needs at least one label
 * @param values
 *            - The value of each setting line, panel by panel, with one value per label for each header
 * @param anchorPoints
 *            - The desktop x and y coordinates of a point on each panel's display, in physical pixels
 * @param position
 *            - Where the panels sit on each display, below OVERLAY_POSITION_COUNT
 * @param overlayColors
 *            - The panel colors in packed ARGB order, indexed by the OVERLAY_COLOR constants
 * @param overlayMetrics
 *            - The panel metrics in unscaled, possibly fractional pixels, indexed by the OVERLAY_METRIC constants
 * @param fontFamily
 *            - The family of the installed font the panels are drawn in
 * @param opacity
 *            - The opacity of the panels, from 0 (invisible) to 255 (opaque)
 * @param durationMs
 *            - How long the panels stay visible, in milliseconds
 *
 * @return Whether the panels were accepted
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_dhk_io_DisplayInfoOverlay_nativeShow(
    JNIEnv *env, jobject obj, jobjectArray headers, jobjectArray labels, jobjectArray values, jintArray anchorPoints,
    jint position, jintArray overlayColors, jfloatArray overlayMetrics, jstring fontFamily, jint opacity,
    jint durationMs) {
    (void) obj;

    if (!headers || !labels || !values || !anchorPoints || !overlayColors || !overlayMetrics || durationMs <= 0 ||
        env->GetArrayLength(labels) == 0 || env->GetArrayLength(overlayColors) < OVERLAY_COLOR_COUNT ||
        env->GetArrayLength(overlayMetrics) < OVERLAY_METRIC_COUNT || position < 0 ||
        position >= OVERLAY_POSITION_COUNT) {
        return JNI_FALSE;
    }

    jsize panelCount = env->GetArrayLength(headers);

    // Every panel needs a value per label and an anchor point, so short arrays are rejected rather than misread
    if (env->GetArrayLength(values) < panelCount * env->GetArrayLength(labels) ||
        env->GetArrayLength(anchorPoints) < panelCount * ANCHOR_FIELD_COUNT) {
        return JNI_FALSE;
    }

    jint colors[OVERLAY_COLOR_COUNT];
    jfloat metrics[OVERLAY_METRIC_COUNT];
    vector<jint> anchors(panelCount * ANCHOR_FIELD_COUNT);

    env->GetIntArrayRegion(overlayColors, 0, OVERLAY_COLOR_COUNT, colors);
    env->GetFloatArrayRegion(overlayMetrics, 0, OVERLAY_METRIC_COUNT, metrics);

    if (!anchors.empty()) {
        env->GetIntArrayRegion(anchorPoints, 0, (jsize) anchors.size(), anchors.data());
    }

    shared_ptr<DisplayInfoOverlayDefinition> definition = make_shared<DisplayInfoOverlayDefinition>();
    definition->headers = readWideStrings(env, headers);
    definition->labels = readWideStrings(env, labels);
    definition->values = readWideStrings(env, values);

    for (jsize panelIndex = 0; panelIndex < panelCount; panelIndex++) {
        definition->anchorPoints.push_back(
            {anchors[panelIndex * ANCHOR_FIELD_COUNT], anchors[panelIndex * ANCHOR_FIELD_COUNT + 1]});
    }

    definition->position = position;

    definition->backgroundColor = colorFromArgb(colors[OVERLAY_COLOR_BACKGROUND]);
    definition->foregroundColor = colorFromArgb(colors[OVERLAY_COLOR_FOREGROUND]);
    definition->borderColor = colorFromArgb(colors[OVERLAY_COLOR_BORDER]);
    definition->fontFamily = readWideString(env, fontFamily);
    definition->fontSize = metrics[OVERLAY_METRIC_FONT_SIZE];
    definition->headerFontSize = metrics[OVERLAY_METRIC_HEADER_FONT_SIZE];
    definition->padding = metrics[OVERLAY_METRIC_PADDING];
    definition->headerGap = metrics[OVERLAY_METRIC_HEADER_GAP];
    definition->lineGap = metrics[OVERLAY_METRIC_LINE_GAP];
    definition->columnGap = metrics[OVERLAY_METRIC_COLUMN_GAP];
    definition->margin = metrics[OVERLAY_METRIC_MARGIN];
    definition->opacity = (BYTE) (opacity < 0 ? 0 : (opacity > MAX_OVERLAY_OPACITY ? MAX_OVERLAY_OPACITY : opacity));
    definition->durationMs = (UINT) durationMs;

    if (!startOverlayThread()) {
        return JNI_FALSE;
    }

    {
        lock_guard<mutex> definitionLock(definitionMutex);
        pendingDefinition = definition;
    }

    HWND targetWindow = controlWindow.load();

    // With nothing to show, any panels already showing are hidden rather than left describing a stale configuration
    if (!targetWindow || !PostMessage(targetWindow, panelCount > 0 ? WM_OVERLAY_SHOW : WM_OVERLAY_HIDE, 0, 0)) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/**
 * Hides every panel that is showing, without waiting for the panels to finish closing.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_DisplayInfoOverlay_nativeHide(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    HWND targetWindow = controlWindow.load();

    if (targetWindow) {
        PostMessage(targetWindow, WM_OVERLAY_HIDE, 0, 0);
    }
}

/**
 * Called when the native library is unloaded. Stops the thread owning the panels and releases the panels still waiting
 * to be shown.
 *
 * @param vm
 *            - The JavaVM pointer
 * @param reserved
 *            - Reserved for future use
 */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;

    stopOverlayThread();

    lock_guard<mutex> definitionLock(definitionMutex);
    pendingDefinition.reset();
}

/**
 * Copies an array of strings from the virtual machine into wide strings, keeping a null element as an empty string so
 * every copied string stays at the same index as its source.
 *
 * @param env
 *            - The JNI environment pointer
 * @param javaStrings
 *            - The strings to copy
 *
 * @return The copied strings
 */
static vector<wstring> readWideStrings(JNIEnv *env, jobjectArray javaStrings) {
    jsize stringCount = env->GetArrayLength(javaStrings);
    vector<wstring> wideStrings;
    wideStrings.reserve((size_t) stringCount);

    for (jsize stringIndex = 0; stringIndex < stringCount; stringIndex++) {
        jstring javaString = (jstring) env->GetObjectArrayElement(javaStrings, stringIndex);

        wideStrings.push_back(readWideString(env, javaString));

        if (javaString) {
            env->DeleteLocalRef(javaString);
        }
    }

    return wideStrings;
}

/**
 * Copies a string from the virtual machine into a wide string.
 *
 * @param env
 *            - The JNI environment pointer
 * @param javaString
 *            - The string to copy, which may be null
 *
 * @return The copied string, or an empty string when there is nothing to copy
 */
static wstring readWideString(JNIEnv *env, jstring javaString) {
    const jchar *stringChars = javaString ? env->GetStringChars(javaString, nullptr) : nullptr;

    if (!stringChars) {
        return wstring();
    }

    wstring wideString((const wchar_t *) stringChars, env->GetStringLength(javaString));

    env->ReleaseStringChars(javaString, stringChars);

    return wideString;
}

/**
 * Converts a color in packed ARGB order to a GDI color, dropping the alpha that GDI has no use for.
 *
 * @param argb
 *            - The color in packed ARGB order
 *
 * @return The GDI color
 */
static COLORREF colorFromArgb(jint argb) {
    return RGB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
}

/**
 * Starts the thread owning the panels unless it is already running, waiting a bounded time for it to publish its
 * window. A thread that finished on its own is joined first so a new one can take its place.
 *
 * @return Whether the thread is running and its window can receive messages
 */
static bool startOverlayThread() {
    if (controlWindow.load()) {
        return true;
    }

    if (overlayThread.joinable()) {
        bool finished = false;

        {
            lock_guard<mutex> startupLock(startupMutex);
            finished = overlayThreadFinished;
        }

        // A thread that is still starting from an earlier request is left to finish rather than waited on again
        if (!finished) {
            return false;
        }

        overlayThread.join();
    }

    {
        lock_guard<mutex> startupLock(startupMutex);
        overlayThreadFinished = false;
    }

    // Creating a thread throws when the system is out of resources, and an exception escaping here ends the process
    try {
        overlayThread = thread(runOverlayThread);
    } catch (const system_error &) {
        return false;
    }

    unique_lock<mutex> startupLock(startupMutex);

    startupSignal.wait_for(startupLock, chrono::milliseconds(THREAD_TIMEOUT_MS),
                           [] { return controlWindow.load() || overlayThreadFinished; });

    return controlWindow.load() != NULL;
}

/**
 * Runs the panels on a dedicated thread, which owns the hidden message-only window and every panel window outright and
 * pumps their messages until it is stopped.
 */
static void runOverlayThread() {
    // Panels are placed and sized in physical pixels on every display, whatever DPI awareness the process declared
    SetThreadDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);

    HINSTANCE instance = GetModuleHandleW(NULL);
    WNDCLASSW windowClass = {};
    windowClass.lpfnWndProc = handleControlEvents;
    windowClass.hInstance = instance;
    windowClass.lpszClassName = CONTROL_CLASS_NAME;

    // Fails harmlessly when an earlier thread could not unregister the class, since the existing class is identical
    RegisterClassW(&windowClass);

    windowClass.lpfnWndProc = handlePanelEvents;
    windowClass.lpszClassName = PANEL_CLASS_NAME;
    RegisterClassW(&windowClass);

    HWND createdWindow = CreateWindowExW(0, CONTROL_CLASS_NAME, L"", 0, 0, 0, 0, 0, HWND_MESSAGE, NULL, instance, NULL);

    {
        lock_guard<mutex> startupLock(startupMutex);

        controlWindow.store(createdWindow);
        overlayThreadFinished = !createdWindow;
    }

    startupSignal.notify_all();

    if (!createdWindow) {
        return;
    }

    MSG message;

    // Blocking pump, since every action is message-driven and there is nothing to poll
    while (GetMessageW(&message, NULL, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }

    hidePanels();

    // Unpublished before the window is destroyed, so no caller posts to a dead handle
    controlWindow.store(NULL);
    DestroyWindow(createdWindow);
    UnregisterClassW(PANEL_CLASS_NAME, instance);
    UnregisterClassW(CONTROL_CLASS_NAME, instance);

    {
        lock_guard<mutex> startupLock(startupMutex);
        overlayThreadFinished = true;
    }

    startupSignal.notify_all();
}

/**
 * Handles the show, hide, and timer messages for the hidden message-only window.
 *
 * @param windowHandle
 *            - The hidden message-only window
 * @param message
 *            - The message identifier
 * @param eventType
 *            - The message's WPARAM, which carries the timer identifier for a timer message
 * @param eventData
 *            - The message's LPARAM
 *
 * @return The result of the message processing
 */
static LRESULT CALLBACK handleControlEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData) {
    switch (message) {
    case WM_OVERLAY_SHOW:
        showPanels();
        return 0;
    case WM_OVERLAY_HIDE:
        hidePanels();
        return 0;
    case WM_TIMER:
        if (eventType != HIDE_TIMER_ID) {
            break;
        }

        hidePanels();
        return 0;
    default:
        break;
    }

    return DefWindowProcW(windowHandle, message, eventType, eventData);
}

/**
 * Shows the panels waiting to be shown at their position in each one's display work area, scaled for that display,
 * and restarts the timer that hides them. Panels already showing are reused, so a repeat only updates them.
 */
static void showPanels() {
    shared_ptr<const DisplayInfoOverlayDefinition> definition;

    {
        lock_guard<mutex> definitionLock(definitionMutex);
        definition = pendingDefinition;
    }

    if (!definition) {
        return;
    }

    size_t panelCount = definition->headers.size();

    // Only surplus panels are destroyed, so every remaining panel window is reused rather than created again
    while (panels.size() > panelCount) {
        DestroyWindow(panels.back().window);
        panels.pop_back();
    }

    panels.resize(panelCount, DisplayInfoPanel{});

    // Every panel is laid out before any is placed, since the panels sharing a display are aligned as one block
    for (size_t panelIndex = 0; panelIndex < panelCount; panelIndex++) {
        DisplayInfoPanel &panel = panels[panelIndex];
        MONITORINFO monitorInfo = {};
        monitorInfo.cbSize = sizeof(monitorInfo);
        UINT dpiY = 0;

        panel.monitor = MonitorFromPoint(definition->anchorPoints[panelIndex], MONITOR_DEFAULTTONEAREST);

        // A display that vanished since the settings were read has nowhere to show its panel
        if (!GetMonitorInfoW(panel.monitor, &monitorInfo)) {
            if (panel.window) {
                DestroyWindow(panel.window);
            }

            panel = DisplayInfoPanel{};
            continue;
        }

        if (GetDpiForMonitor(panel.monitor, MDT_EFFECTIVE_DPI, &panel.dpi, &dpiY) != S_OK) {
            panel.dpi = USER_DEFAULT_SCREEN_DPI;
        }

        panel.workArea = monitorInfo.rcWork;
        layoutPanel(*definition, panelIndex, panel);
    }

    int horizontalAlignment = HORIZONTAL_ALIGNMENTS[definition->position];
    int verticalAlignment = VERTICAL_ALIGNMENTS[definition->position];

    for (size_t panelIndex = 0; panelIndex < panelCount; panelIndex++) {
        DisplayInfoPanel &panel = panels[panelIndex];

        if (!panel.monitor) {
            continue;
        }

        const RECT &workArea = panel.workArea;
        int margin = scaleMetric(definition->margin, panel.dpi);

        // Panels sharing a display stack downward in panel order, with a margin after every panel but the last
        LONG stackHeight = -margin;
        LONG stackOffset = 0;

        for (size_t stackedIndex = 0; stackedIndex < panelCount; stackedIndex++) {
            if (panels[stackedIndex].monitor != panel.monitor) {
                continue;
            }

            LONG stackedHeight = panels[stackedIndex].size.cy + margin;
            stackHeight += stackedHeight;

            if (stackedIndex < panelIndex) {
                stackOffset += stackedHeight;
            }
        }

        LONG freeWidth = workArea.right - workArea.left - 2 * margin - panel.size.cx;
        LONG freeHeight = workArea.bottom - workArea.top - 2 * margin - stackHeight;

        // A stack too large for its display keeps to the work area's top-left edge, so its header stays visible
        freeWidth = freeWidth > 0 ? freeWidth : 0;
        freeHeight = freeHeight > 0 ? freeHeight : 0;

        LONG panelLeft = workArea.left + margin + freeWidth * horizontalAlignment / 2;
        LONG panelTop = workArea.top + margin + freeHeight * verticalAlignment / 2 + stackOffset;

        // A new panel window starts hidden and takes its position, size, and content from its first update
        if (!panel.window) {
            panel.window = CreateWindowExW(PANEL_EXTENDED_STYLE, PANEL_CLASS_NAME, L"", WS_POPUP, 0, 0, 0, 0, NULL,
                                           NULL, GetModuleHandleW(NULL), NULL);
        }

        if (!panel.window) {
            continue;
        }

        updatePanelWindow(*definition, panelIndex, panel, {panelLeft, panelTop});

        // Raised above other topmost windows again, since one may have been shown over the panel meanwhile
        SetWindowPos(panel.window, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_SHOWWINDOW);
    }

    // Setting the timer again replaces the running one, so a repeated show restarts the time the panels stay visible
    SetTimer(controlWindow.load(), HIDE_TIMER_ID, definition->durationMs, NULL);
}

/**
 * Lays out a panel in fonts scaled for its display, storing its size and every position it paints at so placing and
 * painting it never has to measure anything.
 *
 * @param definition
 *            - The definition of the panels being shown
 * @param panelIndex
 *            - The index of the panel to lay out
 * @param panel
 *            - The panel to lay out, whose DPI is already set and whose size and positions are filled in
 */
static void layoutPanel(const DisplayInfoOverlayDefinition &definition, size_t panelIndex, DisplayInfoPanel &panel) {
    DisplayInfoPanelFonts fonts = createPanelFonts(definition, panel.dpi);
    HDC measureDC = CreateCompatibleDC(NULL);
    size_t lineCount = definition.labels.size();

    HGDIOBJ previousFont = SelectObject(measureDC, fonts.headerFont);
    SIZE headerSize = measureTexts(measureDC, definition.headers, panelIndex, 1);

    SelectObject(measureDC, fonts.labelFont);
    SIZE labelColumnSize = measureTexts(measureDC, definition.labels, 0, lineCount);

    SelectObject(measureDC, fonts.valueFont);
    SIZE valueColumnSize = measureTexts(measureDC, definition.values, panelIndex * lineCount, lineCount);

    SelectObject(measureDC, previousFont);
    DeleteDC(measureDC);

    // The bold value font can stand taller than the label font, so each line fits whichever is taller
    LONG lineHeight = valueColumnSize.cy > labelColumnSize.cy ? valueColumnSize.cy : labelColumnSize.cy;
    int padding = scaleMetric(definition.padding, panel.dpi);
    int headerGap = scaleMetric(definition.headerGap, panel.dpi);
    LONG valueColumnOffset = labelColumnSize.cx + scaleMetric(definition.columnGap, panel.dpi);
    LONG contentWidth = valueColumnOffset + valueColumnSize.cx;

    if (headerSize.cx > contentWidth) {
        contentWidth = headerSize.cx;
    }

    LONG separatorTop = padding + headerSize.cy + headerGap;

    panel.headerOrigin = {padding, padding};
    panel.separatorRect = {padding, separatorTop, padding + contentWidth,
                           separatorTop + scaleMetric(LINE_THICKNESS, panel.dpi)};
    panel.firstLineTop = panel.separatorRect.bottom + headerGap;
    panel.lineSpacing = lineHeight + scaleMetric(definition.lineGap, panel.dpi);
    panel.valueColumnLeft = padding + valueColumnOffset;

    // The last setting line ends one line height below its top, without the gap that follows every other line
    LONG contentBottom = panel.firstLineTop + ((LONG) lineCount - 1) * panel.lineSpacing + lineHeight;

    panel.size = {contentWidth + 2 * padding, contentBottom + padding};
}

/**
 * Creates the header, label, and value fonts a panel draws its text in, sized for the given DPI.
 *
 * @param definition
 *            - The definition of the panels being shown
 * @param dpi
 *            - The DPI of the display the panel sits on
 *
 * @return The created fonts, which are deleted when they go out of scope
 */
static DisplayInfoPanelFonts createPanelFonts(const DisplayInfoOverlayDefinition &definition, UINT dpi) {
    LOGFONTW logFont = {};

    logFont.lfCharSet = DEFAULT_CHARSET;
    logFont.lfQuality = CLEARTYPE_QUALITY;
    wcsncpy_s(logFont.lfFaceName, definition.fontFamily.c_str(), _TRUNCATE);

    // A negative height requests the font size itself rather than the taller line height, matching the size supplied
    logFont.lfHeight = -scaleMetric(definition.fontSize, dpi);
    logFont.lfWeight = FW_NORMAL;
    HFONT labelFont = CreateFontIndirectW(&logFont);

    logFont.lfWeight = FW_BOLD;
    HFONT valueFont = CreateFontIndirectW(&logFont);

    logFont.lfHeight = -scaleMetric(definition.headerFontSize, dpi);
    HFONT headerFont = CreateFontIndirectW(&logFont);

    return {headerFont, labelFont, valueFont};
}

/**
 * Measures a run of texts in the font selected into the given device context, where each text's height is the font's
 * full line height whatever characters it holds.
 *
 * @param measureDC
 *            - The device context holding the font to measure in
 * @param texts
 *            - The texts holding the run to measure
 * @param firstIndex
 *            - The index of the first text in the run
 * @param count
 *            - The number of texts in the run
 *
 * @return The width of the widest text and the height of the tallest text in the run, in physical pixels
 */
static SIZE measureTexts(HDC measureDC, const vector<wstring> &texts, size_t firstIndex, size_t count) {
    SIZE runSize = {};
    SIZE textSize = {};

    for (size_t textIndex = firstIndex; textIndex < firstIndex + count; textIndex++) {
        const wstring &text = texts[textIndex];

        GetTextExtentPoint32W(measureDC, text.c_str(), (int) text.size(), &textSize);

        if (textSize.cx > runSize.cx) {
            runSize.cx = textSize.cx;
        }

        if (textSize.cy > runSize.cy) {
            runSize.cy = textSize.cy;
        }
    }

    return runSize;
}

/**
 * Scales a metric given in unscaled pixels to the given DPI, rounding only the scaled result so a fractional metric
 * keeps its precision.
 *
 * @param metric
 *            - The metric in unscaled, possibly fractional pixels
 * @param dpi
 *            - The DPI to scale the metric to
 *
 * @return The metric in whole physical pixels at the given DPI
 */
static int scaleMetric(float metric, UINT dpi) {
    return (int) lroundf(metric * dpi / USER_DEFAULT_SCREEN_DPI);
}

/**
 * Draws a panel into a bitmap and hands it to the panel's window together with the window's position and size, which
 * the system applies in one step, so a moved or resized panel never shows its old content for a frame.
 *
 * @param definition
 *            - The definition of the panels being shown
 * @param panelIndex
 *            - The index of the panel to draw
 * @param panel
 *            - The laid out panel to draw, whose window exists
 * @param panelOrigin
 *            - The top-left corner of the panel's window on the desktop, in physical pixels
 */
static void updatePanelWindow(const DisplayInfoOverlayDefinition &definition, size_t panelIndex,
                              const DisplayInfoPanel &panel, POINT panelOrigin) {
    BITMAPINFO bitmapInfo = {};
    bitmapInfo.bmiHeader.biSize = sizeof(bitmapInfo.bmiHeader);
    bitmapInfo.bmiHeader.biWidth = panel.size.cx;
    bitmapInfo.bmiHeader.biPlanes = 1;
    bitmapInfo.bmiHeader.biBitCount = 32;
    bitmapInfo.bmiHeader.biCompression = BI_RGB;

    // A negative height stores the rows top down, so the pixels run in the same order as the panel's rows
    bitmapInfo.bmiHeader.biHeight = -panel.size.cy;

    HDC paintDC = CreateCompatibleDC(NULL);
    void *pixels = nullptr;
    HBITMAP bitmap = CreateDIBSection(paintDC, &bitmapInfo, DIB_RGB_COLORS, &pixels, NULL, 0);

    if (!bitmap) {
        DeleteDC(paintDC);
        return;
    }

    HGDIOBJ previousBitmap = SelectObject(paintDC, bitmap);

    paintPanel(paintDC, definition, panelIndex, panel);

    // GDI may still be drawing into the bitmap, so it finishes before the pixels are reshaped directly
    GdiFlush();
    shapePanel((BYTE *) pixels, panel, definition.borderColor);

    // The panels' opacity applies on top of each pixel's own coverage of the rounded panel
    POINT sourceOrigin = {0, 0};
    SIZE panelSize = panel.size;
    BLENDFUNCTION blend = {AC_SRC_OVER, 0, definition.opacity, AC_SRC_ALPHA};

    UpdateLayeredWindow(panel.window, NULL, &panelOrigin, &panelSize, paintDC, &sourceOrigin, 0, &blend, ULW_ALPHA);

    SelectObject(paintDC, previousBitmap);
    DeleteObject(bitmap);
    DeleteDC(paintDC);
}

/**
 * Paints a panel's background, its bold header line with a separator beneath, and its setting lines as labels beside
 * bold values, at the positions laid out when the panel was shown. Its rounded corners and border are added afterward.
 *
 * @param paintDC
 *            - The device context holding the panel's bitmap
 * @param definition
 *            - The definition of the panels being shown
 * @param panelIndex
 *            - The index of the panel to paint
 * @param panel
 *            - The laid out panel to paint, whose bitmap is the panel's size
 */
static void paintPanel(HDC paintDC, const DisplayInfoOverlayDefinition &definition, size_t panelIndex,
                       const DisplayInfoPanel &panel) {
    DisplayInfoPanelFonts fonts = createPanelFonts(definition, panel.dpi);
    HBRUSH colorBrush = (HBRUSH) GetStockObject(DC_BRUSH);
    const wstring &header = definition.headers[panelIndex];
    size_t lineCount = definition.labels.size();
    RECT panelRect = {0, 0, panel.size.cx, panel.size.cy};

    SaveDC(paintDC);

    SetDCBrushColor(paintDC, definition.backgroundColor);
    FillRect(paintDC, &panelRect, colorBrush);
    SetDCBrushColor(paintDC, definition.borderColor);
    FillRect(paintDC, &panel.separatorRect, colorBrush);

    SetBkMode(paintDC, TRANSPARENT);
    SetTextColor(paintDC, definition.foregroundColor);
    SelectObject(paintDC, fonts.headerFont);
    TextOutW(paintDC, panel.headerOrigin.x, panel.headerOrigin.y, header.c_str(), (int) header.size());

    for (size_t lineIndex = 0; lineIndex < lineCount; lineIndex++) {
        const wstring &label = definition.labels[lineIndex];
        const wstring &value = definition.values[panelIndex * lineCount + lineIndex];
        LONG lineTop = panel.firstLineTop + (LONG) lineIndex * panel.lineSpacing;

        SelectObject(paintDC, fonts.labelFont);
        TextOutW(paintDC, panel.headerOrigin.x, lineTop, label.c_str(), (int) label.size());
        SelectObject(paintDC, fonts.valueFont);
        TextOutW(paintDC, panel.valueColumnLeft, lineTop, value.c_str(), (int) value.size());
    }

    // Restoring the device context deselects the fonts, so they can be deleted when they go out of scope
    RestoreDC(paintDC, -1);
}

/**
 * Rounds a painted panel's corners and draws its border, smoothing both edges by how much of each pixel they cover.
 * Every pixel ends up premultiplied by its coverage, which is the form the system blends a layered window's pixels in.
 *
 * @param pixels
 *            - The panel's pixels, top row first, with four bytes each in blue, green, red, and alpha order
 * @param panel
 *            - The laid out panel, whose size the pixels span and whose DPI the corners and border are scaled to
 * @param borderColor
 *            - The color of the border
 */
static void shapePanel(BYTE *pixels, const DisplayInfoPanel &panel, COLORREF borderColor) {
    float cornerRadius = (float) scaleMetric(CORNER_RADIUS, panel.dpi);
    float borderThickness = (float) scaleMetric(LINE_THICKNESS, panel.dpi);
    float halfWidth = panel.size.cx / 2.0f;
    float halfHeight = panel.size.cy / 2.0f;
    BYTE borderChannels[] = {GetBValue(borderColor), GetGValue(borderColor), GetRValue(borderColor)};
    BYTE *pixel = pixels;

    for (LONG y = 0; y < panel.size.cy; y++) {
        for (LONG x = 0; x < panel.size.cx; x++, pixel += 4) {
            // How far the pixel's center lies past the center of the nearest corner's arc along each axis
            float cornerOffsetX = fabsf(x + 0.5f - halfWidth) - (halfWidth - cornerRadius);
            float cornerOffsetY = fabsf(y + 0.5f - halfHeight) - (halfHeight - cornerRadius);
            float outlineDistance;

            // Only a pixel beyond both edges of a corner measures to its arc, while every other pixel measures straight
            if (cornerOffsetX > 0 && cornerOffsetY > 0) {
                outlineDistance = sqrtf(cornerOffsetX * cornerOffsetX + cornerOffsetY * cornerOffsetY) - cornerRadius;
            } else {
                outlineDistance = (cornerOffsetX > cornerOffsetY ? cornerOffsetX : cornerOffsetY) - cornerRadius;
            }

            // The border is the band between the panel's outline and the same outline moved inward by its thickness
            float panelCoverage = outlineCoverage(outlineDistance);
            float fillCoverage = outlineCoverage(outlineDistance + borderThickness);
            float borderCoverage = panelCoverage - fillCoverage;

            // A pixel wholly inside the border keeps its painted color, so only the pixels near the edge are blended
            if (fillCoverage < 1) {
                for (int channel = 0; channel < 3; channel++) {
                    pixel[channel] =
                        (BYTE) (pixel[channel] * fillCoverage + borderChannels[channel] * borderCoverage + 0.5f);
                }
            }

            pixel[3] = (BYTE) (panelCoverage * 255 + 0.5f);
        }
    }
}

/**
 * Estimates how much of a pixel lies inside an outline from the distance between the pixel's center and the outline.
 *
 * @param distance
 *            - The distance from the pixel's center to the outline, negative inside it
 *
 * @return The share of the pixel inside the outline, from 0 (none of it) to 1 (all of it)
 */
static float outlineCoverage(float distance) {
    float coverage = 0.5f - distance;

    return coverage < 0 ? 0 : (coverage > 1 ? 1 : coverage);
}

/**
 * Stops the timer that hides the panels and destroys every panel window.
 */
static void hidePanels() {
    HWND timerWindow = controlWindow.load();

    if (timerWindow) {
        KillTimer(timerWindow, HIDE_TIMER_ID);
    }

    for (const DisplayInfoPanel &panel : panels) {
        if (panel.window) {
            DestroyWindow(panel.window);
        }
    }

    panels.clear();
}

/**
 * Handles the messages of a panel window, which keeps the size each show gives it when it moves to another display.
 *
 * @param windowHandle
 *            - The panel window
 * @param message
 *            - The message identifier
 * @param eventType
 *            - The message's WPARAM
 * @param eventData
 *            - The message's LPARAM
 *
 * @return The result of the message processing
 */
static LRESULT CALLBACK handlePanelEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData) {
    switch (message) {
    case WM_DPICHANGED:
        // Each show sizes the panel for its display, so the size suggested when it moves between displays is ignored
        return 0;
    default:
        return DefWindowProcW(windowHandle, message, eventType, eventData);
    }
}

/**
 * Stops the thread owning the panels, which hides them on its way out. A thread that does not stop promptly is
 * abandoned rather than waited on.
 */
static void stopOverlayThread() {
    HWND targetWindow = controlWindow.load();

    if (targetWindow) {
        PostMessage(targetWindow, WM_QUIT, 0, 0);
    }

    if (!overlayThread.joinable()) {
        return;
    }

    unique_lock<mutex> startupLock(startupMutex);
    bool finished = startupSignal.wait_for(startupLock, chrono::milliseconds(THREAD_TIMEOUT_MS),
                                           [] { return overlayThreadFinished; });

    startupLock.unlock();

    if (finished) {
        overlayThread.join();
    } else {
        overlayThread.detach();
    }
}
