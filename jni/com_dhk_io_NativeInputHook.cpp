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
#include "com_dhk_io_NativeInputHook.h"

#include <atomic>
#include <future>
#include <jni.h>
#include <memory>
#include <mutex>
#include <thread>
#include <windows.h>

using namespace std;

static bool startInput(JNIEnv *env, jobject obj);
static void stopInput();
static void runInputThread(jobject ownedRef, shared_ptr<promise<bool>> startupResult);
static bool setRawInput(DWORD flags, HWND targetWindow);
static void releaseInputThreadResources();
LRESULT CALLBACK handleRawInput(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData);
static void reportRawInput(HRAWINPUT rawInputHandle);
static void reportKeyboardInput(const RAWKEYBOARD &keyboardInput);
static void reportMouseInput(const RAWMOUSE &mouseInput);
static void clearPendingException();
static int resolveRawSideKeyCode(const RAWKEYBOARD &keyboardInput);
static int modifierSideIndex(int sideKeyCode);
static bool isOtherModifierSideHeld(int sideKeyCode, bool isPressed);
static int normalizeModifierKey(int virtualKeyCode);
static bool postQuitToInputThread();
static bool joinThreadWithTimeout(thread &targetThread, DWORD timeoutMs);

/**
 * Global JavaVM pointer used to attach the input thread so it can invoke the callbacks.
 */
static JavaVM *jvm = nullptr;

/**
 * Cached method ID for the onKeyEvent(int, boolean) callback.
 */
static jmethodID onKeyEventMethodId = nullptr;

/**
 * Cached method ID for the onMouseEvent(boolean) callback.
 */
static jmethodID onMouseEventMethodId = nullptr;

/**
 * Environment for the reporting thread. Thread local, so a thread abandoned by a timed-out stop keeps its own and can
 * never disturb a later run.
 */
static thread_local JNIEnv *inputThreadEnv = nullptr;

/**
 * Reference the reporting thread reports to, owned by that thread for its whole life. Thread local for the same reason
 * as the environment above.
 */
static thread_local jobject inputThreadRef = nullptr;

/**
 * Hidden message window the reporting thread receives raw input through, owned by that thread and thread local so an
 * abandoned thread tears down its own window rather than a later run's.
 */
static thread_local HWND inputThreadWindow = nullptr;

/**
 * Number of side-distinguished modifier keys, being both sides of shift, control, alt, and the windows key.
 */
static constexpr int MODIFIER_SIDE_COUNT = 8;

/**
 * Whether each side-distinguished modifier key is currently down, indexed by modifierSideIndex. Thread local, since
 * only the reporting thread reads and writes it.
 */
static thread_local bool heldModifierSides[MODIFIER_SIDE_COUNT] = {};

/**
 * Thread that owns the raw input registration and pumps the messages carrying it.
 */
static thread inputThread;

/**
 * Atomic flag indicating whether input is being reported.
 */
static atomic_bool isRunning(false);

/**
 * Serializes starting and stopping so the two can never interleave and corrupt the shared state below.
 */
static mutex startStopMutex;

/**
 * Identifier of the input thread, used to ask it to quit by posting to the thread rather than to its window.
 */
static atomic<DWORD> inputThreadId{0};

/**
 * Registered window class name for the hidden raw input window.
 */
static const wchar_t RAW_INPUT_CLASS_NAME[] = L"DHK_NativeInputHook_RawInputWindow";

/**
 * Usage page identifying generic desktop controls, which is where the keyboard and mouse usages live.
 */
static constexpr USHORT RAW_INPUT_USAGE_PAGE_GENERIC = 0x01;

/**
 * Usage identifying the mouse within the generic desktop page.
 */
static constexpr USHORT RAW_INPUT_USAGE_MOUSE = 0x02;

/**
 * Usage identifying the keyboard within the generic desktop page.
 */
static constexpr USHORT RAW_INPUT_USAGE_KEYBOARD = 0x06;

/**
 * Value reported for a key code or scan code that stands for no key, either a filler alongside a real key or the
 * overrun a keyboard reports when it cannot resolve the combination being held.
 */
static constexpr USHORT RAW_INPUT_NO_KEY = 0xFF;

/**
 * Scan code of the left shift key, used to tell the two shift keys apart since raw input reports neither side.
 */
static constexpr USHORT LEFT_SHIFT_SCAN_CODE = 0x2A;

/**
 * Longest the teardown waits on the input thread before abandoning it, so it can never hang process exit.
 */
