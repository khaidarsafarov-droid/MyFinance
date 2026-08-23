# Crowd RPM privacy invariant

**Local-only:** Map and Stats use only the signed-in user's journal on this
device. There is no community RPM share UI, opt-in toggle, or HTTP publisher.

## Local map aggregation

Heatmap / lane summaries are built from the driver's own loads via
`CrowdMapAggregator` / `CrowdRpmMapper`. Naming still says "crowd" for historical
domain types; scope is the current account only.

Room table `crowd_rates` remains as an unused cache schema; production code does
not upsert network rows into it.

## What must never leave the device for "community" use

If a future network feature is added, the only candidate type is
`AnonymizedRpmSample` (numbers + coarse US state / lane — never trip IDs,
addresses, OCR text, or account identity). Any send must go through
`CrowdRpmShareGate` with an explicit new opt-in (not present in the app today).

## Storage of loads

Journal rows live in the per-account Room file `truckerload_<userId>`.
If `LOCAL_ONLY_MODE=true` or `SYNC_BACKEND_URL` is blank, they stay on-device
only. Otherwise they may also sync to the user's own cloud backup
(`SyncModeStore.allowsCloudCalls()`). Cloud backup is never shown to other
drivers.
