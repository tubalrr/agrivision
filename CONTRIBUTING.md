# Contributing to AgriVision

Thanks for helping improve AgriVision. The repository contains two separate applications: the native Kotlin/Jetpack Compose app at the repository root and a Flutter edition in `flutter_app/`. Keep changes and their verification steps clear about which edition they affect.

## Before opening a change

1. Describe the problem and expected behavior.
2. Use real data only in authorized local testing; never commit farmer or beneficiary records, screenshots with personal data, backups, credentials, keystores, or API tokens.
3. For third-party services, verify the current license, attribution terms, privacy impact, credentials, usage limits, and offline failure behavior.
4. Add or update tests and documentation when data schemas, export formats, permissions, or user-visible behavior change.

## Flutter development

Use Flutter stable 3.47.x (CI baseline) and Android Studio. From the repository root on Windows:

```powershell
git pull origin main
.\build_flutter.ps1
```

The script resolves dependencies, applies the AgriVision branding, runs analysis and widget/unit tests, and builds a debug APK. The expected APK path is:

```text
flutter_app/build/app/outputs/flutter-apk/app-debug.apk
```

For manual, focused feedback while editing:

```powershell
cd flutter_app
flutter pub get
dart run tool/apply_branding.dart
flutter analyze
flutter test
flutter build apk --debug
```

The Flutter edition currently uses local SharedPreferences storage. Do not assume its records have migrated to or sync with the native app's Room database.

## Native Android development

From the repository root:

```powershell
.\gradlew.bat test lint assembleDebug
```

If a command fails, include the failing command and relevant error output in the change description. Do not include local paths that reveal secrets or user data.

## Code expectations

- Keep local records traceable to a farmer/farm ID; never invent demo records in production lists.
- Validate imports, IDs, amounts, and references before mutating persistent data.
- Preserve user records in migrations and restore flows unless a clearly labeled destructive action is confirmed.
- Keep external services optional and preserve core offline workflows where designed.
- Use accessible labels, empty states, error messages, and loading feedback.
- Update `PRIVACY_POLICY.md`, architecture docs, and release notes when data flows or permissions change.
