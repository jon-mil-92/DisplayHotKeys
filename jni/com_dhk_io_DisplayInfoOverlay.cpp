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
#include "com_dhk_io_DisplayInfoOverlay.h"
#include "DisplayInfoOverlay.h"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <dwmapi.h>
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
static bool registerPrivateFonts(JNIEnv *env, jobjectArray fontFiles);
static bool startOverlayThread();
static void runOverlayThread();
static LRESULT CALLBACK handleControlEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void showPanels();
static SIZE layoutPanel(const DisplayInfoOverlayDefinition &definition, size_t panelIndex, DisplayInfoPanel &panel);
static HFONT createPanelFont(const DisplayInfoOverlayDefinition &definition, UINT dpi, bool header);
static int scaleMetric(int metric, UINT dpi);
static void stylePanelWindow(const DisplayInfoOverlayDefinition &definition, DisplayInfoPanel &panel);
static void hidePanels();
static LRESULT CALLBACK handlePanelEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void paintPanel(HWND windowHandle);
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
 * Font files registered privately for this process so the panels can draw in a family that is not installed.
 */
static vector<HANDLE> privateFontResources;

/**
 * Guards the privately registered font files.
 */
static mutex fontMutex;

/**
 * Definition of the panels being shown, read only by the thread owning the panels.
 */
static shared_ptr<const DisplayInfoOverlayDefinition> shownDefinition;

/**
 * Panels being shown, indexed the same as the headers of the shown definition and read only by the thread owning them.
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
 * Index of the gap below the header line in the metrics the caller supplies.
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
 * Number of metrics the caller supplies.
 */
static constexpr int OVERLAY_METRIC_COUNT = com_dhk_io_DisplayInfoOverlay_OVERLAY_METRIC_COUNT;

/**
 * Number of ints per panel in the anchor points the caller supplies.
 */
static constexpr int ANCHOR_FIELD_COUNT = com_dhk_io_DisplayInfoOverlay_ANCHOR_FIELD_COUNT;

/**
 * Highest opacity a panel can have, at which it is fully opaque.
 */
static constexpr jint MAX_OPACITY = 255;

