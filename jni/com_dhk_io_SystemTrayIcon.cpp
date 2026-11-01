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
#include "com_dhk_io_SystemTrayIcon.h"
#include "DisplayConfig.h"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <dwmapi.h>
#include <jni.h>
#include <memory>
#include <mutex>
#include <shellapi.h>
#include <shellscalingapi.h>
#include <string>
#include <thread>
#include <vector>
#include <windows.h>
#include <windowsx.h>

using namespace std;

static wstring readWideString(JNIEnv *env, jstring javaString);
static bool stageIconPixels(JNIEnv *env, jintArray iconPixels, jint iconWidth, jint iconHeight);
static void releaseCallbackTarget(JNIEnv *env);
static void stopMessageLoopThread();
static void dismissTrayMenu();
static UINT getNotificationAreaDpi();
static COLORREF colorFromArgb(jint argb);
static void runMessageLoopThread(unsigned long long session);
static void finishMessageLoopThread(unsigned long long session);
LRESULT CALLBACK handleTrayEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void addTrayIcon();
static void notifyShell(DWORD command, UINT flags);
static void applyPendingIcon();
static HICON createIconFromPixels(const vector<jint> &pixels, int width, int height);
static void openTrayMenu(WPARAM anchorPosition);
static void showTrayMenu(int anchorX, int anchorY);
static int trackTrayMenu(const TrayMenuDefinition &definition, int anchorX, int anchorY);
static void applyMenuAppMode(COLORREF backgroundColor);
static LRESULT CALLBACK styleMenuWindow(int hookCode, WPARAM messageSource, LPARAM messageData);
static UINT trayMenuAlignment(HMONITOR anchorMonitor);
LRESULT CALLBACK handleMenuEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void drawMenuItem(const DRAWITEMSTRUCT &drawItem);
static void invokeMenuItemSelectedCallback(int itemIndex);

/**
 * Signature of the undocumented uxtheme export that sets whether the process draws its menus dark.
 */
typedef int(WINAPI *SetPreferredAppModeFunction)(int appMode);

/**
 * Signature of the undocumented uxtheme export that makes menus pick up a changed app mode.
 */
typedef void(WINAPI *FlushMenuThemesFunction)();

/**
 * Global JavaVM pointer used to attach native threads when invoking callbacks.
 */
static JavaVM *jvm = nullptr;

/**
 * Global reference to the registered instance used for callbacks from native code.
 */
static jobject systemTrayIconGlobalRef = nullptr;

/**
 * Cached method ID for the onMenuItemSelected(int) callback.
 */
static jmethodID onMenuItemSelectedMethodId = nullptr;

/**
 * Guards the callback reference and method ID, since a chosen item is reported from a detached thread that a stop
 * neither waits for nor joins, so the reference must never be deleted while that thread is calling through it.
 */
static mutex callbackMutex;

/**
 * Thread that runs the Windows message loop owning the icon.
 */
static thread messageLoopThread;

/**
 * Whether the icon is started. A starting thread checks it before publishing its window, so a thread that a stop
 * already gave up on ends at once instead of running with nothing left to stop it.
 */
static atomic_bool isRunning(false);

/**
 * Hidden message window that owns the icon and receives its callback message. Read from the calling thread while the
 * thread owning it may be tearing it down, so it is atomic and cleared before the window is destroyed.
 */
static atomic<HWND> messageWindow{NULL};

/**
 * Registered window class name for the hidden message window.
 */
static const wchar_t CLASS_NAME[] = L"DHK_SystemTrayIcon_MessageWindow";

/**
 * Registered window class name for the hidden window that owns an open menu.
 */
static const wchar_t MENU_CLASS_NAME[] = L"DHK_SystemTrayIcon_MenuWindow";

/**
 * Window class name of the task bar, used to read its position without waiting on the shell to answer for it.
 */
static const wchar_t TASKBAR_CLASS_NAME[] = L"Shell_TrayWnd";

/**
 * Registered id of the "TaskbarCreated" broadcast the shell sends.
 */
static UINT taskbarCreatedMessage = 0;

/**
 * Icon the current message loop thread shows. Each thread owns its own, so a thread that a stop gave up on can never
 * destroy the icon of the start that replaced it.
 */
static thread_local HICON trayIcon = NULL;

/**
 * Whether the current message loop thread's icon should be registered with the shell, so a shell restart only re-adds
 * a shown icon.
 */
static thread_local bool iconVisible = false;

/**
 * Hidden message window the current message loop thread created, which identifies its icon to the shell, so a thread
 * that a stop gave up on only ever removes its own icon.
 */
static thread_local HWND iconOwnerWindow = NULL;

/**
 * Number of the latest start, guarded by the startup lock, so a message loop thread that a stop gave up on can tell a
 * newer start has replaced it and leave the shared state to that start.
 */
static unsigned long long currentSession = 0;

/**
 * Guards the staged tooltip and icon payloads handed over from the calling thread.
 */
static mutex pendingPayloadMutex;

/**
 * Signals a waiting start or stop that the message loop thread started or finished.
 */
static condition_variable startupSignal;

/**
 * Guards the session number, the running flag, the published window, and the startup and finish states.
 */
static mutex startupMutex;

/**
 * Whether the icon was registered with the shell during startup.
 */
static bool startupSucceeded = false;

/**
 * Whether the thread owning the icon has finished, so a stop can tell a thread that returned from one still parked in
 * a call that synchronizes with an unresponsive process.
 */
static bool messageLoopFinished = false;

/**
 * Tooltip text staged for the message loop thread to apply.
 */
static wstring pendingTooltip;

/**
 * Icon pixels staged for the message loop thread to apply, in packed ARGB order.
 */
static vector<jint> pendingIconPixels;

/**
 * Width of the staged icon pixels.
 */
static int pendingIconWidth = 0;

/**
 * Height of the staged icon pixels.
 */
static int pendingIconHeight = 0;

/**
 * Menu shown for the icon. Each menu thread takes its own reference, so replacing it never frees one still showing.
 */
static shared_ptr<const TrayMenuDefinition> menuDefinition;

/**
 * Guards the menu definition and the private font backing it.
 */
static mutex menuDefinitionMutex;

/**
 * Font file registered privately for this process so the menu can draw in a family that is not installed.
 */
static HANDLE privateFontResource = NULL;

/**
 * Whether a menu is open or opening, so a click arriving meanwhile never opens a second one.
 */
