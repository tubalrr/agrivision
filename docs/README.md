# AgriVision Documentation

This directory holds practical developer and deployment guidance.

- [Flutter Edition](FLUTTER_EDITION.md) — what the Flutter app currently contains, setup and build commands, storage boundaries, and backup workflow.
- [Data Dictionary](DATA_DICTIONARY.md) — fields for local Flutter registry/activity records and JSON backup format.
- [DA Privacy & Deployment Checklist](DA_PRIVACY_AND_DEPLOYMENT.md) — safeguards to resolve before using real beneficiary/government data.
- [Release Checklist](RELEASE_CHECKLIST.md) — repeatable verification steps for builds and app distribution.

Repository-level references:

- [Project README](../README.md)
- [Privacy Policy](../PRIVACY_POLICY.md)
- [License](../LICENSE)
- [Native Architecture](../ARCHITECTURE.md)
- [External Integrations](../INTEGRATIONS.md)
- [Contributing](../CONTRIBUTING.md)
- [Security Policy](../SECURITY.md)
- [Flutter-specific README](../flutter_app/README.md)

## Source-of-truth reminder

The native Kotlin app and Flutter edition are separate implementations with separate persistence systems. The native app uses Room for structured farm records; the current Flutter edition uses local SharedPreferences for its registry and farm activity records. A record added in one edition does not automatically appear in the other.
