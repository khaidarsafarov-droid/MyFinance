# TruckerLoad — Итоговый отчёт технического аудита (v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9`  
**Версия app:** 1.5.6 (versionCode 11)  
**Промпт:** cursor-audit-prompt (2).pdf — Stages 1–8  
**Метод:** Stages 1–8 static analysis; lint/unit tests **не запускались** в VM (Gradle `org.gradle.java.home` Windows path).

Детальные отчёты: `docs/audit/STAGE1_INVENTORY.md` … `STAGE8_UI_MODERNIZATION.md`.

---

## 1. Executive summary

TruckerLoad — зрелое **local-first Android** приложение (Kotlin / Jetpack Compose / Room) с опциональным Ktor backend, Telegram ingest, cloud sync и сильным доменом парсеров. **Ядро (Relay parse, weekly goal, filters) хорошо покрыто unit-тестами (~754 `@Test` methods).** Release hardening (no backup, R8, secret gates) выше среднего.

**С v1 audit на `main` влиты P0-фиксы:** cloud delete sync (local snapshot push + orphan pull), Google `accountId`/`voiceIdentity`, DuplicateChecker alignment, restore media **warning dialog**.

**Остаются блокеры cloud release:** diesel/paycheck pull без LWW (S-01-R); Home ALL filter full-hydrate (SEC-01); Analytics/VoiceCommandBus races. UI: migration debt (dead Gold/NeoGlass, dual widgets, extended icons), main-thread pressure на Home при software GPU.

Lint «зелёный» только с baseline (**261** issues suppressed). Документация (AGENTS.md «38 tests») устарела.

**Общая оценка:** **local-only / friends APK — готов** с известными UX gaps. **Multi-device cloud** — после fix S-01-R + SEC-01 + тестов merge path.

---

## 2. Критические проблемы (P0)

| ID | Проблема | Status on main |
|----|----------|----------------|
| ~~S-01~~ | Cloud delete resurrection | ✅ **Fixed** |
| ~~S-02~~ | Google accountId 403 | ✅ **Fixed** |
| ~~D-01~~ | DuplicateChecker false skip | ✅ **Fixed** |
| **S-01-R** | Diesel/paycheck incremental pull — no LWW update | ❌ **Open** |
| **SEC-01** | Home ALL/month full-hydrate entire journal | ❌ **Open** (partial: calendar lazy, week scoped) |
| **S-03** | Restore wipes all photos/scans | ⚠️ **Partial** — confirm dialog; behavior still destructive |

---

## 3. Дублирование (топ)

| ID | Duplication | Source of truth |
|----|-------------|-----------------|
| DUP-01 | Room restore loops (Backup + Cloud hydrate) | Shared `BackupRoomApplier` |
| DUP-03 | Telegram paycheck/diesel device vs server | Unified inbound processor |
| DUP-04 | Duplicate rules (3 classes) | `LoadDuplicateRules` kernel |
| DUP-06 | Dual CloudSyncEngine | Hilt wrapper = single entry |
| DUP-15 | RemoteViews + Glance widgets | Glance end-state |
| DUP-10 | Private formatUsd vs MoneyFormat | `MoneyFormat.kt` |
| DUP-19 | Duplicate RU catalogs (1,477 keys × 2 files) | Single default RU catalog |

Полный список: **28 групп** — `STAGE3_DUPLICATION.md`.

---

## 4. Мёртвый код (топ)

| Category | Count | Examples |
|----------|------:|---------|
| Dead composables | ~35 | GoldComponents, TaxTrackerScreen, ForecastCard |
| DI sync wrapper unused | 2 | `cloud.CloudSyncEngine`, SyncStatusTracker |
| Unused public funs | ~142 | Repository scaffolding |
| Unreachable nav | 4 | shortcuts community/friends_live |
| Unused Gradle deps | 4 | Retrofit, maps-utils, logging-interceptor |
| Lint UnusedResources | 67 baselined | widget assets |

Полный список: `STAGE4_DEAD_CODE.md`.

---

## 5. Ошибки логики (топ открытых)

