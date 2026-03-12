$ErrorActionPreference = "Stop"

# Paths
$projectPath = "C:\Scripts\Hours Tracker\AndroidApp"
$updateDir = "Y:\Ready Jobs\.Testing_Updates"
$gradlew = "$projectPath\gradlew.bat"

# 1. Build Debug APK (Testing Version)
Write-Host "Building Debug/Testing APK..." -ForegroundColor Cyan
Set-Location $projectPath
& $gradlew assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# 2. Get Version Info (Quick and dirty regex parse)
$buildFile = "$projectPath\app\build.gradle.kts"
$content = Get-Content $buildFile -Raw
if ($content -match 'versionCode\s*=?\s*(\d+)') {
    $versionCode = $matches[1]
}
else {
    Write-Host "Could not parse versionCode!" -ForegroundColor Red
    exit 1
}
if ($content -match 'versionName\s*=?\s*"([^"]+)"') {
    $versionName = $matches[1]
}
else {
    Write-Host "Could not parse versionName!" -ForegroundColor Red
    exit 1
}

Write-Host "Detected Version: $versionName ($versionCode) [TESTING]" -ForegroundColor Green

# 3. Create Hidden Update Directory
if (-not (Test-Path $updateDir)) {
    Write-Host "Creating update directory: $updateDir" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $updateDir | Out-Null
}

if (Test-Path $updateDir) {
    # Try to set hidden attribute, but don't fail the build if it fails (common on some network shares)
    try {
        $item = Get-Item $updateDir
        if (($item.Attributes -band [System.IO.FileAttributes]::Hidden) -ne [System.IO.FileAttributes]::Hidden) {
            $item.Attributes = $item.Attributes -bor [System.IO.FileAttributes]::Hidden
        }
    }
    catch {
        Write-Host "Warning: Could not set Hidden attribute on $updateDir. Proceeding anyway." -ForegroundColor Yellow
    }
}
else {
    Write-Host "Error: Failed to create $updateDir" -ForegroundColor Red
    exit 1
}

# 4. Clean up old APKs (only this app's APKs)
Write-Host "Cleaning up old Timecard APKs in $updateDir..." -ForegroundColor Cyan
Get-ChildItem -Path $updateDir -Filter "timecard-*.apk" | Remove-Item -Force

# 5. Copy APK (Use Debug APK)
$debugApk = "$projectPath\app\build\outputs\apk\debug\app-debug.apk"
$targetApk = "$updateDir\timecard-v$versionCode-debug.apk"

if (Test-Path $debugApk) {
    Write-Host "Copying APK to $targetApk" -ForegroundColor Cyan
    Copy-Item $debugApk $targetApk -Force
    Write-Host "Success! Timecard Testing Update deployed." -ForegroundColor Green
}
else {
    Write-Host "Debug APK not found at $debugApk" -ForegroundColor Red
    exit 1
}
