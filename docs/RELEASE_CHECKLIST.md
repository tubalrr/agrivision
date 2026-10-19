# AgriVision Release Checklist

Use this checklist for a distributable build. A successful debug build is not the same as a signed, production-approved release.

## Build and testing

- [ ] Pull the intended commit and verify a clean local working tree.
- [ ] Flutter edition: run `.\build_flutter.ps1` from the repository root.
- [ ] Confirm `flutter analyze` has no issues.
- [ ] Confirm `flutter test` passes.
- [ ] Confirm the Android debug APK build completes and install it on a test phone/emulator.
- [ ] Native edition: run `.\gradlew.bat test lint assembleDebug` on Windows.
- [ ] Test empty state, creating/editing/deleting records, search, reports, backup, valid restore, invalid restore, cancel, and duplicate-ID merge behavior.
- [ ] Verify the launcher icon/app label and that update installation doesn't unexpectedly clear local data.
- [ ] Test on a device without network access to verify the intended offline functions.

## Privacy and data

- [ ] Review `PRIVACY_POLICY.md` against actual code and providers.
- [ ] For real agency use, complete `DA_PRIVACY_AND_DEPLOYMENT.md`; replace generic repository contact with the responsible controller/privacy officer.
- [ ] Ensure no real farmer records, contact numbers, exact coordinates, incident photos, or backup files were committed.
- [ ] Confirm reports/backups are sent only to user-selected destinations and warn staff that exported data may be retained outside the app.
- [ ] Verify permission prompts are understandable and occur only for relevant features.
- [ ] Re-check all API keys, Firebase setup, Google Maps restrictions, Firestore rules, network calls, and third-party terms.

## Signing and distribution

- [ ] Select the application ID and versioning strategy for the edition being released.
- [ ] Generate a release-signed APK/AAB with a keystore held outside the repository/CI logs.
- [ ] Store release credentials in a secure secret store, not in source files.
- [ ] Test the exact signed artifact; record its checksum and commit.
- [ ] Do not describe this prototype as a live DA service, cloud-synced multi-user database, official disease diagnosis, or real-time price feed unless each feature is implemented and verified.
- [ ] Keep dependency notices and project license available to users.
