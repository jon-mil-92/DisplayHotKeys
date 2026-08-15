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
#include <comdef.h>
#include <shellapi.h>
#include <string>
#include <taskschd.h>
#include <wchar.h>
#include <windows.h>

using namespace std;

wstring getLaunchDirectory();
bool runRegisteredTask(const wstring &appExePath);
bool taskRunsExecutable(IRegisteredTask *registeredTask, const wstring &appExePath);
bool startExecutableDirectly(const wstring &appExePath);

/**
 * Name of the task that starts the application.
 */
static const wchar_t *TASK_NAME = L"Display Hot Keys";

/**
 * File name of the application executable that this launcher starts.
 */
static const wchar_t *APP_EXE_NAME = L"DisplayHotKeys.exe";

/**
 * Exit code reported when the application was started.
 */
static const int EXIT_STARTED = 0;

/**
 * Exit code reported when neither start path could launch the application.
 */
static const int EXIT_NOT_STARTED = 1;

/**
 * Entry point that starts the application through its registered task so it gains elevated rights without a consent
 * prompt after the first launch, falling back to starting the executable directly when no usable task is registered.
 *
 * @return Zero when the application was started, or one when neither start path succeeded
 */
int WINAPI wWinMain(HINSTANCE, HINSTANCE, PWSTR, int) {
    wstring launchDirectory = getLaunchDirectory();

    if (launchDirectory.empty()) {
        return EXIT_NOT_STARTED;
    }

    wstring appExePath = launchDirectory + L"\\" + APP_EXE_NAME;

    if (runRegisteredTask(appExePath)) {
        return EXIT_STARTED;
    }

    /*
     * No task is registered yet, it points at a different copy, or the scheduler refused to start it. Starting the
     * executable directly elevates through its own manifest, which costs one consent prompt and lets the application
     * register the task, so later launches go through the prompt-free path above
     */
    return startExecutableDirectly(appExePath) ? EXIT_STARTED : EXIT_NOT_STARTED;
}

/**
 * Resolves the directory holding this launcher, which is also where the application executable sits.
 *
 * @return The launcher's directory without a trailing separator, or an empty string when it cannot be resolved
 */
wstring getLaunchDirectory() {
    wchar_t modulePath[MAX_PATH];
    DWORD pathLength = GetModuleFileNameW(nullptr, modulePath, MAX_PATH);

    // A truncated path would silently resolve to the wrong executable, so treat a full buffer as a failure
    if (pathLength == 0 || pathLength == MAX_PATH) {
        return wstring();
    }

    wstring fullPath(modulePath, pathLength);
    size_t separatorIndex = fullPath.find_last_of(L'\\');

    if (separatorIndex == wstring::npos) {
        return wstring();
    }

    return fullPath.substr(0, separatorIndex);
}

/**
 * Starts the registered task when it drives this copy of the application. The scheduler builds the elevated token
 * itself, so the application starts with the rights it needs and no consent prompt appears.
 *
 * @param appExePath
 *            - The full path of the application executable the task must run
 *
 * @return True if the task was started, false otherwise
 */