static atomic_bool menuShowing(false);

/**
 * Hidden window owning the open menu, published so a dismissal from another thread can reach it.
 */
static atomic<HWND> menuWindow{NULL};

/**
 * App mode last applied to the process's menus, or -1 before any, so the theme is only flushed when it changes.
 */
static atomic_int appliedAppMode(-1);

/**
 * Definition of the menu the current thread is showing, read by the window procedure while the menu is open.
 */
static thread_local const TrayMenuDefinition *activeMenuDefinition = nullptr;

/**
 * Font the current thread draws its open menu's items in.
 */
static thread_local HFONT activeMenuFont = NULL;

/**
 * DPI of the display the current thread's open menu is shown on.
 */
static thread_local UINT activeMenuDpi = USER_DEFAULT_SCREEN_DPI;

/**
 * Identifier distinguishing the icon within the owning window.
 */
static constexpr UINT TRAY_ICON_ID = 1;

/**
 * How long (ms) a start or stop waits on the thread owning the icon before answering without it, bounding the caller's
 * wait on a thread parked in a call to a busy or unresponsive process.
 */
static constexpr int THREAD_TIMEOUT_MS = 2000;

/**
 * Atom of the window class the system draws menus in, known by the class name "#32768".
 */
static constexpr WORD MENU_WINDOW_CLASS_ATOM = 0x8000;

/**
 * Private message the shell posts to the owning window for icon events.
 */
static constexpr UINT WM_TRAY_ICON_NOTIFY = WM_APP + 1;

/**
 * Private message asking the message loop thread to apply the staged icon.
 */
static constexpr UINT WM_TRAY_APPLY_ICON = WM_APP + 2;

/**
 * Private message asking the message loop thread to show or hide the icon.
 */
static constexpr UINT WM_TRAY_SET_VISIBLE = WM_APP + 3;

/**
 * Private message asking the thread showing the menu to close it.
 */
static constexpr UINT WM_TRAY_CLOSE_MENU = WM_APP + 4;

/**
 * Index of the menu background color in the colors the caller supplies, taken from the generated header so the two
 * sides of the boundary can never disagree.
 */
static constexpr int MENU_COLOR_BACKGROUND = com_dhk_io_SystemTrayIcon_MENU_COLOR_BACKGROUND;

/**
 * Index of the item text color in the colors the caller supplies.
 */
static constexpr int MENU_COLOR_FOREGROUND = com_dhk_io_SystemTrayIcon_MENU_COLOR_FOREGROUND;

/**
 * Index of the highlighted item's fill color in the colors the caller supplies.
 */
static constexpr int MENU_COLOR_SELECTION_BACKGROUND = com_dhk_io_SystemTrayIcon_MENU_COLOR_SELECTION_BACKGROUND;

/**
 * Index of the highlighted item's text color in the colors the caller supplies.
 */
static constexpr int MENU_COLOR_SELECTION_FOREGROUND = com_dhk_io_SystemTrayIcon_MENU_COLOR_SELECTION_FOREGROUND;

/**
 * Index of the menu border color in the colors the caller supplies.
 */
static constexpr int MENU_COLOR_BORDER = com_dhk_io_SystemTrayIcon_MENU_COLOR_BORDER;

/**
 * Number of colors the caller supplies.
 */
static constexpr int MENU_COLOR_COUNT = com_dhk_io_SystemTrayIcon_MENU_COLOR_COUNT;

/**
 * Index of the item width in the metrics the caller supplies.
 */
static constexpr int MENU_METRIC_ITEM_WIDTH = com_dhk_io_SystemTrayIcon_MENU_METRIC_ITEM_WIDTH;

/**
 * Index of the item height in the metrics the caller supplies.
 */
static constexpr int MENU_METRIC_ITEM_HEIGHT = com_dhk_io_SystemTrayIcon_MENU_METRIC_ITEM_HEIGHT;

/**
 * Index of the text inset in the metrics the caller supplies.
 */
static constexpr int MENU_METRIC_TEXT_INSET = com_dhk_io_SystemTrayIcon_MENU_METRIC_TEXT_INSET;

/**
 * Index of the font size in the metrics the caller supplies.
 */
static constexpr int MENU_METRIC_FONT_SIZE = com_dhk_io_SystemTrayIcon_MENU_METRIC_FONT_SIZE;

/**
 * Number of metrics the caller supplies.
 */
static constexpr int MENU_METRIC_COUNT = com_dhk_io_SystemTrayIcon_MENU_METRIC_COUNT;

/**
 * Ordinal of the undocumented uxtheme export that sets the process's preferred app mode.
 */
static constexpr WORD SET_PREFERRED_APP_MODE_ORDINAL = 135;

/**
 * Ordinal of the undocumented uxtheme export that refreshes menus after the app mode changes.
 */
static constexpr WORD FLUSH_MENU_THEMES_ORDINAL = 136;

/**
 * App mode that draws the process's menus dark whatever the system setting.
 */
static constexpr int APP_MODE_FORCE_DARK = 2;

/**
 * App mode that draws the process's menus light whatever the system setting.
 */
static constexpr int APP_MODE_FORCE_LIGHT = 3;

/**
 * Perceived brightness, out of 255, below which a menu background counts as dark.
 */
static constexpr int DARK_LUMINANCE_THRESHOLD = 128;

/**
 * Called when the native library is loaded. Caches the JavaVM pointer for attaching native threads to invoke callbacks.
 *
 * @param vm
 *            - The JavaVM pointer
 * @param reserved
 *            - Reserved for future use
 *
 * @return The JNI version supported by this library
 */
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    jvm = vm;

    return JNI_VERSION_1_6;
}

