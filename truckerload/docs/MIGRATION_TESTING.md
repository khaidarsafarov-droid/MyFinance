# Room migration testing (Phase 1)

## Schema export

- `AppDatabase` uses `exportSchema = true` (Room 2.7, version **30**).
- KSP writes JSON under `app/schemas/com.truckerload.data.local.AppDatabase/`.
- Committed snapshots: **`28.json`**, **`29.json`**, **`30.json`** (30 = voice room description + moderator).
- `app/build.gradle.kts` sets `room.schemaLocation` and mounts that folder as androidTest assets.

Regenerate after entity changes:

```bash
cd truckerload
sh ./gradlew :app:kspDebugKotlin
```

## Tests

| Test | Where | What |
|------|--------|------|
| `migrate6To30_smoke` | androidTest + Robolectric | Fixture v6 → all migrations → row count + PRAGMA columns |
| `migrate22To23` | androidTest | Index adds on diesel/paychecks |
| `migrate25To26` | androidTest + Robolectric | Media columns + queue; idempotent |
| `migrate27To28` | androidTest | `serviceName` via `addColumnIfMissing` |
| `migrate28To29` | androidTest | Uses exported schemas, `validate=true` |
| `migrate29To30` | androidTest | Voice room `description` + `moderatorId` |
| `MigrationHelpersTest` | unit | `hasTable` / `hasColumn` / `addColumnIfMissing` |
| blocked legacy | Robolectric | v1–5 throw `UnsupportedDatabaseUpgradeException` |

CI-friendly path: `sh ./gradlew :app:testDebugUnitTest` (Robolectric).

Device path: `sh ./gradlew :app:connectedDebugAndroidTest` (needs emulator).

## Helpers

`MigrationHelpers.kt`:

- `hasTable` / `hasColumn`
- `addColumnIfMissing(table, column, definitionSql)` — idempotent ALTER
- `dropColumnIfExists` — best-effort (SQLite 3.35+)
- `execLogged` — DDL with log line

Migrations **25→26** and **27→28** use `addColumnIfMissing` so re-entry after a partial upgrade does not crash.

## Pre-v6 policy

`fallbackToDestructiveMigrationFrom(1..5)` was removed. Opening v1–5 throws
`UnsupportedDatabaseUpgradeException` with:
`Требуется переустановка. Сделайте бэкап.`

## Session repairs

Per-user one-shot flag `session_repair_v1_done_${userId}` in `StartupRepairStore`.
Traced via `Trace.beginSection("session_repair")`; if wall time > 500ms → Crashlytics
`slow_session_repair`. Inflated-miles repair uses SQL prefilter
`totalMiles >= 10000 AND totalRate/totalMiles < 0.5` (no `drivenMiles` column).
