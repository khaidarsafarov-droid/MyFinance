# Этап 7 — Дополнительные проверки (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этапы 1–6 v2  
**Метод:** static grep + string catalog diff. **Код не менялся.**

---

## a11y (accessibility)

| Signal | Result |
|--------|--------|
| `Icon(` in presentation | ~132 calls |
| `contentDescription` in presentation | ~159 — **most icons labeled** |
| `contentDescription = null` on decorative icons | Present (ProfileHeader, Theme) — acceptable if adjacent text |
| `semantics { }` blocks | **4 files** — weak systematic coverage |
| `testTag` | **0** — no Compose UI test hooks |
| `AccessibilitySettingsSection` | Reduce-motion toggle in Settings ✅ |

**Gaps:** No TalkBack audit performed; back buttons generally use `R.string.common_back` ✅. TaxTracker (unwired) has proper back CD but hardcoded tax labels.

---

## i18n (локализация)

| Signal | Result |
|--------|--------|
| String keys (`<string name=`) | **1,477** per locale — **fully synced** (default, ru, en) |
| `stringResource(` in presentation | ~880 calls |
| Hardcoded `Text("…")` (3+ chars) | ~11 — mostly dynamic `$` amounts / TaxTracker EN |
| Duplicate RU catalogs | `values/` + `values-ru/` — **same 1,477 keys** (Stage 3 DUP-19); redundant maintenance |
| Voice phrases | RU/EN hardcoded in Kotlin + `voice_assistant.xml` — drift risk vs nav strings |
| Community/friends strings | ~20 keys × 3 locales — **orphaned feature** (shortcuts only) |

**Дельта v2:** `backup_restore_confirm_*` strings added RU + EN (P0 restore warning) ✅.

**Gaps:** `TaxTrackerScreen` — `"SE Tax (~15.3%)"`, `"Federal Tax:"` hardcoded EN. Voice keyword tables not generated from `strings.xml`.

---

## Logging / monitoring

| Area | Result |
|------|--------|
| `LogRedactor` | Telegram/sync paths ✅ |
| Auth / Drive / cloud sync logs | Exception messages often **without** redaction (`CloudSyncWorker`, `UserComponentManager` logs userId) |
| Sensitive fields | CDL encrypted in Room ✅; cloud profile JSON omits CDL on serialize ✅ |
| User-visible sync failures | **`SyncStatusTracker` unused in UI** — no monitoring surface |
| Crash reporting | Firebase optional (`FIREBASE_CONFIGURED`) — no mandatory prod crash pipeline |

---

## UI/UX consistency

| Pattern | Result |
|---------|--------|
| Cancel button | Mostly `R.string.common_cancel`; **exceptions:** `SettingsDataSection`, `GoogleDriveSyncSection` → `android.R.string.cancel` |
| Destructive actions | Restore now has **confirm dialog** with media wipe disclosure ✅ (P0 partial) |
| Loading / error states | ~17/22 ViewModels lack full loading+error model (unchanged) |
| Empty states | Home: flow semantics ✅; Analytics: implicit empty lists |
| Offline | Connectivity banner present ✅ |
| Same action, different behavior | Backup restore vs cloud hydrate — different media handling (documented Stage 2) |

---

## Mobile / adaptive

| Signal | Result |
|--------|--------|
| Tablet layouts | `TabletHomeDashboard`, `ListDetailLayout` ✅ |
| `supports-screens` | large/xlarge enabled in manifest ✅ |
| Compose-only | No web/responsive CSS concerns — N/A |

---

## Dependencies / vulnerabilities

- Versions pinned in `gradle/libs.versions.toml`; compileSdk 35 cap noted in comments.
- Lint baseline: **24** `NewerVersionAvailable`.
- Unused deps (Stage 4): Retrofit, maps-utils, logging-interceptor — **no npm audit equivalent run**.

---

## .env.example vs code

| Check | Result |
|-------|--------|
| `.env.example` vars | 20 entries — consumed by `AppConfig.fromEnvironment()` ✅ |
| Missing from example | `APP_ENV`, `HOST`, `PORT` (docker-compose uses defaults) |
| Android `local.properties.example` | Documents `LOCAL_ONLY_MODE`, keys — separate from backend `.env` ✅ |

---

## Documentation staleness

| Doc | Issue |
|-----|-------|
| AGENTS.md | «38 tests» → actual **~754** `@Test` methods |
| AGENTS.md | «single-module» vs README multi-module (`:shared`, `:backend`) |
| PROJECT_OVERVIEW.md | Room version references may lag (AppDatabase **v34**) |
| QUALITY_1000 inventory | File counts, lint counts outdated |
| AUDIT_FINAL_REPORT.md (v1) | Lists P0 items **fixed on main** — needs v2 final refresh |

---

## Статус

**Этап 7 (Audit v2) завершён.**

**Следующий шаг (после подтверждения):** **Этап 8** — облегчение и модернизация UI (Compose/Android: APK weight, recomposition, main-thread, loading strategy).
