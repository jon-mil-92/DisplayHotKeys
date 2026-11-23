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
#include "com_dhk_io_WindowPlacement.h"
#include "WindowPlacement.h"

#include <jawt.h>
#include <jawt_md.h>
#include <jni.h>
#include <shellscalingapi.h>
#include <string>
#include <windows.h>

using namespace std;

static HWND nativeWindowOf(JNIEnv *env, jobject window);
static jstring deviceNameOf(JNIEnv *env, HMONITOR monitor);
static bool readDisplayInfo(HMONITOR monitor, MONITORINFOEXW &displayInfo);
static wstring readWideString(JNIEnv *env, jstring javaString);
static BOOL CALLBACK matchDisplayName(HMONITOR monitor, HDC monitorDC, LPRECT monitorRect, LPARAM lookupData);
static void packRect(jint *packedValues, int xIndex, const RECT &rect);
static jintArray toJavaIntArray(JNIEnv *env, const jint *packedValues, jsize valueCount);

/**
 * Index of the display's DPI in the display areas returned to the caller, taken from the generated header so the two
 * sides of the boundary can never disagree.
 */
static constexpr int DISPLAY_DPI_INDEX = com_dhk_io_WindowPlacement_DISPLAY_DPI_INDEX;

/**
 * Index of the display bounds' x, y, width, and height in the display areas returned to the caller.
 */
static constexpr int DISPLAY_BOUNDS_INDEX = com_dhk_io_WindowPlacement_DISPLAY_BOUNDS_INDEX;

/**
 * Index of the working area's x, y, width, and height in the display areas returned to the caller.
 */
static constexpr int DISPLAY_WORKING_AREA_INDEX = com_dhk_io_WindowPlacement_DISPLAY_WORKING_AREA_INDEX;

/**
 * Number of ints in the display areas returned to the caller.
 */
static constexpr int DISPLAY_AREA_LENGTH = com_dhk_io_WindowPlacement_DISPLAY_AREA_LENGTH;

/**
 * Index of the window bounds' x, y, width, and height in the window bounds returned to the caller.
 */
static constexpr int WINDOW_BOUNDS_INDEX = com_dhk_io_WindowPlacement_WINDOW_BOUNDS_INDEX;

/**
 * Index of the flag that is 1 when the window is minimized or maximized in the window bounds returned to the caller.
 */
static constexpr int WINDOW_MINIMIZED_OR_MAXIMIZED_INDEX =
    com_dhk_io_WindowPlacement_WINDOW_MINIMIZED_OR_MAXIMIZED_INDEX;

/**
 * Index of the window's own DPI in the window bounds returned to the caller.
 */
static constexpr int WINDOW_DPI_INDEX = com_dhk_io_WindowPlacement_WINDOW_DPI_INDEX;

/**
 * Number of ints in the window bounds returned to the caller.
 */
static constexpr int WINDOW_BOUNDS_LENGTH = com_dhk_io_WindowPlacement_WINDOW_BOUNDS_LENGTH;

/**
 * Gets the GDI device name of the display a window is mostly on.
 *
 * @param env
 *            - The JNI environment pointer
 * @param cls
 *            - The calling class
 * @param window
 *            - The window whose display to name
 *
 * @return The display's device name, or null if the window has no native window
 */
extern "C" JNIEXPORT jstring JNICALL Java_com_dhk_io_WindowPlacement_getWindowDisplayName(JNIEnv *env, jclass cls,
                                                                                          jobject window) {
    (void) cls;

    HWND windowHandle = nativeWindowOf(env, window);

    if (!windowHandle) {
        return nullptr;
    }

    return deviceNameOf(env, MonitorFromWindow(windowHandle, MONITOR_DEFAULTTONEAREST));
}

/**
 * Gets the GDI device name of the primary display.
 *
 * @param env
 *            - The JNI environment pointer
 * @param cls
 *            - The calling class
 *
 * @return The primary display's device name, or null if it could not be read
 */
extern "C" JNIEXPORT jstring JNICALL Java_com_dhk_io_WindowPlacement_getPrimaryDisplayName(JNIEnv *env, jclass cls) {
    (void) cls;

    // The primary display always holds the desktop origin
    return deviceNameOf(env, MonitorFromPoint({0, 0}, MONITOR_DEFAULTTOPRIMARY));
}

/**
 * Gets the DPI, bounds, and working area of the attached display with the given GDI device name, all in physical
 * pixels.
 *
 * @param env
 *            - The JNI environment pointer
 * @param cls
 *            - The calling class
 * @param displayName
 *            - The device name of the display
 *
 * @return The display's DPI, bounds, and working area, or null if no display with that name is attached
 */