bool runRegisteredTask(const wstring &appExePath) {
    if (FAILED(CoInitializeEx(nullptr, COINIT_MULTITHREADED))) {
        return false;
    }

    ITaskService *taskService = nullptr;
    IRegisteredTask *registeredTask = nullptr;
    ITaskFolder *rootFolder = nullptr;
    IRunningTask *runningTask = nullptr;
    BSTR rootFolderPath = SysAllocString(L"\\");
    BSTR taskName = SysAllocString(TASK_NAME);
    bool taskStarted = false;

    /*
     * An empty variant serves as both the connect credentials, which target the local machine as the current user, and
     * the run argument list. It holds no allocation, so it needs no matching clear
     */
    VARIANT emptyArgument;
    VariantInit(&emptyArgument);

    if (rootFolderPath != nullptr && taskName != nullptr &&
        SUCCEEDED(CoCreateInstance(CLSID_TaskScheduler, nullptr, CLSCTX_INPROC_SERVER, IID_ITaskService,
                                   reinterpret_cast<void **>(&taskService))) &&
        SUCCEEDED(taskService->Connect(emptyArgument, emptyArgument, emptyArgument, emptyArgument)) &&
        SUCCEEDED(taskService->GetFolder(rootFolderPath, &rootFolder)) &&
        SUCCEEDED(rootFolder->GetTask(taskName, &registeredTask)) && taskRunsExecutable(registeredTask, appExePath)) {
        taskStarted = SUCCEEDED(registeredTask->Run(emptyArgument, &runningTask));
    }

    SysFreeString(rootFolderPath);
    SysFreeString(taskName);

    if (runningTask != nullptr) {
        runningTask->Release();
    }

    if (registeredTask != nullptr) {
        registeredTask->Release();
    }

    if (rootFolder != nullptr) {
        rootFolder->Release();
    }

    if (taskService != nullptr) {
        taskService->Release();
    }

    CoUninitialize();

    return taskStarted;
}

/**
 * Determines whether the registered task runs the given executable. A portable copy can move or be duplicated, which
 * leaves a task pointing at a different folder that must not be started in this copy's place.
 *
 * @param registeredTask
 *            - The registered task to inspect
 * @param appExePath
 *            - The full path of the application executable the task must run
 *
 * @return True if the task's first action runs the given executable, false otherwise
 */
bool taskRunsExecutable(IRegisteredTask *registeredTask, const wstring &appExePath) {
    ITaskDefinition *taskDefinition = nullptr;
    IActionCollection *actions = nullptr;
    IAction *action = nullptr;
    IExecAction *execAction = nullptr;
    BSTR taskExePath = nullptr;
    bool pathsMatch = false;

    if (SUCCEEDED(registeredTask->get_Definition(&taskDefinition)) &&
        SUCCEEDED(taskDefinition->get_Actions(&actions)) && SUCCEEDED(actions->get_Item(1, &action)) &&
        SUCCEEDED(action->QueryInterface(IID_IExecAction, reinterpret_cast<void **>(&execAction))) &&
        SUCCEEDED(execAction->get_Path(&taskExePath)) && taskExePath != nullptr) {
        // Windows paths are case insensitive, so compare without regard to case
        pathsMatch = _wcsicmp(taskExePath, appExePath.c_str()) == 0;
    }

    if (taskExePath != nullptr) {
        SysFreeString(taskExePath);
    }

    if (execAction != nullptr) {
        execAction->Release();
    }

    if (action != nullptr) {
        action->Release();
    }

    if (actions != nullptr) {
        actions->Release();
    }

    if (taskDefinition != nullptr) {
        taskDefinition->Release();
    }

    return pathsMatch;
}

/**
 * Starts the application executable directly, letting its own manifest raise it to the rights it needs.
 *
 * @param appExePath
 *            - The full path of the application executable to start
 *
 * @return True if the executable was started, false otherwise
 */
bool startExecutableDirectly(const wstring &appExePath) {
    SHELLEXECUTEINFOW executeInfo = {};
    executeInfo.cbSize = sizeof(executeInfo);
    executeInfo.fMask = SEE_MASK_NOCLOSEPROCESS;
    executeInfo.lpVerb = L"open";
    executeInfo.lpFile = appExePath.c_str();
    executeInfo.nShow = SW_SHOWNORMAL;

    /*
     * ShellExecuteEx honors the target's requested execution level, unlike CreateProcess, which fails outright when the
     * manifest asks for elevation this launcher does not hold
     */
    if (!ShellExecuteExW(&executeInfo)) {
        return false;
    }

    if (executeInfo.hProcess != nullptr) {
        CloseHandle(executeInfo.hProcess);
    }

    return true;
}
