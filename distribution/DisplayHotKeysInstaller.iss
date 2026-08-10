#define MyAppName "Display Hot Keys"
#define MyAppVersion "4.0.3"
#define MyAppCopyright "Copyright (C) 2026 Jonathan R. Miller"
#define MyAppPublisher "Jonathan R. Miller"
#define MyAppURL "https://github.com/jon-mil-92/DisplayHotKeys"
#define MyAppExeName "DisplayHotKeys.exe"
#define DistDir SourcePath
#define ProjectDir SourcePath + "\.."

[Setup]
AppId={{8600871E-B870-4E14-807C-E37606DD0855}
AppName={#MyAppName}
VersionInfoVersion={#MyAppVersion}
AppVersion={#MyAppVersion}
AppCopyright={#MyAppCopyright}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
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
RestartApplications=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#DistDir}\jpackage-out\DisplayHotKeys\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#DistDir}\AGPL_3.0_LICENSE_3RD_PARTY.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#DistDir}\APACHE_2.0_LICENSE_3RD_PARTY.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#ProjectDir}\LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#DistDir}\README.txt"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[InstallDelete]
Type: filesandordirs; Name: {app}\DisplayHotKeys.exe;
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
procedure ForceCloseRunningApp;
var
  TaskKillResultCode: Integer;
begin
  // Force close the running app (the launcher and its JVM child) before its files are removed
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/f /t /im {#MyAppExeName}', '',
    SW_HIDE, ewWaitUntilTerminated, TaskKillResultCode);

  // Give Windows a moment to release the file handles the terminated processes held
  Sleep(1000);
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
    ForceCloseRunningApp;
end;
