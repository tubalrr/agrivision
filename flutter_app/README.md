# AgriVision Flutter Edition

A premium Flutter interface for the DA staff workflow. The original Kotlin/Jetpack Compose Android application remains in the repository unchanged while the Flutter edition is built and validated separately.

## Current Flutter features

- Premium green/cream DA staff overview with live counts from locally saved records.
- Farmer & Farm Registry with search, registration application, editable statuses, and DA review notes.
- Farm Operations screen with crops, livestock, finance, and tasks tabs.
- Add/edit/delete farm activity records linked to a selected farmer/farm application.
- Local persistence using SharedPreferences (this is device-local storage, not cloud sync).
- Full JSON backup and restore for farmer and farm activity records, with validation, confirmation, and safe merge (no local-only records are deleted).
- DA reports for farmer applications and operational records with all-farm or selected-farmer scope.
- Share actual CSV or JSON report files using Android's share sheet.
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

This is the first Flutter migration milestone, not a replacement for every existing native feature yet. The Flutter registry uses its own local SharedPreferences store, supports manual JSON backup/restore, and does **not** automatically import or sync the existing Kotlin Room database. Existing Kotlin farm maps, crop/livestock/finance modules, weather, scan history, and the full DA PDF/JSON report exporter have not been migrated in this milestone.

The Flutter DA Reports screen exports Farmer Applications, Crop Records, Livestock Records, Finance Records, Task Register, and Complete Farm Activity in CSV or JSON. A PDF report template, official DA submission workflow, and remaining native maps/weather/scan/market functions are still future migration work. Cloud sync, verified DA staff authentication, and role-based access are not connected in this prototype.

## CI

The Flutter workflow bootstraps the Android platform, runs `flutter analyze` and tests, builds a debug APK, and uploads the APK as a GitHub Actions artifact.
