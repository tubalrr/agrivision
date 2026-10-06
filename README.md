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

## Status

🚧 Android foundation

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