| ID | Description | Proposed fix |
|----|-------------|--------------|
| S-01-R | Paycheck/diesel stale on pull | LWW upsert in `mergeSnapshotIntoRoom` |
| D-02 | Blank date → wrong week | LoadValidator + parser repair |
| D-03 | Goal gross vs yield mismatch | Recompute yield from week loads |
| V-01 | AnalyticsViewModel refresh race | Cancel job + generation id |
| V-02 | VoiceCommandBus command loss | Channel queue |
| V-03 | Archive years from filtered loads | DAO `distinctYears()` |
| UX-01 | Sync errors invisible | Wire status to UI |

Полный список: **46 findings** — `STAGE2_LOGIC_CORRECTNESS.md`.

---

## 6. UI: производительность и модернизация

*(Этап 8 — Compose/Android, не web)*

| Rank | Issue | Fix | Effect |
|------|-------|-----|--------|
| 1 | ALL filter full-hydrate | Room paging + SQL year | Memory/jank ↓↓↓ |
| 2 | `material-icons-extended` | Core icons only | APK size ↓↓ |
| 3 | Dead Gradle deps | Remove Retrofit/maps-utils | Build/APK ↓ |
| 4 | Home 10+ StateFlow collectors | Decompose scopes | Recomposition ↓↓ |
| 5 | Dual widget pipelines | Glance-only | Maintenance ↓↓ |
| 6–12 | Dead UI stacks, MoneyFormat, Maps lazy init, sync banner, form scaffold, `@Immutable`, Analytics cancel | See `STAGE8_UI_MODERNIZATION.md` | UX/consistency ↑ |

**Software GPU / emulator:** AOT compile + disable animations (AGENTS.md) for usable manual QA.

---

## 7. Рекомендации по приоритету

### P0 — до multi-device cloud

1. **Diesel/paycheck LWW on pull** (S-01-R) + integration test
2. **Home ALL paging** (SEC-01)
3. **Scoped restore** or media manifest (S-03 completion)

### P1 — следующий sprint

4. AnalyticsViewModel race (V-01)
5. VoiceCommandBus queue (V-02)
6. Unified Telegram processor (DUP-03)
7. Single CloudSyncEngine entry (DUP-06)
8. SyncStatusTracker → UI (UX-01)
9. LoadValidator blank date (D-02)

### P2 — UI lightening / tech debt

10. `material-icons-extended` → core (UI-2)
11. Remove Retrofit / maps-utils (UI-3)
12. Glance-only widgets (DUP-15)
13. Delete Gold/NeoGlass/GlassCard
14. MoneyFormat consolidation (DUP-10)
15. Journal form scaffold (DUP-12)
16. One Russian strings catalog (DUP-19)

### P3 — product decision

17. TaxTracker — wire nav or remove
18. Community/friends shortcuts — routes or remove
19. Cerebras/TURN BuildConfig — implement or remove

---

## 8. Test coverage summary

- **Strong:** parsers, filters, goal math, Room, **`DuplicateCheckerTest`**, **`CloudSyncPolicyTest`**
- **Partial:** backend `AuthenticatedUserAccountIdTest`
- **Weak:** CloudSyncEngine merge, diesel LWW, AnalyticsViewModel, VoiceCommandBus, presentation (~14% file ratio)
- **~23%** test files are architecture guards

---

## 9. Ограничения аудита

- Lint / `:app:testDebugUnitTest` **не выполнялись** в cloud VM (Gradle JDK path)
- Нет prod Crashlytics / traffic data
- Fixes **не применялись** в audit v2 — только документация (P0 fixes уже на `main` from prior sprint)

---

## 10. Следующие шаги (требуют подтверждения)

1. Подтвердить P0 sprint: **S-01-R + SEC-01 + S-03 scoped restore**
2. Подтвердить UI sprint: **icons-extended + dead deps + Glance-only**
3. Подтвердить dead code removal clusters (Stage 4)
4. Re-run tests + lint on dev machine with Linux JDK override

---

*Audit v2 completed per cursor-audit-prompt (2).pdf Stages 1–8.*
