# AgriVision Privacy Policy

**Last updated: 19 October 2026**

This notice describes the behavior documented in the AgriVision repository's current native Android app and separate Flutter edition. It is a project privacy notice for a development/open-source application; it is not a certification of legal compliance and is not a substitute for the privacy notice of a government agency or organization that deploys the app.

## 1. What the app can store

Depending on which edition and features are used, records entered by a user may include:

- Farmer or beneficiary names and contact details.
- Farm names, province, municipality, barangay, land tenure, farm area, commodities, and manually entered or saved field coordinates.
- Crop, livestock, task, input/inventory, production, income/expense, incident, assistance, review-note, and status-history records.
- Photos or local file references attached to field/incident workflows in the native Android app.
- Locally generated CSV, JSON, PDF, or ZIP reports/backups when the selected workflow produces them.

Only enter information that you are authorized to record. Do not place passwords, government IDs, bank credentials, or unrelated sensitive information in notes.

## 2. Where records are stored

### Flutter edition

The Flutter edition stores farmer application records and farmer-linked farm activities in the app's local SharedPreferences storage on the device. It does not currently sync these operational records to a cloud database or official Department of Agriculture (DA) server.

Reports can be shared using the device's operating-system share sheet. The manual JSON backup contains farmer and farm activity data. Restoring a backup merges records by stable IDs: matching IDs are updated from the backup, new records are added, and local-only records are retained. The backup importer validates the format and relationships before saving.

### Native Kotlin Android edition

The native Android app stores its structured farm-management data in a local Room database and uses app-private file storage for applicable evidence files. Operational farm records are not automatically submitted to an official DA service. Native backup/restore workflows have different semantics from the Flutter edition; review the in-app confirmation before restoring.

Local storage is not a promise of complete protection against someone who already has access to an unlocked device. Exported JSON/CSV data and generated backup files are not encrypted by an additional AgriVision-specific encryption layer. Keep devices locked and store backup files privately.

## 3. Network services and third parties

Network behavior depends on which native-app features are configured and used:

- **Weather (native Android):** when a user requests farm weather, the configured Open-Meteo integration sends saved latitude/longitude coordinates for the forecast. The app documentation says farmer names, contact details, financial records, and incident reports are not included in that weather request. Review [Open-Meteo](https://open-meteo.com/) terms and privacy information before deployment.
- **Maps (native Android):** the native app uses Google Maps Compose when its Maps API configuration is provided. Map display can involve requests to Google services. Review Google's current Maps Platform terms, privacy disclosures, and key restrictions.
- **Optional Firebase account/profile support (native Android):** if a developer or deploying organization supplies `app/google-services.json` and enables the configured Firebase flow, Firebase Authentication handles account sign-in and Firestore stores limited user-profile metadata. The repository's Firestore rules allow users to access their own `users/{uid}` profile and deny other collections by default. This is not farm-record cloud sync. The owner of the configured Firebase project controls its settings, logs, retention, and access.
- **File picker and share sheet (Flutter):** the file picker reads the JSON file deliberately selected by the user. When a report or backup is shared, the receiving app/service chosen in the Android share sheet may receive and retain that content under its own policies.
- **Normal platform/network metadata:** Android and external providers may process connection and diagnostic metadata as part of operating their services, according to their own terms.

The current Flutter edition has no connected central backend, authenticated DA account system, advertising service, or remote farm-record sync configured. The app does not claim live DA submission.

## 4. Why data is used

Locally entered records are used to display the registry, link activities to the correct farmer/farm, filter and prepare reports, and create backups/restores. Optional third-party calls are used only for the corresponding feature (for example, weather or configured sign-in).

The organization deploying AgriVision is responsible for determining and communicating its lawful basis, official purposes, authorized users, sharing arrangements, and retention schedule for any real beneficiary or government records.

## 5. Permissions

The native Android manifest declares Internet, camera, optional coarse/fine location, and notification permissions. Runtime requests should occur in connection with the relevant feature. Camera access is used by applicable photo/evidence flows; location is optional and may be used for user-triggered coordinate capture; notification permission is needed for enabled reminders. The Flutter edition uses the system file picker and share sheet when the user chooses those actions.

You can deny a permission in Android settings, but the feature that relies on it may not work.

## 6. Retention and deletion

- Local database/preferences records generally remain until removed through the relevant app workflow, app data is cleared, or the app is uninstalled.
- A backup exported or copied outside AgriVision is a separate file. Deleting a record from the app does not delete copies saved to Downloads, cloud drives, email, messaging apps, or another recipient's device.
- Firebase account/profile data, if configured by a deploying organization, follows that project's configuration and the applicable provider/organization retention rules.
- This development repository does not define a centrally enforced retention period for an organization deploying it.

## 7. Security and responsible use

Use a screen lock, limit access to devices containing farm records, store backups in trusted locations, and verify recipients before sharing reports. Do not commit production data, backups, API keys, keystores, Firebase configuration, or credentials to source control. The app's current offline-first prototype is not a substitute for organization-managed access controls, encryption-at-rest requirements, audit administration, disaster recovery, or a formal incident response procedure.

## 8. Data rights and questions

Individuals may have privacy rights under applicable law, including the Philippine Data Privacy Act of 2012 (Republic Act No. 10173), as applicable to the responsible personal information controller. Review the [National Privacy Commission's Data Privacy Act resource](https://privacy.gov.ph/Data-Privacy-Act/) and its [Implementing Rules and Regulations](https://privacy.gov.ph/implementing-rules%20regulations-data-privacy-act-2012/).

For this public repository, use the [GitHub repository](https://github.com/tubalrr/agrivision) to contact the project maintainer about documentation. Do not post a farmer's name, contact number, exact location, incident details, backup file, or other personal data in a public issue. Before using this software with real government/beneficiary data, the deploying organization must publish its own complete privacy notice and provide an actual privacy-officer/controller contact, request process, and applicable retention schedule.

## 9. Changes to this notice

This notice must be updated when material app behavior, providers, stored data, or deployment arrangements change. The organization distributing a modified build is responsible for checking the notice against that build and its actual service configuration.
