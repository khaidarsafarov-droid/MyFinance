# TruckerLoad — Итоговый отчёт технического аудита

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-stage1-9ae7`  
**Версия app:** 1.5.6 (versionCode 11)  
**Метод:** Stages 1–7, static analysis; lint/tests не запускались (SDK unavailable in VM).

Детальные отчёты: `docs/audit/STAGE1_INVENTORY.md` … `STAGE7_ADDITIONAL.md`.

---

## 1. Executive summary

TruckerLoad — зрелое local-first Android-приложение (Kotlin/Compose/Room) с опциональным Ktor backend, Telegram ingest, cloud sync и богатым доменом парсеров. **Ядро бизнес-логики (Relay parse, weekly goal, filters) хорошо покрыто unit-тестами.** Release hardening (no backup, R8, secret gates) сильнее среднего.

Перед релизом критичны **три системных риска:** (1) cloud sync **не распространяет удаления** и может воскрешать удалённые loads; (2) Google local fallback даёт **403 account_mismatch** на snapshot sync; (3) restore backup **без предупреждения стирает все фото/сканы**. Дополнительно: DuplicateChecker блокирует легитимные same-day loads; presentation layer ~14% test coverage; ~35 мёртвых UI composables и dual widget pipelines.

Lint «зелёный» только с baseline (260 issues suppressed). Документация (AGENTS.md test count, Room version) устарела.

**Общая оценка:** функционально богатое приложение, **не готово к multi-device cloud release** без fix P0 sync/auth. Local-only / friends APK — приемлемо с известными UX gaps.

---

## 2. Критические проблемы (P0)

| ID | Проблема | Impact |
|----|----------|--------|
| **S-01** | `CloudSyncPolicy.mergeById`: deleted load absent locally → remote re-inserted on push | Multi-device data inconsistency; deletes never sync |
| **S-02** | Client `accountId=google_<hex>` vs backend `user.id=UUID` → 403 | Cloud sync broken for Google local fallback |
| **S-03** | `BackupService.restoreFromJson` wipes all photos/scans | Irreversible media loss on restore |
| **D-01** | `DuplicateChecker` route+date → skip without rate check | Legitimate loads not imported |
| **SEC-01** | Home ALL/month filters full-hydrate entire journal | OOM/jank on large fleets |

---

## 3. Дублирование (топ)

| ID | Duplication | Keep as source of truth |
|----|-------------|---------------------------|
| DUP-01 | Room restore loops (BackupService + CloudSyncEngine) | Shared `BackupRoomApplier` |
| DUP-03 | Telegram paycheck/diesel (device vs server) | `TelegramMessageParser` |
| DUP-04 | Duplicate rules (checker/audit/import) | `LoadDuplicateRules` kernel |
| DUP-06 | Dual CloudSyncEngine name + entry points | Rename object; Hilt wrapper only |
| DUP-15 | 4 widget providers (RemoteViews + Glance) | Glance end-state |
| DUP-10 | 5× private formatUsd/Rpm vs MoneyFormat | `MoneyFormat.kt` |
| DUP-19 | Duplicate RU string catalogs (~1500 lines) | Single `values/` or `values-ru/` |

Полный список: 28 групп в `STAGE3_DUPLICATION.md`.

---

## 4. Мёртвый код (топ)

| Category | Count | Examples |
|----------|------:|---------|
| Dead composables | ~35 | GoldComponents, NeoGlassTypography, TaxTrackerScreen, ForecastCard |
| Unused public funs | ~142 | Repository/DAO scaffolding |
| Unreachable nav | 4 | shortcuts community/friends_live, Routes.PROFILE_SETUP |
| Unused Gradle deps | 4 | Retrofit, maps-utils, logging-interceptor |
| Unused BuildConfig | 7 | Cerebras, TURN, Directions |
| Lint UnusedResources | 67 baselined | widget drawables/colors |

Полный список: `STAGE4_DEAD_CODE.md`. **Не удалять без подтверждения** — часть intentional stubs.

---

## 5. Ошибки логики (топ)

| ID | Description | Proposed fix |
|----|-------------|--------------|
| D-02 | Blank date passes LoadValidator → wrong week | Require date; derive from referenceMillis |
| D-03 | WeeklyGoal gross vs yield inconsistent | Recompute yield from in-memory loads |
| V-01 | AnalyticsViewModel stale refresh race | Cancel job + generation guard |
| V-02 | VoiceCommandBus overwrites commands | Channel queue |
| V-03 | Archive years from filtered loads only | Dedicated years query |
| S-09 | deleteLoad no outbox enqueue | enqueue DELETE + fix merge |
| S-14 | Email Supabase fallback on auth rejection | Fallback only on network/5xx |
| UX-01 | Sync errors invisible | Wire SyncStatusTracker to UI |

Полный список: 45 findings в `STAGE2_LOGIC_CORRECTNESS.md`.

---

## 6. Рекомендации по приоритету

### P0 — до cloud/multi-device release

1. **Tombstones or snapshot-level LWW** for cloud delete (S-01 + S-09 + incremental pull)
2. **Align Google accountId** client ↔ backend (S-02)
3. **Scoped restore** or explicit media wipe warning (S-03)
4. **DuplicateChecker** align with DuplicateAudit rules (D-01)
5. **Tests** for above paths

### P1 — следующий sprint

6. AnalyticsViewModel refresh race (V-01)
7. VoiceCommandBus queue (V-02)
8. Unified Telegram inbound processor (DUP-03)
9. Single CloudSyncEngine entry (DUP-06)
10. Home archive years + paging policy (V-03, SEC-01 partial)
11. SyncStatusTracker → UI banner (UX-01)
12. LoadValidator blank date (D-02)

### P2 — tech debt / polish

13. Widget → Glance only (DUP-15)
14. MoneyFormat consolidation (DUP-10)
15. Journal form scaffold (DUP-12)
16. Remove dead Gold/NeoGlass/GlassCard (Stage 4)
17. One Russian strings catalog (DUP-19)
18. Remove Retrofit/maps-utils deps
19. Extend LogRedactor to auth/cloud
20. Refresh AGENTS.md / PROJECT_OVERVIEW.md

### P3 — backlog / product decision

21. TaxTracker — wire nav or remove
22. Community/friends shortcuts — implement routes or remove
23. Cerebras/TURN BuildConfig — implement or remove fields

---

## 7. Test coverage summary

- **Strong:** parsers, filters, goal math, Room repos, Telegram redaction tests
- **Weak:** presentation (14%), DuplicateChecker, cloud delete, AuthRepositoryImpl behavioral, AnalyticsViewModel
- **~23%** test files are architecture guards (source `contains`), not runtime tests

---

## 8. Ограничения аудита

- Lint и unit tests **не выполнялись** в VM (Android SDK path missing)
- Нет prod traffic / Crashlytics data
- Community/friends may be removed intentionally — помечено «manual check»
- Fixes **не применялись** — только документация

---

## 9. Следующие шаги (требуют подтверждения)

1. Выбрать scope первого fix sprint (рекомендуем P0 sync bundle)
2. Подтвердить удаление dead code clusters vs keep stubs
3. Решить судьбу TaxTracker / community shortcuts
4. После fixes — re-run `:app:testDebugUnitTest` + `:app:lintDebug` on dev machine

---

*Audit completed per cursor-audit-prompt.pdf Stages 1–7.*