static constexpr DWORD THREAD_JOIN_TIMEOUT_MS = 500;

/**
 * Number of times the quit request is posted before the input thread is left to be abandoned.
 */
static constexpr int QUIT_POST_ATTEMPTS = 5;

/**
 * Delay between quit request attempts, giving a full message queue time to drain.
 */
static constexpr DWORD QUIT_POST_RETRY_DELAY_MS = 10;

/**
 * Called when the native library is loaded. Caches the JavaVM pointer for attaching the input thread.
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
 * Starts reporting keyboard and mouse input.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 *
 * @return Whether reporting started
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_dhk_io_NativeInputHook_nativeStart(JNIEnv *env, jobject obj) {
    lock_guard<mutex> startStopLock(startStopMutex);

    return startInput(env, obj) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Stops reporting keyboard and mouse input.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_NativeInputHook_nativeStop(JNIEnv *env, jobject obj) {
    (void) env;
    (void) obj;

    lock_guard<mutex> startStopLock(startStopMutex);

    stopInput();
}

/**
 * Registers for raw input and starts the thread that reports it. A thread left over from a previous run is reaped
 * first, since assigning over a joinable thread would terminate the process.
 *
 * @param env
 *            - The JNI environment pointer
 * @param obj
 *            - The calling object instance
 *
 * @return Whether input reporting started
 */
static bool startInput(JNIEnv *env, jobject obj) {
    if (isRunning.load()) {
        return true;
    }

    joinThreadWithTimeout(inputThread, THREAD_JOIN_TIMEOUT_MS);

    jclass inputHookClass = env->GetObjectClass(obj);

    if (!inputHookClass) {
        return false;
    }

    onKeyEventMethodId = env->GetMethodID(inputHookClass, "onKeyEvent", "(IZ)V");
    onMouseEventMethodId = env->GetMethodID(inputHookClass, "onMouseEvent", "(Z)V");

    env->DeleteLocalRef(inputHookClass);

    if (!onKeyEventMethodId || !onMouseEventMethodId) {
        env->ExceptionClear();

        return false;
    }

    jobject globalRef = env->NewGlobalRef(obj);

    if (!globalRef) {
        return false;
    }

    /*
     * The result is shared with the thread rather than held here, so a thread abandoned by a timed-out stop reports
     * into its own run's promise instead of one a later run is waiting on
     */
    shared_ptr<promise<bool>> startupResult = make_shared<promise<bool>>();
    future<bool> startupOutcome = startupResult->get_future();

    try {
        // Handed to the thread, which owns the reference for its whole life and releases it however that life ends
        inputThread = thread(runInputThread, globalRef, startupResult);
    } catch (const system_error &e) {
        // A thread that never started cannot release the reference, so it is released here instead
        env->DeleteGlobalRef(globalRef);

        return false;
    }

    bool started = startupOutcome.get();

    // Only marked running once reporting is known to have started, so a failed start never looks like a live one
    isRunning.store(started);

    if (!started) {
        stopInput();
    }

    return started;
}

/**
 * Stops reporting input, abandoning the thread when it does not finish promptly so teardown can never hang. Every
 * resource an abandoned thread still holds is its own, so it releases them itself as it finishes.
 */
static void stopInput() {
    isRunning.store(false);

    postQuitToInputThread();
    joinThreadWithTimeout(inputThread, THREAD_JOIN_TIMEOUT_MS);

    // Cleared here rather than by the thread, so an abandoned one never clears the id a later run has published
    inputThreadId.store(0);
}

/**
 * Input runner executed on a dedicated thread. Registers for raw keyboard and mouse input and pumps the messages that
 * carry it, reporting each one straight to the virtual machine.
 *
 * @param ownedRef
 *            - The reference to report to, released by this thread when it exits
 */
