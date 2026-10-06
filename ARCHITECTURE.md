# AgriVision Architecture

## Phase 1 — Offline-first Farm Operations

Room is the **only runtime source of truth** for structured farm data.

The application boundary is:

`Compose UI → FarmViewModel → FarmRepository → FarmDao → AgriDatabase`

The UI only observes immutable state and sends user intents to the ViewModel. It does not read SharedPreferences, parse JSON storage, or write directly to Room.

## Registry hierarchy

The registry follows:

```
Farmer
└── Farm
    ├── Field 1
    ├── Field 2
    └── Field 3
```

Farmer stores person/beneficiary identity, Farm stores the agricultural holding, and Field stores the spatial production unit. Each field has a stable field ID, area in hectares, local location text, optional latitude/longitude, land tenure, crop, planting date, expected harvest, and current status.

Coordinates are stored as explicit registry data. The field form does not request GPS permission; coordinates can be entered manually. This keeps the registry offline-first while making field records ready for a future map layer.

## Crop lifecycle

Crop records are field-linked and no longer model a crop as only a name, area, and stage.

The lifecycle is represented as:

```
Field
  ↓
Land Preparation
  ↓
Planting
  ↓
Growing
  ↓
Fertilization
  ↓
Pest/Disease Monitoring
  ↓
Harvest
  ↓
Production
  ↓
Sales
```

A crop keeps its linked `fieldId`, crop identity, planting/expected-harvest dates, current lifecycle status, and operational area. A separate lifecycle-event table records each stage update with date, notes, input/activity, quantity, and unit. This provides an auditable crop history and gives future production, sales, incident, and map workflows a stable crop/field relationship.

## Core domain

AgriVision is designed around **Farm Operations**, not a livestock-only model:

- Farm / farmer registry
- Livestock
- Crops
- Feed logs
- Production
- Expenses
- Sales
- Inventory
- Equipment
- Farm tasks
- Field incidents
- Incident audit events
- Agricultural assistance
- Report submission state

Operational relationship:

`Farm → Livestock/Crops → Feed & Production → Expenses/Sales → Inventory → Dashboard → Reports`

Government workflow:

`FARMER → FIELD EVENT → EVIDENCE → VALIDATION → ASSISTANCE → OUTCOME`

## Persistence

Room 2.8.5 is used for the current stable offline database layer. Room provides the local relational storage, reactive DAO queries, and database-backed dashboard totals. citeturn984604search0

All runtime CRUD flows go through `FarmViewModel` and `FarmRepository`.

The previous SharedPreferences + JSON storage path is no longer used by the UI. A one-time `LegacyPreferencesMigrator` reads old local data when present, writes it into Room, then clears the old preferences after a successful migration. This exists only as a transition path for users upgrading from the earlier prototype.

## IDs and farm scope

Operational records carry a `farmId`. The current prototype uses a single local `default-farm` context so the database is multi-farm ready without exposing exact location data.

Records use stable string IDs. Dates are stored as `yyyy-MM-dd` text for predictable local filtering and reporting.

## Presentation structure

The Compose presentation layer is split by responsibility so the Activity only owns the Android entry point and document launchers:

```
ui/
├── dashboard/
├── farm/
├── production/
├── reports/
├── tasks/
├── profile/
├── incidents/
├── components/
└── theme/
navigation/
```

The flow remains:

`Compose UI → FarmViewModel → FarmRepository → FarmDao → AgriDatabase`

## Dashboard calculations

Sales, expenses, production count, and livestock totals are queried by Room/DAO and exposed through ViewModel state.

The Dashboard does not calculate totals from a separate persistence cache.

## Backup and restore

Backup and restore now operate through the ViewModel/repository boundary. JSON is a transport/export format only; it is not the runtime datastore.

Restoring a backup replaces the Room dataset through a database transaction.

## Migration strategy

The current source has completed the persistence architecture migration for the existing farm-management records:

1. Room schema and DAOs
2. Repository boundary
3. ViewModel state and commands
4. One-time legacy preferences migration
5. UI read-only state + ViewModel intents
6. Room-backed backup and restore

The next phase can add dedicated Feed Log UI and richer production records without reintroducing a second datastore.

## Government integration

The DA-oriented workflow is intentionally separated from official transmission.

No claim of official DA API integration is made. A connected, authenticated, authorized backend can be added later without replacing the offline-first local domain model.
