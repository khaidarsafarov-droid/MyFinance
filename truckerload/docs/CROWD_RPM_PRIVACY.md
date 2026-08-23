# Crowd RPM privacy invariant

The app is **local-first**. Geographic efficiency is a heatmap of **this
driver's own loads** on the device. There is no community opt-in, no
anonymized upload, and no HTTP Crowd RPM publisher.

## What the map shows

Heatmap rows come from local loads via `CrowdRpmMapper` (rpm + miles +
2-letter states). Other drivers' rates are never requested or sent.

`CrowdRpmShareGate.payloadOrNull` always returns `null`.

Room table `crowd_rates` is an unused cache leftover; production code does
not upsert it from loads and does not publish it.

## What must never leave the device for Crowd RPM

- `Load.id`, `tripId`
- `rawMessage` / OCR text
- exact PU/DEL addresses, city names, facility codes
- calendar dates or timestamps
- user name, nickname, email, account id

Do **not** log Crowd RPM samples (same rule as JWT / local paths / OCR / signed URLs).

## Storage of loads

Journal rows live in the per-account Room file `truckerload_<userId>`.
If `LOCAL_ONLY_MODE=true` or `SYNC_BACKEND_URL` is blank, they stay on-device
only. Otherwise they may also sync to the user's cloud backup (`SyncModeStore.allowsCloudCalls()`).
Cloud backup is never shown to other drivers.