/**
 * Shows one panel per header in the top-right corner of its display, replacing any panels already showing and
 * restarting the time they stay visible. Everything is copied here, so the panels never call back for any of it.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param headers
 *            - The bold first line of each panel
 * @param lines
 *            - The setting lines of every panel, panel by panel, with linesPerPanel lines for each header
 * @param linesPerPanel
 *            - The number of setting lines in each panel
 * @param anchorPoints
 *            - The desktop x and y coordinates of a point on each panel's display, in physical pixels
 * @param overlayColors
 *            - The panel colors in packed ARGB order, indexed by the OVERLAY_COLOR constants
 * @param overlayMetrics
 *            - The panel metrics in unscaled pixels, indexed by the OVERLAY_METRIC constants
 * @param fontFamily
 *            - The family of the font the panels are drawn in
 * @param fontFiles
 *            - The font files to register privately for the family, or null when the family is installed or the fonts
 *            were already registered, which lasts for the life of the process
 * @param opacity
 *            - The opacity of the panels, from 0 (invisible) to 255 (opaque)
 * @param durationMs
 *            - How long the panels stay visible, in milliseconds
 *
 * @return Whether the panels were accepted and any supplied fonts are registered
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_dhk_io_DisplayInfoOverlay_nativeShow(
    JNIEnv *env, jobject obj, jobjectArray headers, jobjectArray lines, jint linesPerPanel, jintArray anchorPoints,
    jintArray overlayColors, jintArray overlayMetrics, jstring fontFamily, jobjectArray fontFiles, jint opacity,
    jint durationMs) {
    (void) obj;

    if (!headers || !lines || !anchorPoints || !overlayColors || !overlayMetrics || linesPerPanel < 0 ||
        durationMs <= 0 || env->GetArrayLength(overlayColors) < OVERLAY_COLOR_COUNT ||
        env->GetArrayLength(overlayMetrics) < OVERLAY_METRIC_COUNT) {
        return JNI_FALSE;
    }

    jsize panelCount = env->GetArrayLength(headers);

    // Every panel needs its full set of lines and an anchor point, so short arrays are rejected rather than misread
    if (env->GetArrayLength(lines) < panelCount * linesPerPanel ||
        env->GetArrayLength(anchorPoints) < panelCount * ANCHOR_FIELD_COUNT) {
        return JNI_FALSE;
    }

    jint colors[OVERLAY_COLOR_COUNT];
    jint metrics[OVERLAY_METRIC_COUNT];
    vector<jint> anchors(panelCount * ANCHOR_FIELD_COUNT);

    env->GetIntArrayRegion(overlayColors, 0, OVERLAY_COLOR_COUNT, colors);
    env->GetIntArrayRegion(overlayMetrics, 0, OVERLAY_METRIC_COUNT, metrics);

    if (!anchors.empty()) {
        env->GetIntArrayRegion(anchorPoints, 0, (jsize) anchors.size(), anchors.data());
    }

    shared_ptr<DisplayInfoOverlayDefinition> definition = make_shared<DisplayInfoOverlayDefinition>();
    definition->headers = readWideStrings(env, headers);
    definition->lines = readWideStrings(env, lines);
    definition->linesPerPanel = linesPerPanel;

    for (jsize panelIndex = 0; panelIndex < panelCount; panelIndex++) {
        definition->anchorPoints.push_back(
            {anchors[panelIndex * ANCHOR_FIELD_COUNT], anchors[panelIndex * ANCHOR_FIELD_COUNT + 1]});
    }

    definition->backgroundColor = colorFromArgb(colors[OVERLAY_COLOR_BACKGROUND]);
    definition->foregroundColor = colorFromArgb(colors[OVERLAY_COLOR_FOREGROUND]);
    definition->borderColor = colorFromArgb(colors[OVERLAY_COLOR_BORDER]);
    definition->fontFamily = readWideString(env, fontFamily);
    definition->fontSize = metrics[OVERLAY_METRIC_FONT_SIZE];
    definition->headerFontSize = metrics[OVERLAY_METRIC_HEADER_FONT_SIZE];
    definition->padding = metrics[OVERLAY_METRIC_PADDING];
    definition->headerGap = metrics[OVERLAY_METRIC_HEADER_GAP];
    definition->lineGap = metrics[OVERLAY_METRIC_LINE_GAP];
    definition->margin = metrics[OVERLAY_METRIC_MARGIN];
    definition->opacity = (BYTE) (opacity < 0 ? 0 : (opacity > MAX_OPACITY ? MAX_OPACITY : opacity));
    definition->durationMs = (UINT) durationMs;

    // Registered before the panels are handed over, so no panel is ever drawn before its font exists
    bool fontsRegistered = registerPrivateFonts(env, fontFiles);

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

    // A failed registration reports false, so the caller supplies the fonts again with its next panels
    return fontsRegistered ? JNI_TRUE : JNI_FALSE;
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
 * Called when the native library is unloaded. Stops the thread owning the panels and releases the private fonts and
 * the panels still waiting to be shown.
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

    {
        lock_guard<mutex> definitionLock(definitionMutex);
        pendingDefinition.reset();
    }

    lock_guard<mutex> fontLock(fontMutex);

    for (HANDLE fontResource : privateFontResources) {
        RemoveFontMemResourceEx(fontResource);
    }

    privateFontResources.clear();
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
 * Registers the given font files privately for this process, once for its whole life, so the panels can draw in a
 * family that is not installed. A partial registration is undone, so a later call can try every file again.
 *
 * @param env
 *            - The JNI environment pointer
 * @param fontFiles
 *            - The font files to register, or null when there is nothing to register
 *
 * @return Whether the fonts are registered, which is also true when no files were supplied
 */
