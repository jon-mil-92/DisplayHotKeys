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

// Windows must be included before the shell and control headers, which depend on its types and macros
#include <windows.h>

#include <atomic>
#include <condition_variable>
#include <jni.h>
#include <mutex>
#include <shellapi.h>
#include <shellscalingapi.h>
#include <string>
#include <thread>
#include <vector>
#include <windowsx.h>

using namespace std;

static UINT getNotificationAreaDpi();
static void runMessageLoopThread();
LRESULT CALLBACK handleTrayEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void addTrayIcon();
static void removeTrayIcon();
static void applyPendingIcon();
static void reportMenuRequested();
static HICON createIconFromPixels(const vector<jint> &pixels, int width, int height);
static void invokeMenuRequestedCallback(int anchorX, int anchorY, const RECT &iconBounds);

/**
 * Global JavaVM pointer used to attach native threads when invoking callbacks.
 */
static JavaVM *jvm = nullptr;

/**
 * Global reference to the registered instance used for callbacks from native code.
 */
static jobject systemTrayIconGlobalRef = nullptr;

/**
 * Cached method ID for the onMenuRequested(int, int, int, int, int, int) callback.
 */
static jmethodID onMenuRequestedMethodId = nullptr;

/**
 * Thread that runs the Windows message loop owning the icon.
 */
static thread messageLoopThread;

/**
 * Atomic flag indicating whether the message loop thread is running.
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
 * Window class name of the task bar, used to read its position without waiting on the shell to answer for it.
 */
static const wchar_t TASKBAR_CLASS_NAME[] = L"Shell_TrayWnd";

/**
 * Whether the window class was registered, so it can be unregistered at shutdown.
 */
static atomic_bool classRegistered(false);

/**
 * Registered id of the "TaskbarCreated" broadcast the shell sends.
 */
static UINT taskbarCreatedMessage = 0;

/**
 * Icon currently shown in the notification area, owned by the message loop thread.
 */
static HICON trayIcon = NULL;

/**
 * Whether the icon should currently be registered with the shell, so a shell restart only re-adds a shown icon.
 */
static atomic_bool iconVisible(false);

/**
 * Guards the staged tooltip and icon payloads handed over from the calling thread.
 */
static mutex pendingPayloadMutex;

/**
 * Signals the calling thread that the message loop thread finished its startup attempt.
 */
static condition_variable startupSignal;

/**
 * Guards the startup completion state.
 */
static mutex startupMutex;

/**
 * Whether the message loop thread finished its startup attempt, whether or not it succeeded.
 */
static bool startupComplete = false;

/**
 * Whether the icon was registered with the shell during startup.
 */
static bool startupSucceeded = false;

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
 * Identifier distinguishing the icon within the owning window.
 */
static constexpr UINT TRAY_ICON_ID = 1;

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
 * Starts the notification area icon. Stores a global reference to the registered instance, resolves the callback,
 * stages the initial tooltip and icon, and launches the message loop thread that owns the icon.
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
 * @return Whether the icon was registered
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_dhk_io_SystemTrayIcon_nativeStart(JNIEnv *env, jobject obj,
                                                                                jstring tooltip, jintArray iconPixels,
                                                                                jint iconWidth, jint iconHeight) {
    if (isRunning.load()) {
        return JNI_TRUE;
    }

    jobject globalRef = env->NewGlobalRef(obj);

    if (!globalRef) {
        return JNI_FALSE;
    }

    jclass trayIconClass = env->GetObjectClass(obj);

    if (!trayIconClass) {
        env->DeleteGlobalRef(globalRef);
        return JNI_FALSE;
    }

    jmethodID menuRequestedMethodId = env->GetMethodID(trayIconClass, "onMenuRequested", "(IIIIII)V");

    if (!menuRequestedMethodId) {
        env->ExceptionClear();
        env->DeleteGlobalRef(globalRef);

        return JNI_FALSE;
    }

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);

        pendingTooltip.clear();

        if (tooltip) {
            const jchar *tooltipChars = env->GetStringChars(tooltip, nullptr);

            if (tooltipChars) {
                pendingTooltip.assign((const wchar_t *) tooltipChars, env->GetStringLength(tooltip));
                env->ReleaseStringChars(tooltip, tooltipChars);
            }
        }

        pendingIconPixels.clear();
        pendingIconWidth = 0;
        pendingIconHeight = 0;

        if (iconPixels && iconWidth > 0 && iconHeight > 0) {
            jsize pixelCount = env->GetArrayLength(iconPixels);

            if (pixelCount >= (jsize) (iconWidth * iconHeight)) {
                pendingIconPixels.resize(iconWidth * iconHeight);
                env->GetIntArrayRegion(iconPixels, 0, iconWidth * iconHeight, pendingIconPixels.data());
                pendingIconWidth = iconWidth;
                pendingIconHeight = iconHeight;
            }
        }
    }

    // All JNI setup succeeded; commit to globals and start thread
    systemTrayIconGlobalRef = globalRef;
    onMenuRequestedMethodId = menuRequestedMethodId;

    {
        lock_guard<mutex> startupLock(startupMutex);
        startupComplete = false;
        startupSucceeded = false;
    }

    isRunning.store(true);
    messageLoopThread = thread(runMessageLoopThread);

    // Wait for the icon to be registered so the return value reflects the real outcome
    bool started = false;

    {
        unique_lock<mutex> startupLock(startupMutex);
        startupSignal.wait(startupLock, [] { return startupComplete; });
        started = startupSucceeded;
    }

    // Reap a failed start, so a later attempt is not assigning over a joinable thread
    if (!started && messageLoopThread.joinable()) {
        messageLoopThread.join();
    }

    return started ? JNI_TRUE : JNI_FALSE;
}