/**
 * Starts the notification area icon on a message loop thread of its own, waiting only a bounded time for the shell to
 * register it. The calling object instance is kept as the target of the menu's callback until the icon stops.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param tooltip
 *            - The tooltip text to show for the icon
 * @param iconPixels
 *            - The icon pixels in packed ARGB order, row by row from the top
 * @param iconWidth
 *            - The icon width in pixels
 * @param iconHeight
 *            - The icon height in pixels
 *
 * @return Whether the icon started, which includes a start still waiting on a busy shell
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_dhk_io_SystemTrayIcon_nativeStart(JNIEnv *env, jobject obj,
                                                                                 jstring tooltip, jintArray iconPixels,
                                                                                 jint iconWidth, jint iconHeight) {
    if (isRunning.load()) {
        return JNI_TRUE;
    }

    // Join a thread that already ended on its own, since starting a new one over it would end the process
    if (messageLoopThread.joinable()) {
        messageLoopThread.join();
    }

    jmethodID menuItemSelectedMethodId = env->GetMethodID(env->GetObjectClass(obj), "onMenuItemSelected", "(I)V");
    jobject globalRef = menuItemSelectedMethodId ? env->NewGlobalRef(obj) : nullptr;

    if (!globalRef) {
        env->ExceptionClear();
        return JNI_FALSE;
    }

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);
        pendingTooltip = readWideString(env, tooltip);
    }

    stageIconPixels(env, iconPixels, iconWidth, iconHeight);

    {
        lock_guard<mutex> callbackLock(callbackMutex);
        systemTrayIconGlobalRef = globalRef;
        onMenuItemSelectedMethodId = menuItemSelectedMethodId;
    }

    unsigned long long session = 0;

    {
        lock_guard<mutex> startupLock(startupMutex);
        session = ++currentSession;
        startupSucceeded = false;
        messageLoopFinished = false;
        isRunning.store(true);
    }

    // Creating a thread throws when the system is out of resources, and an exception escaping here ends the process
    try {
        messageLoopThread = thread(runMessageLoopThread, session);
    } catch (const system_error &) {
        stopMessageLoopThread();
        releaseCallbackTarget(env);

        return JNI_FALSE;
    }

    // Wait only a bounded time, since registering the icon round-trips to a shell that answers slowly while busy
    unique_lock<mutex> startupLock(startupMutex);

    startupSignal.wait_for(startupLock, chrono::milliseconds(THREAD_TIMEOUT_MS),
                           [] { return startupSucceeded || messageLoopFinished; });

    // A start still waiting on a busy shell is not a failure, since its icon appears as soon as the shell answers
    bool failed = !startupSucceeded && messageLoopFinished;

    startupLock.unlock();

    // A failed thread has already finished, so it is joined at once and the reference it would have used is released
    if (failed) {
        messageLoopThread.join();
        releaseCallbackTarget(env);
    }

    return failed ? JNI_FALSE : JNI_TRUE;
}

/**
 * Stops the notification area icon and closes any open menu, then releases the calling object instance so nothing
 * native keeps it reachable.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_SystemTrayIcon_nativeStop(JNIEnv *env, jobject obj) {
    (void) obj;

    // Runs even when the thread already ended on its own, so the reference is always released
    stopMessageLoopThread();
    releaseCallbackTarget(env);
}

/**
 * Replaces the icon shown in the notification area. Only the thread that owns the icon may hand it to the shell, so
 * the replacement is passed to that thread and success means it was handed over rather than finished.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param iconPixels
 *            - The icon pixels in packed ARGB order, row by row from the top
 * @param iconWidth
 *            - The icon width in pixels
 * @param iconHeight
 *            - The icon height in pixels
 *
 * @return Whether the replacement was handed over to the thread that owns the icon
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_dhk_io_SystemTrayIcon_nativeSetIcon(JNIEnv *env, jobject obj,
                                                                                   jintArray iconPixels, jint iconWidth,
                                                                                   jint iconHeight) {
    (void) obj;

    HWND targetWindow = messageWindow.load();

    if (!targetWindow || !stageIconPixels(env, iconPixels, iconWidth, iconHeight)) {
        return JNI_FALSE;
    }

    return PostMessage(targetWindow, WM_TRAY_APPLY_ICON, 0, 0) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Shows or hides the icon without releasing the native resources backing it, so it can be shown again cheaply.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param visible
 *            - Whether the icon should be shown
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_SystemTrayIcon_nativeSetVisible(JNIEnv *env, jobject obj,
                                                                                  jboolean visible) {
    (void) env;
    (void) obj;

    HWND targetWindow = messageWindow.load();

    if (targetWindow) {
        PostMessage(targetWindow, WM_TRAY_SET_VISIBLE, visible == JNI_TRUE ? 1 : 0, 0);
    }
}

/**
 * Gets the notification area icon size for the scale of the display hosting the notification area.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 *
 * @return The icon width and height in pixels, or null when the size is unavailable
 */
extern "C" JNIEXPORT jintArray JNICALL Java_com_dhk_io_SystemTrayIcon_nativeGetIconSize(JNIEnv *env, jobject obj) {
    (void) obj;

    UINT notificationAreaDpi = getNotificationAreaDpi();
    jint iconSize[2] = {GetSystemMetricsForDpi(SM_CXSMICON, notificationAreaDpi),
                        GetSystemMetricsForDpi(SM_CYSMICON, notificationAreaDpi)};

    if (iconSize[0] <= 0 || iconSize[1] <= 0) {
        return nullptr;
    }

    jintArray iconSizeArray = env->NewIntArray(2);

    if (iconSizeArray) {
        env->SetIntArrayRegion(iconSizeArray, 0, 2, iconSize);
    }

    return iconSizeArray;
}