static bool registerPrivateFonts(JNIEnv *env, jobjectArray fontFiles) {
    if (!fontFiles) {
        return true;
    }

    lock_guard<mutex> fontLock(fontMutex);

    if (!privateFontResources.empty()) {
        return true;
    }

    jsize fileCount = env->GetArrayLength(fontFiles);

    for (jsize fileIndex = 0; fileIndex < fileCount; fileIndex++) {
        jbyteArray fontFile = (jbyteArray) env->GetObjectArrayElement(fontFiles, fileIndex);

        if (!fontFile) {
            continue;
        }

        vector<jbyte> fontBytes(env->GetArrayLength(fontFile));
        DWORD registeredFontCount = 0;

        env->GetByteArrayRegion(fontFile, 0, (jsize) fontBytes.size(), fontBytes.data());
        env->DeleteLocalRef(fontFile);

        // The system keeps its own copy of the font, so the buffer can go as soon as this returns
        HANDLE fontResource =
            AddFontMemResourceEx(fontBytes.data(), (DWORD) fontBytes.size(), nullptr, &registeredFontCount);

        if (!fontResource) {
            for (HANDLE registeredResource : privateFontResources) {
                RemoveFontMemResourceEx(registeredResource);
            }

            privateFontResources.clear();

            return false;
        }

        privateFontResources.push_back(fontResource);
    }

    return true;
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
 * Shows the panels waiting to be shown in the top-right corner of each one's display work area, scaled for that
 * display, and restarts the timer that hides them. Panels already showing are reused, so a repeat only updates them.
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

    // Only surplus panels are destroyed, so every remaining panel keeps the index its window was tagged with
    while (panels.size() > panelCount) {
        DestroyWindow(panels.back().window);
        panels.pop_back();
    }

    panels.resize(panelCount, DisplayInfoPanel{});
    shownDefinition = definition;

    // Panels sharing a display stack downwards, so the bottom edge of the last panel placed on each display is kept
    vector<pair<HMONITOR, LONG>> stackBottoms;

    for (size_t panelIndex = 0; panelIndex < panelCount; panelIndex++) {
        DisplayInfoPanel &panel = panels[panelIndex];
        HMONITOR monitor = MonitorFromPoint(definition->anchorPoints[panelIndex], MONITOR_DEFAULTTONEAREST);
        MONITORINFO monitorInfo = {};
        monitorInfo.cbSize = sizeof(monitorInfo);
        UINT dpiY = 0;

        // A display that vanished since the settings were read has nowhere to show its panel
        if (!GetMonitorInfoW(monitor, &monitorInfo)) {
            if (panel.window) {
                DestroyWindow(panel.window);
            }

            panel = DisplayInfoPanel{};
            continue;
        }

        if (GetDpiForMonitor(monitor, MDT_EFFECTIVE_DPI, &panel.dpi, &dpiY) != S_OK) {
            panel.dpi = USER_DEFAULT_SCREEN_DPI;
        }

        SIZE panelSize = layoutPanel(*definition, panelIndex, panel);
        int margin = scaleMetric(definition->margin, panel.dpi);
        LONG panelTop = monitorInfo.rcWork.top + margin;

        for (const pair<HMONITOR, LONG> &stackBottom : stackBottoms) {
            if (stackBottom.first == monitor && stackBottom.second + margin > panelTop) {
                panelTop = stackBottom.second + margin;
            }
        }

        stackBottoms.push_back({monitor, panelTop + panelSize.cy});

        LONG panelLeft = monitorInfo.rcWork.right - margin - panelSize.cx;

        if (panel.window) {
            stylePanelWindow(*definition, panel);

            // Raised above other topmost windows again, since one may have been shown over the panel meanwhile
            SetWindowPos(panel.window, HWND_TOPMOST, panelLeft, panelTop, panelSize.cx, panelSize.cy,
                         SWP_NOACTIVATE | SWP_SHOWWINDOW);
            InvalidateRect(panel.window, nullptr, FALSE);
            continue;
        }

        panel.window = CreateWindowExW(PANEL_EXTENDED_STYLE, PANEL_CLASS_NAME, L"", WS_POPUP, panelLeft, panelTop,
                                       panelSize.cx, panelSize.cy, NULL, NULL, GetModuleHandleW(NULL), NULL);

        if (!panel.window) {
            continue;
        }

        SetWindowLongPtrW(panel.window, GWLP_USERDATA, (LONG_PTR) panelIndex);
        stylePanelWindow(*definition, panel);
        ShowWindow(panel.window, SW_SHOWNOACTIVATE);
    }

    // Setting the timer again replaces the running one, so a repeated show restarts the time the panels stay visible
    SetTimer(controlWindow.load(), HIDE_TIMER_ID, definition->durationMs, NULL);
}

