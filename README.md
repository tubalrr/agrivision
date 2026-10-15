# AgriVision

**AgriVision — See Your Farm. Know What To Do.**

A phone-first agriculture management app focused on understanding farm activity and helping farmers decide what needs attention.

## Android App

Built with:

- Kotlin
- Jetpack Compose
- Material 3
- Offline-first architecture

## Planned Features

- Farm dashboard
- Farm fields and crops
- Planting and harvest tracking
- Seeds, fertilizer, and other inputs
- Farm tasks and calendar
- Expenses and sales
- Inventory
- Farm reports
- Farm map
- Photo field records
- Backup and restore

## Latest Version

**Version 1.19.0** · Android version code: `23`

This release hardens the Field Mapping System. Farm, field and incident boundaries are now checked for valid coordinates, duplicate adjacent points, zero-area shapes and self-crossing edges before they are saved or restored. The map editor shows validation errors directly and asks for explicit confirmation before saving a mapping. A single field location point remains supported; area boundaries require a valid 3+ point polygon. These safeguards protect the operational relationship Farm → Fields → Crops → Incidents without changing existing stored records unless the user edits or restores a map.

**Version 1.18.0** · Android version code: `22`

This release adds finance controls: new finance and production entries use a date picker with ISO dates; Finance filters this month, last month, this year or all time; a six-month cash-flow trend separates dated income and expenses; unpaid sales and expense balances can be marked paid without duplicate transactions; crop and livestock results use only explicitly linked saved records; livestock shows linked production and feed usage separately; exports include transaction IDs, settlement status and source links. These are recorded actuals, not forecasts. Legacy transactions migrate as Paid because earlier app versions did not track settlement, so AgriVision does not guess which historic records remain outstanding.

**Version 1.17.0** · Android version code: `21`

This release hardens backup and restore. The exported `.zip` archive contains the full local JSON data snapshot and attached incident photos, with size checks and SHA-256 integrity verification for every bundled photo. Restore now validates the archive and record relationships, checks duplicate stable IDs and numeric/GPS values, stages photo files privately, and presents a record-count preview plus a final Replace & Restore confirmation before touching the database. The database replacement remains transactional; failed validation or restoration preserves existing Room data, and restore replaces rather than merges records. Older JSON backups remain supported, with inaccessible photo references clearly flagged and detached rather than silently left broken. Guardrails cap each photo at 12 MB, aggregate photos at 250 MB, and the manifest at 64 MB.

**Version 1.16.0** · Android version code: `20`

This release adds optional source links for financial income/expenses, a crop cost-and-profit summary using only explicitly linked transactions, Room migration 10 → 11, and backup support for the new links. It also adds opt-in local reminders based on saved due tasks, near-term harvest dates, low/expired stock, and active high-priority/review-pending cases; optional GPS coordinate capture for fields/incidents; private local copying of attached incident photos; coordinate range validation; JUnit tests for crop lifecycle/cost calculations; and CI checks for tests, lint, debug APK, and an unsigned release APK artifact. Build status does not replace on-device validation. The release APK is unsigned until release-signing credentials are configured.

Production-readiness boundaries: data remains on-device/offline-first; cloud account sync and official DA API transmission are not configured. New ZIP backups include incident photo files for cross-device restore. Older JSON-only backups do not contain photo bytes, so photos can only be kept if their files are still readable on the restoring device. Daily notification delivery requires the user to enable reminders and allow notifications; Android may defer background work.

**Version 1.15.1** · Android version code: `19`

This patch prevents accidental app exits from Android edge-swipe/back gestures. Back from any non-Dashboard tab returns to Dashboard; back while on Dashboard opens an explicit Exit AgriVision confirmation with Stay in app and Exit app actions.

**Version 1.15.0** · Android version code: `18`

This release reorganizes the dashboard around farmer priorities: Farm Action Center and financial/health snapshots appear before incident history and analytics. Duplicate action-queue/report prompts are removed, recent incident previews are capped at three, spacing and card proportions are tightened, and dashboard accent colors better respect the selected light/dark palette. Existing records, task shortcuts, inventory actions, case triage, and analytics remain connected to saved farm data.

**Version 1.14.1** · Android version code: `17`

This patch fixes theme palette updates to remain compatible with existing non-composable farm helpers. The selected appearance still persists locally and updates both the shared surface colors and Material 3 color scheme.

**Version 1.14.0** · Android version code: `16`

This release adds a persistent Appearance setting with two distinct visual themes: Field & Harvest Light and AgriVision Pro Dark. The Pro palette uses deep forest-green surfaces, higher-contrast text, luminous green actions and restrained harvest-brass accents. The selection is saved locally and applies across screens using the shared palette and Material 3 color scheme. Reset defaults returns to Field & Harvest.

**Version 1.13.0** · Android version code: `15`

This release refreshes the global Field & Harvest visual system with a clearer forest-green, soft-sage and harvest-gold palette, more complete Material 3 semantic colors for surfaces, outlines and error states, and a refined rounded bottom-navigation dock with stronger active-tab contrast. Existing screens, records and navigation behavior are retained.

**Version 1.12.0** · Android version code: `14`