/**
 * Sets the items and style of the menu shown for the icon. Everything is copied here, so opening the menu never calls
 * back for any of it and stays fast however busy the caller is.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 * @param itemTexts
 *            - The text of each item, in the order the items are shown and reported
 * @param menuColors
 *            - The menu colors in packed ARGB order, indexed by the MENU_COLOR constants
 * @param menuMetrics
 *            - The menu metrics in unscaled pixels, indexed by the MENU_METRIC constants
 * @param fontFamily
 *            - The family of the font the items are drawn in
 * @param fontData
 *            - The font file to register privately for the family, or null when the family is installed or the font
 *            was already registered, which lasts for the life of the process
 *
 * @return Whether the menu was accepted and any supplied font is registered
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_dhk_io_SystemTrayIcon_nativeSetMenu(JNIEnv *env, jobject obj, jobjectArray itemTexts, jintArray menuColors,
                                             jintArray menuMetrics, jstring fontFamily, jbyteArray fontData) {
    (void) obj;

    if (!itemTexts || !menuColors || !menuMetrics || env->GetArrayLength(menuColors) < MENU_COLOR_COUNT ||
        env->GetArrayLength(menuMetrics) < MENU_METRIC_COUNT) {
        return JNI_FALSE;
    }

    jint colors[MENU_COLOR_COUNT];
    jint metrics[MENU_METRIC_COUNT];

    env->GetIntArrayRegion(menuColors, 0, MENU_COLOR_COUNT, colors);
    env->GetIntArrayRegion(menuMetrics, 0, MENU_METRIC_COUNT, metrics);

    shared_ptr<TrayMenuDefinition> definition = make_shared<TrayMenuDefinition>();
    jsize itemCount = env->GetArrayLength(itemTexts);

    for (jsize itemIndex = 0; itemIndex < itemCount; itemIndex++) {
        jstring itemText = (jstring) env->GetObjectArrayElement(itemTexts, itemIndex);

        definition->itemTexts.push_back(readWideString(env, itemText));

        if (itemText) {
            env->DeleteLocalRef(itemText);
        }
    }

    definition->backgroundColor = colorFromArgb(colors[MENU_COLOR_BACKGROUND]);
    definition->foregroundColor = colorFromArgb(colors[MENU_COLOR_FOREGROUND]);
    definition->selectionBackgroundColor = colorFromArgb(colors[MENU_COLOR_SELECTION_BACKGROUND]);
    definition->selectionForegroundColor = colorFromArgb(colors[MENU_COLOR_SELECTION_FOREGROUND]);
    definition->borderColor = colorFromArgb(colors[MENU_COLOR_BORDER]);
    definition->fontFamily = readWideString(env, fontFamily);
    definition->fontSize = metrics[MENU_METRIC_FONT_SIZE];
    definition->itemWidth = metrics[MENU_METRIC_ITEM_WIDTH];
    definition->itemHeight = metrics[MENU_METRIC_ITEM_HEIGHT];
    definition->textInset = metrics[MENU_METRIC_TEXT_INSET];

    lock_guard<mutex> definitionLock(menuDefinitionMutex);

    // Registered once and before the definition is published, so no menu is ever drawn before its font exists
    if (fontData && !privateFontResource) {
        vector<jbyte> fontBytes(env->GetArrayLength(fontData));
        DWORD registeredFontCount = 0;

        env->GetByteArrayRegion(fontData, 0, (jsize) fontBytes.size(), fontBytes.data());

        // The system keeps its own copy of the font, so the buffer can go as soon as this returns
        privateFontResource =
            AddFontMemResourceEx(fontBytes.data(), (DWORD) fontBytes.size(), nullptr, &registeredFontCount);
    }

    menuDefinition = definition;

    // A failed registration reports false, so the caller supplies the font again with its next menu
    return !fontData || privateFontResource ? JNI_TRUE : JNI_FALSE;
}

/**
 * Closes the menu when one is open, without waiting for it to finish closing.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_SystemTrayIcon_nativeDismissMenu(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    dismissTrayMenu();
}

/**
 * Called when the native library is unloaded. Stops the icon and releases everything this library still holds, which
 * is the menu, the private font, and the reference to the registered instance.
 *
 * @param vm
 *            - The JavaVM pointer
 * @param reserved
 *            - Reserved for future use
 */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void) reserved;

    stopMessageLoopThread();

    {
        lock_guard<mutex> definitionLock(menuDefinitionMutex);

        menuDefinition.reset();

        if (privateFontResource) {
            RemoveFontMemResourceEx(privateFontResource);
            privateFontResource = NULL;
        }
    }

    JNIEnv *env = nullptr;

    // With no environment the reference is only forgotten, which is safe since the virtual machine is shutting down
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    releaseCallbackTarget(env);
    jvm = nullptr;
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
 * Stages icon pixels for the message loop thread to apply, rejecting pixels that do not fill the given size.
 *
 * @param env
 *            - The JNI environment pointer
 * @param iconPixels
 *            - The icon pixels in packed ARGB order, row by row from the top
 * @param iconWidth
 *            - The icon width in pixels
 * @param iconHeight
 *            - The icon height in pixels
 *
 * @return Whether the pixels were staged
 */
static bool stageIconPixels(JNIEnv *env, jintArray iconPixels, jint iconWidth, jint iconHeight) {
    if (!iconPixels || iconWidth <= 0 || iconHeight <= 0 || env->GetArrayLength(iconPixels) < iconWidth * iconHeight) {
        return false;
    }

    lock_guard<mutex> payloadLock(pendingPayloadMutex);

    pendingIconPixels.resize(iconWidth * iconHeight);
    env->GetIntArrayRegion(iconPixels, 0, iconWidth * iconHeight, pendingIconPixels.data());
    pendingIconWidth = iconWidth;
    pendingIconHeight = iconHeight;

    return true;
}

/**
 * Deletes the global reference to the registered instance and forgets its callback, so nothing native keeps the
 * instance reachable once the icon is gone.
 *
 * @param env
 *            - The JNI environment pointer, or null when none is available and the reference can only be forgotten
 */
static void releaseCallbackTarget(JNIEnv *env) {
    // Taken so a menu thread still reporting a choice finishes its call before the reference goes away
    lock_guard<mutex> callbackLock(callbackMutex);

    if (systemTrayIconGlobalRef && env) {
        env->DeleteGlobalRef(systemTrayIconGlobalRef);
    }

    systemTrayIconGlobalRef = nullptr;
    onMenuItemSelectedMethodId = nullptr;
}

/**
 * Stops the thread owning the icon after closing an open menu. A thread that does not stop promptly is abandoned rather
 * than waited on, since it can be parked in a call that synchronizes with an unresponsive process.
 */
static void stopMessageLoopThread() {
    HWND targetWindow = NULL;

    {
        /*
         * Locked against a starting thread publishing its window. Either this stop finds the window and quits it, or
         * the thread sees the stop and never publishes, so a thread can never be left running unseen
         */
        lock_guard<mutex> startupLock(startupMutex);

        isRunning.store(false);
        targetWindow = messageWindow.load();
    }

    dismissTrayMenu();

    if (targetWindow) {
        PostMessage(targetWindow, WM_QUIT, 0, 0);
    }

    if (!messageLoopThread.joinable()) {
        return;
    }

    unique_lock<mutex> startupLock(startupMutex);
    bool finished = startupSignal.wait_for(startupLock, chrono::milliseconds(THREAD_TIMEOUT_MS),
                                           [] { return messageLoopFinished; });

    startupLock.unlock();

    if (finished) {
        messageLoopThread.join();
    } else {
        messageLoopThread.detach();
    }
}

