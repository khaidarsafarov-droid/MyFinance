# Этап 2 — Проверка логики и корректности функций (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этап 1 v2 (`docs/audit/STAGE1_INVENTORY.md`)  
**Метод:** статический разбор исходников + сопоставление с unit-тестами. **Код не менялся.**

---

## Резюме этапа

Проверены критические доменные пути (парсеры, импорт, goal math), слой sync/auth/data, и 11 ViewModels.  
**Всего задокументировано 46 находок:** 1 critical (остаточный), 6 high, 22 medium, 15 low, плюс **4 закрытых P0/P1** из audit v1.

### Статус P0 из audit v1 (перепроверка на текущем `main`)

| ID | Было | Сейчас | Доказательство |
|----|------|--------|----------------|
| S-01 | Cloud delete resurrection | **Закрыто** (push = local snapshot; pull удаляет orphans) | `CloudSyncEngine.kt:149-165`, `223-230`, `CloudSyncPolicy.kt:52-63`, `CloudSyncPolicyTest` |
| S-02 | Google accountId 403 | **Закрыто** | `Application.kt:301-304`, `Repositories.kt:18-20`, `AuthenticatedUserAccountIdTest` |
| D-01 | DuplicateChecker false skip | **Закрыто** | `DuplicateChecker.kt:58-65`, `DuplicateCheckerTest` |
| S-03 | Restore wipes media без warning | **Частично** — UI предупреждает, поведение destructive | `SettingsDataSection.kt:172-192`, `backup_restore_confirm_body` (RU/EN) |

### Наиболее опасные **открытые** для релиза

1. **Diesel/paycheck sync — нет LWW на pull** для уже существующих строк (только orphan-delete + insert missing).
2. **AnalyticsViewModel.refresh()** — race без cancel/generation guard.
3. **VoiceCommandBus** — перезапись необработанной команды.
4. **Blank date** на Relay one-liner → текущая неделя в stats.
5. **HomeViewModel.availableYears()** — годы из filter-scoped loads.

---

## Легенда severity

| Уровень | Критерий |
|---------|----------|
| **Critical** | Потеря данных, неверные финансовые итоги в prod, security breach |
| **High** | Существенная функциональная поломка или data inconsistency |
| **Medium** | Ошибки UX, silent failures, race при типичном использовании |
| **Low** | Edge cases, tech debt, несоответствие docs/tests |
| **Resolved** | Закрыто на текущем `main`; оставлено для трассировки |

---

## 0. Закрытые находки (audit v1 → main)

### S-01 · **Resolved** · Cloud delete propagation

| | |
|---|---|
| **Было** | `mergeById` на push воскрешал remote-only loads после локального delete. |
| **Сейчас** | Push публикует **только local Room** (`localSnapshotForPush`). Pull удаляет orphans (`orphanLocalIds` / `orphanLocalIntIds`). |
| **Остаточный риск** | См. **S-01-R** — diesel/paycheck updates на pull. |

### S-02 · **Resolved** · Google accountId mismatch

| | |
|---|---|
| **Было** | `google_<hex>` ≠ UUID → 403 `account_mismatch`. |
| **Сейчас** | Backend `acceptsAccountId(id \| voiceIdentity)`; JWT несёт `voiceIdentity`. |

### D-01 · **Resolved** · DuplicateChecker false positive

| | |
|---|---|
| **Было** | route+date → skip без сравнения rate. |
| **Сейчас** | `isLikelySameLoad()` — skip только при identical/tripId/stops+rate match. Тесты добавлены. |

### S-03 · **Partial** · Restore backup media wipe

| | |
|---|---|
| **Было** | Restore без предупреждения стирает фото/сканы. |
| **Сейчас** | Confirm dialog с явным текстом про удаление медиа. **Поведение restore по-прежнему destructive** — scoped restore / media manifest не реализованы. |

---

## 1. Domain — парсеры, импорт, goal math

### D-02 · High · `LoadMessageParser` + `LoadValidator` — blank date

| | |
|---|---|
| **Файлы** | `LoadMessageParser.kt`, `domain/import/LoadValidator.kt:9-22`, `WeekUtils` |
| **Проблема** | Relay one-liner без `Pu-time` → `date=""`. Validator **не проверяет date**. `getLoadReportingWeek` fallback → **текущая неделя**. |
| **Edge case** | AGENTS.md hello-world paste (Pu/Del address only) парсится, но попадает в текущую неделю stats. |
| **Fix** | Требовать date в validator; derive from `referenceMillis`; repair на import. |

### D-03 · High · `WeeklyGoalCalculator` — inconsistent gross vs yield

| | |
|---|---|
| **Файлы** | `domain/goal/WeeklyGoalCalculator.kt:28-45` |
| **Проблема** | `currentGross = maxOf(sqlGross, loadGross)` для weekNumber=0 rows, но `actualDailyYield` / `totalActiveDays` из SQL без пересчёта. |
| **Fix** | Пересчитывать yield из in-memory `weekLoads` когда gross скорректирован. |

