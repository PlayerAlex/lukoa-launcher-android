param(
    [string]$AndroidHome = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

if (-not [string]::IsNullOrWhiteSpace($AndroidHome)) {
    $env:ANDROID_HOME = $AndroidHome
}

if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME) -and -not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
    $env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
}

if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    throw "ANDROID_HOME or ANDROID_SDK_ROOT is not set."
}

if (-not (Test-Path -LiteralPath $env:ANDROID_HOME)) {
    throw "Android SDK directory does not exist: $env:ANDROID_HOME"
}

$GradleCommandInfo = Get-Command gradle -ErrorAction SilentlyContinue
$GradleCommand = if ($GradleCommandInfo) { $GradleCommandInfo.Source } else { $null }
if (-not $GradleCommand) {
    $CachedGradle = Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists" -Recurse -Filter gradle.bat -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($CachedGradle) {
        $GradleCommand = $CachedGradle.FullName
    }
}

if (-not $GradleCommand) {
    throw "gradle command was not found. Install Gradle, or import this project in Android Studio."
}

& $GradleCommand ":app:assembleDebug"
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path -LiteralPath $Apk)) {
    throw "APK was not created: $Apk"
}

[pscustomobject]@{
    ok = $true
    apk = $Apk
} | ConvertTo-Json
