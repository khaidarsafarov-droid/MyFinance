# Crowd RPM privacy invariant

Crowd RPM is an **opt-in, anonymized numbers-only** summary. Loads, routes, and
rates stay on the device (and in the user's own cloud backup when sync is on).

## What may be aggregated

The only shareable type is `AnonymizedRpmSample`:

| Field         | Meaning |
|---------------|---------|
| `rpm`         | `totalRate / totalMiles` |
| `miles`       | loaded miles |
| `region`      | optional 2-letter US state or `WA-OR` lane — **never** city, street, or facility code |
| `weekNumber`  | ISO week of year (integer only) |

Mapper: `com.truckerload.domain.crowd.CrowdRpmMapper`.
Gate: `CrowdRpmShareGate` (requires `crowdStatsOptIn == true`).

There is **no HTTP Crowd RPM publisher** in this app. The map heatmap is a
**local** view of the driver's own loads. Room table `crowd_rates` is a cache
for future network rows; production code does not upsert it from loads.

## What must never be included

- `Load.id`, `tripId`
- `rawMessage` / OCR text
- exact PU/DEL addresses, city names, facility codes
- calendar dates or timestamps
- user name, nickname, email, account id

Do **not** log Crowd RPM samples (same rule as JWT / local paths / OCR / signed URLs).

## Opt-in

- Default: **off** (`crowdStatsOptIn = false`).
- First visit to the Map heatmap shows a one-shot consent dialog.
- Decline hides the Crowd RPM block entirely (no stub / sample data).
- Settings → Privacy can turn participation on or off at any time.

## Storage of loads

Journal rows live in the per-account Room file `truckerload_<userId>`.
If `LOCAL_ONLY_MODE=true` or `SYNC_BACKEND_URL` is blank, they stay on-device
only. Otherwise they may also sync to the user's cloud backup (`SyncModeStore.allowsCloudCalls()`).
Cloud backup is **not** Crowd RPM and is never shown to other drivers.
