# Этап 5 — Ошибки, уязвимости, производительность

**Дата:** 2026-08-23  
**Метод:** static analysis (lint **не запускался** — Android SDK недоступен в VM). Baseline: `app/lint-baseline.xml`.

---

## Lint baseline (260 issues baselined)

| Issue type | Count |
|------------|------:|
| UnusedResources | 67 |
| UseTomlInstead | 45 |
| GradleDependency | 37 |
| PluralsCandidate | 24 |
| NewerVersionAvailable | 24 |
| Other | 63 |

AGENTS.md: без baseline `:app:lintDebug` падает с сотнями ошибок. Lint «зелёный» только с baseline.

---

## Critical / High

| ID | Severity | Finding |
|----|----------|---------|
| SEC-01 | **High** | Home `loadsFromDb`: ALL/month/day filters **full-hydrate** entire journal + stops in memory |
| SEC-02 | **High** | Cloud sync delete resurrection (Stage 2 S-01) — data integrity |
| SEC-03 | **High** | Google accountId 403 on cloud sync (Stage 2 S-13) |

---

## Security

| ID | Sev | Finding | Fix direction |
|----|-----|---------|---------------|
| S5-01 | Info | `allowBackup=false`, release R8+shrink, cleartext blocked | ✅ Good |
| S5-02 | Info | Bot/AI secrets gated; release verify task | ✅ Good |
| S5-03 | Low | No `dataExtractionRules` (Android 12+) | Add XML |
| S5-04 | Low | Public Google OAuth client ID in repo | Expected for OAuth |
| S5-05 | Medium | BuildConfig Maps/TURN keys extractable from APK | Document risk |
| S5-06 | Medium | PBKDF2 verifiers in plaintext fallback prefs | Keystore-only for verifiers |
| S5-07 | Medium | LogRedactor only on Telegram paths | Extend to auth/Drive/cloud |
| S5-08 | Info | Room: no dynamic SQL / injection | ✅ Good |
| S5-09 | Info | Backend JWT + Google RS256 + webhook secret compare | ✅ Good |
| S5-10 | Medium | Rate limit per IP not per user; in-memory only | Key by user.id |
| S5-11 | Info | No CORS — correct for native API | N/A |

---

## Performance

| ID | Sev | Finding |
|----|-----|---------|
| P5-01 | Medium | Default week view: dual load (full week hydrate + Room paging) |
| P5-02 | Medium | N+1 in `getByStops`, backfill routines |
| P5-03 | Low | Search `LIKE '%query%'` — no index use |
| P5-04 | Info | LoadEntity indexes well defined | ✅ |
| P5-05 | Info | Widget uses SQL aggregates | ✅ |

---

## Error UX

| ID | Sev | Finding |
|----|-----|---------|
| UX-01 | Medium | `SyncStatusTracker` errors **not shown in UI** |
| UX-02 | Medium | Raw server messages in login/sign-up toasts |
| UX-03 | Info | Offline banner, auth session banners, delete undo | ✅ Good |

---

## Статус

**Этап 5 завершён** (static; lint run blocked by missing SDK).
