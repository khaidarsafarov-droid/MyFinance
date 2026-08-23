# Этап 7 — Дополнительные проверки

**Дата:** 2026-08-23

---

## a11y

| Signal | Result |
|--------|--------|
| `Icon(` vs `contentDescription` in presentation | ~137 icons, ~161 descriptions — **presentation icons labeled** |
| `semantics { }` usage | 4 files only — weak |
| `testTag` | 0 |
| `AccessibilitySettingsSection` | Settings only |

**Gap:** Low systematic semantics; spot-check back buttons recommended.

---

## i18n

| Signal | Result |
|--------|--------|
| String keys | 1,474 × 3 locales (default, ru, en) — **fully synced** |
| `stringResource(` calls | ~896 |
| Hardcoded `Text("…")` | ~11 (mostly dynamic data) |
| Duplicate RU catalogs | `values/` + `values-ru/` both 1525 lines (Stage 3 DUP-19) |
| Voice phrases | RU/EN hardcoded in Kotlin vs strings.xml |

**Gap:** TaxTracker EN labels; voice keyword drift.

---

## Logging / monitoring

- `LogRedactor` on Telegram paths ✅
- Auth/Drive/cloud: exception messages logged without redaction
- `CameraViewModel` logs file paths
- No user-visible sync failure monitoring (`SyncStatusTracker` unused in UI)

---

## UI/UX consistency

- Cancel: mostly `R.string.common_cancel`; **exception:** `GoogleDriveSyncSection` uses `android.R.string.cancel`
- Loading states: 17/22 ViewModels lack full loading+error model (per QUALITY inventory)
- Empty states: Home uses flow semantics; Analytics implicit empty lists

---

## Dependencies

From `libs.versions.toml` — pinned, comments note compileSdk 36 cap blocks some updates. Lint baseline: 24 NewerVersionAvailable.

Unused deps (Stage 4): Retrofit, maps-utils, logging-interceptor.

---

## .env.example vs code

- `.env.example`: 20 vars — minimal subset
- Missing from example: `APP_ENV`, `HOST`, `PORT`, S3/MinIO wiring vars used by docker-compose
- All example vars consumed by `AppConfig.fromEnvironment()` ✅

---

## Documentation staleness

| Doc | Issue |
|-----|-------|
| AGENTS.md | «38 tests» → actual ~748 |
| PROJECT_OVERVIEW.md | «Room v8» → AppDatabase v34 |
| AGENTS.md | «single-module» vs README multi-module |
| QUALITY_1000 inventory | File counts, lint counts outdated |

---

## Статус

**Этап 7 завершён.**
