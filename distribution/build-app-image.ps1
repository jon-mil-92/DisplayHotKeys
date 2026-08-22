# Builds the Display Hot Keys jpackage app-image and injects the UAC/DPI manifest into the launcher

$ErrorActionPreference = 'Stop'
$distDir = $PSScriptRoot
$projectDir = Split-Path $distDir -Parent
$version = '4.1.1'
$jarName = "DisplayHotKeys-$version.jar"
$copyright = "Copyright $([char]0x00A9) 2026 Jonathan R. Miller"
$inputDir = Join-Path $distDir 'input'
$outDir = Join-Path $distDir 'jpackage-out'
$appImage = Join-Path $outDir 'DisplayHotKeys'
$appExe = Join-Path $appImage 'DisplayHotKeys.exe'
$manifest = Join-Path $distDir 'DisplayHotKeys.manifest'
$icon = Join-Path $distDir 'dhk.ico'
$resourceDir = Join-Path $distDir 'jpackage-resources'
$launcherProperties = Join-Path $resourceDir 'DisplayHotKeys.properties'
$dllNames = @('SetDisplay.dll', 'GetDisplay.dll', 'DisplayEventNotifier.dll', 'SystemTrayIcon.dll')
$startLauncherName = 'DisplayHotKeysLauncher.exe'
$startLauncher = Join-Path $distDir $startLauncherName
$uninstallScript = Join-Path $distDir 'uninstall.bat'

# The license lives at the project root while the readme and third-party licenses ship from the distribution folder
$docFiles = @(
    (Join-Path $projectDir 'LICENSE.txt'),
    (Join-Path $distDir 'README.txt'),
    (Join-Path $distDir 'AGPL_3.0_LICENSE_3RD_PARTY.txt'),
    (Join-Path $distDir 'APACHE_2.0_LICENSE_3RD_PARTY.txt')
)

