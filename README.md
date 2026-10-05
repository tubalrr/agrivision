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

## Google Satellite Field Map

The dashboard uses Google Maps Compose in satellite mode. To enable map tiles on your device:

1. Enable **Maps SDK for Android** in your Google Maps Platform project.
2. Create an API key and restrict it to this Android app (package name + SHA-1).
3. In the local project root, add this to `local.properties`:

```properties
MAPS_API_KEY=YOUR_API_KEY
```

The key is read locally and is not stored in the repository. Long-press the actual field on the satellite map to pin its location; the selected coordinates are saved locally on the phone.
