@echo off
setlocal EnableExtensions

rem Cleans up what a portable copy of Display Hot Keys leaves outside its own folder: the scheduled task, the
rem startup folder fallback, and optionally the saved settings. Deleting the folder alone leaves the task
rem registered, where it fails to start at every login.

set "APP_NAME=Display Hot Keys"
set "APP_EXE=DisplayHotKeys.exe"
set "SETTINGS_DIR=%USERPROFILE%\Documents\DisplayHotKeys"
set "SETTINGS_FILE=%SETTINGS_DIR%\settings.ini"
set "LOCK_FILE=%SETTINGS_DIR%\DisplayHotKeys.lock"
set "STARTUP_FILE=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\StartDisplayHotKeys.bat"
set "ELEVATION_SCRIPT=%TEMP%\DisplayHotKeysElevate.vbs"

rem The task runs with the highest available privileges, so deleting it requires the same; re-launch elevated
net session >nul 2>&1

if not errorlevel 1 goto :elevated

echo Requesting administrator privileges to remove the scheduled task...

rem Windows offers no built-in way to raise a consent prompt from a batch file, so hand the re-launch to the shell
> "%ELEVATION_SCRIPT%" echo CreateObject("Shell.Application").ShellExecute "%~f0", "", "", "runas", 1

cscript //nologo "%ELEVATION_SCRIPT%"

del "%ELEVATION_SCRIPT%" >nul 2>&1

exit /b 0

:elevated

echo.
echo Display Hot Keys - Portable Uninstall
echo.

rem Close the running app first, since it re-registers the task on exit and holds the settings file open
tasklist /fi "imagename eq %APP_EXE%" 2>nul | findstr /i /c:"%APP_EXE%" >nul

if errorlevel 1 goto :queryTask

taskkill /f /t /im "%APP_EXE%" >nul 2>&1

echo Closed the running application.

:queryTask

rem The query reports a missing task on stderr yet still exits zero, so detect it by the absence of stdout instead
schtasks /Query /TN "%APP_NAME%" 2>nul | findstr /b /l /i "TaskName" >nul

if errorlevel 1 goto :taskMissing

schtasks /Delete /TN "%APP_NAME%" /F >nul 2>&1

rem The delete also exits zero when it fails, so confirm removal by querying for the task again
schtasks /Query /TN "%APP_NAME%" 2>nul | findstr /b /l /i "TaskName" >nul

if not errorlevel 1 goto :taskFailed

echo Removed the "%APP_NAME%" scheduled task.

goto :startupFile

:taskFailed

echo Could not remove the "%APP_NAME%" scheduled task. Remove it in Task Scheduler.

goto :startupFile

:taskMissing

echo No "%APP_NAME%" scheduled task is registered.

:startupFile

rem The app falls back to a startup folder batch file when the task cannot be registered, so clear that too
if not exist "%STARTUP_FILE%" goto :settings

del /f /q "%STARTUP_FILE%" >nul 2>&1

if not exist "%STARTUP_FILE%" echo Removed the startup folder entry.

:settings

if not exist "%SETTINGS_FILE%" goto :settingsMissing

echo.
echo Saved settings were found at:
echo   %SETTINGS_FILE%
echo.
echo Keeping them preserves saved slots.
echo.

set "DELETE_SETTINGS="
set /p "DELETE_SETTINGS=Do you also want to delete the saved settings? [Y/N]: "

if /i not "%DELETE_SETTINGS%"=="y" goto :settingsKept

del /f /q "%SETTINGS_FILE%" >nul 2>&1

rem The app leaves its single instance lock file behind on exit, which would otherwise keep the folder populated
del /f /q "%LOCK_FILE%" >nul 2>&1

rem Leave the folder alone unless the removed files were the only things in it
rd "%SETTINGS_DIR%" >nul 2>&1

echo Deleted the saved settings.

goto :done

:settingsKept

echo Kept the saved settings.

goto :done

:settingsMissing

echo No saved settings were found.

:done

echo.
echo Cleanup complete. You can now delete the Display Hot Keys folder.
echo.

pause

endlocal

exit /b 0