### D-04 · Major · `CsvLoadParser` — ambiguous column headers

`contains("trip")`, `contains("rate")` — false positives (`corporate`, `milestone`).

### D-05 · Minor · `ParserFactory` / `MessageTypeDetector`

HTML detection до CSV/Relay; `PLAIN_TEXT` не fallback на `FlexibleLoadParser`; CSV detection не quote-aware.

### D-06 · Minor · `PaycheckTextParser`

`Grand Total` matche раньше `Net Pay` → net может записаться как gross.

### D-07 · Minor · `GoalMoneyMath.expectedGrossByNow`

`daysActive.coerceAtLeast(1)` — в воскресенье уже ~14% expected progress до первого лоуда.

### D-08 · Minor · `LoadFilterUseCase` / week filter

Фильтр доверяет persisted `weekNumber`/`year` без recompute из stops → stale loads в wrong week.

### Хорошо покрыто тестами

`RelayMessageParserTest`, `LoadMessageParserTest`, `CsvLoadParserTest`, `ImportTripDedupTest`, `LoadValidatorTest`, `WeeklyGoalCalculatorTest`, `LoadFilterUseCaseMatrixTest`, `GoalMoneyMathTest`, **`DuplicateCheckerTest`** (новый).

---

## 2. Sync / auth / data layer

### S-01-R · **Critical (residual)** · Diesel/paycheck — нет LWW update на incremental pull

| | |
|---|---|
| **Файлы** | `CloudSyncEngine.kt:248-272` |
| **Проблема** | Loads: LWW + orphan delete ✅. Diesel/paychecks: только **orphan delete + insert missing** — существующие строки с тем же id **не обновляются**, даже если remote `updatedAt` новее. |
| **Edge case** | Device A редактирует paycheck amount → sync → Device B сохраняет старую сумму. |
| **Fix** | Применить тот же LWW pattern, что для loads (или upsert by id с `remoteWins`). |
| **Тесты** | ❌ нет integration test для paycheck/diesel merge. |

### S-04 · High · Два CloudSyncEngine, status tracker мёртв

| | |
|---|---|
| **Файлы** | `data/sync/CloudSyncEngine.kt` (object), `data/sync/cloud/CloudSyncEngine.kt` (class) |
| **Проблема** | Production вызывает legacy **object** напрямую. Injectable wrapper обновляет `SyncStatusTracker`, но **нигде не используется** callers. |

### S-05 · Medium · Hybrid backend read swallows errors

`AccountCloudBackend.kt` — remote read failure → silent fallback на stale local mirror.

### S-06 · Medium · Outbox bloat + semantic mismatch

Каждая mutation = новый UUID row; worker ignores entityType/op — full snapshot push only; `OP_DELETE` never enqueued.

### S-07 · Medium · `syncLoadsCdc` tripId race

Check `getExistingTripIds` вне transaction → duplicate tripId при concurrent Telegram updates.

### S-08 · Medium · Auth email Supabase fallback

`signInEmailSupabase` onFailure → local credentials match → login даже при wrong server password.

### S-09 · Medium · Worker scheduling vs SyncMode

`MainActivity` gates workers on `!LOCAL_ONLY_MODE` only — ignores user `DEVICE_ONLY` pref.

### S-10 · Medium · Telegram poll `tryLock` skip

Overlapping FGS + Worker poll → silent skip, 15 min до retry.

### S-11 · Low · `markSynced` only on push success

Pull-only sessions leave `lastSyncedAt` stale.

### S-12 · Low · GoogleDrive auto-push failures

Upload fail → Log.w only, no retry notification.

---

## 3. Presentation ViewModels

### V-01 · **High** · `AnalyticsViewModel.refresh()` race

| | |
|---|---|
| **Файл** | `presentation/screens/analytics/AnalyticsViewModel.kt:73-99` |
| **Проблема** | Каждый `refresh()` — новый `viewModelScope.launch` без cancel; медленный первый запрос перезаписывает более новый period. |
| **Fix** | `refreshJob?.cancel()` + generation id guard. |

### V-02 · **High** · `VoiceCommandBus` command loss

| | |
|---|---|
| **Файлы** | `voice/VoiceCommandBus.kt:10-18` |
| **Проблема** | Single `MutableStateFlow<AppVoiceAction?>` — `offer()` overwrites unconsumed command. |
| **Fix** | Channel queue или SharedFlow с buffer. |

### V-03 · **High** · `HomeViewModel.availableYears()`

| | |
|---|---|
| **Файл** | `HomeViewModel.kt:506-510` |
| **Проблема** | Years из `_uiState.value.loads` (filter-scoped) → archive year picker неполный при фильтре THIS_WEEK. |
| **Fix** | Dedicated DAO query `distinctYears()`. |

### V-04 · **High** · `TaxTrackerViewModel` (orphaned)

Screen not in NavGraph; `loadTaxData()` no cancellation; gross = paychecks only; per-diem double-count; hardcoded 2024 brackets; `errorMessage` never shown.