static void runInputThread(jobject ownedRef, shared_ptr<promise<bool>> startupResult) {
    inputThreadRef = ownedRef;

    inputThreadId.store(GetCurrentThreadId());

    HINSTANCE moduleHandle = GetModuleHandleW(NULL);
    WNDCLASSW windowClass = {};

    windowClass.lpfnWndProc = handleRawInput;
    windowClass.hInstance = moduleHandle;
    windowClass.lpszClassName = RAW_INPUT_CLASS_NAME;

    RegisterClassW(&windowClass);

    inputThreadWindow =
        CreateWindowExW(0, RAW_INPUT_CLASS_NAME, L"", 0, 0, 0, 0, 0, HWND_MESSAGE, NULL, moduleHandle, NULL);

    /*
     * Raw input is used rather than a low-level hook for two reasons. It still arrives while a window of equal or
     * higher integrity has focus, and it is delivered as an ordinary queued message, so reporting it can never delay
     * the input every other application is waiting on
     */
    bool registered = inputThreadWindow != NULL && setRawInput(RIDEV_INPUTSINK, inputThreadWindow);

    // Attached as a daemon so this thread can never hold up virtual machine shutdown
    bool attached = jvm != nullptr && jvm->AttachCurrentThreadAsDaemon((void **) &inputThreadEnv, nullptr) == 0;

    /*
     * Force the message queue to exist before startup is signalled. Without it a stop that follows immediately could
     * post the quit request before the queue is created, losing it and leaving this thread pumping forever
     */
    MSG message;

    PeekMessage(&message, NULL, WM_USER, WM_USER, PM_NOREMOVE);

    startupResult->set_value(registered && attached);

    if (registered && attached) {
        // Blocking pump, since raw input is delivered as a message and the loop also receives the quit request
        while (GetMessage(&message, NULL, 0, 0) > 0) {
            DispatchMessage(&message);
        }
    }

    releaseInputThreadResources();
}

/**
 * Registers or removes the raw input subscription covering the keyboard and the mouse.
 *
 * @param flags
 *            - The registration flags, either the subscription flags or the flag that removes it
 * @param targetWindow
 *            - The window to deliver input to, or null when removing the subscription
 *
 * @return Whether the request succeeded
 */
static bool setRawInput(DWORD flags, HWND targetWindow) {
    RAWINPUTDEVICE devices[2] = {};

    devices[0].usUsagePage = RAW_INPUT_USAGE_PAGE_GENERIC;
    devices[0].usUsage = RAW_INPUT_USAGE_KEYBOARD;
    devices[0].dwFlags = flags;
    devices[0].hwndTarget = targetWindow;

    devices[1].usUsagePage = RAW_INPUT_USAGE_PAGE_GENERIC;
    devices[1].usUsage = RAW_INPUT_USAGE_MOUSE;
    devices[1].dwFlags = flags;
    devices[1].hwndTarget = targetWindow;

    return RegisterRawInputDevices(devices, 2, sizeof(devices[0])) != FALSE;
}

/**
 * Releases everything the reporting thread owns. Each resource is held in thread local state, so a thread abandoned
 * by a timed-out stop cleans itself up without disturbing the run that replaced it.
 */
static void releaseInputThreadResources() {
    if (inputThreadWindow) {
        setRawInput(RIDEV_REMOVE, NULL);

        DestroyWindow(inputThreadWindow);
        inputThreadWindow = NULL;
    }

    // Unregistering a class that was never registered simply fails, so the attempt needs no guard
    UnregisterClassW(RAW_INPUT_CLASS_NAME, GetModuleHandleW(NULL));

    /*
     * The reference is released even when the attach failed and there is no environment to release it through, since
     * it pins everything it names for as long as it lives
     */
    if (!inputThreadEnv && inputThreadRef && jvm) {
        jvm->AttachCurrentThreadAsDaemon((void **) &inputThreadEnv, nullptr);
    }

    if (inputThreadEnv) {
        if (inputThreadRef) {
            inputThreadEnv->DeleteGlobalRef(inputThreadRef);
        }

        jvm->DetachCurrentThread();

        inputThreadEnv = nullptr;
    }

    inputThreadRef = nullptr;
}

/**
 * Window procedure for the hidden raw input window. Forwards raw input to the reporter and defers everything else to
 * the default handling.
 *
 * @param windowHandle
 *            - The window receiving the message
 * @param message
 *            - The message identifier
 * @param eventType
 *            - The message specific first parameter
 * @param eventData
 *            - The message specific second parameter
 *
 * @return The result of handling the message
 */
LRESULT CALLBACK handleRawInput(HWND windowHandle, UINT message, WPARAM eventType, LPARAM eventData) {
    if (message == WM_INPUT) {
        reportRawInput((HRAWINPUT) eventData);
    }

    return DefWindowProcW(windowHandle, message, eventType, eventData);
}

/**
 * Reads one raw input event and hands it to the reporter for its device.
 *
 * @param rawInputHandle
 *            - The handle to the raw input to read
 */
