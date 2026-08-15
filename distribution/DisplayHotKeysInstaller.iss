#define MyAppName "Display Hot Keys"
#define MySettingsDirName "DisplayHotKeys"
#define MySettingsFileName "settings.ini"
#define MyLockFileName "DisplayHotKeys.lock"
#define ProfileListKey "SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList"
#define MyAppVersion "4.0.5"
#define MyAppCopyright "Copyright (C) 2026 Jonathan R. Miller"
#define MyAppPublisher "Jonathan R. Miller"
#define MyAppURL "https://github.com/jon-mil-92/DisplayHotKeys"
#define MyAppExeName "DisplayHotKeys.exe"
#define MyAppLauncherExeName "DisplayHotKeysLauncher.exe"
#define DistDir SourcePath
#define ProjectDir SourcePath + "\.."

[Setup]
AppId={{8600871E-B870-4E14-807C-E37606DD0855}
AppName={#MyAppName}
VersionInfoVersion={#MyAppVersion}
VersionInfoDescription={#MyAppName} Installer
AppVersion={#MyAppVersion}
AppCopyright={#MyAppCopyright}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
UninstallDisplayIcon={app}\{#MyAppExeName}
DefaultDirName={autopf}\{#MyAppName}
DisableDirPage=yes
DisableProgramGroupPage=yes
LicenseFile={#ProjectDir}\LICENSE.txt
OutputDir={#DistDir}
OutputBaseFilename=DisplayHotKeysInstaller
SetupIconFile={#DistDir}\dhk.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
CloseApplications=force
CloseApplicationsFilter=*.exe,*.dll
RestartApplications=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
// The portable cleanup script ships only in the portable package, so exclude it
Source: "{#DistDir}\jpackage-out\DisplayHotKeys\*"; DestDir: "{app}"; Excludes: "uninstall.bat"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppLauncherExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppLauncherExeName}"; Tasks: desktopicon

[InstallDelete]
Type: filesandordirs; Name: {app}\DisplayHotKeys.exe;
Type: filesandordirs; Name: {app}\DisplayHotKeysLauncher.exe;
Type: filesandordirs; Name: {app}\SetDisplay.exe;
Type: filesandordirs; Name: {app}\APACHE_LICENSE_3RD_PARTY.txt;
Type: filesandordirs; Name: {app}\LICENSE.txt;
Type: filesandordirs; Name: {app}\README.txt;
Type: filesandordirs; Name: {app}\JDK;
Type: filesandordirs; Name: {app}\app;
Type: filesandordirs; Name: {app}\runtime;
Type: filesandordirs; Name: {app}\AGPL_3.0_LICENSE_3RD_PARTY.txt;
Type: filesandordirs; Name: {app}\APACHE_2.0_LICENSE_3RD_PARTY.txt;
Type: filesandordirs; Name: {app}\EnumDisplayIds.dll;
Type: filesandordirs; Name: {app}\EnumDisplayModes.dll;
Type: filesandordirs; Name: {app}\SetDisplay.dll;
Type: filesandordirs; Name: {app}\GetDisplay.dll;
Type: filesandordirs; Name: {app}\DisplayEventNotifier.dll;

[Code]
const
  // Blank line between message box paragraphs, since Pascal strings carry no escape sequence for a line break
  NewParagraph = #13#10#13#10;

procedure ForceCloseRunningApp;
var
  TaskKillResultCode: Integer;
begin
  // Force close the running app (the launcher and its JVM child) before its files are replaced or removed
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/f /t /im {#MyAppExeName}', '',
    SW_HIDE, ewWaitUntilTerminated, TaskKillResultCode);

  // The launcher is a separate image, so it survives the kill above and can still hold a handle
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/f /t /im {#MyAppLauncherExeName}', '',
    SW_HIDE, ewWaitUntilTerminated, TaskKillResultCode);

  // Give Windows a moment to release the file handles the terminated processes held
  Sleep(1500);
end;

procedure RemoveTask;
var
  TaskDeleteResultCode: Integer;
begin
  // Delete the task that would otherwise outlive the uninstall and fail to start every login
  Exec(ExpandConstant('{sys}\schtasks.exe'), '/Delete /TN "{#MyAppName}" /F', '',
    SW_HIDE, ewWaitUntilTerminated, TaskDeleteResultCode);

  // The command above only reaches the uninstalling user, so drop the stored definition to clear all tasks
  DeleteFile(ExpandConstant('{sys}\Tasks\{#MyAppName}'));
end;

function GetProfileSettingsDirs: TStringList;
var
  ProfileSids: TArrayOfString;
  ProfilePath: String;
  SettingsDir: String;
  SidIndex: Integer;
begin
  Result := TStringList.Create;

  if not RegGetSubkeyNames(HKEY_LOCAL_MACHINE, '{#ProfileListKey}', ProfileSids) then
    Exit;

  for SidIndex := 0 to GetArrayLength(ProfileSids) - 1 do
  begin
    // Only user accounts hold settings, so this prefix skips the built-in system and service profiles
    if Pos('S-1-5-21-', ProfileSids[SidIndex]) <> 1 then
      Continue;

    if not RegQueryStringValue(HKEY_LOCAL_MACHINE, '{#ProfileListKey}\' + ProfileSids[SidIndex], 'ProfileImagePath',
      ProfilePath) then
      Continue;

    // The app builds this path from the profile root itself, so resolve it the same way rather than via a shell folder
    SettingsDir := ProfilePath + '\Documents\{#MySettingsDirName}';

    if DirExists(SettingsDir) then
      Result.Add(SettingsDir);
  end;
end;

procedure RemoveSettings;
var
  SettingsDirs: TStringList;
  SettingsPrompt: String;
  DirIndex: Integer;
begin
  SettingsDirs := GetProfileSettingsDirs;

  try
    // Only offer to delete settings that actually exist, so a clean uninstall is not interrupted
    if SettingsDirs.Count = 0 then
      Exit;

    SettingsPrompt := 'Do you also want to delete the saved {#MyAppName} settings for all users?';
    SettingsPrompt := SettingsPrompt + NewParagraph;
    SettingsPrompt := SettingsPrompt + 'Keeping them preserves saved slots if you reinstall.';

    if SuppressibleMsgBox(SettingsPrompt, mbConfirmation, MB_YESNO or MB_DEFBUTTON2, IDNO) <> IDYES then
      Exit;

    for DirIndex := 0 to SettingsDirs.Count - 1 do
    begin
      DeleteFile(SettingsDirs[DirIndex] + '\{#MySettingsFileName}');

      // The app leaves its single-instance lock file behind on exit, which would otherwise keep the folder populated
      DeleteFile(SettingsDirs[DirIndex] + '\{#MyLockFileName}');

      // Leave the folder alone unless the removed files were the only things in it
      RemoveDir(SettingsDirs[DirIndex]);
    end;
  finally
    SettingsDirs.Free;
  end;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  // Close the app before the in-use scan runs, so an upgrade over a running copy never prompts
  ForceCloseRunningApp;

  // An empty result lets the installation proceed
  Result := '';
end;

function InitializeUninstall: Boolean;
begin
  // Close the app before the in-use scan runs, so the uninstaller never prompts about the running processes
  ForceCloseRunningApp;
  Result := True;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
  begin
    RemoveTask;
    RemoveSettings;
  end;
end;