extern "C" JNIEXPORT jintArray JNICALL Java_com_dhk_io_WindowPlacement_getDisplayArea(JNIEnv *env, jclass cls,
                                                                                      jstring displayName) {
    (void) cls;

    DisplayLookup lookup{readWideString(env, displayName), NULL, {}};

    // A display's handle is replaced whenever the displays change, so the display is found by its device name instead
    EnumDisplayMonitors(NULL, nullptr, matchDisplayName, (LPARAM) &lookup);

    UINT dpiX = 0;
    UINT dpiY = 0;

    if (!lookup.monitor || GetDpiForMonitor(lookup.monitor, MDT_EFFECTIVE_DPI, &dpiX, &dpiY) != S_OK) {
        return nullptr;
    }

    jint packedDisplayArea[DISPLAY_AREA_LENGTH];
    packedDisplayArea[DISPLAY_DPI_INDEX] = (jint) dpiX;
    packRect(packedDisplayArea, DISPLAY_BOUNDS_INDEX, lookup.displayInfo.rcMonitor);
    packRect(packedDisplayArea, DISPLAY_WORKING_AREA_INDEX, lookup.displayInfo.rcWork);

    return toJavaIntArray(env, packedDisplayArea, DISPLAY_AREA_LENGTH);
}

/**
 * Gets the bounds of a window in physical pixels, whether it is minimized or maximized, and the DPI Windows last scaled
 * it for.
 *
 * @param env
 *            - The JNI environment pointer
 * @param cls
 *            - The calling class
 * @param window
 *            - The window whose bounds to get
 *
 * @return The window's bounds, whether it is minimized or maximized, and its DPI, or null without a native window
 */
extern "C" JNIEXPORT jintArray JNICALL Java_com_dhk_io_WindowPlacement_getWindowBounds(JNIEnv *env, jclass cls,
                                                                                       jobject window) {
    (void) cls;

    HWND windowHandle = nativeWindowOf(env, window);
    RECT windowRect;

    if (!windowHandle || !GetWindowRect(windowHandle, &windowRect)) {
        return nullptr;
    }

    jint packedWindowBounds[WINDOW_BOUNDS_LENGTH];
    packRect(packedWindowBounds, WINDOW_BOUNDS_INDEX, windowRect);

    // Neither a minimized window, which is parked off screen, nor a maximized one shows where the window is placed
    bool minimizedOrMaximized = IsIconic(windowHandle) || IsZoomed(windowHandle);
    packedWindowBounds[WINDOW_MINIMIZED_OR_MAXIMIZED_INDEX] = minimizedOrMaximized ? 1 : 0;

    // Windows rescales a window for a display change on its own schedule, so its DPI tells whether it has yet
    packedWindowBounds[WINDOW_DPI_INDEX] = (jint) GetDpiForWindow(windowHandle);

    return toJavaIntArray(env, packedWindowBounds, WINDOW_BOUNDS_LENGTH);
}

/**
 * Sends a window the move and size notifications Windows sends after it moves or resizes, for the bounds it already
 * has, so a handler that keeps its own copy of the window's bounds reads them again.
 *
 * @param env
 *            - The JNI environment pointer
 * @param cls
 *            - The calling class
 * @param window
 *            - The window to notify
 */
extern "C" JNIEXPORT void JNICALL Java_com_dhk_io_WindowPlacement_resyncWindowBounds(JNIEnv *env, jclass cls,
                                                                                     jobject window) {
    (void) cls;

    HWND windowHandle = nativeWindowOf(env, window);
    POINT clientOrigin = {0, 0};
    RECT clientRect;

    // A minimized window is parked off screen, so it keeps the bounds it had until it is restored
    if (!windowHandle || IsIconic(windowHandle) || !ClientToScreen(windowHandle, &clientOrigin) ||
        !GetClientRect(windowHandle, &clientRect)) {
        return;
    }

    // Each notification carries what the one Windows sends does, the client area's position and its size
    SendMessageW(windowHandle, WM_MOVE, 0, MAKELPARAM(clientOrigin.x, clientOrigin.y));
    SendMessageW(windowHandle, WM_SIZE, SIZE_RESTORED, MAKELPARAM(clientRect.right, clientRect.bottom));
}

/**
 * Gets the native window behind a window object through the AWT native interface. The drawing surface is unlocked
 * before the handle is returned, so no lock is held while the window is queried or notified.
 *
 * @param env
 *            - The JNI environment pointer
 * @param window
 *            - The window object whose native window to get (may be null)
 *
 * @return The native window's handle, or NULL if the window object has none
 */