### V-05 · Medium · `AddLoadViewModel` preview debounce

450ms debounce без проверки «text still current» → stale preview overwrite.

### V-06 · Medium · `EditLoadViewModel` / `LoadDetailViewModel`

Edit: load once in init; Detail: concurrent finish + dispute → stale base load.

### V-07 · Medium · `GoalViewModel` loading flicker

`isLoading = (progress == null)` + `WhileSubscribed(5000)` → spinner on re-navigation.

### V-08 · Medium · `SettingsViewModel.exportCsv()`

No in-flight guard; `getAll()` on Main thread → jank on large journal.

### V-09 · Medium · `AuthViewModel` legacy Google

`FallBackToLegacy` leaves `isLoading=true` if activity destroyed before callback.

### V-10 · Medium · `VoiceAssistantViewModel`

No cancel on new transcript while dispatcher running.

### V-11 · Low · Optimistic update mismatch

`applyOptimisticUpdate` / `revertOptimisticUpdate` wired but Add/Edit call optimistic **after** persist.

### V-12 · Low · `HomeRoomPagingPolicyTest` drift

Test policy ≠ real `usesRoomPaging()` when branches.

---

## 4. Противоречия бизнес-логики (cross-layer)

| # | Противоречие | Статус |
|---|-------------|--------|
| X-01 | Duplicate audit vs ingest rules | **Закрыто** — `DuplicateChecker` aligned с `DuplicateAuditUseCase` |
| X-02 | Financial advisor docs «deterministic» vs `ChatViewModel` AI stream | **Открыто** |
| X-03 | App Actions shortcuts `community`/`friends_live` vs NavGraph | **Открыто** |
| X-04 | `LOCAL_ONLY_MODE` «fully offline» vs Telegram polling still runs | **Открыто** |
| X-05 | Backup restore «journal restore» vs wipes all media | **Частично** — warning есть, wipe остаётся |

---

## 5. Покрытие тестами (gap analysis)

| Область | Покрыто | Пробел |
|---------|---------|--------|
| Relay/CSV parsers | ✅ strong | blank-date one-liner, ambiguous CSV headers |
| DuplicateChecker | ✅ **new tests** | integration с `LoadProcessor` |
| CloudSyncPolicy | ✅ push/orphan tests | diesel/paycheck LWW pull |
| CloudSyncEngine | architecture guard only | end-to-end delete + paycheck edit sync |
| ViewModels | 4/11 audited | Analytics, Voice, Edit, Detail, Settings, Auth |
| Auth Google→backend id | ✅ `AuthenticatedUserAccountIdTest` | client PUT e2e |
| Backup restore media wipe | UX string only | destructive restore behavior |

**Unit tests (baseline):** `:app:testDebugUnitTest` — **194** test files; domain core well covered; **S-01-R** и VM races — нет.

---

## 6. Таблица приоритетов (обновлено для v2)

| P | ID | Finding | Status |
|---|-----|---------|--------|
| ~~P0~~ | S-01 | Cloud delete resurrection | ✅ Resolved |
| ~~P0~~ | S-02 | Google accountId 403 | ✅ Resolved |
| ~~P1~~ | D-01 | DuplicateChecker false skip | ✅ Resolved |
| **P0** | **S-01-R** | Diesel/paycheck LWW on pull | **New — open** |
| **P1** | S-03 | Restore wipes media (scoped restore) | Partial (warning only) |
| **P1** | V-01 | Analytics refresh race | Open |
| **P1** | V-02 | VoiceCommandBus loss | Open |
| **P1** | D-02 | Blank date validation | Open |
| **P2** | V-03 | Archive years from filtered loads | Open |
| **P2** | S-04 | Dual CloudSyncEngine | Open |
| **P2** | S-09 | Worker scheduling vs SyncMode | Open |
| **P2** | D-03 | Goal gross/yield mismatch | Open |
| **P3** | V-04/T-0 | TaxTracker orphaned | Open |
| **P3** | D-04–D-08 | Parser/goal minors | Open |

---

## 7. Что проверено и выглядит корректно

- **Server snapshot LWW** (`putLww` WHERE updated_at) — stale writes rejected ✅
- **Telegram webhook** constant-time secret compare ✅
- **Home delete undo** — optimistic hide + rollback on failure, tested ✅
- **BackupRestoreParser** — rejects empty/chart-note files ✅
- **LoadFilter period logic** — Sun–Sat trucking weeks, DEL-bump — tested ✅
- **Auth account isolation** — per-user Room DB rebuild on login ✅
- **Cloud push authoritative local snapshot** — deletes propagate on next sync ✅ (loads)
- **Restore confirm dialog** — media wipe disclosed before action ✅

---

## Статус этапа

**Этап 2 (Audit v2) завершён.** Исправления **не применялись** — только документирование и proposed fixes.

**Следующий шаг (после подтверждения):** Этап 3 — поиск дублирования кода.