/**
 * Asks the thread showing the menu to close it. The request is posted rather than sent, so the caller never waits on
 * a menu thread that is still claiming the foreground.
 */
static void dismissTrayMenu() {
    HWND openMenuWindow = menuWindow.load();

    if (openMenuWindow) {
        PostMessage(openMenuWindow, WM_TRAY_CLOSE_MENU, 0, 0);
    }
}

/**
 * Gets the DPI of the display hosting the notification area, which is not necessarily the primary display. Sizing the
 * icon from the system DPI would leave it scaled for the wrong display whenever the task bar sits on another one.
 *
 * @return The DPI of the display hosting the notification area, or the system DPI when it is unavailable
 */
static UINT getNotificationAreaDpi() {
    HWND taskbarWindow = FindWindowW(TASKBAR_CLASS_NAME, NULL);
    RECT taskbarRect = {};
    UINT dpiX = 0;
    UINT dpiY = 0;

    /*
     * Read the task bar's rectangle from the window itself rather than through the shell's app bar interface. That
     * interface answers on the shell's own thread, which blocks for as long as the shell takes to finish rebuilding
     * the task bar after a display change - exactly when this is asked
     */
    if (!taskbarWindow || !GetWindowRect(taskbarWindow, &taskbarRect) ||
        GetDpiForMonitor(MonitorFromRect(&taskbarRect, MONITOR_DEFAULTTONEAREST), MDT_EFFECTIVE_DPI, &dpiX, &dpiY) !=
            S_OK) {
        return GetDpiForSystem();
    }

    return dpiX;
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
 * Runs the icon on a dedicated thread, which owns its hidden window and icon outright and pumps their messages until
 * it is stopped. A thread that a stop or a newer start already gave up on ends without touching the shared state.
 *
 * @param session
 *            - The number of the start that launched this thread
 */
static void runMessageLoopThread(unsigned long long session) {
    WNDCLASSW windowClass = {};
    windowClass.lpfnWndProc = handleTrayEvents;
    windowClass.hInstance = GetModuleHandleW(NULL);
    windowClass.lpszClassName = CLASS_NAME;

    // Fails harmlessly when an earlier stop could not unregister the class, since the existing class is identical
    RegisterClassW(&windowClass);

    // The menu owner's class is registered here rather than per menu, so opening a menu never waits on registering it
    windowClass.lpfnWndProc = handleMenuEvents;
    windowClass.lpszClassName = MENU_CLASS_NAME;
    RegisterClassW(&windowClass);

    HWND createdWindow = CreateWindowExW(0, CLASS_NAME, L"", 0, 0, 0, 0, 0, NULL, NULL, GetModuleHandleW(NULL), NULL);
    bool published = false;

    {
        lock_guard<mutex> startupLock(startupMutex);

        /*
         * Publish only while this is still the current, running start. A stop or newer start that has already given
         * up on this thread will never post it a quit message, so it must end here instead of entering the loop
         */
        published = createdWindow && session == currentSession && isRunning.load();

        if (published) {
            messageWindow.store(createdWindow);
        }
    }

    if (!published) {
        DestroyWindow(createdWindow);
        finishMessageLoopThread(session);

        return;
    }

    iconOwnerWindow = createdWindow;

    /*
     * The shell broadcasts this registered message to top-level windows when it is recreated (explorer.exe restart).
     * The app runs elevated, so UIPI would drop it from the lower-integrity shell unless this window lets it through
     */
    taskbarCreatedMessage = RegisterWindowMessageW(L"TaskbarCreated");
    ChangeWindowMessageFilterEx(createdWindow, taskbarCreatedMessage, MSGFLT_ALLOW, NULL);

    applyPendingIcon();
    iconVisible = true;
    addTrayIcon();

    {
        lock_guard<mutex> startupLock(startupMutex);

        // A thread a newer start replaced stays silent, so it never reports success on that start's behalf
        if (session == currentSession) {
            startupSucceeded = true;
        }
    }

    startupSignal.notify_all();

    MSG message;

    // Blocking pump, since every action is message-driven and there is nothing to poll
    while (GetMessageW(&message, NULL, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }

    notifyShell(NIM_DELETE, 0);

    if (trayIcon) {
        DestroyIcon(trayIcon);
    }

    HWND publishedWindow = createdWindow;

    /*
     * Unpublished before the window is destroyed, so no caller posts to a dead handle, and only while it is still this
     * thread's window, so the window of a newer start is never cleared
     */
    messageWindow.compare_exchange_strong(publishedWindow, NULL);
    DestroyWindow(createdWindow);

    finishMessageLoopThread(session);
}

/**
 * Reports the message loop thread as finished and unregisters its window classes, unless a newer start has replaced
 * it, in which case the shared state and the classes belong to that start and are left alone.
 *
 * @param session
 *            - The number of the start that launched the finishing thread
 */
static void finishMessageLoopThread(unsigned long long session) {
    {
        lock_guard<mutex> startupLock(startupMutex);

        if (session != currentSession) {
            return;
        }

        isRunning.store(false);

        // A class whose window a closing menu still holds cannot be unregistered yet, so the next stop retries it
        UnregisterClassW(CLASS_NAME, GetModuleHandleW(NULL));
        UnregisterClassW(MENU_CLASS_NAME, GetModuleHandleW(NULL));

        // Announced last, so a waiting start or stop only joins this thread once there is nothing left for it to run
        messageLoopFinished = true;
    }

    startupSignal.notify_all();
}

/**
 * Handles the messages of the hidden window that owns the icon. These are the icon's own events, the work the calling
 * thread hands over, and the broadcast that re-adds the icon after the shell restarts.
 *
 * @param windowHandle
 *            - The handle to the hidden message window
 * @param message
 *            - The message identifier
 * @param eventType
 *            - The WPARAM event type
 * @param eventData
 *            - The LPARAM event data
 *
 * @return The result of message processing
 */
LRESULT CALLBACK handleTrayEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData) {
    // The registered TaskbarCreated id is resolved at runtime, so it cannot be a switch case
    if (message == taskbarCreatedMessage && taskbarCreatedMessage != 0) {
        if (iconVisible) {
            addTrayIcon();
        }

        return 0;
    }

    switch (message) {
    case WM_TRAY_SET_VISIBLE:
        // Only a change reaches the shell, so a repeated request never adds the icon twice
        if ((eventType != 0) != iconVisible) {
            iconVisible = eventType != 0;

            if (iconVisible) {
                addTrayIcon();
            } else {
                notifyShell(NIM_DELETE, 0);
            }
        }

        return 0;

    case WM_TRAY_APPLY_ICON:
        applyPendingIcon();
        return 0;

    case WM_TRAY_ICON_NOTIFY:
        // Either button opens the menu, so restoring always goes through the menu's own item
        if (LOWORD(eventData) == WM_LBUTTONUP || LOWORD(eventData) == WM_CONTEXTMENU) {
            openTrayMenu(eventType);
        }

        return 0;

    default:
        return DefWindowProcW(windowHandle, message, eventType, eventData);
    }
}

/**
 * Registers the icon with the shell and requests the modern callback behavior, which reports the event position and
 * delivers a context menu request for both right clicks and the keyboard menu key.
 */
static void addTrayIcon() {
    notifyShell(NIM_ADD, NIF_ICON | NIF_MESSAGE | NIF_TIP | NIF_SHOWTIP);
    notifyShell(NIM_SETVERSION, 0);
}

/**
 * Sends the shell a command about the current thread's icon. Every field the commands use is filled in, so each
 * command simply takes the ones its flags name.
 *
 * @param command
 *            - The NIM command to send
 * @param flags
 *            - The NIF flags naming the fields the command carries
 */
static void notifyShell(DWORD command, UINT flags) {
    NOTIFYICONDATAW iconData = {};
    iconData.cbSize = sizeof(iconData);
    iconData.hWnd = iconOwnerWindow;
    iconData.uID = TRAY_ICON_ID;
    iconData.uFlags = flags;
    iconData.uCallbackMessage = WM_TRAY_ICON_NOTIFY;
    iconData.hIcon = trayIcon;
    iconData.uVersion = NOTIFYICON_VERSION_4;

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);
        wcsncpy_s(iconData.szTip, pendingTooltip.c_str(), _TRUNCATE);
    }

    Shell_NotifyIconW(command, &iconData);
}

