param(
    [Parameter(Mandatory = $true)]
    [string]$VersionName,

    [Parameter(Mandatory = $true)]
    [int]$VersionCode,

    [string]$ReleaseTitle = "",
    [string]$ReleaseNotes = "",
    [string]$ReleaseNotesFile = "",
    [switch]$AutoNotes,
    [string]$AutoNotesFrom = "",
    [string]$AutoNotesSource = "",
    [ValidateSet("Long", "Short")]
    [string]$AutoNotesMode = "Long",
    [string[]]$CurrentHighlights = @(),
    [string]$AndroidHome = "",
    [switch]$SkipBuild,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"

function Get-GhPath {
    $command = Get-Command gh -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $fallback = "C:\Program Files\GitHub CLI\gh.exe"
    if (Test-Path -LiteralPath $fallback) {
        return $fallback
    }

    throw "gh not found. Install GitHub CLI first."
}

function Invoke-Git {
    param(
        [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
        [string[]]$CommandArgs
    )

    & git @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw ('git command failed: git {0}' -f ($CommandArgs -join ' '))
    }
}

function Invoke-Gh {
    param(
        [Parameter(Position = 0)]
        [string]$GhPath,
        [Parameter(Position = 1, ValueFromRemainingArguments = $true)]
        [string[]]$CommandArgs
    )

    & $GhPath @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw ('gh command failed: gh {0}' -f ($CommandArgs -join ' '))
    }
}

function Update-VersionInGradle {
    param(
        [string]$GradlePath,
        [string]$NextVersionName,
        [int]$NextVersionCode
    )

    $content = Get-Content -LiteralPath $GradlePath -Raw -Encoding UTF8
    $content = [regex]::Replace($content, 'versionCode\s*=\s*\d+', ('versionCode = {0}' -f $NextVersionCode), 1)
    $content = [regex]::Replace($content, 'versionName\s*=\s*"[^"]+"', ('versionName = "{0}"' -f $NextVersionName), 1)
    Set-Content -LiteralPath $GradlePath -Value $content -Encoding UTF8 -NoNewline
}

function Resolve-ReleaseNotesPath {
    param(
        [string]$ProjectRoot,
        [string]$Version,
        [string]$Notes,
        [string]$NotesFilePath,
        [bool]$UseAutoNotes,
        [string]$AutoNotesFromVersion,
        [string]$AutoNotesSourcePath,
        [string]$AutoNotesFormat,
        [string[]]$CurrentVersionHighlights
    )

    if (-not [string]::IsNullOrWhiteSpace($NotesFilePath)) {
        return (Resolve-Path -LiteralPath $NotesFilePath -ErrorAction Stop).Path
    }

    if ($UseAutoNotes) {
        $generatorScript = Join-Path $ProjectRoot "generate-release-notes.ps1"
        if (-not (Test-Path -LiteralPath $generatorScript)) {
            throw ('Release note generator not found: {0}' -f $generatorScript)
        }

        $tempPath = Join-Path $env:TEMP ("lukoa-release-notes-{0}.md" -f $Version)
        $generatorParams = @{
            TargetVersion = $Version
            OutputFile = $tempPath
            Format = $AutoNotesFormat
        }

        if (-not [string]::IsNullOrWhiteSpace($AutoNotesFromVersion)) {
            $generatorParams["FromVersion"] = $AutoNotesFromVersion
        }
        if (-not [string]::IsNullOrWhiteSpace($AutoNotesSourcePath)) {
            $generatorParams["SourceFile"] = $AutoNotesSourcePath
        }
        if ($CurrentVersionHighlights.Count -gt 0) {
            $generatorParams["CurrentHighlights"] = $CurrentVersionHighlights
        }

        & $generatorScript @generatorParams
        if ($LASTEXITCODE -ne 0) {
            throw "Automatic release note generation failed."
        }
        return $tempPath
    }

    $defaultNotes = if ([string]::IsNullOrWhiteSpace($Notes)) {
@"
## Updated

- Release v$Version
- See the commit history for details
"@
    } else {
        $Notes
    }

    $tempPath = Join-Path $env:TEMP ("lukoa-release-notes-{0}.md" -f $Version)
    Set-Content -LiteralPath $tempPath -Value $defaultNotes -Encoding UTF8
    return $tempPath
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$ghPath = Get-GhPath

if (-not [string]::IsNullOrWhiteSpace($AndroidHome)) {
    $env:ANDROID_HOME = $AndroidHome
}

Invoke-Gh $ghPath auth status

$gitRoot = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Current directory is not a Git repository."
}

