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
#include "com_dhk_io_DisplayEventNotifier.h"
#include "DisplayConfig.h"

#include <algorithm>
#include <atomic>
#include <cctype>
#include <chrono>
#include <dbt.h>
#include <jni.h>
#include <string>
#include <thread>
#include <vector>
#include <windows.h>

using namespace std;

/**
 * Global Java VM pointer used to attach native threads when invoking Java callbacks.
 */
static JavaVM *jvm = nullptr;

/**
 * Global reference to the Java DisplayEventNotifier instance used for callbacks from native code.
 */
static jobject displayEventNotifierGlobalRef = nullptr;

/**
 * Cached method ID for the Java callback DisplayEventNotifier.onNativeNotify().
 */
static jmethodID onNativeNotifyMethodId = nullptr;

/**
 * Cached method ID for the Java callback DisplayEventNotifier.onShellRestart(); null when the method is unavailable.
 */
static jmethodID onShellRestartMethodId = nullptr;

/**
 * Thread that runs the Windows message loop and polling logic.
 */
static thread messageLoopThread;

/**
 * Atomic flag indicating whether the message loop thread should continue running.
 */
static atomic_bool isRunning(false);

/**
 * Hidden message window used to receive broadcasts such as WM_DEVICECHANGE and WM_DISPLAYCHANGE.
 */
static HWND messageWindow = NULL;

/**
 * Handle returned by RegisterDeviceNotification for monitor device interface notifications.
 */
static HDEVNOTIFY deviceNotificationHandle = NULL;

/**
 * Registered window class name for the hidden message window.
 */
static const wchar_t CLASS_NAME[] = L"DHK_DisplayEventNotifier_MessageWindow";

/**
 * Whether the window class was registered, so it can be unregistered at shutdown.
 */
static atomic_bool classRegistered(false);

/**
 * Registered id of the "TaskbarCreated" broadcast the shell sends.
 */
static UINT taskbarCreatedMessage = 0;

/**
 * Debounce interval (ms) limiting how often Java can be notified, even after stabilization.
 */
static constexpr long DEBOUNCE_MS = 200;

/**
 * Stabilization interval (ms) the display configuration must stay unchanged before notifying Java.
 */
static constexpr long STABILIZATION_MS = 600;

/**
 * Polling interval (ms) for querying visible display paths, balancing CPU usage against responsiveness.
 */
static constexpr long POLL_INTERVAL_MS = 400;

/**
 * Timestamp of the last notification forwarded to Java, used for debounce logic.
 */
static chrono::steady_clock::time_point lastNotifyTime;

/**
 * Timestamp of the last time the visible display ID set changed, used for stabilization logic.
 */
static chrono::steady_clock::time_point lastStateChangeTime;

/**
 * Timestamp of the last poll of the display configuration, used to control polling frequency.
 */
static chrono::steady_clock::time_point lastPollTime;

/**
 * Last-known visible display signatures (stable ID plus resolution, position, rotation, DPI scale) used to detect real
 * configuration changes, including resolution, DPI, and orientation.
 */
static vector<string> lastVisibleSignatures;

/**
 * Cached normalized version of lastVisibleSignatures to avoid re-normalizing repeatedly.
 */
static vector<string> lastNormalizedSignatures;

/**
 * Last normalized set actually notified to Java, preventing repeated notifications for the same stable config.
 */
static vector<string> lastNotifiedNormalizedSignatures;

/**
 * Windows-universal monitor device interface class GUID (GUID_DEVINTERFACE_MONITOR), identical on every machine and
 * OS version. Used only as the RegisterDeviceNotification class filter for monitor DBT_DEVICEARRIVAL /
 * DBT_DEVICEREMOVECOMPLETE events. Kept as a literal because MinGW-w64 headers do not define the named constant.
 */
static constexpr GUID MONITOR_DEVICE_INTERFACE_GUID = {
    0xe6f07b5f, 0xee97, 0x4a90, {0xb0, 0x76, 0x33, 0xf5, 0x7b, 0xf4, 0xea, 0xa7}};

/**
 * Forward declaration of the window procedure used by the hidden message window.
 */
LRESULT CALLBACK handleEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);

/**
 * Forward declaration of the helper that invokes a cached Java callback method on the notifier instance.
 */
static void invokeJavaCallback(jmethodID methodId);