/**
 * Shows an icon built from the staged pixels. The previous icon is destroyed after the replacement lands, so the shell
 * never renders from a freed handle.
 */
static void applyPendingIcon() {
    HICON newIcon = NULL;

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);

        if (!pendingIconPixels.empty()) {
            newIcon = createIconFromPixels(pendingIconPixels, pendingIconWidth, pendingIconHeight);
        }
    }

    if (!newIcon) {
        return;
    }

    HICON previousIcon = trayIcon;
    trayIcon = newIcon;

    // The tooltip is restated alongside the icon, since an update only carries the fields it declares
    if (iconVisible) {
        notifyShell(NIM_MODIFY, NIF_ICON | NIF_TIP | NIF_SHOWTIP);
    }

    if (previousIcon) {
        DestroyIcon(previousIcon);
    }
}

/**
 * Creates an icon from pixels in packed ARGB order. The pixels are copied straight into a top-down 32-bit bitmap
 * carrying alpha, so no row flip or channel conversion is needed.
 *
 * @param pixels
 *            - The icon pixels in packed ARGB order, row by row from the top
 * @param width
 *            - The icon width in pixels
 * @param height
 *            - The icon height in pixels
 *
 * @return The created icon, or null when it could not be created
 */
static HICON createIconFromPixels(const vector<jint> &pixels, int width, int height) {
    BITMAPV5HEADER bitmapHeader = {};
    bitmapHeader.bV5Size = sizeof(bitmapHeader);
    bitmapHeader.bV5Width = width;

    // A negative height requests a top-down bitmap, matching the row order the pixels arrive in
    bitmapHeader.bV5Height = -height;
    bitmapHeader.bV5Planes = 1;
    bitmapHeader.bV5BitCount = 32;
    bitmapHeader.bV5Compression = BI_BITFIELDS;
    bitmapHeader.bV5RedMask = 0x00FF0000;
    bitmapHeader.bV5GreenMask = 0x0000FF00;
    bitmapHeader.bV5BlueMask = 0x000000FF;
    bitmapHeader.bV5AlphaMask = 0xFF000000;

    void *bitmapBits = nullptr;
    HBITMAP colorBitmap = CreateDIBSection(NULL, (BITMAPINFO *) &bitmapHeader, DIB_RGB_COLORS, &bitmapBits, NULL, 0);

    if (bitmapBits) {
        memcpy(bitmapBits, pixels.data(), pixels.size() * sizeof(jint));
    }

    // Monochrome rows are padded to whole 16-bit words, and an all-zero mask leaves the shape to the alpha channel
    vector<BYTE> maskBits((size_t) ((width + 15) / 16) * 2 * height, 0);
    HBITMAP maskBitmap = CreateBitmap(width, height, 1, 1, maskBits.data());
    ICONINFO iconInfo = {TRUE, 0, 0, maskBitmap, colorBitmap};
    HICON icon = bitmapBits && maskBitmap ? CreateIconIndirect(&iconInfo) : NULL;

    // The bitmaps are copied into the icon, so they are released either way
    DeleteObject(colorBitmap);
    DeleteObject(maskBitmap);

    return icon;
}

/**
 * Opens the menu where the icon event happened, on a thread of its own. The icon's thread keeps pumping its messages,
 * so the icon stays responsive however long the menu waits for the foreground or for a choice.
 *
 * @param anchorPosition
 *            - The WPARAM of the event, packing the anchor's x and y in physical screen pixels
 */
static void openTrayMenu(WPARAM anchorPosition) {
    // Only one menu is open at a time, so a request arriving while one is showing is dropped
    if (menuShowing.exchange(true)) {
        return;
    }

    // Detached rather than joined, since the thread lives for as long as the menu stays open
    try {
        thread(showTrayMenu, GET_X_LPARAM(anchorPosition), GET_Y_LPARAM(anchorPosition)).detach();
    } catch (const system_error &) {
        menuShowing.store(false);
    }
}

/**
 * Shows the menu on the calling thread and reports the chosen item once the menu has closed. The report comes last so
 * the menu is fully torn down before the chosen action runs.
 *
 * @param anchorX
 *            - The x coordinate to anchor the menu to, in physical screen pixels
 * @param anchorY
 *            - The y coordinate to anchor the menu to, in physical screen pixels
 */
