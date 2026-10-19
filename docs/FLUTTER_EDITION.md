# Flutter Edition Guide

**Status:** active development / partial migration.

The Flutter edition is a separate app in `flutter_app/`. It is not yet a replacement for the full native Kotlin application.

## Prerequisites

- Flutter stable SDK; the repository's CI baseline is Flutter 3.47.x.
- Android Studio and Android SDK/toolchain.
- A connected Android phone or emulator for interactive testing.

## Build on Windows

From the repository root:

```powershell
git pull origin main
.\build_flutter.ps1
```

The helper will:

1. Create the Android platform project if it does not exist.
2. Run `flutter pub get`.
3. Apply the maintained AgriVision launcher icon and app label.
4. Run `flutter analyze` and `flutter test`.
5. Build a debug APK.

Output:

```text
flutter_app/build/app/outputs/flutter-apk/app-debug.apk
```

For manual iteration:

```powershell
cd flutter_app
flutter pub get
dart run tool/apply_branding.dart
flutter analyze
flutter test
flutter build apk --debug
```

## Implemented Flutter capabilities

- Material 3 DA staff overview with counts derived from local saved records.
- Farmer & Farm Registry with search, create/edit, review status, and notes.
- Farmer-linked Farm Operations tabs for Crops, Livestock, Finance, and Tasks.
- Add/edit/delete farm activity entries linked to the selected farmer's application ID.
- DA report builder for Farmer Applications, Crop Records, Livestock Records, Finance Records, Task Register, and Complete Farm Activity.
- CSV and JSON report sharing via the Android share sheet.
- Full JSON local backup/export and validated restore/merge.

## Persistence boundaries

- Farmer applications: SharedPreferences key `agrivision_flutter_registry_v1`.
- Farm activities: SharedPreferences key `agrivision_flutter_operations_v1`.
- JSON backup format name: `agrivision.local-backup`, schema version `1`.
- Backups are limited to 10 MiB by the importer.
- Restore validates required IDs and farmer references. Matching IDs are updated by the imported record; new IDs are added; local-only records remain. Restore does not automatically delete existing records.
- Storage is on the current device. It is not automatically migrated from or synced with native Room.

## Known limitations

The Flutter edition does not yet include all native modules such as the persisted satellite/farm map, complete crop/livestock life cycles, weather, scan history, market prices, full native DA PDF/ZIP reports, a verified DA staff login, or an official DA submission workflow. The Flutter finance/livestock/crop/task areas are general farm activity records; they are not yet equivalents of the complete native domain modules.

There is no connected central server in this edition. Do not promise multi-device sync, centralized access control, official DA submission, live market prices, or trained disease detection unless those are separately implemented and tested.

## Dependencies

Current primary plugins:

- `shared_preferences`: local key-value persistence.
- `file_picker`: user-selected backup JSON import.
- `share_plus`: report and backup sharing.

Version updates should be validated together because native Android plugin and Gradle compatibility matters. Run the full helper script after dependency changes.
