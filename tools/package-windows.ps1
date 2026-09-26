# Build a separate, self-contained GUI snapshot; never replace a running image.
[CmdletBinding()]
param(
    [string] $JdkHome = $env:JAVA_HOME,
    # Internal entry from :packageWindows after its Gradle dependencies complete.
    [switch] $FromGradle
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ($env:OS -ne 'Windows_NT') {
    throw 'Build the Windows application image on Windows.'
}
if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    throw 'Set JAVA_HOME or pass -JdkHome with a JDK 21 or newer containing jpackage.'
}
$jpackage = Join-Path $JdkHome 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
    throw "jpackage was not found: $jpackage"
}
$jdkVersion = & $jpackage --version
if ($LASTEXITCODE -ne 0 -or $jdkVersion -notmatch '^(\d+)' -or [int]$Matches[1] -lt 21) {
    throw 'Packaging requires JDK 21 or newer.'
}

$repo = Split-Path -Parent $PSScriptRoot
$icon = Join-Path $repo 'tools\icons\seedv6.ico'
if (-not (Test-Path -LiteralPath $icon -PathType Leaf)) {
    throw "Application icon is missing: $icon"
}
Push-Location $repo
try {
    if (-not $FromGradle) {
        # Direct script usage retains the same build-only behavior as :packageWindows.
        # No clean: preserve existing outputs and all current working-tree sources.
        & .\gradlew.bat :app:installDist
        if ($LASTEXITCODE -ne 0) { throw 'Gradle distribution build failed.' }
    }

    $developmentLib = Join-Path $repo 'app\build\install\seedv6\lib'
    if (-not (Test-Path -LiteralPath (Join-Path $developmentLib 'app.jar'))) {
        throw "Application JAR is missing from $developmentLib"
    }
    $snapshot = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
    $destination = Join-Path $repo "dist\windows\$snapshot"
    if (Test-Path -LiteralPath $destination) {
        throw "Snapshot already exists; refusing to overwrite: $destination"
    }
    # Stamp only an isolated packaging copy. Development resources/JARs keep
    # their Development run identity, even immediately after packaging.
    $inputDirectory = Join-Path $repo "app\build\desktop-input\$snapshot"
    New-Item -ItemType Directory -Path $inputDirectory -ErrorAction Stop | Out-Null
    Copy-Item -LiteralPath (Join-Path $developmentLib 'app.jar') -Destination $inputDirectory
    Get-ChildItem -LiteralPath $developmentLib -Filter '*.jar' |
        Where-Object { $_.Name -ne 'app.jar' } |
        Copy-Item -Destination $inputDirectory
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::Open(
        (Join-Path $inputDirectory 'app.jar'), [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $versionEntry = $archive.GetEntry('com/ohinteractive/seedv6/application-version.properties')
        if ($null -eq $versionEntry) { throw 'Application JAR has no embedded version identity.' }
        $reader = [System.IO.StreamReader]::new($versionEntry.Open())
        try { $metadata = ConvertFrom-StringData -StringData $reader.ReadToEnd() }
        finally { $reader.Dispose() }
        $appVersion = "$($metadata.major).$($metadata.minor).$($metadata.patch)"
        if ($appVersion -notmatch '^\d+\.\d+\.\d+$' -or $metadata.build -notmatch '^\d+$') {
            throw 'Application JAR has invalid embedded version numbers.'
        }
        $desktopEntry = $archive.CreateEntry('com/ohinteractive/seedv6/desktop-build.properties')
        $writer = [System.IO.StreamWriter]::new($desktopEntry.Open(), [System.Text.UTF8Encoding]::new($false))
        try {
            $builtAt = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss'Z'")
            $writer.Write("builtAt=$builtAt`n")
        } finally { $writer.Dispose() }
    } finally { $archive.Dispose() }
    # jpackage copies all runtime JARs and links a private runtime. The existing
    # Swing entry point opens the GUI directly; omitting --win-console is intentional.
    & $jpackage --type app-image --name SeedV6-NNUE `
        --input $inputDirectory --main-jar app.jar `
        --main-class com.ohinteractive.seedv6.gui.SwingLauncher `
        --app-version $appVersion `
        --icon $icon `
        --dest $destination
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed; no usable image is claimed.' }

    $image = Join-Path $destination 'SeedV6-NNUE'
    $launcher = Join-Path $image 'SeedV6-NNUE.exe'
    if (-not (Test-Path -LiteralPath $launcher -PathType Leaf) -or
        -not (Test-Path -LiteralPath (Join-Path $image 'runtime\bin\server\jvm.dll'))) {
        throw "Incomplete application image: $image"
    }
    Write-Host "Distribution: $image"
    Write-Host "Launch:       $launcher"
    Write-Host 'Keep the complete image folder together. Training stays in the existing configured external store.'
} finally {
    Pop-Location
}
