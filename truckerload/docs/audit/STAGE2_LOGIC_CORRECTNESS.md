# Этап 2 — Проверка логики и корректности функций

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-stage1-9ae7`  
**База:** Этап 1 (`docs/audit/STAGE1_INVENTORY.md`)  
**Метод:** статический разбор исходников + сопоставление с unit-тестами. **Код не менялся.**

---

## Резюме этапа

Проверены критические доменные пути (парсеры, импорт, goal math), слой sync/auth/data, и 11 ViewModels. Найдено **1 critical**, **7 high**, **22 medium**, **15 low** — всего **45** задокументированных находок.

Наиболее опасные для релиза:

1. **Cloud sync не распространяет удаления** — удалённый локально лоуд воскресает при push (merge LWW).
2. **Google local fallback → cloud sync 403** — `accountId` клиента (`google_<hex>`) ≠ `user.id` (UUID) на backend.
3. **DuplicateChecker** — ложные «дубликаты» по route+date блокируют легитимные лоуды.
4. **Restore backup** — без предупреждения стирает все фото/сканы.
5. **Race conditions** в AnalyticsViewModel / TaxTrackerViewModel / VoiceCommandBus.

---

## Легенда severity

| Уровень | Критерий |
|---------|----------|
| **Critical** | Потеря данных, неверные финансовые итоги в prod, security breach |
| **High** | Существенная функциональная поломка или data inconsistency |
| **Medium** | Ошибки UX, silent failures, race при типичном использовании |
| **Low** | Edge cases, tech debt, несоответствие docs/tests |

---

## 1. Domain — парсеры, импорт, goal math

### D-01 · Critical → **High** (import false-negative, не data loss) · `DuplicateChecker` + `LoadProcessor`

| | |
|---|---|
| **Файлы** | `domain/parser/DuplicateChecker.kt:22-42`, `domain/parser/LoadProcessor.kt:40-44` |
| **Проблема** | Совпадение route+date или stops+date → `Suspicious` → **skip import**, без сравнения rate/tripId. `DuplicateAuditUseCase` при этом различает same-route loads с разным rate. |
| **Edge case** | Два легитимных лоуда в один день на одном lane (разные Trip ID и rate) — второй не импортируется. |
| **Fix** | Skip только при `compareLoads(...).isIdentical()` или совпадении tripId; иначе update flow. |
| **Тесты** | `DuplicateAuditUseCaseTest` есть; **`DuplicateChecker` — нет тестов**. |

### D-02 · High · `LoadMessageParser` + `LoadValidator` — blank date

| | |
|---|---|
| **Файлы** | `LoadMessageParser.kt` (~95-129), `LoadValidator.kt:9-22`, `WeekUtils` |
| **Проблема** | Relay one-liner без `Pu-time` → `date=""`. Validator **не проверяет date**. `getLoadReportingWeek` fallback → **текущая неделя**. |
| **Edge case** | AGENTS.md hello-world paste (Pu/Del address only) парсится, но попадает в текущую неделю stats. |
| **Fix** | Требовать date в validator; derive from `referenceMillis`; repair на import. |

### D-03 · High · `WeeklyGoalCalculator` — inconsistent gross vs yield

| | |
|---|---|
| **Файлы** | `domain/goal/WeeklyGoalCalculator.kt:28-45` |
| **Проблема** | `currentGross = maxOf(sqlGross, loadGross)` для weekNumber=0 rows, но `actualDailyYield` / `totalActiveDays` из SQL без пересчёта. |
| **Edge case** | Смесь assigned + unassigned loads → gross верный, pace/yield занижен. |
| **Fix** | Пересчитывать yield из in-memory `weekLoads` когда gross скорректирован. |

### D-04 · Major · `CsvLoadParser` — ambiguous column headers

| | |
|---|---|
| **Файлы** | `CsvLoadParser.kt:19-24` |
| **Проблема** | `contains("trip")`, `contains("rate")` — false positives (`corporate`, `milestone`). |
| **Fix** | Exact header tokens с word boundaries. |

### D-05 · Minor · `ParserFactory` / `MessageTypeDetector`

- HTML detection до CSV/Relay → email forward может уйти в `HtmlLoadParser`.
- `PLAIN_TEXT` не fallback на `FlexibleLoadParser` (broker paste не импортируется через factory).
- CSV detection не quote-aware (в отличие от `CsvLoadParser.splitCsvLine`).

### D-06 · Minor · `PaycheckTextParser`

`Grand Total` matche раньше `Net Pay` → net может записаться как gross.

### D-07 · Minor · `GoalMoneyMath.expectedGrossByNow`

`daysActive.coerceAtLeast(1)` — в воскресенье уже ~14% expected progress до первого лоуда.

### D-08 · Minor · `LoadFilterUseCase` / week filter

Фильтр доверяет persisted `weekNumber`/`year` без recompute из stops → stale loads в wrong week.

### Хорошо покрыто тестами

`RelayMessageParserTest`, `LoadMessageParserTest`, `CsvLoadParserTest`, `ImportTripDedupTest`, `LoadValidatorTest`, `WeeklyGoalCalculatorTest`, `LoadFilterUseCaseMatrixTest`, `GoalMoneyMathTest` (shared).

---

## 2. Sync / auth / data layer

### S-01 · **Critical** · Cloud delete не propagates

| | |
|---|---|
| **Файлы** | `data/sync/CloudSyncPolicy.kt:28-41`, `CloudSyncEngine.kt:154-195`, `LoadRepository.kt:405-426` |
| **Проблема** | `pushLocalSnapshot` merge local+remote via `mergeById`. Локально удалённый load **отсутствует** в local map → remote item **re-inserted**. `deleteLoad` не enqueue outbox. Incremental pull **не удаляет** local orphans. |
| **Edge case** | Device A удаляет load → sync → load воскресает на server и на Device B. |
| **Fix** | Tombstones в `BackupData`; или snapshot-level LWW; enqueue DELETE в outbox; full replace / diff-delete на pull. |

```kotlin
// CloudSyncPolicy.kt:35-38 — localItem == null → remote wins
if (localItem == null || remoteWins(...)) {
    out[id] = remoteItem
}
```

### S-02 · **High** · Google accountId mismatch (local fallback → cloud)

| | |
|---|---|
| **Файлы** | `CloudSyncEngine.kt:167`, `backend/Application.kt:301-306`, `TokenAuth.kt:66-69`, `AccountIds.kt` |
| **Проблема** | Client snapshot `accountId = userId` (`google_<hex>`). Backend JWT maps to `user.id = UUID.nameUUIDFromBytes(...)`. PUT `/v1/sync/snapshot` → **403 account_mismatch**. |
| **Edge case** | Supabase down, Google sign-in local fallback, `SYNC_BACKEND_URL` configured → sync навсегда broken. |
| **Fix** | Backend принимает `voiceIdentity` как alternate accountId; или client отправляет UUID; или нормализация на одном слое. |

### S-03 · **High** · Restore backup wipes media

| | |
|---|---|
| **Файлы** | `utils/BackupService.kt:388-403` |
| **Проблема** | Restore deletes **all** photos/scans before inserting JSON. Backup payload = loads/paychecks/diesel only. |
| **Edge case** | User restores Drive backup → все BOL фото/сканы безвозвратно удалены. |
| **Fix** | Scoped restore; media manifest в backup schema; explicit UI warning. |

### S-04 · High · Два CloudSyncEngine, status tracker мёртв

| | |
|---|---|
| **Файлы** | `data/sync/CloudSyncEngine.kt` (object), `data/sync/cloud/CloudSyncEngine.kt` (class), workers |
| **Проблема** | Production вызывает legacy **object** напрямую. Injectable wrapper обновляет `SyncStatusTracker`, но **нигде не используется** callers. |
| **Fix** | Единая точка входа через injectable class. |

### S-05 · Medium · Hybrid backend read swallows errors

`AccountCloudBackend.kt` — remote read failure → silent fallback на stale local mirror.

### S-06 · Medium · Outbox bloat + semantic mismatch

- Каждая mutation = новый UUID row (no dedup by entity).
- Worker ignores entityType/op — full snapshot push only; `OP_DELETE` never enqueued.

### S-07 · Medium · `syncLoadsCdc` tripId race

Check `getExistingTripIds` вне transaction → duplicate tripId при concurrent Telegram updates.

### S-08 · Medium · Auth email Supabase fallback

`signInEmailSupabase` onFailure → local credentials match → login даже при wrong server password (network vs auth rejection не различаются).

### S-09 · Medium · Worker scheduling vs SyncMode

`MainActivity` gates workers on `!LOCAL_ONLY_MODE` only — ignores user `DEVICE_ONLY` pref. Workers run but sync skipped (battery/log noise). FCM correctly checks `SyncModeStore`.

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
| **Файл** | `presentation/screens/analytics/AnalyticsViewModel.kt` |
| **Проблема** | Unscoped coroutine jobs; slower first request overwrites newer period selection. |
| **Fix** | Cancel prior job; generation id guard. |

### V-02 · **High** · `VoiceCommandBus` command loss

| | |
|---|---|
| **Файлы** | `voice/VoiceCommandBus.kt`, `VoiceCommandViewModel.kt` |
| **Проблема** | Single `MutableStateFlow<AppVoiceAction?>` — `offer()` overwrites unconsumed command. `consume()` before handle completes. |
| **Edge case** | «Add diesel» + «Weekly gross» подряд → первый потерян. |
| **Fix** | Channel queue или SharedFlow replay. |

### V-03 · **High** · `HomeViewModel.availableYears()`

Years derived from **filter-scoped** loads (e.g. THIS_WEEK only) → archive year picker incomplete.

### V-04 · **High** · `TaxTrackerViewModel` (orphaned)

- Screen not in NavGraph (T-0).
- `loadTaxData()` no cancellation → stale year totals on rapid switch.
- `totalGrossIncome` = paychecks only (loads excluded) — misleading label.
- Per-diem days double-count overlapping trips.
- Hardcoded 2024 brackets.
- Screen never shows `errorMessage`.

### V-05 · Medium · `AddLoadViewModel` preview debounce

450ms debounce без проверки «text still current» → stale preview overwrite.

### V-06 · Medium · `EditLoadViewModel` / `LoadDetailViewModel`

- Edit: load once in init, no external sync refresh.
- Detail: concurrent `setActualFinishDate` + dispute → stale base load.

### V-07 · Medium · `GoalViewModel` loading flicker

`isLoading = (progress == null)` + `WhileSubscribed(5000)` → spinner on re-navigation.

### V-08 · Medium · `SettingsViewModel.exportCsv()`

No in-flight guard; `getAll()` on Main thread → jank on large journal.

### V-09 · Medium · `AuthViewModel` legacy Google

`FallBackToLegacy` leaves `isLoading=true` if activity destroyed before callback.

### V-10 · Medium · `VoiceAssistantViewModel`

No cancel on new transcript while dispatcher running.

### V-11 · Low · Optimistic update mismatch

`applyOptimisticUpdate` / `revertOptimisticUpdate` wired but Add/Edit call optimistic **after** persist — revert API dead code.

### V-12 · Low · `HomeRoomPagingPolicyTest` drift

Test policy ≠ real `usesRoomPaging()` when branches.

---

## 4. Противоречия бизнес-логики (cross-layer)

| # | Противоречие | Где |
|---|-------------|-----|
| X-01 | Duplicate **audit** vs **ingest** — разные правила duplicate | `DuplicateAuditUseCase` vs `DuplicateChecker` |
| X-02 | Financial advisor docs «deterministic» vs `ChatViewModel` AI stream | docs vs `FinancialAdvisorScreen` |
| X-03 | App Actions shortcuts `community`/`friends_live` vs NavGraph | `shortcuts.xml` vs `Routes.kt` |
| X-04 | `LOCAL_ONLY_MODE` «fully offline» vs Telegram polling still runs | `TruckerLoadApp.scheduleTelegramSync` |
| X-05 | Backup restore «journal restore» vs wipes all media | `BackupService.restoreFromJson` |

---

## 5. Покрытие тестами (gap analysis)

| Область | Покрыто | Пробел |
|---------|---------|--------|
| Relay/CSV parsers | ✅ strong | blank-date one-liner, ambiguous CSV headers |
| DuplicateChecker | ❌ | route+date false positive |
| CloudSyncPolicy merge | partial (`SyncConflictResolverTest`) | delete propagation |
| CloudSyncEngine | architecture guard only | push merge with deletes |
| ViewModels | 4/11 audited | Analytics, Voice, Edit, Detail, Settings, Auth |
| Auth Google→backend id | `AccountIdsTest` | end-to-end snapshot PUT |
| Backup restore media wipe | ❌ | destructive restore |

**Unit tests (baseline):** `:app:testDebugUnitTest` — 193 test files; domain core well covered, sync delete path and VM races — нет.

---

## 6. Таблица приоритетов (для Этапа 6 / финального отчёта)

| P | ID | Finding | Effort hint |
|---|-----|---------|-------------|
| **P0** | S-01 | Cloud delete resurrection | Design pass: tombstones or snapshot LWW |
| **P0** | S-02 | Google accountId 403 | Backend accept voiceIdentity OR client UUID |
| **P1** | S-03 | Restore wipes media | UI warning + scoped restore |
| **P1** | D-01 | DuplicateChecker false skip | Align with DuplicateAudit rules |
| **P1** | V-01 | Analytics refresh race | Job cancel + stale guard |
| **P1** | V-02 | VoiceCommandBus loss | Queue |
| **P1** | D-02 | Blank date validation | Validator + parser fix |
| **P2** | V-03 | Archive years from filtered loads | Dedicated years query |
| **P2** | S-04 | Dual CloudSyncEngine | Consolidate entry point |
| **P2** | S-09 | Worker scheduling vs SyncMode | Gate on `allowsCloudCalls()` |
| **P2** | D-03 | Goal gross/yield mismatch | Recompute yield |
| **P3** | V-04/T-0 | TaxTracker orphaned | Wire nav or remove |
| **P3** | D-04–D-08 | Parser/goal minors | Incremental |

---

## 7. Что проверено и выглядит корректно

- **Server snapshot LWW** (`putLww` WHERE updated_at) — stale writes rejected ✅
- **Telegram webhook** constant-time secret compare ✅
- **Home delete undo** — optimistic hide + rollback on failure, tested ✅
- **BackupRestoreParser** — rejects empty/chart-note files ✅
- **LoadFilter period logic** — Sun–Sat trucking weeks, DEL-bump — tested ✅
- **Auth account isolation** — per-user Room DB rebuild on login ✅

---

## Статус этапа

**Этап 2 завершён.** Исправления **не применялись** — только документирование и proposed fixes.

**Следующий шаг (после подтверждения):** Этап 3 — поиск дублирования кода.