/**
 * Stops the notification area icon, shuts down the message loop thread, and releases the global reference.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_SystemTrayIcon_nativeStop(JNIEnv *env, jobject obj) {
    (void) obj;

    if (!isRunning.load()) {
        return;
    }

    isRunning.store(false);

    HWND targetWindow = messageWindow.load();

    if (targetWindow) {
        PostMessage(targetWindow, WM_QUIT, 0, 0);
    }

    if (messageLoopThread.joinable()) {
        messageLoopThread.join();
    }

    if (systemTrayIconGlobalRef) {
        env->DeleteGlobalRef(systemTrayIconGlobalRef);
        systemTrayIconGlobalRef = NULL;
    }

    onMenuRequestedMethodId = nullptr;
}

/**
 * Replaces the icon shown in the notification area. The pixels are staged here and applied on the thread that owns the
 * icon, since the shell interface is only safe to call from that thread.
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
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_SystemTrayIcon_nativeSetIcon(JNIEnv *env, jobject obj,
                                                                              jintArray iconPixels, jint iconWidth,
                                                                              jint iconHeight) {
    (void) obj;

    HWND targetWindow = messageWindow.load();

    if (!targetWindow || !iconPixels || iconWidth <= 0 || iconHeight <= 0) {
        return;
    }

    jsize pixelCount = env->GetArrayLength(iconPixels);

    if (pixelCount < (jsize) (iconWidth * iconHeight)) {
        return;
    }

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);

        pendingIconPixels.resize(iconWidth * iconHeight);
        env->GetIntArrayRegion(iconPixels, 0, iconWidth * iconHeight, pendingIconPixels.data());
        pendingIconWidth = iconWidth;
        pendingIconHeight = iconHeight;
    }

    PostMessage(targetWindow, WM_TRAY_APPLY_ICON, 0, 0);
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

    if (!targetWindow) {
        return;
    }

    PostMessage(targetWindow, WM_TRAY_SET_VISIBLE, visible == JNI_TRUE ? 1 : 0, 0);
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

    int iconSize[2];
    UINT notificationAreaDpi = getNotificationAreaDpi();

    if (notificationAreaDpi != 0) {
        iconSize[0] = GetSystemMetricsForDpi(SM_CXSMICON, notificationAreaDpi);
        iconSize[1] = GetSystemMetricsForDpi(SM_CYSMICON, notificationAreaDpi);
    } else {
        iconSize[0] = GetSystemMetrics(SM_CXSMICON);
        iconSize[1] = GetSystemMetrics(SM_CYSMICON);
    }

    if (iconSize[0] <= 0 || iconSize[1] <= 0) {
        return nullptr;
    }

    jintArray iconSizeArray = env->NewIntArray(2);

    if (!iconSizeArray) {
        return nullptr;
    }

    env->SetIntArrayRegion(iconSizeArray, 0, 2, iconSize);

    return iconSizeArray;
}

/**
 * Called when the native library is unloaded. Releases global references and clears the JVM pointer to avoid leaks.
 *
 * @param vm
 *            - The JavaVM pointer
 * @param reserved
 *            - Reserved for future use
 */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;

    isRunning.store(false);

    HWND unloadWindow = messageWindow.load();

    if (unloadWindow) {
        PostMessage(unloadWindow, WM_QUIT, 0, 0);
    }

    if (messageLoopThread.joinable()) {
        messageLoopThread.join();
    }

    if (systemTrayIconGlobalRef && jvm) {
        JNIEnv *env = nullptr;

        if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK && env) {
            env->DeleteGlobalRef(systemTrayIconGlobalRef);
        }

        systemTrayIconGlobalRef = NULL;
    }

    onMenuRequestedMethodId = nullptr;
    jvm = nullptr;
}

