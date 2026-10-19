# AgriVision Flutter Edition

A premium Flutter interface for the DA staff workflow. The original Kotlin/Jetpack Compose Android application remains in the repository unchanged while the Flutter edition is built and validated separately.

## Current Flutter features

- Premium green/cream DA staff overview with live counts from locally saved records.
- Farmer & Farm Registry with search, add/edit application form, status, and review notes.
- Local persistence using SharedPreferences (this is device-local storage, not cloud sync).
- DA Farmer Applications CSV export through the Android share sheet.
- Empty states instead of invented farmer or farm records.
- Responsive Material 3 layout and bottom navigation.

## Windows setup

Install Flutter and Android Studio first. Open PowerShell in the repository root:

```powershell
cd flutter_app
flutter create --platforms=android --project-name=agrivision_flutter .
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

The debug APK is created at:

```
flutter_app/build/app/outputs/flutter-apk/app-debug.apk
```

Open the `flutter_app` folder (not the repository root) in Android Studio to work on the Flutter edition. After the first `flutter create` command, the `android/` platform project is generated locally.

## Current scope and data warning

This is the first Flutter migration milestone, not a replacement for every existing native feature yet. The Flutter registry uses its own local SharedPreferences store and does **not** automatically import or sync the existing Kotlin Room database. Existing Kotlin farm maps, crop/livestock/finance modules, weather, scan history, and the full DA PDF/JSON report exporter have not been migrated in this milestone.

The Flutter DA Reports screen currently exports the Farmer Applications register as CSV. PDF/JSON and the other report categories will be migrated and tested in later stages. Cloud sync, verified DA staff authentication, and role-based access are not connected in this prototype.

## CI

The Flutter workflow bootstraps the Android platform, runs `flutter analyze` and tests, builds a debug APK, and uploads the APK as a GitHub Actions artifact.