$normalizedProjectRoot = [System.IO.Path]::GetFullPath($projectRoot).TrimEnd('\')
$normalizedGitRoot = [System.IO.Path]::GetFullPath($gitRoot).TrimEnd('\')

if ($normalizedGitRoot -ne $normalizedProjectRoot) {
    throw ('Run this script from the project Git root only. Current Git root: {0}' -f $normalizedGitRoot)
}

$branch = (& git branch --show-current).Trim()
if ([string]::IsNullOrWhiteSpace($branch)) {
    throw "No active branch found."
}

$gradleFile = Join-Path $projectRoot "app\build.gradle.kts"

if ($ValidateOnly) {
    [pscustomobject]@{
        ok = $true
        branch = $branch
        versionName = $VersionName
        versionCode = $VersionCode
        gradleFile = $gradleFile
        projectRoot = $normalizedProjectRoot
        gh = $ghPath
        autoNotes = $AutoNotes
        autoNotesMode = $AutoNotesMode
    } | ConvertTo-Json
    exit 0
}

Update-VersionInGradle -GradlePath $gradleFile -NextVersionName $VersionName -NextVersionCode $VersionCode

if (-not $SkipBuild) {
    $buildScript = Join-Path $projectRoot "build-debug.ps1"
    if (-not (Test-Path -LiteralPath $buildScript)) {
        throw ('Build script not found: {0}' -f $buildScript)
    }

    & powershell -ExecutionPolicy Bypass -File $buildScript -AndroidHome $AndroidHome
    if ($LASTEXITCODE -ne 0) {
        throw "APK build failed."
    }
}

$apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path -LiteralPath $apkPath)) {
    throw ('APK not found: {0}' -f $apkPath)
}

$outputsDir = (Resolve-Path (Join-Path $projectRoot "..\..\outputs")).Path
$releaseApkPath = Join-Path $outputsDir ('lukoa-launcher-v{0}.apk' -f $VersionName)
Copy-Item -LiteralPath $apkPath -Destination $releaseApkPath -Force

$statusLines = git status --short
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read git status."
}

if ($statusLines) {
    Invoke-Git add -A
    Invoke-Git commit -m ('Release {0}' -f $VersionName)
}

$tagName = 'v{0}' -f $VersionName
& git rev-parse --verify --quiet ('refs/tags/{0}' -f $tagName) 1>$null 2>$null
if ($LASTEXITCODE -eq 0) {
    throw ('Tag already exists: {0}' -f $tagName)
}

Invoke-Git tag $tagName
Invoke-Git push origin $branch
Invoke-Git push origin $tagName

$notesPath = Resolve-ReleaseNotesPath `
    -ProjectRoot $projectRoot `
    -Version $VersionName `
    -Notes $ReleaseNotes `
    -NotesFilePath $ReleaseNotesFile `
    -UseAutoNotes $AutoNotes.IsPresent `
    -AutoNotesFromVersion $AutoNotesFrom `
    -AutoNotesSourcePath $AutoNotesSource `
    -AutoNotesFormat $AutoNotesMode `
    -CurrentVersionHighlights $CurrentHighlights
$title = if ([string]::IsNullOrWhiteSpace($ReleaseTitle)) { $tagName } else { $ReleaseTitle }

& $ghPath release view $tagName 1>$null 2>$null
if ($LASTEXITCODE -eq 0) {
    Invoke-Gh $ghPath release upload $tagName $releaseApkPath --clobber
} else {
    Invoke-Gh $ghPath release create $tagName $releaseApkPath --title $title --notes-file $notesPath --latest
}

[pscustomobject]@{
    ok = $true
    branch = $branch
    versionName = $VersionName
    versionCode = $VersionCode
    tag = $tagName
    apk = $releaseApkPath
} | ConvertTo-Json

