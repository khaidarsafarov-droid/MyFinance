# Этап 6 — Тестовое покрытие (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этапы 1–5 v2  
**Метод:** file/analysis inventory. **Tests не запускались** в этой VM-сессии (Gradle JDK path — см. Stage 5).

---

## Объём

| Scope | Prod `.kt` | Test files | `@Test` methods (approx) |
|-------|----------:|----------:|-------------------------:|
| `:app` main | 595 | **194** unit + 5 instrumented | **~754** |
| `:backend:server` | ~11 prod | **6** | ~30+ |
| `:shared/*` | 17 | **6** | — |
| **Total test classes w/ @Test** | — | **~205** | — |

AGENTS.md claim «38 tests» — **устарело** (относится к раннему smoke subset, не полному suite).

**Дельта v2 (post-P0):** добавлены `DuplicateCheckerTest`, расширен `CloudSyncPolicyTest` (push snapshot / orphans), backend `AuthenticatedUserAccountIdTest`.

---

## Покрытие по слоям (`:app`)

| Package | Prod files | Test files | Test/prod % |
|---------|----------:|----------:|------------:|
| domain/ | 104 | 62 | **60%** |
| data/ | 148 | 58 | **39%** |
| sync/ | 49 | 11 | **22%** |
| presentation/ | 215 | 30 | **14%** |
| utils/ | 40 | 19 | **48%** |

**22 ViewModels** — dedicated behavioral tests: **Home** (3 files), **TaxTracker** (1, orphaned screen). Остальные ~19 VM — **без dedicated tests**.

**12+ screen packages** без UI/VM tests: analytics, auth flows, detail, edit, settings, map, …

---

## Критичные пробелы

| Path | Risk | Status v2 |
|------|------|-----------|
| `DuplicateChecker` / `LoadProcessor` | Ingest false duplicate skip | **Partial** — `DuplicateCheckerTest` (unit rules); нет integration с `LoadProcessor` |
| Cloud sync delete propagation | Cross-device data loss | **Partial** — `CloudSyncPolicyTest` (policy); **нет** `CloudSyncEngine` merge/orphan integration |
| Diesel/paycheck pull LWW | Stale financial data (S-01-R) | **None** |
| `AnalyticsViewModel` refresh race | Wrong dashboard period | **None** |
| `AuthRepositoryImpl` login flows | Auth bypass / wrong session | Architecture guards only |
| `VoiceCommandBus` | Command loss | **None** |
| `BackupService.restoreFromJson` media wipe | Destructive restore | **None** (UX string only) |
| Google snapshot PUT e2e | accountId round-trip | **Partial** — backend unit test; no client PUT test |

---

## Закрытые / улучшенные (audit v1 → main)

| Recommended (v1) | Status |
|------------------|--------|
| `DuplicateCheckerTest` — route+date false positive | ✅ **Added** |
| `CloudSyncPolicyTest` — push must not resurrect deletes | ✅ **Added** (`localSnapshotForPush`, `orphanLocalIds`) |
| `GoogleAccountIdSnapshotTest` | ✅ **Partial** — `AuthenticatedUserAccountIdTest` on backend |
| `AnalyticsViewModelTest` | ❌ Open |
| `BackupRestoreMediaWipeTest` | ❌ Open |

---

## Качество тестов

| Type | ~Count | Assessment |
|------|-------:|------------|
| Behavioral (parsers, Room, filters, backup) | ~150 files | **Meaningful** |
| Architecture guards (`contains` source / file exists) | ~45 files (~23%) | Structural only — не заменяют behavioral |
| Empty/stub `@Test` bodies | 0 | ✅ |
| Tests for dead code (TaxTracker, FuelAnalytics) | 2 files | **Soft orphan** — документируют unwired features |

**Strong examples:** `RelayMessageParserTest`, `LoadFilterUseCaseMatrixTest`, `DeleteLoadClearsPhotosTest`, `MediaCloudClientTest`, `HomeViewModelPendingDeleteTest`, **`DuplicateCheckerTest`**, **`CloudSyncPolicyTest`**.

---

## Рекомендуемые тесты (P0/P1, обновлено)

| P | Test | Covers |
|---|------|--------|
| **P0** | `CloudSyncEngineMergeTest` | Orphan load delete on pull; diesel/paycheck LWW update |
| **P1** | `AnalyticsViewModelTest` | Stale refresh / period race |
| **P1** | `VoiceCommandBusTest` | Queue vs overwrite |
| **P1** | `LoadProcessorDuplicateIntegrationTest` | End-to-end ingest + DuplicateChecker |
| **P2** | `BackupRestoreMediaWipeTest` | Document destructive photo/scan delete |
| **P2** | `RemoteAccountCloudClientSnapshotTest` | Client PUT with `google_<hex>` accountId |
| **P2** | `AuthRepositoryImplSignInTest` | Supabase fallback vs local credentials |

---

## CI / gates (reference)

From README / AGENTS.md (when Gradle env OK):

```bash
sh ./gradlew :shared:contract:jvmTest :shared:domain:jvmTest \
  :backend:server:test :app:testDebugUnitTest :app:assembleDebug
```

Additional gate: `:app:checkKotlinFileSize` (600-line cap).

---

## Статус

**Этап 6 (Audit v2) завершён.**

**Следующий шаг (после подтверждения):** Этап 7 — дополнительные проверки (a11y, i18n, logging, UX consistency).