static void reportRawInput(HRAWINPUT rawInputHandle) {
    RAWINPUT rawInput = {};
    UINT rawInputSize = sizeof(rawInput);

    if (GetRawInputData(rawInputHandle, RID_INPUT, &rawInput, &rawInputSize, sizeof(RAWINPUTHEADER)) == (UINT) -1) {
        return;
    }

    if (rawInput.header.dwType == RIM_TYPEKEYBOARD) {
        reportKeyboardInput(rawInput.data.keyboard);
    } else if (rawInput.header.dwType == RIM_TYPEMOUSE) {
        reportMouseInput(rawInput.data.mouse);
    }
}

/**
 * Reports one raw keyboard event, reading only its virtual key code.
 *
 * @param keyboardInput
 *            - The raw keyboard event to report
 */
static void reportKeyboardInput(const RAWKEYBOARD &keyboardInput) {
    /*
     * Some keys arrive as a filler carrying no key code, and a keyboard reports an overrun scan code when more keys
     * are held than it can resolve. Neither stands for a key that was actually pressed
     */
    if (keyboardInput.VKey >= RAW_INPUT_NO_KEY || keyboardInput.MakeCode == RAW_INPUT_NO_KEY) {
        return;
    }

    if (!inputThreadEnv || !onKeyEventMethodId) {
        return;
    }

    int sideKeyCode = resolveRawSideKeyCode(keyboardInput);
    bool isPressed = (keyboardInput.Flags & RI_KEY_BREAK) == 0;

    /*
     * Both sides of a modifier report the one code, so releasing either would otherwise read as the whole modifier
     * being released. The release is held back until the other side is up, leaving one code for the caller to track
     */
    if (isOtherModifierSideHeld(sideKeyCode, isPressed)) {
        return;
    }

    inputThreadEnv->CallVoidMethod(inputThreadRef, onKeyEventMethodId, (jint) normalizeModifierKey(sideKeyCode),
                                   isPressed ? JNI_TRUE : JNI_FALSE);

    clearPendingException();
}

/**
 * Reports one raw mouse event, ignoring everything but the left button since no other button is observed.
 *
 * @param mouseInput
 *            - The raw mouse event to report
 */
static void reportMouseInput(const RAWMOUSE &mouseInput) {
    USHORT buttonFlags = mouseInput.usButtonFlags;
    bool isLeftButtonEvent = (buttonFlags & (RI_MOUSE_LEFT_BUTTON_DOWN | RI_MOUSE_LEFT_BUTTON_UP)) != 0;

    if (!isLeftButtonEvent || !inputThreadEnv || !onMouseEventMethodId) {
        return;
    }

    jboolean isPressed = (buttonFlags & RI_MOUSE_LEFT_BUTTON_DOWN) != 0 ? JNI_TRUE : JNI_FALSE;

    inputThreadEnv->CallVoidMethod(inputThreadRef, onMouseEventMethodId, isPressed);

    clearPendingException();
}

/**
 * Reports and clears an exception left pending by a callback, so the next call into the virtual machine is not made
 * while one is still outstanding.
 */
static void clearPendingException() {
    if (inputThreadEnv->ExceptionCheck()) {
        inputThreadEnv->ExceptionDescribe();
        inputThreadEnv->ExceptionClear();
    }
}

/**
 * Resolves the side-distinguished key code for a raw keyboard event. Raw input reports the side-neutral modifier for
 * shift, control, and alt, so the side is recovered from the scan code for shift and the extended flag for the other
 * two. The windows keys already arrive as distinct codes and need no such work.
 *
 * @param keyboardInput
 *            - The raw keyboard event to resolve
 *
 * @return The side-distinguished virtual key code
 */
static int resolveRawSideKeyCode(const RAWKEYBOARD &keyboardInput) {
    bool isExtendedKey = (keyboardInput.Flags & RI_KEY_E0) != 0;

    switch (keyboardInput.VKey) {
    case VK_SHIFT:
        return keyboardInput.MakeCode == LEFT_SHIFT_SCAN_CODE ? VK_LSHIFT : VK_RSHIFT;
    case VK_CONTROL:
        return isExtendedKey ? VK_RCONTROL : VK_LCONTROL;
    case VK_MENU:
        return isExtendedKey ? VK_RMENU : VK_LMENU;
    default:
        return (int) keyboardInput.VKey;
    }
}

/**
 * Maps a side-distinguished modifier key code onto its index in the held-side flags, giving each modifier an adjacent
 * pair of indices with its left side first.
 *
 * @param sideKeyCode
 *            - The key code to map
 *
 * @return The index of that key, or -1 when it is not a side-distinguished modifier
 */
