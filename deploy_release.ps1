$ErrorActionPreference = "Stop"

# Paths
$projectPath = "C:\Scripts\Hours Tracker\AndroidApp"
$updateDir = "Y:\Ready Jobs\.Updates"
$gradlew = "$projectPath\gradlew.bat"

# 1. Build Release APK
Write-Host "Building Release APK..." -ForegroundColor Cyan
Set-Location $projectPath
Write-Host "Running Clean Build..." -ForegroundColor Cyan
Remove-Item -Recurse -Force "$projectPath\app\build\intermediates\lint-cache" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\build-cache" -ErrorAction SilentlyContinue
& $gradlew clean assembleRelease --rerun-tasks --no-build-cache -x lintVitalRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# 2. Get Version Info
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

Write-Host "Detected Version: $versionName ($versionCode) [RELEASE]" -ForegroundColor Green

# 3. Create Hidden Update Directory
if (-not (Test-Path $updateDir)) {
    Write-Host "Creating update directory: $updateDir" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $updateDir | Out-Null
}

if (Test-Path $updateDir) {
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

# 4. Clean up old APKs
Write-Host "Cleaning up old Timecard APKs in $updateDir..." -ForegroundColor Cyan
Get-ChildItem -Path $updateDir -Filter "*.apk" | Where-Object { $_.Name -match "timecard|app-release|app-debug" } | Remove-Item -Force

# 5. Copy APK (Use Release APK)
$releaseApk = "$projectPath\app\build\outputs\apk\release\app-release.apk"
$targetApk = "$updateDir\timecard-v$versionCode-release.apk"

if (Test-Path $releaseApk) {
    Write-Host "Copying APK to $targetApk" -ForegroundColor Cyan
    Copy-Item $releaseApk $targetApk -Force
    Write-Host "Success! Timecard RELEASE Update deployed." -ForegroundColor Green
}
else {
    Write-Host "Release APK not found at $releaseApk" -ForegroundColor Red
    exit 1
}
