#define MyAppName "Display Hot Keys"
#define MyVersionInfoVersion "1.0.0.0"
#define MyAppVersion "3.0.0"
#define MyAppCopyright "Copyright © 2026 Jonathan R. Miller"
#define MyAppPublisher "Jonathan R. Miller"
#define MyAppURL "https://github.com/jon-mil-92/DisplayHotKeys"
#define MyAppExeName "DisplayHotKeys.exe"

[Setup]
AppId={{8600871E-B870-4E14-807C-E37606DD0855}
AppName={#MyAppName}
VersionInfoVersion={#MyVersionInfoVersion}
AppVersion={#MyAppVersion}
AppCopyright={#MyAppCopyright}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DisableDirPage=yes
DisableProgramGroupPage=yes
LicenseFile=C:\Users\jonRock\git\DisplayHotKeys\LICENSE.txt
OutputDir=C:\Users\jonRock\git\DisplayHotKeys\distribution
OutputBaseFilename=DisplayHotKeysInstaller
SetupIconFile=C:\Users\jonRock\git\DisplayHotKeys\distribution\dhk.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\AGPL_3.0_LICENSE_3RD_PARTY.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\APACHE_2.0_LICENSE_3RD_PARTY.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\README.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\SetDisplay.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\GetDisplay.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\DisplayEventNotifier.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\jonRock\git\DisplayHotKeys\distribution\JDK\*"; DestDir: "{app}\JDK"; Flags: ignoreversion recursesubdirs createallsubdirs

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
Type: filesandordirs; Name: {app}\AGPL_3.0_LICENSE_3RD_PARTY.txt;
Type: filesandordirs; Name: {app}\APACHE_2.0_LICENSE_3RD_PARTY.txt;
Type: filesandordirs; Name: {app}\EnumDisplayIds.dll;
Type: filesandordirs; Name: {app}\EnumDisplayModes.dll;
Type: filesandordirs; Name: {app}\SetDisplay.dll;
Type: filesandordirs; Name: {app}\GetDisplay.dll;
Type: filesandordirs; Name: {app}\DisplayEventNotifier.dll;