/**
 * Normalizes a signature list by trimming whitespace from each entry and sorting the result so comparisons are
 * order-insensitive.
 *
 * @param rawSignatures
 *            - The signature strings to normalize (trim + sort)
 *
 * @return A new vector of the trimmed, sorted signatures
 */
static vector<string> normalizeSignatures(const vector<string> &rawSignatures) {
    vector<string> normalizedIds;
    normalizedIds.reserve(rawSignatures.size());

    for (const auto &id : rawSignatures) {
        string trimmedId = id;

        while (!trimmedId.empty() && isspace((unsigned char) trimmedId.front())) {
            trimmedId.erase(trimmedId.begin());
        }

        while (!trimmedId.empty() && isspace((unsigned char) trimmedId.back())) {
            trimmedId.pop_back();
        }

        normalizedIds.push_back(std::move(trimmedId));
    }

    sort(normalizedIds.begin(), normalizedIds.end());

    return normalizedIds;
}

/**
 * Runs the common stabilization, debounce, query, and compare logic shared by event-driven notifications
 * (WM_DEVICECHANGE, WM_DISPLAYCHANGE) and periodic polling. It compares the current visible display signatures to the
 * last-known set, records the new state and timestamp without notifying when they differ, checks that an unchanged set
 * has stayed stable for STABILIZATION_MS, and notifies Java exactly once per real change when stable and debounced.
 *
 * @return Whether a real display configuration change was detected and Java was notified
 */
static bool processPotentialDisplayChange() {
    auto now = chrono::steady_clock::now();
    vector<string> currentVisibleSignatures;

    try {
        currentVisibleSignatures = getVisibleDisplaySignatures();
    } catch (...) {
        currentVisibleSignatures.clear();
    }

    // Normalize once and reuse
    auto normalizedCurrent = normalizeSignatures(currentVisibleSignatures);

    // If normalized differs from lastNormalizedSignatures, update state and reset stabilization timer
    if (normalizedCurrent != lastNormalizedSignatures) {
        lastVisibleSignatures = currentVisibleSignatures;
        lastNormalizedSignatures = normalizedCurrent;
        lastStateChangeTime = now;

        return false;
    }

    // At this point normalizedCurrent == lastNormalizedSignatures, so check stabilization
    auto elapsedSinceStateChange = chrono::duration_cast<chrono::milliseconds>(now - lastStateChangeTime).count();

    if (elapsedSinceStateChange < STABILIZATION_MS) {
        return false;
    }

    // Debounce check to ensure we don't notify too frequently
    auto elapsedSinceLastNotify = chrono::duration_cast<chrono::milliseconds>(now - lastNotifyTime).count();

    if (elapsedSinceLastNotify < DEBOUNCE_MS) {
        return false;
    }

    /*
     * Suppress notification if the normalized set equals the last-notified set. This prevents re-notifying the same
     * stable configuration repeatedly and matches the user's preference to only care about current state
     */
    if (!lastNotifiedNormalizedSignatures.empty() && normalizedCurrent == lastNotifiedNormalizedSignatures) {
        return false;
    }

    /*
     * Update lastNotifyTime before invoking the callback so elapsed calculations remain consistent even if the
     * callback blocks briefly
     */
    lastNotifyTime = now;
    invokeJavaCallback(onNativeNotifyMethodId);

    // Record what we notified
    lastNotifiedNormalizedSignatures = normalizedCurrent;

    return true;
}

/**
 * Invokes a cached no-argument Java callback on the stored DisplayEventNotifier instance, attaching the current thread
 * to the JVM if necessary and detaching it when done. A null method id is ignored so an unavailable callback is safe.
 *
 * @param methodId
 *            - The cached method id to invoke, or null to do nothing
 */
