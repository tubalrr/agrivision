# Security Policy

## Scope

AgriVision is an active development repository with a native Kotlin Android app and a separate Flutter edition. It is not currently represented as a hosted DA service or a production multi-user backend.

## Reporting a concern

Please do not publish working credentials, private farm records, backup files, exploitable details, or personal information in a public issue. If GitHub private vulnerability reporting is available for this repository, use that feature. Otherwise, contact the repository maintainer privately through GitHub and provide only the minimum information needed to investigate.

A report should include the affected edition, relevant commit/version, impact, and safe reproduction steps. Remove names, phone numbers, exact coordinates, and other real personal data from logs and screenshots.

## Current security boundaries

- Flutter farmer/farm activities are stored locally and are not synced to an authenticated central server.
- Manual Flutter JSON backups are data files, not encrypted archives.
- The native app may enable Firebase Authentication/profile storage only when a developer supplies its own Firebase configuration. The repository's Firestore rules deny all non-profile collections by default.
- Official DA transmission, centralized role-based authorization, managed key rotation, and organization-level incident response are not provided by this prototype.

## Maintainer expectations

Prioritize issues that expose personal data, permit unauthorized record access or mutation, bypass restore validation, expose credentials, or allow unsafe external input. Fixes should include regression tests and updates to the privacy/security documentation where needed.

There is no guaranteed response time or supported-release SLA for this development project.