/**
 * Measures a panel's header and setting lines in fonts scaled for the panel's display, storing the line heights the
 * panel paints with.
 *
 * @param definition
 *            - The definition of the panels being shown
 * @param panelIndex
 *            - The index of the panel to measure
 * @param panel
 *            - The panel to measure, whose DPI is already set and whose line heights are filled in
 *
 * @return The size of the panel's window, in physical pixels
 */
static SIZE layoutPanel(const DisplayInfoOverlayDefinition &definition, size_t panelIndex, DisplayInfoPanel &panel) {
    HDC measureDC = CreateCompatibleDC(NULL);
    HFONT headerFont = createPanelFont(definition, panel.dpi, true);
    HFONT lineFont = createPanelFont(definition, panel.dpi, false);
    HGDIOBJ previousFont = SelectObject(measureDC, headerFont);
    TEXTMETRICW textMetrics = {};
    SIZE textSize = {};
    const wstring &header = definition.headers[panelIndex];

    GetTextMetricsW(measureDC, &textMetrics);
    GetTextExtentPoint32W(measureDC, header.c_str(), (int) header.size(), &textSize);
    panel.headerHeight = textMetrics.tmHeight;

    LONG contentWidth = textSize.cx;

    SelectObject(measureDC, lineFont);
    GetTextMetricsW(measureDC, &textMetrics);
    panel.lineHeight = textMetrics.tmHeight;

    for (int lineIndex = 0; lineIndex < definition.linesPerPanel; lineIndex++) {
        const wstring &line = definition.lines[panelIndex * definition.linesPerPanel + lineIndex];

        GetTextExtentPoint32W(measureDC, line.c_str(), (int) line.size(), &textSize);

        if (textSize.cx > contentWidth) {
            contentWidth = textSize.cx;
        }
    }

    SelectObject(measureDC, previousFont);
    DeleteObject(headerFont);
    DeleteObject(lineFont);
    DeleteDC(measureDC);

    int padding = scaleMetric(definition.padding, panel.dpi);
    int lineCount = definition.linesPerPanel;
    int linesHeight = 0;

    if (lineCount > 0) {
        linesHeight = scaleMetric(definition.headerGap, panel.dpi) + lineCount * panel.lineHeight +
                      (lineCount - 1) * scaleMetric(definition.lineGap, panel.dpi);
    }

    return {contentWidth + 2 * padding, panel.headerHeight + linesHeight + 2 * padding};
}

/**
 * Creates the font a panel draws its header or setting lines in, sized for the given DPI.
 *
 * @param definition
 *            - The definition of the panels being shown
 * @param dpi
 *            - The DPI of the display the panel sits on
 * @param header
 *            - Whether to create the bold header font rather than the setting lines' font
 *
 * @return The created font, which the caller deletes
 */
static HFONT createPanelFont(const DisplayInfoOverlayDefinition &definition, UINT dpi, bool header) {
    LOGFONTW logFont = {};

    // A negative height requests the font size itself rather than the taller line height, matching the size supplied
    logFont.lfHeight = -scaleMetric(header ? definition.headerFontSize : definition.fontSize, dpi);
    logFont.lfWeight = header ? FW_BOLD : FW_NORMAL;
    logFont.lfCharSet = DEFAULT_CHARSET;
    logFont.lfQuality = CLEARTYPE_QUALITY;
    wcsncpy_s(logFont.lfFaceName, definition.fontFamily.c_str(), _TRUNCATE);

    return CreateFontIndirectW(&logFont);
}

/**
 * Scales a metric given in unscaled pixels to the given DPI.
 *
 * @param metric
 *            - The metric in unscaled pixels
 * @param dpi
 *            - The DPI to scale the metric to
 *
 * @return The metric in physical pixels at the given DPI
 */
static int scaleMetric(int metric, UINT dpi) {
    return MulDiv(metric, dpi, USER_DEFAULT_SCREEN_DPI);
}

/**
 * Applies the panels' opacity to a panel window, and asks the system to round its corners and color its border.
 * Windows 10 rejects both requests, so there the panel keeps square corners and draws its own border.
 *
 * @param definition
 *            - The definition of the panels being shown
 * @param panel
 *            - The panel to style, whose window exists
 */