/**
 * Gets the DPI of the display hosting the notification area, which is not necessarily the primary display. Sizing the
 * icon from the system DPI would leave it scaled for the wrong display whenever the taskbar sits on another one.
 *
 * @return The DPI of the display hosting the notification area, or 0 when it is unavailable
 */
static UINT getNotificationAreaDpi() {
    HWND taskbarWindow = FindWindowW(TASKBAR_CLASS_NAME, NULL);

    if (!taskbarWindow) {
        return 0;
    }

    /*
     * Read the task bar's rectangle from the window itself rather than through the shell's app bar interface. That
     * interface answers on the shell's own thread, which blocks for as long as the shell takes to finish rebuilding
     * the task bar after a display change - exactly when this is asked
     */
    RECT taskbarRect = {};

    if (!GetWindowRect(taskbarWindow, &taskbarRect)) {
        return 0;
    }

    HMONITOR taskbarMonitor = MonitorFromRect(&taskbarRect, MONITOR_DEFAULTTONEAREST);

    if (!taskbarMonitor) {
        return 0;
    }

    UINT dpiX = 0;
    UINT dpiY = 0;

    if (GetDpiForMonitor(taskbarMonitor, MDT_EFFECTIVE_DPI, &dpiX, &dpiY) != S_OK) {
        return 0;
    }

    return dpiX;
}

/**
 * Message loop runner executed on a dedicated thread. Creates a hidden window that owns the icon, registers the icon
 * with the shell, and pumps messages until asked to stop. This thread exclusively owns the icon and its handle.
 */
