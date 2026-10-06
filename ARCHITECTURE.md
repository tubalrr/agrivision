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

## Livestock lifecycle

Livestock is modeled as a group with a stable group ID, initial population, current population, and status rather than only a count.

The lifecycle is represented as:

```
Livestock Group
  ↓
Population
  ↓
Feed
  ↓
Health
  ↓
Mortality
  ↓
Production
  ↓
Sales
```

A separate livestock lifecycle event table records the operational history for each group, including stage, date, notes, feed/medicine/product reference, quantity, unit, and amount. Population events can set the current population while mortality events reduce it, keeping the group count synchronized with recorded events.

## Production source links

Production records must reference an actual farm source instead of storing only a generic product and quantity.

Supported production sources are:

```
Crop → Field → Harvest → Production
Livestock Group → Production
```

Each production record stores `sourceType`, `sourceId`, and (for crop production) `fieldId` plus the recorded area. This allows a report such as “Rice · Field A · 2.5 ha · 4,200 kg · 2026-10-05” to remain traceable to the registered crop cycle and field.

New production entries require a valid crop or livestock source. Legacy records may remain unlinked for backward compatibility and are displayed as legacy/unlinked records until they are re-entered with a source.

## Inputs & inventory

Inventory is a structured stock system rather than a display-only list.

Each inventory item keeps category, remaining stock, unit, purchase price, supplier, date acquired, and expiry date. Inventory transactions record purchases and usage with a source reference.

Farm activities can automatically deduct inputs when the lifecycle event selects an inventory item and records a quantity. Crop activities that consume inventory include land preparation, planting, fertilization, and pest/disease monitoring. Livestock activities that consume inventory include feed and health records.

The deduction and activity record are written in the same Room transaction. Exact item name and normalized unit matching prevents silent deduction of the wrong stock. Insufficient or mismatched stock is recorded as an activity note instead of making the inventory quantity negative.

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