This release adds manual inventory stock movements for Stock In, Usage, and Loss/Waste. Every movement updates the current balance and is saved to the existing inventory transaction history in the same database transaction. The app blocks usage/loss entries that exceed available stock and shows recent manual movements, including their dates and notes. No new database table or sample data is introduced.

**Version 1.11.1** · Android version code: `13`

This patch makes overdue task metrics actionable. Tapping Overdue in Tasks filters to unfinished past-due tasks across all dates, and the Dashboard's overdue count opens the same filtered view. The filter is also available alongside All, Open, and Completed. The count and list are computed from saved task dates and completion state.

**Version 1.11.0** · Android version code: `12`

This release replaces the gamified achievements card with a practical Farm Action Center. It surfaces overdue and upcoming tasks, crops near their expected harvest dates, low/out-of-stock or soon-to-expire inventory, and high-priority or review-pending field incidents. Action buttons open the corresponding Tasks, Inventory, Crops, or incident review workflow. Reminders are derived from existing saved data and date fields; no sample farm statistics or new data schema are introduced.

**Version 1.10.0** · Android version code: `11`

This release adds Farm Achievements to the dashboard: locally calculated XP, farm levels, and 11 milestones for field registration, crop/livestock records, production, task completion, finance, inventory, incidents, and assistance. Progress updates from existing farm records with visible locked/earned badges and a next milestone, without a new database table or fictional sample data.

**Version 1.9.0** · Android version code: `10`

This release adds a dashboard analytics center with six-month sales-versus-expenses charts and production activity trends derived from dated records. Current-month sales, expenses, and net cash flow summaries use recorded data; empty-state guidance is shown when there are no recognized dates rather than inventing values. Existing farm health, task triage, incident workflow, and farm operations summaries remain available.

**Version 1.8.0** · Android version code: `9`

This release adds App Preferences for remembering the last tab, toggling navigation haptics, confirming before backup import, and using a compact bottom navigation bar. Preferences are saved locally on the device, and users can reset them to defaults. Backup restore now offers a confirmation prompt by default.

Version 1.7.0 upgraded the Profile Center with a farmer identity hero, registration completeness indicator, farm detail cards, at-a-glance field and assistance metrics, more readable registry identifiers, and full-width backup/restore actions. The existing farmer registry dialog, field registry, assistance workflow, and local Room-data backup flows are preserved.

Version 1.6.0 upgraded Tasks with a dedicated farm task center, overdue/open/completed/stock-alert metrics, date shortcuts for today and tomorrow, a date picker, task status filters, category icons, clearer due-state indicators, and farm-specific category chips when creating tasks. Task records continue to use the existing local storage model and save/toggle flow.

## Optional online data sources

AgriVision remains offline-first. These public data sources are candidates for optional, read-only farm insights; they do not upload farm records or replace local records.

- **Open-Meteo** — weather forecasts by coordinates for planning field activities. Review the current API terms and attribution requirements before production use: https://open-meteo.com/
- **NASA POWER** — historical and near-real-time meteorological and solar data that can support longer-term crop and farm planning: https://power.larc.nasa.gov/docs/
- **Nominatim (OpenStreetMap)** — optional reverse geocoding for map coordinates. Use only in compliance with the public service usage policy; do not send personal farmer details: https://nominatim.org/release-docs/latest/api/Overview/

Integration rules:
1. Online lookups are opt-in and must show when the device is offline.
2. Never put API keys or personal data in source control, URLs, logs, or analytics.
3. Treat forecasts as estimates, display source and update time, and never overwrite a farmer's saved data.
4. Keep the existing local workflows usable without internet access.
5. Recheck licensing, service limits, attribution, and commercial-use conditions before release.

Reference lists: [public-apis](https://github.com/public-apis/public-apis), [awesome-selfhosted](https://github.com/awesome-selfhosted/awesome-selfhosted), [Agent-Reach](https://github.com/Panniantong/Agent-Reach), and [API-mega-list](https://github.com/cporter202/API-mega-list). These are discovery catalogs, not guarantees that every linked service is free, stable, or suitable for production.

## Field Mapping System

The Map tab is a functional operational layer, not a decorative map. It uses Google Maps Compose in satellite mode and reads/writes the local Room registry.

The map can show:

- Farm boundary polygon
- Individual field boundaries and field coordinates
- Crop linked to each field and the crop's current lifecycle stage
- Incident markers, severity and workflow status
- Incident affected-area polygons
- Latitude/longitude captured from map points and stored with the field/incident
- Map editing with long-press points, undo, clear and save

The relationship is:

```
Map
 ├── Farm
 ├── Fields
 ├── Crops
 └── Incidents
```

Map geometry is stored locally for the offline-first workflow and included in backup/case-package JSON. The app does not request device location permission for this feature.

### Local Google Maps setup

The API key is kept out of source control. Add this to the project root `local.properties`:

```properties
MAPS_API_KEY=YOUR_API_KEY
```

Then enable **Maps SDK for Android** for the Google Maps Platform project used by the key. The Android manifest receives the value through the `com.google.android.geo.API_KEY` metadata placeholder.