static HWND nativeWindowOf(JNIEnv *env, jobject window) {
    JAWT awt;
    awt.version = JAWT_VERSION_9;

    if (!window || !JAWT_GetAWT(env, &awt)) {
        return NULL;
    }

    JAWT_DrawingSurface *drawingSurface = awt.GetDrawingSurface(env, window);

    if (!drawingSurface) {
        return NULL;
    }

    HWND windowHandle = NULL;

    // The first lock also reports a changed surface, so only the error bit means the surface could not be locked
    if ((drawingSurface->Lock(drawingSurface) & JAWT_LOCK_ERROR) == 0) {
        JAWT_DrawingSurfaceInfo *surfaceInfo = drawingSurface->GetDrawingSurfaceInfo(drawingSurface);

        if (surfaceInfo) {
            windowHandle = ((JAWT_Win32DrawingSurfaceInfo *) surfaceInfo->platformInfo)->hwnd;
            drawingSurface->FreeDrawingSurfaceInfo(surfaceInfo);
        }

        drawingSurface->Unlock(drawingSurface);
    }

    awt.FreeDrawingSurface(drawingSurface);

    return windowHandle;
}

/**
 * Gets the GDI device name of a display.
 *
 * @param env
 *            - The JNI environment pointer
 * @param monitor
 *            - The handle of the display to name
 *
 * @return The display's device name, or null if it could not be read
 */
static jstring deviceNameOf(JNIEnv *env, HMONITOR monitor) {
    MONITORINFOEXW displayInfo;

    if (!readDisplayInfo(monitor, displayInfo)) {
        return nullptr;
    }

    wstring deviceName(displayInfo.szDevice);

    return env->NewString((const jchar *) deviceName.c_str(), (jsize) deviceName.size());
}

/**
 * Reads a display's bounds, working area, and GDI device name.
 *
 * @param monitor
 *            - The handle of the display to read
 * @param displayInfo
 *            - Receives the display's bounds, working area, and device name
 *
 * @return Whether the display could be read
 */
static bool readDisplayInfo(HMONITOR monitor, MONITORINFOEXW &displayInfo) {
    displayInfo.cbSize = sizeof(displayInfo);

    return monitor && GetMonitorInfoW(monitor, (MONITORINFO *) &displayInfo);
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
 * Checks one attached display against the display being looked up, and stops the enumeration once the names match.
 *
 * @param monitor
 *            - The handle of the attached display
 * @param monitorDC
 *            - Unused, since the enumeration covers the whole desktop
 * @param monitorRect
 *            - Unused, since the display's bounds are read along with its name
 * @param lookupData
 *            - The DisplayLookup to match against and fill in
 *
 * @return FALSE to stop the enumeration once the display is found, otherwise TRUE
 */
static BOOL CALLBACK matchDisplayName(HMONITOR monitor, HDC monitorDC, LPRECT monitorRect, LPARAM lookupData) {
    (void) monitorDC;
    (void) monitorRect;

    DisplayLookup *lookup = (DisplayLookup *) lookupData;

    if (!readDisplayInfo(monitor, lookup->displayInfo) || lookup->name != lookup->displayInfo.szDevice) {
        return TRUE;
    }

    lookup->monitor = monitor;

    return FALSE;
}

/**
 * Packs a rectangle as its x, y, width, and height, in that order, starting at the given index.
 *
 * @param packedValues
 *            - The packed ints to store the rectangle in
 * @param xIndex
 *            - The index of the rectangle's x among the packed ints
 * @param rect
 *            - The rectangle to store
 */
static void packRect(jint *packedValues, int xIndex, const RECT &rect) {
    packedValues[xIndex] = rect.left;
    packedValues[xIndex + 1] = rect.top;
    packedValues[xIndex + 2] = rect.right - rect.left;
    packedValues[xIndex + 3] = rect.bottom - rect.top;
}

/**
 * Copies packed ints into a new int array for the virtual machine.
 *
 * @param env
 *            - The JNI environment pointer
 * @param packedValues
 *            - The packed ints to copy
 * @param valueCount
 *            - The number of packed ints to copy
 *
 * @return The new int array, or null if it could not be allocated
 */
static jintArray toJavaIntArray(JNIEnv *env, const jint *packedValues, jsize valueCount) {
    jintArray javaIntArray = env->NewIntArray(valueCount);

    if (javaIntArray) {
        env->SetIntArrayRegion(javaIntArray, 0, valueCount, packedValues);
    }

    return javaIntArray;
}