static void showTrayMenu(int anchorX, int anchorY) {
    shared_ptr<const TrayMenuDefinition> definition;

    {
        lock_guard<mutex> definitionLock(menuDefinitionMutex);
        definition = menuDefinition;
    }

    int selectedItem = definition ? trackTrayMenu(*definition, anchorX, anchorY) : -1;

    menuShowing.store(false);

    if (selectedItem >= 0) {
        invokeMenuItemSelectedCallback(selectedItem);
    }
}

/**
 * Shows the menu at the anchor and waits for it to close. The system runs the menu, so it opens, tracks the pointer,
 * and closes on an outside click or the escape key without involving any other thread.
 *
 * @param definition
 *            - The menu to show
 * @param anchorX
 *            - The x coordinate to anchor the menu to, in physical screen pixels
 * @param anchorY
 *            - The y coordinate to anchor the menu to, in physical screen pixels
 *
 * @return The index of the chosen item, or -1 when the menu closed without a choice
 */
static int trackTrayMenu(const TrayMenuDefinition &definition, int anchorX, int anchorY) {
    HWND ownerWindow = CreateWindowExW(WS_EX_TOOLWINDOW, MENU_CLASS_NAME, L"", WS_POPUP, 0, 0, 0, 0, NULL, NULL,
                                       GetModuleHandleW(NULL), NULL);

    if (!ownerWindow) {
        return -1;
    }

    // Published before the menu opens, so a dismissal requested in the meantime waits in the queue and closes it
    menuWindow.store(ownerWindow);

    POINT anchor = {anchorX, anchorY};
    HMONITOR anchorMonitor = MonitorFromPoint(anchor, MONITOR_DEFAULTTONEAREST);
    UINT dpiY = 0;

    // Sized for the display it opens on, which can differ in scale from every other display
    if (GetDpiForMonitor(anchorMonitor, MDT_EFFECTIVE_DPI, &activeMenuDpi, &dpiY) != S_OK) {
        activeMenuDpi = USER_DEFAULT_SCREEN_DPI;
    }

    LOGFONTW logFont = {};

    // A negative height requests the font size itself rather than the taller line height, matching the size supplied
    logFont.lfHeight = -MulDiv(definition.fontSize, activeMenuDpi, USER_DEFAULT_SCREEN_DPI);
    logFont.lfWeight = FW_NORMAL;
    logFont.lfCharSet = DEFAULT_CHARSET;
    logFont.lfQuality = CLEARTYPE_QUALITY;
    wcsncpy_s(logFont.lfFaceName, definition.fontFamily.c_str(), _TRUNCATE);

    HMENU menu = CreatePopupMenu();
    HBRUSH backgroundBrush = CreateSolidBrush(definition.backgroundColor);
    MENUINFO menuInfo = {};
    menuInfo.cbSize = sizeof(menuInfo);
    menuInfo.fMask = MIM_BACKGROUND;
    menuInfo.hbrBack = backgroundBrush;
    SetMenuInfo(menu, &menuInfo);

    // Commands are numbered from one, because the menu reports zero when it closes without a choice
    for (size_t itemIndex = 0; itemIndex < definition.itemTexts.size(); itemIndex++) {
        AppendMenuW(menu, MF_OWNERDRAW, itemIndex + 1, nullptr);
    }

    activeMenuDefinition = &definition;
    activeMenuFont = CreateFontIndirectW(&logFont);
    applyMenuAppMode(definition.backgroundColor);

    // Scoped to this thread, so it only ever sees the menu window this thread is about to create
    HHOOK menuStyleHook = SetWindowsHookExW(WH_CALLWNDPROC, styleMenuWindow, NULL, GetCurrentThreadId());

    /*
     * The menu only closes on an outside click while its owner is the foreground window. Claiming it here rather
     * than on the thread owning the icon means a foreground window slow to let go only delays the menu
     */
    SetForegroundWindow(ownerWindow);

    UINT command = (UINT) TrackPopupMenuEx(menu, trayMenuAlignment(anchorMonitor) | TPM_RIGHTBUTTON | TPM_RETURNCMD,
                                           anchorX, anchorY, ownerWindow, NULL);

    UnhookWindowsHookEx(menuStyleHook);
    DestroyMenu(menu);

    // Cleared before the window is destroyed, so a dismissal can never post to a destroyed or recycled handle
    menuWindow.store(NULL);
    DestroyWindow(ownerWindow);
    DeleteObject(backgroundBrush);
    DeleteObject(activeMenuFont);

    return (int) command - 1;
}

/**
 * Draws the process's menus dark or light to match the menu background, so the frame the system draws around the
 * items matches them. The exports are undocumented, so a system without them simply keeps its own frame.
 *
 * @param backgroundColor
 *            - The menu background color
 */
static void applyMenuAppMode(COLORREF backgroundColor) {
    // Weighted by how bright each channel appears, so a saturated blue does not read as light
    int luminance =
        (GetRValue(backgroundColor) * 299 + GetGValue(backgroundColor) * 587 + GetBValue(backgroundColor) * 114) / 1000;
    int appMode = luminance < DARK_LUMINANCE_THRESHOLD ? APP_MODE_FORCE_DARK : APP_MODE_FORCE_LIGHT;

    if (appliedAppMode.exchange(appMode) == appMode) {
        return;
    }

    // Loaded once and kept for the life of the process, so repeated theme changes never stack up module references
    static const HMODULE uxThemeModule = LoadLibraryExW(L"uxtheme.dll", NULL, LOAD_LIBRARY_SEARCH_SYSTEM32);

    if (!uxThemeModule) {
        return;
    }

    // Cast through a plain pointer, since the exports' real signatures differ from the generic procedure type
    static const SetPreferredAppModeFunction setPreferredAppMode =
        (SetPreferredAppModeFunction) (void *) GetProcAddress(uxThemeModule,
                                                              MAKEINTRESOURCEA(SET_PREFERRED_APP_MODE_ORDINAL));
    static const FlushMenuThemesFunction flushMenuThemes =
        (FlushMenuThemesFunction) (void *) GetProcAddress(uxThemeModule, MAKEINTRESOURCEA(FLUSH_MENU_THEMES_ORDINAL));

    if (setPreferredAppMode && flushMenuThemes) {
        setPreferredAppMode(appMode);
        flushMenuThemes();
    }
}

