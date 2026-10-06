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

Farmer stores person/beneficiary identity, Farm stores the agricultural holding, and Field stores the spatial production unit. Each field has a stable field ID, area in hectares, local location text, optional latitude/longitude, land tenure, crop, planting date, expected harvest, current status, and optional polygon geometry.

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

## Financial system

Financial records are categorized instead of using free-form labels.

Income:
```
Income
 ├── Crop Sales
 ├── Livestock Sales
 └── Other Income
```

Expenses:
```
Expenses
 ├── Seeds
 ├── Fertilizer
 ├── Feed
 ├── Labor
 ├── Fuel
 ├── Medicine
 └── Equipment
```

The current financial KPI remains:

Income - Expenses = Net Income

Expense records now keep a separate date and note, while income records persist their income category. The finance screen shows totals by category so the farm can see where money is earned and spent.

The next costing phase can add explicit allocation links from expenses to a crop, field, or production cycle, allowing AgriVision to calculate cost per crop/field/production cycle without replacing the financial ledger.

## Incident & DA workflow

Incidents are first-class workflow records, not UI-only status flags.

The enforced workflow is:

Incident Draft
   ↓
Submitted
   ↓
Under Review
   ↓
Verified
   ↓
Assistance
   ↓
Completed

A report can also move from Under Review to Returned, then must be submitted again. The repository enforces valid transitions so the UI cannot skip review or close a case prematurely.

Each incident stores a stable incident ID plus farmer ID, farm ID, field ID, commodity, affected area/quantity, incident date, severity, evidence reference, optional incident coordinates, reviewer, linked assistance request ID, review notes, and resolution.

Status history is stored as immutable incident events. Each event records the previous status, new status, actor, note, and timestamp. This provides an auditable case timeline independent of the current incident status.

Assistance remains linked through incidentId and assistanceRequestId. Requesting assistance moves a verified incident into Assistance in the same Room transaction. Completion is allowed only when the linked assistance record is marked completed and a resolution is supplied.

Coordinates are explicitly entered as incident data; the form does not request GPS permission. Incident affected-area geometry can also be stored as a polygon. Evidence is retained as a local file reference in the offline-first phase. Official DA transmission is still a future connected-backend concern.

## Field Mapping System

The Map tab is a real operational layer backed by the same Room registry used by the rest of the app. It uses satellite imagery as the basemap and renders farm/field/incident geometry from persisted records.

The layers are:

```
Map
 ├── Farm boundary
 ├── Fields
 │    ├── field polygon
 │    └── field coordinate
 ├── Crops
 │    └── field-linked crop + current stage
 └── Incidents
      ├── incident coordinate
      ├── affected-area polygon
      └── severity/status
```

Mapping actions are performed in the Map editor. A long press adds a coordinate point to the current draft. Farm and incident areas require at least three points for a polygon; a field can be saved with one point for location-only mapping or three or more points for a boundary. Saving a field/incident polygon also stores its centroid as the record's latitude/longitude so both the area and its representative coordinate remain available.

Geometry is serialized into Room text columns for the offline-first schema and is included in backup/case-package JSON. The map intentionally does not request device location permission and never depends on live GPS for rendering existing records.

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
7. Structured financial categories

The next phase can add dedicated Feed Log UI and richer production records without reintroducing a second datastore.

## Database versioning

The current Room schema is version 10. Migration 7 → 8 adds persisted incomeCategory to sales, migration 8 → 9 adds structured incident workflow metadata plus status-history fields, and migration 9 → 10 adds farm/field/incident map geometry.

## Government integration

The DA-oriented workflow is intentionally separated from official transmission.

No claim of official DA API integration is made. A connected, authenticated, authorized backend can be added later without replacing the offline-first local domain model.


## DA-ready reports (Priority 12)

AgriVision reports are generated from the Room-backed `FarmSnapshot`, not directly from UI state. The report layer supports 11 report types: Farmer Profile, Farm Registry, Crop Production, Livestock Inventory, Farm Inputs, Expenses, Sales, Incidents, Assistance, Harvest, and Field Summary.

Each report can be exported as PDF, CSV, or JSON. PDF uses Android `PdfDocument`; CSV is escaped for spreadsheet import; JSON is versioned with report metadata. Harvest is derived from production records marked `Harvest`, while Farm Inputs combines inventory with recorded crop/livestock lifecycle inputs. Android's document picker is used for user-selected destinations.
