$ErrorActionPreference = "Stop"

$flutterApp = Join-Path $PSScriptRoot "flutter_app"
if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
    throw "Flutter was not found on PATH. Install Flutter and reopen PowerShell."
}

Push-Location $flutterApp
try {
    if (-not (Test-Path (Join-Path $flutterApp "android"))) {
        flutter create --platforms=android --project-name=agrivision_flutter .
        if ($LASTEXITCODE -ne 0) { throw "Flutter Android project generation failed." }
    }

    flutter pub get
    if ($LASTEXITCODE -ne 0) { throw "flutter pub get failed." }

    dart run tool/apply_branding.dart
    if ($LASTEXITCODE -ne 0) { throw "Applying AgriVision branding failed." }

    flutter analyze
    if ($LASTEXITCODE -ne 0) { throw "flutter analyze failed." }

    flutter test
    if ($LASTEXITCODE -ne 0) { throw "flutter test failed." }

    flutter build apk --debug
    if ($LASTEXITCODE -ne 0) { throw "Flutter debug APK build failed." }

    Write-Host ""
    Write-Host "Build complete: flutter_app\build\app\outputs\flutter-apk\app-debug.apk" -ForegroundColor Green
}
finally {
    Pop-Location
}