static void runMessageLoopThread() {
    WNDCLASSW windowClass = {};
    windowClass.lpfnWndProc = handleTrayEvents;
    windowClass.hInstance = GetModuleHandleW(NULL);
    windowClass.lpszClassName = CLASS_NAME;

    if (RegisterClassW(&windowClass) != 0) {
        classRegistered.store(true);
    }

    HWND createdWindow = CreateWindowExW(0, CLASS_NAME, L"", 0, 0, 0, 0, 0, NULL, NULL, GetModuleHandleW(NULL), NULL);
    messageWindow.store(createdWindow);

    if (!createdWindow) {
        if (classRegistered.load()) {
            UnregisterClassW(CLASS_NAME, GetModuleHandleW(NULL));
            classRegistered.store(false);
        }

        isRunning.store(false);

        {
            lock_guard<mutex> startupLock(startupMutex);
            startupComplete = true;
            startupSucceeded = false;
        }

        startupSignal.notify_all();

        return;
    }

    // The shell broadcasts this registered message to top-level windows when it is recreated (explorer.exe restart)
    taskbarCreatedMessage = RegisterWindowMessageW(L"TaskbarCreated");

    /*
     * The app runs elevated, so UIPI would drop this broadcast from the lower-integrity shell by default. Allow the
     * specific message through the per-window filter so the restart is still delivered
     */
    if (taskbarCreatedMessage != 0) {
        ChangeWindowMessageFilterEx(createdWindow, taskbarCreatedMessage, MSGFLT_ALLOW, NULL);
    }

    applyPendingIcon();

    iconVisible.store(true);
    addTrayIcon();

    {
        lock_guard<mutex> startupLock(startupMutex);
        startupComplete = true;
        startupSucceeded = true;
    }

    startupSignal.notify_all();

    MSG message;

    // Blocking pump, since every action is message-driven and there is nothing to poll
    while (GetMessage(&message, NULL, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessage(&message);
    }

    isRunning.store(false);

    removeTrayIcon();

    if (trayIcon) {
        DestroyIcon(trayIcon);
        trayIcon = NULL;
    }

    HWND windowToDestroy = messageWindow.exchange(NULL);

    // Cleared before the window is destroyed, so a caller can never post to a destroyed or recycled handle
    if (windowToDestroy) {
        DestroyWindow(windowToDestroy);
    }

    if (classRegistered.load()) {
        UnregisterClassW(CLASS_NAME, GetModuleHandleW(NULL));
        classRegistered.store(false);
    }
}

/**
 * Window procedure for the hidden message window. Handles the icon's callback message, the private messages that hand
 * work over from the calling thread, and re-adds the icon when the shell is recreated.
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
    if (taskbarCreatedMessage != 0 && message == taskbarCreatedMessage) {
        if (iconVisible.load()) {
            addTrayIcon();
        }

        return 0;
    }

    switch (message) {
    case WM_TRAY_ICON_NOTIFY: {
        UINT iconEvent = LOWORD(eventData);

        // Either button opens the menu, so restoring always goes through the menu's own item
        if (iconEvent == WM_LBUTTONUP || iconEvent == WM_CONTEXTMENU) {
            reportMenuRequested();
        }

        return 0;
    }

    case WM_TRAY_APPLY_ICON:
        applyPendingIcon();
        return 0;

    case WM_TRAY_SET_VISIBLE: {
        bool shouldShow = eventType != 0;

        if (shouldShow != iconVisible.load()) {
            iconVisible.store(shouldShow);

            if (shouldShow) {
                addTrayIcon();
            } else {
                removeTrayIcon();
            }
        }

        return 0;
    }

    case WM_DESTROY:
        PostQuitMessage(0);
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
    NOTIFYICONDATAW iconData = {};
    iconData.cbSize = sizeof(iconData);
    iconData.hWnd = messageWindow.load();
    iconData.uID = TRAY_ICON_ID;
    iconData.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP | NIF_SHOWTIP;
    iconData.uCallbackMessage = WM_TRAY_ICON_NOTIFY;
    iconData.hIcon = trayIcon;

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);
        wcsncpy_s(iconData.szTip, pendingTooltip.c_str(), _TRUNCATE);
    }

    Shell_NotifyIconW(NIM_ADD, &iconData);

    iconData.uVersion = NOTIFYICON_VERSION_4;
    Shell_NotifyIconW(NIM_SETVERSION, &iconData);
}

/**
 * Removes the icon from the notification area, leaving the icon handle intact so it can be shown again.
 */
static void removeTrayIcon() {
    NOTIFYICONDATAW iconData = {};
    iconData.cbSize = sizeof(iconData);
    iconData.hWnd = messageWindow.load();
    iconData.uID = TRAY_ICON_ID;

    Shell_NotifyIconW(NIM_DELETE, &iconData);
}

/**
 * Builds an icon from the staged pixels and shows it, destroying the previous icon only after the replacement is in
 * place so the shell never renders from a freed handle.
 */
static void applyPendingIcon() {
    vector<jint> pixels;
    int width = 0;
    int height = 0;

    {
        lock_guard<mutex> payloadLock(pendingPayloadMutex);

        if (pendingIconPixels.empty() || pendingIconWidth <= 0 || pendingIconHeight <= 0) {
            return;
        }

        pixels = pendingIconPixels;
        width = pendingIconWidth;
        height = pendingIconHeight;
    }

    HICON newIcon = createIconFromPixels(pixels, width, height);

    if (!newIcon) {
        return;
    }

    HICON previousIcon = trayIcon;
    trayIcon = newIcon;

    if (iconVisible.load()) {
        NOTIFYICONDATAW iconData = {};
        iconData.cbSize = sizeof(iconData);
        iconData.hWnd = messageWindow.load();
        iconData.uID = TRAY_ICON_ID;
        iconData.uFlags = NIF_ICON;
        iconData.hIcon = trayIcon;

        Shell_NotifyIconW(NIM_MODIFY, &iconData);
    }

    if (previousIcon) {
        DestroyIcon(previousIcon);
    }
}

/**
 * Reports a menu request with the cursor position and the icon's bounds, so the caller can anchor to the icon itself
 * rather than to the cursor, which can sit away from the icon when it is in the overflow area.
 */
static void reportMenuRequested() {
    POINT cursorPosition = {};

    if (!GetCursorPos(&cursorPosition)) {
        cursorPosition.x = 0;
        cursorPosition.y = 0;
    }

    NOTIFYICONIDENTIFIER iconIdentifier = {};
    iconIdentifier.cbSize = sizeof(iconIdentifier);
    iconIdentifier.hWnd = messageWindow.load();
    iconIdentifier.uID = TRAY_ICON_ID;

    RECT iconBounds = {};

    // Fall back to the cursor position so the caller still has a usable anchor when the bounds are unavailable
    if (Shell_NotifyIconGetRect(&iconIdentifier, &iconBounds) != S_OK) {
        iconBounds.left = cursorPosition.x;
        iconBounds.top = cursorPosition.y;
        iconBounds.right = cursorPosition.x;
        iconBounds.bottom = cursorPosition.y;
    }

    /*
     * Claim the foreground while the click still grants the right to take it. A menu shown by a process that does not
     * own the foreground never receives the click that should dismiss it, so it would linger until one of its items is
     * chosen
     */
    SetForegroundWindow(messageWindow.load());

    invokeMenuRequestedCallback(cursorPosition.x, cursorPosition.y, iconBounds);
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

    HDC screenDeviceContext = GetDC(NULL);

    if (!screenDeviceContext) {
        return NULL;
    }

    void *bitmapBits = nullptr;
    HBITMAP colorBitmap = CreateDIBSection(screenDeviceContext, (BITMAPINFO *) &bitmapHeader, DIB_RGB_COLORS,
                                           &bitmapBits, NULL, 0);

    ReleaseDC(NULL, screenDeviceContext);

    if (!colorBitmap || !bitmapBits) {
        if (colorBitmap) {
            DeleteObject(colorBitmap);
        }

        return NULL;
    }

    memcpy(bitmapBits, pixels.data(), (size_t) width * (size_t) height * sizeof(jint));

    // The mask is unused for a 32-bit bitmap carrying alpha, but the icon still requires one
    HBITMAP maskBitmap = CreateBitmap(width, height, 1, 1, NULL);

    if (!maskBitmap) {
        DeleteObject(colorBitmap);
        return NULL;
    }

    ICONINFO iconInfo = {};
    iconInfo.fIcon = TRUE;
    iconInfo.hbmColor = colorBitmap;
    iconInfo.hbmMask = maskBitmap;

    HICON icon = CreateIconIndirect(&iconInfo);

    // The bitmaps are copied into the icon, so they are released immediately either way
    DeleteObject(colorBitmap);
    DeleteObject(maskBitmap);

    return icon;
}

/**
 * Invokes the menu request callback on the registered instance, attaching the current thread to the JVM if necessary
 * and detaching it when done.
 *
 * @param anchorX
 *            - The x coordinate to anchor the menu to, in physical screen pixels
 * @param anchorY
 *            - The y coordinate to anchor the menu to, in physical screen pixels
 * @param iconBounds
 *            - The bounds of the icon, in physical screen pixels
 */
static void invokeMenuRequestedCallback(int anchorX, int anchorY, const RECT &iconBounds) {
    if (!jvm || !systemTrayIconGlobalRef || !onMenuRequestedMethodId) {
        return;
    }

    JNIEnv *env = nullptr;
    bool attachedToJvm = false;
    jint getEnvResult = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (getEnvResult == JNI_EDETACHED) {
        if (jvm->AttachCurrentThread((void **) &env, nullptr) != 0) {
            return;
        }

        attachedToJvm = true;
    } else if (getEnvResult != JNI_OK) {
        return;
    }

    // Re-check the global ref after attaching
    if (!systemTrayIconGlobalRef) {
        if (attachedToJvm) {
            jvm->DetachCurrentThread();
        }

        return;
    }

    env->CallVoidMethod(systemTrayIconGlobalRef, onMenuRequestedMethodId, (jint) anchorX, (jint) anchorY,
                        (jint) iconBounds.left, (jint) iconBounds.top, (jint) iconBounds.right,
                        (jint) iconBounds.bottom);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    if (attachedToJvm) {
        jvm->DetachCurrentThread();
    }
}