/**
 * Colors the border of the menu window and rounds its corners as the system creates it, before it is ever shown.
 * Windows 10 rejects both requests, so its menus keep their own square border.
 *
 * @param hookCode
 *            - Whether the hook should process the message
 * @param messageSource
 *            - Whether the message was sent by the current thread
 * @param messageData
 *            - The message being delivered
 *
 * @return The result of the next hook in the chain
 */
static LRESULT CALLBACK styleMenuWindow(int hookCode, WPARAM messageSource, LPARAM messageData) {
    const CWPSTRUCT *message = (const CWPSTRUCT *) messageData;

    if (hookCode == HC_ACTION && message->message == WM_CREATE &&
        GetClassWord(message->hwnd, GCW_ATOM) == MENU_WINDOW_CLASS_ATOM) {
        COLORREF borderColor = activeMenuDefinition->borderColor;

        // Requested explicitly, since Windows 11 leaves an owner drawn menu square unless asked to round it
        DWM_WINDOW_CORNER_PREFERENCE cornerPreference = DWMWCP_ROUNDSMALL;

        DwmSetWindowAttribute(message->hwnd, DWMWA_BORDER_COLOR, &borderColor, sizeof(borderColor));
        DwmSetWindowAttribute(message->hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, &cornerPreference,
                              sizeof(cornerPreference));
    }

    return CallNextHookEx(NULL, hookCode, messageSource, messageData);
}

/**
 * Picks the alignment that grows the menu away from the task bar on the given display, the way the system aligns its
 * own tray menus. The system still turns the menu back on whichever side it would not fit.
 *
 * @param anchorMonitor
 *            - The display the menu opens on
 *
 * @return The horizontal and vertical alignment flags for the menu
 */
static UINT trayMenuAlignment(HMONITOR anchorMonitor) {
    MONITORINFO monitorInfo = {};
    monitorInfo.cbSize = sizeof(monitorInfo);

    if (!GetMonitorInfoW(anchorMonitor, &monitorInfo)) {
        return TPM_LEFTALIGN | TPM_BOTTOMALIGN;
    }

    // The task bar sits on the edge where the work area falls short of the display
    UINT horizontalAlignment = monitorInfo.rcWork.right < monitorInfo.rcMonitor.right ? TPM_RIGHTALIGN : TPM_LEFTALIGN;
    UINT verticalAlignment = monitorInfo.rcWork.bottom < monitorInfo.rcMonitor.bottom ? TPM_BOTTOMALIGN : TPM_TOPALIGN;

    return horizontalAlignment | verticalAlignment;
}

/**
 * Handles the messages of the hidden window that owns an open menu, sizing and painting its owner drawn items and
 * closing the menu when another thread asks.
 *
 * @param windowHandle
 *            - The handle to the hidden menu owner window
 * @param message
 *            - The message identifier
 * @param eventType
 *            - The WPARAM event type
 * @param eventData
 *            - The LPARAM event data
 *
 * @return The result of message processing
 */
LRESULT CALLBACK handleMenuEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData) {
    switch (message) {
    case WM_MEASUREITEM: {
        MEASUREITEMSTRUCT *measureItem = (MEASUREITEMSTRUCT *) eventData;
        const TrayMenuDefinition &definition = *activeMenuDefinition;
        int itemWidth = MulDiv(definition.itemWidth, activeMenuDpi, USER_DEFAULT_SCREEN_DPI);

        // The system pads the width of every owner drawn item by about a check mark, so that much is taken back here
        int measuredWidth = itemWidth - GetSystemMetricsForDpi(SM_CXMENUCHECK, activeMenuDpi) + 1;

        measureItem->itemWidth = (UINT) (measuredWidth > 0 ? measuredWidth : itemWidth);
        measureItem->itemHeight = (UINT) MulDiv(definition.itemHeight, activeMenuDpi, USER_DEFAULT_SCREEN_DPI);

        return TRUE;
    }

    case WM_DRAWITEM:
        drawMenuItem(*(const DRAWITEMSTRUCT *) eventData);
        return TRUE;

    case WM_TRAY_CLOSE_MENU:
        EndMenu();
        return 0;

    default:
        return DefWindowProcW(windowHandle, message, eventType, eventData);
    }
}

/**
 * Paints an item in the supplied colors and font, highlighted while the pointer or keyboard has it selected.
 *
 * @param drawItem
 *            - The paint request for the item
 */
static void drawMenuItem(const DRAWITEMSTRUCT &drawItem) {
    const TrayMenuDefinition &definition = *activeMenuDefinition;
    const wstring &itemText = definition.itemTexts[drawItem.itemID - 1];
    bool selected = (drawItem.itemState & ODS_SELECTED) != 0;
    HDC deviceContext = drawItem.hDC;
    RECT textRect = drawItem.rcItem;

    textRect.left += MulDiv(definition.textInset, activeMenuDpi, USER_DEFAULT_SCREEN_DPI);

    // The device context belongs to the menu, so everything changed on it is put back afterward
    int savedState = SaveDC(deviceContext);

    SetDCBrushColor(deviceContext, selected ? definition.selectionBackgroundColor : definition.backgroundColor);
    FillRect(deviceContext, &drawItem.rcItem, (HBRUSH) GetStockObject(DC_BRUSH));
    SelectObject(deviceContext, activeMenuFont);
    SetBkMode(deviceContext, TRANSPARENT);
    SetTextColor(deviceContext, selected ? definition.selectionForegroundColor : definition.foregroundColor);
    DrawTextW(deviceContext, itemText.c_str(), (int) itemText.size(), &textRect,
              DT_LEFT | DT_VCENTER | DT_SINGLELINE | DT_NOPREFIX | DT_END_ELLIPSIS);

    RestoreDC(deviceContext, savedState);
}

/**
 * Invokes the item selection callback on the registered instance from the menu's own thread, which is attached to the
 * JVM only for the call. The callback reference is held under its lock throughout, so a stop cannot delete it.
 *
 * @param itemIndex
 *            - The index of the chosen item
 */
static void invokeMenuItemSelectedCallback(int itemIndex) {
    lock_guard<mutex> callbackLock(callbackMutex);
    JNIEnv *env = nullptr;

    if (!jvm || !systemTrayIconGlobalRef || jvm->AttachCurrentThread((void **) &env, nullptr) != JNI_OK) {
        return;
    }

    env->CallVoidMethod(systemTrayIconGlobalRef, onMenuItemSelectedMethodId, (jint) itemIndex);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    jvm->DetachCurrentThread();
}