static void stylePanelWindow(const DisplayInfoOverlayDefinition &definition, DisplayInfoPanel &panel) {
    COLORREF borderColor = definition.borderColor;
    DWM_WINDOW_CORNER_PREFERENCE cornerPreference = DWMWCP_ROUND;

    SetLayeredWindowAttributes(panel.window, 0, definition.opacity, LWA_ALPHA);
    DwmSetWindowAttribute(panel.window, DWMWA_WINDOW_CORNER_PREFERENCE, &cornerPreference, sizeof(cornerPreference));

    panel.drawFrame =
        FAILED(DwmSetWindowAttribute(panel.window, DWMWA_BORDER_COLOR, &borderColor, sizeof(borderColor)));
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
    shownDefinition.reset();
}

/**
 * Handles the messages of a panel window, which paints itself and lets every click fall through to what is beneath.
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
    case WM_PAINT:
        paintPanel(windowHandle);
        return 0;
    case WM_ERASEBKGND:
        // The paint fills the whole panel, so skipping the erase avoids a flash of an unpainted background
        return 1;
    case WM_NCHITTEST:
        return HTTRANSPARENT;
    case WM_MOUSEACTIVATE:
        return MA_NOACTIVATE;
    case WM_DPICHANGED:
        // Each show sizes the panel for its display, so the size suggested when it moves between displays is ignored
        return 0;
    default:
        return DefWindowProcW(windowHandle, message, eventType, eventData);
    }
}

/**
 * Paints a panel's background, its border where the system does not draw one, its bold header line, and its setting
 * lines beneath, using the layout measured when the panel was shown.
 *
 * @param windowHandle
 *            - The panel window to paint
 */
static void paintPanel(HWND windowHandle) {
    PAINTSTRUCT paint;
    HDC paintDC = BeginPaint(windowHandle, &paint);
    size_t panelIndex = (size_t) GetWindowLongPtrW(windowHandle, GWLP_USERDATA);

    if (!shownDefinition || panelIndex >= panels.size()) {
        EndPaint(windowHandle, &paint);
        return;
    }

    const DisplayInfoOverlayDefinition &definition = *shownDefinition;
    const DisplayInfoPanel &panel = panels[panelIndex];
    HFONT headerFont = createPanelFont(definition, panel.dpi, true);
    HFONT lineFont = createPanelFont(definition, panel.dpi, false);
    HBRUSH colorBrush = (HBRUSH) GetStockObject(DC_BRUSH);
    int padding = scaleMetric(definition.padding, panel.dpi);
    RECT clientRect = {};

    GetClientRect(windowHandle, &clientRect);
    SaveDC(paintDC);

    SetDCBrushColor(paintDC, definition.backgroundColor);
    FillRect(paintDC, &clientRect, colorBrush);

    if (panel.drawFrame) {
        SetDCBrushColor(paintDC, definition.borderColor);
        FrameRect(paintDC, &clientRect, colorBrush);
    }

    SetBkMode(paintDC, TRANSPARENT);
    SetTextColor(paintDC, definition.foregroundColor);
    SelectObject(paintDC, headerFont);

    const wstring &header = definition.headers[panelIndex];
    RECT textRect = {padding, padding, clientRect.right - padding, padding + panel.headerHeight};
    UINT textFormat = DT_LEFT | DT_TOP | DT_SINGLELINE | DT_NOPREFIX;

    DrawTextW(paintDC, header.c_str(), (int) header.size(), &textRect, textFormat);
    SelectObject(paintDC, lineFont);

    textRect.top = textRect.bottom + scaleMetric(definition.headerGap, panel.dpi);

    for (int lineIndex = 0; lineIndex < definition.linesPerPanel; lineIndex++) {
        const wstring &line = definition.lines[panelIndex * definition.linesPerPanel + lineIndex];

        textRect.bottom = textRect.top + panel.lineHeight;
        DrawTextW(paintDC, line.c_str(), (int) line.size(), &textRect, textFormat);
        textRect.top = textRect.bottom + scaleMetric(definition.lineGap, panel.dpi);
    }

    RestoreDC(paintDC, -1);
    DeleteObject(headerFont);
    DeleteObject(lineFont);
    EndPaint(windowHandle, &paint);
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
