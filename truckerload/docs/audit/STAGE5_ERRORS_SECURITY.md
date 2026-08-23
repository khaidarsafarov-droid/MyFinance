# Этап 5 — Ошибки, уязвимости, производительность (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этапы 1–4 v2  
**Метод:** static analysis + lint-baseline inventory. **Lint/compile не запускались** в этой VM-сессии (`org.gradle.java.home` в repo указывает на Windows JBR; override `~/.gradle/gradle.properties` отсутствует — см. AGENTS.md).

---

## Lint baseline (`app/lint-baseline.xml`)

**261** issue записей baselined:

| Issue type | Count |
|------------|------:|
| UnusedResources | 67 |
| UseTomlInstead | 45 |
| GradleDependency | 37 |
| PluralsCandidate | 24 |
| NewerVersionAvailable | 24 |
| Other (Typography, Icon, Ktx, …) | 64 |

AGENTS.md: без baseline `:app:lintDebug` падает с **~663** non-baselined issues. Lint «зелёный» только с baseline.

**CredentialManagerMisuse** (1) и **DataExtractionRules** (1) — в baseline, не исправлены.

---

## Critical / High (data integrity & perf)

| ID | Severity | Finding | Status v2 |
|----|----------|---------|-----------|
| SEC-01 | **High** | Home `loadsFromDb`: фильтры **ALL / THIS_MONTH / YESTERDAY / CALENDAR_DATE** → `watchLoads()` full-hydrate всего журнала + stops в память; Room paging только для week/dispute | **Open** (частично улучшено: calendar dots lazy, week filters scoped) |
| SEC-02 | ~~High~~ | Cloud sync delete resurrection | **Resolved** (Stage 2 S-01) |
| SEC-03 | ~~High~~ | Google accountId 403 | **Resolved** (Stage 2 S-02) |
| SEC-04 | **High** | Diesel/paycheck incremental pull без LWW update существующих строк | **New** (Stage 2 S-01-R) — cross-device stale financial data |

---

## Security

| ID | Sev | Finding | Fix direction |
|----|-----|---------|---------------|
| S5-01 | Info | `allowBackup=false`, release R8+shrink, `networkSecurityConfig` | ✅ Good |
| S5-02 | Info | Bot/AI secrets gated; release verify task | ✅ Good |
| S5-03 | Low | No `dataExtractionRules` (Android 12+); baselined | Add XML |
| S5-04 | Low | Public Google OAuth client ID in repo | Expected for OAuth |
| S5-05 | Medium | BuildConfig Maps/TURN/Cerebras keys extractable from APK if set | Document risk; server-side proxy where possible |
| S5-06 | Medium | PBKDF2 verifiers in plaintext fallback prefs (local-only auth path) | Keystore-only for verifiers |
| S5-07 | Medium | `LogRedactor` primarily on Telegram paths; auth/Drive/cloud logs less covered | Extend redaction |
| S5-08 | Info | Room: parameterized queries, no dynamic SQL | ✅ Good |
| S5-09 | Info | Backend JWT + webhook `constantTimeEquals`; signed media URLs | ✅ Good |
| S5-10 | Medium | Rate limit per IP (`ipRateLimiter`), in-memory only — shared NAT / multi-user IP | Key by `user.id` where authenticated |
| S5-11 | Info | No CORS — correct for native API | N/A |
| S5-12 | Low | `acceptsAccountId` accepts client `voiceIdentity` — intentional; verify no cross-user id collision | Monitor; tests added ✅ |

---

## Performance

| ID | Sev | Finding |
|----|-----|---------|
| P5-01 | **High** | Default **ALL** filter + year archive: full journal hydrate on Home (no paging) — OOM/jank risk on large journals |
| P5-02 | Medium | Week filters: dual path (scoped Flow + paging) — redundant work when paging enabled |
| P5-03 | Medium | N+1 in `getByStops`, route backfill routines |
| P5-04 | Low | Search `LIKE '%query%'` — no index use |
| P5-05 | Info | LoadEntity indexes well defined | ✅ |
| P5-06 | Info | Widget uses SQL aggregates | ✅ |
| P5-07 | **High (UI)** | Compose home animations + software GPU → main-thread pressure (AGENTS.md ANR notes) — **→ Этап 8** |

---

## Error UX

| ID | Sev | Finding |
|----|-----|---------|
| UX-01 | Medium | `SyncStatusTracker` errors **not shown in UI** (tracker wired to unused injectable engine) |
| UX-02 | Medium | Raw server messages in login/sign-up toasts |
| UX-03 | Info | Offline banner, auth session banners, delete undo, **restore media wipe confirm** | ✅ Improved (S-03 partial) |

---

## Compiler / static

| Check | Result |
|-------|--------|
| `:app:compileDebugKotlin` | **Not run** (Gradle `org.gradle.java.home` Windows path) |
| `:app:lintDebug` | **Not run** (same) |
| Unit tests (prior runs on main) | P0 tests pass (`CloudSyncPolicyTest`, `DuplicateCheckerTest`, backend accountId) |

---

## Статус

**Этап 5 (Audit v2) завершён** — static + baseline; runtime lint deferred to environment with Linux JDK override.

**Следующий шаг (после подтверждения):** Этап 6 — тестовое покрытие.