try {
    # Resolve required tools without assuming a fixed drive or PATH layout
    $mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source

    if (-not $mvn) {
        throw 'mvn not found on PATH (install Maven or add it to PATH)'
    }

    # Resolve GNU make so the native DLLs can be rebuilt from the makefile before packaging
    $make = (Get-Command make -ErrorAction SilentlyContinue).Source

    if (-not $make) {
        throw 'make not found on PATH (install msys2/mingw make or add it to PATH)'
    }

    # Prefer the JDK named by JAVA_HOME, else fall back to jpackage on PATH
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
        $jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
    } else {
        $jpackage = (Get-Command jpackage -ErrorAction SilentlyContinue).Source
    }

    if (-not $jpackage) {
        throw 'jpackage not found (set JAVA_HOME to a JDK 21 or add jpackage to PATH)'
    }

    # Find the newest Windows SDK mt.exe under Program Files on whatever drive Windows uses
    $mt = @("${env:ProgramFiles(x86)}\Windows Kits\10\bin", "$env:ProgramFiles\Windows Kits\10\bin") |
        Where-Object { Test-Path $_ } |
        ForEach-Object { Get-ChildItem (Join-Path $_ '*\x64\mt.exe') -ErrorAction SilentlyContinue } |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName

    if (-not $mt) {
        throw 'mt.exe not found (install the Windows 10/11 SDK)'
    }

    # Bundle the builder's JAVA_HOME as the app runtime; it must be a JDK 21 (the app targets release 21)
    if (-not ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe')))) {
        throw 'JAVA_HOME is not set to a valid JDK (bin\java.exe not found)'
    }

    $runtime = $env:JAVA_HOME

    # Read the feature version from the JDK release file rather than spawning java.exe
    $releaseFile = Join-Path $runtime 'release'

    if (-not (Test-Path $releaseFile)) {
        throw "Cannot verify the JDK version: no release file at $runtime"
    }

    $versionLine = Get-Content $releaseFile | Where-Object { $_ -like 'JAVA_VERSION=*' } | Select-Object -First 1

    if (-not $versionLine -or $versionLine -notmatch 'JAVA_VERSION="?(\d+)') {
        throw "Cannot determine the JDK feature version from $releaseFile"
    }

    $featureVersion = [int]$Matches[1]

    if ($featureVersion -ne 21) {
        throw "JAVA_HOME must be a JDK 21, but found JDK $featureVersion at $runtime"
    }

    Write-Host "Using JAVA_HOME runtime: $runtime (JDK $featureVersion)"

    # Build the native DLLs from the makefile first so distribution holds fresh libraries before packaging
    Write-Host 'Building native DLLs with make...'

    # The makefile recipe shells out to unix tools (rm, cp, mkdir, g++, windres) that live in msys' bin dirs
    $msysUsrBin = Split-Path $make -Parent
    $mingwBin = Join-Path (Split-Path $msysUsrBin -Parent) 'mingw64\bin'
    $env:PATH = "$msysUsrBin;$mingwBin;$env:PATH"

    # The makefile derives its javac and JDK includes from JAVA_HOME
    & $make -C $projectDir all

    if ($LASTEXITCODE -ne 0) {
        throw 'Native DLL build (make) failed'
    }

    # Verify the remaining packaging inputs exist before building
    $dllFiles = $dllNames | ForEach-Object { Join-Path $distDir $_ }
    $requiredInputs = @($icon, $manifest, $launcherProperties, $startLauncher, $uninstallScript) + $docFiles + $dllFiles

    foreach ($required in $requiredInputs) {
        if (-not (Test-Path $required)) {
            throw "Required build input not found: $required"
        }
    }

    # Clean generated artifacts from any previous run so the build always starts from a known state
    Write-Host 'Cleaning previous build artifacts...'

    foreach ($dir in @($inputDir, $outDir)) {
        if (Test-Path $dir) {
            Remove-Item $dir -Recurse -Force
        }
    }

    # Build the fat jar (jpackage bundles this single jar-with-dependencies)
    Write-Host 'Building fat jar with Maven...'

    & $mvn -f (Join-Path $projectDir 'pom.xml') clean package

    if ($LASTEXITCODE -ne 0) {
        throw 'Maven build failed'
    }

    # Stage the jpackage input
    New-Item -ItemType Directory -Path $inputDir | Out-Null
    Copy-Item (Join-Path $projectDir "target\$jarName") $inputDir

    foreach ($dll in $dllNames) {
        Copy-Item (Join-Path $distDir $dll) $inputDir
    }

    $jpackageArgs = @(
        '--type', 'app-image',
        '--name', 'DisplayHotKeys',
        '--app-version', $version,
        '--vendor', 'Jonathan R. Miller',
        '--copyright', $copyright,
        '--description', 'Display Hot Keys - Apply display settings with hot keys',
        '--icon', $icon,
        '--resource-dir', $resourceDir,
        '--input', $inputDir,
        '--main-jar', $jarName,
        '--main-class', 'com.dhk.main.DhkDriver',
        '--runtime-image', $runtime,
        '--dest', $outDir,
        '--java-options', '--enable-native-access=ALL-UNNAMED',
        '--java-options', '-Djava.library.path=$APPDIR',
        '--java-options', '-Xms150m',
        '--java-options', '-Xmx150m',
        '--java-options', '-XX:+UseZGC',
        '--java-options', '-XX:+AlwaysPreTouch',
        '--java-options', '-XX:+UnlockExperimentalVMOptions',
        '--java-options', '-XX:SoftMaxHeapSize=140m',
        '--java-options', '-XX:ZUncommitDelay=100',
        '--java-options', '-XX:+ZGenerational',
        '--java-options', '-XX:MaxHeapFreeRatio=10',
        '--java-options', '-XX:MinHeapFreeRatio=5',
        '--java-options', '-XX:MaxMetaspaceSize=32m',
        '--java-options', '-XX:CompressedClassSpaceSize=16m',
        '--java-options', '-XX:ReservedCodeCacheSize=16m',
        '--java-options', '-XX:ThreadStackSize=256'
    )

    Write-Host 'Running jpackage...'

    & $jpackage @jpackageArgs

    if ($LASTEXITCODE -ne 0) {
        throw 'jpackage failed'
    }

    # jpackage marks the app executable read-only, which blocks the resource rewrite; clear it first
    Set-ItemProperty -Path $appExe -Name IsReadOnly -Value $false

    # Replace the first resource with the manifest that raises the app to the rights it needs
    Write-Host "Injecting manifest with $mt ..."

    & $mt -nologo -manifest $manifest "-outputresource:$appExe;#1"

    if ($LASTEXITCODE -ne 0) {
        throw 'mt.exe manifest injection failed'
    }

    # Place the unelevated launcher beside the app executable so shortcuts start the app without a consent prompt
    Write-Host 'Copying the launcher into the app image...'

    Copy-Item $startLauncher $appImage

    # A portable copy has no uninstaller, so ship the cleanup script that removes the task it leaves behind
    Write-Host 'Copying the portable uninstall script into the app image...'

    Copy-Item $uninstallScript $appImage

    # Ship the readme and licenses beside the app so the image is the complete portable package
    Write-Host 'Copying the readme and license files into the app image...'

    foreach ($docFile in $docFiles) {
        Copy-Item $docFile $appImage
    }

    # The input folder is only staging for jpackage, so clean it up
    Remove-Item $inputDir -Recurse -Force

    Write-Host ''
    Write-Host "Done. App image at: $appImage"
    Write-Host 'Next: Compile DisplayHotKeysInstaller.iss (Inno Setup) to build the installer.'

    $buildExitCode = 0
} catch {
    Write-Host ''
    Write-Host "BUILD FAILED: $($_.Exception.Message)" -ForegroundColor Red

    # Report the failure through the exit code so a caller chaining this script can detect it
    $buildExitCode = 1
} finally {
    # Keep the window open when double-clicked so the build output stays readable
    Write-Host ''
    Read-Host 'Press Enter to close this window'

    exit $buildExitCode
}