static int modifierSideIndex(int sideKeyCode) {
    switch (sideKeyCode) {
    case VK_LSHIFT:
        return 0;
    case VK_RSHIFT:
        return 1;
    case VK_LCONTROL:
        return 2;
    case VK_RCONTROL:
        return 3;
    case VK_LMENU:
        return 4;
    case VK_RMENU:
        return 5;
    case VK_LWIN:
        return 6;
    case VK_RWIN:
        return 7;
    default:
        return -1;
    }
}

/**
 * Records the state of a side-distinguished modifier key and reports whether its other side is still down, so a
 * release that leaves the modifier held can be held back.
 *
 * @param sideKeyCode
 *            - The side-distinguished key code that changed state
 * @param isPressed
 *            - Whether that key was pressed rather than released
 *
 * @return Whether the other side of the modifier is still down
 */
static bool isOtherModifierSideHeld(int sideKeyCode, bool isPressed) {
    int sideIndex = modifierSideIndex(sideKeyCode);

    if (sideIndex < 0) {
        return false;
    }

    heldModifierSides[sideIndex] = isPressed;

    // Each modifier owns an adjacent pair of indices, with its left side first, so its other side flips the low bit
    return !isPressed && heldModifierSides[sideIndex ^ 1];
}

/**
 * Reports a modifier as its side-independent code, so a hot key bound to one names both. Reported alongside the
 * side-distinguished code rather than instead of it, since held state has to tell the two sides apart. Windows has no
 * side-neutral code for its own key, so the left one stands for both.
 *
 * @param virtualKeyCode
 *            - The side-distinguished virtual key code
 *
 * @return The side-independent virtual key code, or the original code when it is not a modifier
 */
static int normalizeModifierKey(int virtualKeyCode) {
    switch (virtualKeyCode) {
    case VK_LSHIFT:
    case VK_RSHIFT:
        return VK_SHIFT;
    case VK_LCONTROL:
    case VK_RCONTROL:
        return VK_CONTROL;
    case VK_LMENU:
    case VK_RMENU:
        return VK_MENU;
    case VK_RWIN:
        return VK_LWIN;
    default:
        return virtualKeyCode;
    }
}

/**
 * Asks the input thread to leave its message loop. The post can fail transiently when the target queue is full, so it
 * is retried briefly before the thread is left to be abandoned.
 *
 * @return Whether the quit request was delivered
 */
static bool postQuitToInputThread() {
    DWORD targetThreadId = inputThreadId.load();

    if (targetThreadId == 0) {
        return false;
    }

    for (int attempt = 0; attempt < QUIT_POST_ATTEMPTS; attempt++) {
        if (PostThreadMessage(targetThreadId, WM_QUIT, 0, 0)) {
            return true;
        }

        // Only a full queue is worth waiting on, since any other failure means the thread is already gone
        if (GetLastError() != ERROR_NOT_ENOUGH_QUOTA) {
            return false;
        }

        Sleep(QUIT_POST_RETRY_DELAY_MS);
    }

    return false;
}

/**
 * Joins a thread, giving up after the timeout so a wedged thread can never hang teardown or process exit. An abandoned
 * thread is detached, since joining it later would block just as long.
 *
 * @param targetThread
 *            - The thread to join
 * @param timeoutMs
 *            - The longest to wait before abandoning the thread
 *
 * @return Whether the thread finished within the timeout
 */
static bool joinThreadWithTimeout(thread &targetThread, DWORD timeoutMs) {
    if (!targetThread.joinable()) {
        return true;
    }

    HANDLE threadHandle = (HANDLE) targetThread.native_handle();

    // A missing handle is treated as a timeout, since joining it could block for as long as the thread is wedged
    if (!threadHandle || WaitForSingleObject(threadHandle, timeoutMs) != WAIT_OBJECT_0) {
        targetThread.detach();

        return false;
    }

    targetThread.join();

    return true;
}

/**
 * Called when the native library is unloaded. Stops reporting so nothing outlives the library. The input thread owns
 * the reference it reports to and releases it itself, so there is nothing left to clean up here.
 *
 * @param vm
 *            - The JavaVM pointer
 * @param reserved
 *            - Reserved for future use
 */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;

    lock_guard<mutex> startStopLock(startStopMutex);

    stopInput();

    // Cleared so an input thread abandoned by the stop above stops reporting rather than calling into a dying machine
    onKeyEventMethodId = nullptr;
    onMouseEventMethodId = nullptr;
}