static void invokeJavaCallback(jmethodID methodId) {
    if (!jvm || !displayEventNotifierGlobalRef || !methodId) {
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
    if (!displayEventNotifierGlobalRef) {
        if (attachedToJvm) {
            jvm->DetachCurrentThread();
        }

        return;
    }

    env->CallVoidMethod(displayEventNotifierGlobalRef, methodId);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    if (attachedToJvm) {
        jvm->DetachCurrentThread();
    }
}

/**
 * Message loop runner executed on a dedicated thread. Creates a hidden top-level window to receive WM_DISPLAYCHANGE,
 * WM_DEVICECHANGE, and the shell's TaskbarCreated broadcasts, registers for monitor device interface notifications, and
 * periodically polls the visible display configuration via getVisibleDisplaySignatures().
 */
static void runMessageLoopThread() {
    WNDCLASSW windowClass = {};
    windowClass.lpfnWndProc = handleEvents;
    windowClass.hInstance = GetModuleHandleW(NULL);
    windowClass.lpszClassName = CLASS_NAME;

    if (RegisterClassW(&windowClass) != 0) {
        classRegistered.store(true);
    }

    messageWindow = CreateWindowExW(0, CLASS_NAME, L"", 0, 0, 0, 0, 0, NULL, NULL, GetModuleHandleW(NULL), NULL);

    if (!messageWindow) {
        // If window creation failed, ensure we unregister class if we registered it
        if (classRegistered.load()) {
            UnregisterClassW(CLASS_NAME, GetModuleHandleW(NULL));
            classRegistered.store(false);
        }

        return;
    }

    DEV_BROADCAST_DEVICEINTERFACE_W notificationFilter;
    SecureZeroMemory(&notificationFilter, sizeof(notificationFilter));
    notificationFilter.dbcc_size = sizeof(notificationFilter);
    notificationFilter.dbcc_devicetype = DBT_DEVTYP_DEVICEINTERFACE;
    notificationFilter.dbcc_classguid = MONITOR_DEVICE_INTERFACE_GUID;

    deviceNotificationHandle =
        RegisterDeviceNotificationW(messageWindow, &notificationFilter, DEVICE_NOTIFY_WINDOW_HANDLE);

    // The shell broadcasts this registered message to top-level windows when it is recreated (explorer.exe restart)
    taskbarCreatedMessage = RegisterWindowMessageW(L"TaskbarCreated");

    /*
     * The app runs elevated, so UIPI would drop this broadcast from the lower-integrity shell by default. Allow the
     * specific message through the per-window filter so the restart is still delivered
     */
    if (taskbarCreatedMessage != 0) {
        ChangeWindowMessageFilterEx(messageWindow, taskbarCreatedMessage, MSGFLT_ALLOW, NULL);
    }

    // Initialize timers so first notify is not blocked by debounce or poll
    lastNotifyTime = chrono::steady_clock::now() - chrono::milliseconds(DEBOUNCE_MS);
    lastPollTime = chrono::steady_clock::now() - chrono::milliseconds(POLL_INTERVAL_MS);

    try {
        lastVisibleSignatures = getVisibleDisplaySignatures();
    } catch (...) {
        lastVisibleSignatures.clear();
    }

    // Cache normalized initial state and avoid notifying on startup
    lastNormalizedSignatures = normalizeSignatures(lastVisibleSignatures);
    lastNotifiedNormalizedSignatures = lastNormalizedSignatures;

    lastStateChangeTime = chrono::steady_clock::now();

    MSG message;

    while (isRunning.load()) {
        // Process all pending messages first
        while (PeekMessage(&message, NULL, 0, 0, PM_REMOVE)) {
            if (message.message == WM_QUIT) {
                isRunning.store(false);
                break;
            }

            TranslateMessage(&message);
            DispatchMessage(&message);
        }

        // Poll at a modest interval to reduce CPU usage
        auto now = chrono::steady_clock::now();
        auto elapsedSinceLastPoll = chrono::duration_cast<chrono::milliseconds>(now - lastPollTime).count();

        if (elapsedSinceLastPoll >= POLL_INTERVAL_MS) {
            processPotentialDisplayChange();
            lastPollTime = now;
        }

        // Sleep briefly to avoid busy-waiting; keep responsive to messages
        this_thread::sleep_for(chrono::milliseconds(50));
    }

    // Cleanup resources deterministically
    if (messageWindow) {
        DestroyWindow(messageWindow);
        messageWindow = NULL;
    }

    if (deviceNotificationHandle) {
        UnregisterDeviceNotification(deviceNotificationHandle);
        deviceNotificationHandle = NULL;
    }

    if (classRegistered.load()) {
        UnregisterClassW(CLASS_NAME, GetModuleHandleW(NULL));
        classRegistered.store(false);
    }
}

/**
 * Window procedure for the hidden message window. Handles monitor arrival/removal and display change broadcasts,
 * forwarding meaningful changes to Java via the common display-change processing logic, and forwards the shell's
 * TaskbarCreated broadcast as a separate shell-restart callback.
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
LRESULT CALLBACK handleEvents(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData) {
    // The registered TaskbarCreated id is resolved at runtime, so it cannot be a switch case
    if (taskbarCreatedMessage != 0 && message == taskbarCreatedMessage) {
        invokeJavaCallback(onShellRestartMethodId);

        return 0;
    }

    switch (message) {
    case WM_DEVICECHANGE: {
        if (eventType == DBT_DEVICEARRIVAL || eventType == DBT_DEVICEREMOVECOMPLETE) {
            DEV_BROADCAST_HDR *broadcastHeader = (DEV_BROADCAST_HDR *) eventData;

            if (broadcastHeader != NULL && broadcastHeader->dbch_devicetype == DBT_DEVTYP_DEVICEINTERFACE) {
                processPotentialDisplayChange();
            }
        }

        return 0;
    }

    case WM_DISPLAYCHANGE:
        processPotentialDisplayChange();
        return 0;

    case WM_DESTROY:
        PostQuitMessage(0);
        return 0;

    default:
        return DefWindowProcW(windowHandle, message, eventType, eventData);
    }
}

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
 * Starts native display event notifications. Stores a global reference to the Java DisplayEventNotifier instance,
 * resolves the onNativeNotify callback and the optional onShellRestart callback, and launches the message loop thread.
 * Performs basic JNI error checks and does not start the thread if setup fails.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The Java DisplayEventNotifier instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_DisplayEventNotifier_nativeStart(JNIEnv *env, jobject obj) {
    if (isRunning.load()) {
        return;
    }

    // Create a global ref and validate it
    jobject globalRef = env->NewGlobalRef(obj);

    if (!globalRef) {
        return;
    }

    jclass notifierClass = env->GetObjectClass(obj);

    if (!notifierClass) {
        env->DeleteGlobalRef(globalRef);
        return;
    }

    jmethodID nativeNotifyMethodId = env->GetMethodID(notifierClass, "onNativeNotify", "()V");

    if (!nativeNotifyMethodId) {
        env->DeleteGlobalRef(globalRef);
        return;
    }

    // The shell-restart callback is optional; a null id simply skips it while the display-change callback still fires
    jmethodID shellRestartMethodId = env->GetMethodID(notifierClass, "onShellRestart", "()V");

    if (!shellRestartMethodId) {
        env->ExceptionClear();
    }

    // All JNI setup succeeded; commit to globals and start thread
    displayEventNotifierGlobalRef = globalRef;
    onNativeNotifyMethodId = nativeNotifyMethodId;
    onShellRestartMethodId = shellRestartMethodId;

    isRunning.store(true);
    messageLoopThread = thread(runMessageLoopThread);
}

/**
 * Stops native display event notifications, shuts down the message loop thread, and releases the global Java reference.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The Java DisplayEventNotifier instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_DisplayEventNotifier_nativeStop(JNIEnv *env, jobject obj) {
    (void) obj;

    if (!isRunning.load()) {
        return;
    }

    isRunning.store(false);

    if (messageWindow) {
        PostMessage(messageWindow, WM_QUIT, 0, 0);
    }

    if (messageLoopThread.joinable()) {
        messageLoopThread.join();
    }

    if (displayEventNotifierGlobalRef) {
        env->DeleteGlobalRef(displayEventNotifierGlobalRef);
        displayEventNotifierGlobalRef = NULL;
    }

    onNativeNotifyMethodId = nullptr;
    onShellRestartMethodId = nullptr;
}

/**
 * Called when the native library is unloaded. Releases global references and clears the JVM pointer to avoid leaks.
 */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;

    // Ensure the notifier is stopped and resources are released
    isRunning.store(false);

    if (messageWindow) {
        PostMessage(messageWindow, WM_QUIT, 0, 0);
    }

    if (messageLoopThread.joinable()) {
        messageLoopThread.join();
    }

    // Delete global ref if present
    if (displayEventNotifierGlobalRef && jvm) {
        JNIEnv *env = nullptr;

        if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK && env) {
            env->DeleteGlobalRef(displayEventNotifierGlobalRef);
        }

        displayEventNotifierGlobalRef = NULL;
    }

    onNativeNotifyMethodId = nullptr;
    onShellRestartMethodId = nullptr;
    jvm = nullptr;
}
