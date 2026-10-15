# AgriVision integrations and source policy

AgriVision is an offline-first Android app. External sources are added only when they provide a clear farm workflow benefit, have acceptable terms, and can fail without blocking local records.

## Active integration

### Open-Meteo — current conditions and five-day outlook

Used by the optional Dashboard Farm Weather card. A user must tap the weather button. The request sends saved latitude/longitude coordinates to Open-Meteo; it does not include farmer names, contact details, finances, crop records, or incident reports. The card displays temperature, feels-like temperature, humidity, precipitation, wind, five days of high/low temperatures and rain outlook, source name, and the device fetch time.

Weather is informational, not a guarantee or a substitute for local observation. When the service fails, local farm tasks, records, maps, reports, and backups remain available. Review Open-Meteo's current terms, attribution rules, and usage limits before distribution:
- https://open-meteo.com/

## Local Feed Logs → Inventory

Feed-log save and stock reconciliation run in one Room database transaction. For a new feed log, the app looks for an inventory item whose name matches the feed name, then checks that units match and that sufficient stock exists. A successful deduction creates a `USAGE` inventory ledger entry linked to the feed-log ID. Re-saving the same feed-log ID cannot deduct stock a second time.

If the inventory item is missing, the units differ, or stock is insufficient, the feed log is still retained and a clear note explains why inventory was not changed. Feed consumption is deliberately not converted into a finance expense automatically because purchase expenses may already be recorded; doing both could double-count costs. Feed stock is stored locally, remains available offline, and is included in normal Room-backed backup/restore.

## How the four reference repositories are used

- **public-apis/public-apis** — API discovery catalogue. Open-Meteo was selected for a specific farm use case. Entries in the catalogue are not assumed to be reliable, free for every use, or production-ready.
- **cporter202/API-mega-list** — additional discovery catalogue. Every candidate must be assessed independently for terms, rate limits, data quality, privacy, uptime, and required credentials; the app must not embed a bulk list of API keys or call arbitrary endpoints.
- **awesome-selfhosted/awesome-selfhosted** — reference for future user-controlled hosting options. No cloud sync server is connected today. Before adding a self-hosted sync target, AgriVision needs authenticated access, TLS, encryption, server-side access controls, backup/restore tests, versioned APIs, conflict resolution, and a clear deletion/export path.
- **Panniantong/Agent-Reach** — developer-side research/agent tooling, not an Android runtime dependency. Its Python/CLI and authenticated web-source workflows are not embedded in the app. Browser cookies, social account sessions, API credentials, and private agent state must never be copied into farm records or shipped in the APK.

## Candidates, not yet enabled

- **NASA POWER** can provide historical meteorological and solar datasets useful for climatology and seasonal planning. It is not yet integrated; it should be added only with a clearly labeled historical window, units, source/update timestamps, and tests for missing data.
- **Reverse geocoding** may help translate coordinates into a human-readable location. Do not send farmer identity or contact details with coordinate lookup. Do not make repeated map-tap calls to a shared public geocoding endpoint. Use an approved provider or properly operated service and obey its current usage policy.
- Other API or self-hosted services remain options, not default dependencies.

## Integration rules

1. Room remains the source of truth for farm records; remote data never overwrites locally entered values.
2. Online calls are user-triggered and non-blocking. Offline workflows must continue to work.
3. Never commit API keys, personal access tokens, passwords, or session cookies.
4. Distinguish estimates from saved facts. Label source, units, time window, and update time.
5. Test error, timeout, malformed-response, empty-data, and offline states.
6. Verify licensing, attribution, commercial-use terms, privacy implications, and rate limits before enabling any new service.
