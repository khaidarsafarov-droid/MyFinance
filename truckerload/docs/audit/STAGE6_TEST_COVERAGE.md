# Этап 6 — Тестовое покрытие

**Дата:** 2026-08-23  
**Метод:** file analysis (tests **не запускались** — SDK unavailable).

---

## Объём

| Scope | Prod files | Test files | Ratio |
|-------|----------:|----------:|------:|
| `:app` main | 595 | 193 unit + 5 instrumented | ~32% |
| `@Test` methods (app) | — | ~748 | — |
| `:backend:server` | 11 | 5 | — |
| `:shared/*` | 17 | 6 | — |

AGENTS.md claim «38 tests» — **устарело** (actual ~748 methods).

---

## Покрытие по слоям

| Package | Prod | Test | % |
|---------|-----:|-----:|--:|
| domain/ | 104 | 61 | 59% |
| data/ | 148 | 58 | 39% |
| sync/ | 49 | 11 | 22% |
| presentation/ | 215 | 30 | **14%** |
| utils/ | 40 | 19 | 48% |

**22 ViewModels** — dedicated tests только для Home, TaxTracker.

**12 screen packages** без тестов: analytics, auth, detail, edit, settings, …

---

## Критичные пробелы (без тестов)

| Path | Risk |
|------|------|
| `DuplicateChecker` / `LoadProcessor` | Core ingest — false duplicate skip |
| Cloud sync delete propagation | Data loss across devices |
| `AnalyticsViewModel` refresh race | Wrong dashboard period |
| `AuthRepositoryImpl` login flows | Guard tests only, no behavioral |
| `OutboundSyncWorker` / `OP_DELETE` | DELETE never enqueued; untested |
| `VoiceCommandBus` | Command loss |

---

## Качество тестов

| Type | ~Count | Assessment |
|------|-------:|------------|
| Behavioral (parsers, Room, filters) | ~148 files | **Meaningful** |
| Architecture guards (`contains` source) | ~45 files (~23%) | Structural only |
| Empty/stub tests | 0 | ✅ |

**Strong:** RelayMessageParserTest, LoadFilterUseCaseMatrixTest, DeleteLoadClearsPhotosTest, MediaCloudClientTest, HomeViewModelPendingDeleteTest.

---

## Рекомендуемые тесты (P0/P1)

1. `DuplicateCheckerTest` — route+date false positive
2. `CloudSyncPolicyTest` — delete absent from local map must not resurrect remote
3. `AnalyticsViewModelTest` — stale refresh guard
4. `GoogleAccountIdSnapshotTest` — client accountId vs backend JWT subject
5. `BackupRestoreMediaWipeTest` — restore deletes photos (document or fix)

---

## Статус

**Этап 6 завершён.**
