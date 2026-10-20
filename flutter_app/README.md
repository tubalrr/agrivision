# AgriVision — Farmer Edition

A farmer-focused Flutter app for keeping personal farm details, crop and livestock entries, farm finances, and everyday tasks together.

## Current Flutter features

- Farmer home dashboard with crop, livestock, and open-task counts for the selected farm profile.
- My Farm Profiles for adding and editing your own farmer and farm details.
- My Farm Log for recording crops, livestock, finance, and daily tasks under a selected farm profile.
- Offline Farm Log modules for crops, livestock, finance, tasks, inventory, production, equipment, field-coordinate notes, pest/disease observations, and incidents.
- My Reports exports farm profiles and farm activity records as CSV, JSON, or locally generated PDF.
- Offline Insights calculates categorized income/expenses, stock-in/stock-out balances, and due/overdue tasks from saved entries.
- Device-local storage using SharedPreferences; there is no automatic cloud sync or multi-device account.
- Full JSON backup and restore with validation, confirmation, and a non-destructive merge; schema v1 backups remain supported.
- Animated HTML loading screen and the supplied green-leaf AgriVision branding.
- Farm logs, summary calculations, and PDF reports are generated locally. Field mapping stores manually entered coordinates/notes only, and task reminders are shown in-app rather than scheduled as Android system notifications.
- No sample farmer or farm records are pre-populated.

## Windows setup

Install Flutter stable (Flutter 3.47.x is the CI baseline) and Android Studio. In PowerShell, start at the repository root:

```powershell
git pull origin main
.\build_flutter.ps1
```

The helper generates the Android platform folder when needed, resolves packages, applies the AgriVision app name and leaf launcher icon, runs `flutter analyze`, runs `flutter test`, and builds the debug APK.

For manual development, enter `flutter_app` and run:

```powershell
flutter pub get
dart run tool/apply_branding.dart
flutter analyze
flutter test
flutter build apk --debug
```

APK output:

```
flutter_app/build/app/outputs/flutter-apk/app-debug.apk
```

Open the `flutter_app` folder (not the repository root) in Android Studio. The `android/` platform project is generated locally by `flutter create`.

## Data and privacy notes

Profiles and activity records stay on this device until you export/share them. JSON backups can contain your name, contact number, and farm information, so keep exported files private. The Flutter edition does not share its data store with the older native Kotlin/Room edition and does not automatically submit records to a government system. There is no connected login, cloud sync, or official DA workflow in this prototype.

## CI

GitHub Actions runs Flutter analysis, tests, a debug APK build, and uploads the APK as an artifact.
