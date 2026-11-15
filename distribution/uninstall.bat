@echo off

rem Cleans up what a portable copy of Display Hot Keys leaves outside its own folder, which is the scheduled task,
rem the startup folder fallback, and optionally the saved settings. The app does the cleanup itself, so every prompt
rem appears in the app's language, and it asks Windows for the administrator rights that removing the task needs.

start "" "%~dp0DisplayHotKeys.exe" --uninstall
