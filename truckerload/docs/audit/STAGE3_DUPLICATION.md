# Этап 3 — Поиск дублирования (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этапы 1–2 v2  
**Метод:** grep, diff, static analysis. **Код не менялся.**

---

## Резюме

Найдено **28 групп дублирования** (8 high-impact, 12 medium, 8 low/dead code) — **без изменений состава** относительно audit v1; перепроверены call sites на текущем `main`.

Главные источники расхождения логики со временем:

1. **Room full-replace restore** — два независимых delete+insert цикла (`BackupService` vs `CloudSyncEngine.applyFullHydration`).
2. **Telegram paycheck/diesel** — device parser vs server inbox processor (diesel hash mismatch).
3. **Duplicate rules** — три класса (`DuplicateChecker`, `DuplicateAuditUseCase`, `ImportTripDedup`); **логика checker↔audit выровнена на main**, но **общего kernel нет**.
4. **UI migration debt** — BentoGlass активен (~35 экранов); Gold/GlassCard/NeoGlass **0 внешних call sites** + dual widget pipelines.
5. **Formatting helpers** — `MoneyFormat` vs 5 private `formatUsd` в analytics/charts/widgets.

**Связь с Этапом 8:** DUP-11/12/14/15 — основной вход в UI lightening (удаление dead stacks, Glance-only, MoneyFormat unify).

Для каждой группы указан рекомендуемый **source of truth** и план объединения.

---

## Легенда similarity

| Уровень | Значение |
|---------|----------|
| **Identical** | Copy-paste или 100% делегат |
| **Near-copy** | >50% общей логики, расходятся в деталях |
| **Conceptual** | Одна задача, разные API/слои (часто OK) |
| **Dead alias** | Deprecated wrapper без call sites |

---

## 1. Domain / data — бизнес-логика

### DUP-01 · Near-copy · Room full-replace restore

| | |
|---|---|
| **Файлы** | `utils/BackupService.kt:388-403`, `data/sync/CloudSyncEngine.kt:197-219` (`applyFullHydration`) |
| **Что делает каждый** | BackupService: delete photos/scans + loads/paychecks/diesel/history → reinsert JSON. CloudSyncEngine: delete loads/paychecks/diesel/history → reinsert snapshot (без media guard). |
| **Similarity** | **Near-copy** (~80% loop) |
| **Source of truth** | `BackupService.restoreFromJson` + `BackupData` schema |
| **План** | Extract `BackupRoomApplier.applyFullReplace(BackupData, mode=LOCAL\|CLOUD)`; CloudSyncEngine вызывает shared applier; incremental merge — только в engine. |
| **Confidence** | **High** (`CloudArchitectureGuardTest` уже проверяет оба пути) |

---

### DUP-02 · Near-copy · Snapshot assembly (3 места)

| | |
|---|---|
| **Файлы** | `BackupService.createBackupJson` / `createAutoBackup`, `CloudSyncEngine.pushLocalSnapshot`, `GoogleDriveBackupService` (upload JSON) |
| **Similarity** | **Near-copy** — все собирают Load + Paycheck + Diesel из repositories |
| **Source of truth** | `BackupData` + `BackupDataCodec` |
| **План** | Единый `BackupSnapshotFactory.fromRoom(db): BackupData`; Drive и cloud — transport only. |
| **Confidence** | **High** |

---

### DUP-03 · Near-copy · Telegram paycheck/diesel insert

| | |
|---|---|
| **Файлы** | `sync/telegram/TelegramMessageParser.kt` (~215-287), `sync/ServerTelegramMessageProcessor.kt` (~54-118) |
| **Similarity** | **Near-copy** — week derivation, insert shape; diesel dedup различается (`SHA-256` vs `hashCode`) |
| **Source of truth** | `TelegramMessageParser` + `TelegramLoadHandler` |
| **План** | Extract `TelegramInboundProcessor`; server path = thin wrapper без UI strings; unify diesel dedup на stable hash. |
| **Confidence** | **High** |

---

### DUP-04 · Conceptual · Duplicate checking (3 реализации)

| | |
|---|---|
| **Файлы** | `DuplicateChecker`, `DuplicateAuditUseCase`, `ImportTripDedup` |
| **Similarity** | **Conceptual** — tripId overlap; route/stops в checker+audit |
| **Дельта v2** | `DuplicateChecker.isLikelySameLoad()` теперь использует `compareLoads` как audit (Stage 2 D-01 **закрыт**). **Структурное дублирование 3 классов остаётся** — drift риск на уровне orchestration (import still tripId-only). |
| **Source of truth** | Shared kernel `LoadDuplicateRules` (tripId, route fingerprint, stops hash, keeper pick) |
| **План** | Три orchestrator'а остаются (live ingest / batch janitor / pre-import list), но правила — одни. `ImportTripDedup` → delegate `LoadDuplicateRules`. |
| **Confidence** | **High** |

---

### DUP-05 · Conceptual · Parser packages

| | |
|---|---|
| **Файлы** | `domain/parser/*` (core) vs `domain/import/parser/*` (adapters) |
| **Similarity** | **Conceptual** — import delegates to core (OK). **Near-copy detection:** `MessageTypeDetector` vs `MessageClassifier` relay heuristics. |
| **Source of truth** | `domain/parser/LoadMessageParser` + `MessageParseService` |
| **План** | Import = format adapters only. `MessageTypeDetector.isRelayFormat()` → delegate `MessageClassifier`. `ParserFactory` PLAIN_TEXT → fallback `FlexibleLoadParser`. |
| **Confidence** | **High** |

**Sub-overlaps:**

| Pair | Similarity |
|------|------------|
| `RelayMessageParser` / `TextLoadParser` / `MessageParseService` | Near-copy entry paths |
| `HtmlLoadParser` / `TelegramHtmlExportParser` | Conceptual (Ksoup scrape) |
| `CsvLoadParser` / `DelimitedLoadParser` | Conceptual (разные форматы) |

---

### DUP-06 · Naming collision · Dual CloudSyncEngine

| | |
|---|---|
| **Файлы** | `data/sync/CloudSyncEngine.kt` (object, ~316 LOC), `data/sync/cloud/CloudSyncEngine.kt` (@Singleton, delegates) |
| **Similarity** | **Identical name, different packages** — logic NOT duplicated, entry points ARE |
| **Source of truth** | Object → rename `AccountSnapshotSyncEngine`; all callers через injectable wrapper |
| **План** | Route Workers/MainActivity через Hilt class; kill direct object calls. |
| **Confidence** | **High** |

---

### DUP-07 · Intentional split · Goal math (OK with minor overlap)

| | |
|---|---|
| **Файлы** | `shared/domain/goal/*` vs `app/domain/goal/*` |
| **Similarity** | **Conceptual** — KMP math vs Load-aware orchestration. Minor near-copy: `actualDailyYield` formula. |
| **Source of truth** | Shared: `GoalMoneyMath`, DTOs. App: `WeeklyGoalCalculator`, `LoadYieldCalculator`. |
| **План** | Move `actualDailyYield(gross, days)` into `GoalMoneyMath`; app calls shared. |
| **Confidence** | **High** |

---

### DUP-08 · Conceptual · Identity triple-store

| | |
|---|---|
| **Файлы** | `AuthStore`, `UserProfileStore`, `ProfileRepository`/`DriverProfileEntity`, `CloudSyncEngine.serializeDriverProfile()` (4-й JSON) |
| **Similarity** | **Conceptual** — displayName/avatar/phone в 3–4 местах |
| **Source of truth** | AuthStore=session; UserProfileStore=login seed; Room=UI profile; cloud via `ProfileRepository` codec |
| **План** | Route cloud profile serialize/deserialize через `ProfileRepository`, не ad-hoc JSONObject в engine. |
| **Confidence** | **Medium** |

---

### DUP-09 · Pattern scatter · Week/date normalization

| | |
|---|---|
| **Файлы** | `WeekUtils`, `LoadDateRepair`, `ScheduledTimeParsing`; call sites в parsers + repositories |
| **Similarity** | **Pattern duplication** — `.withReportingWeek()` на многих границах, не logic dup |
| **Source of truth** | `WeekUtils.getLoadReportingWeek` + `LoadDateRepair.resolveRelayYear` |
| **План** | Centralize в `LoadRepository.insertLoad`/`updateLoad`; parsers не set week fields individually. |
| **Confidence** | **High** (utils hierarchy correct); **medium** on removing all parser call sites |

---

### DUP-10 · Near-copy · Private money formatters

| | |
|---|---|
| **Файлы** | `MoneyFormat.kt` vs private `formatUsd/formatRpm/formatMiles` in `AnalyticsScreen`, `AnalyticsBarCharts`, `WeeklyRevenueLineChart`, `AnimatedCircularProgress`, `LoadExporter` |
| **Similarity** | **Near-copy** (~90%); RPM format inconsistent (`$%.2f` vs `$%.2f/mi`) |
| **Source of truth** | `presentation/utils/MoneyFormat.kt` |
| **План** | Replace all private formatters; add `formatMiles` to MoneyFormat if needed. |
| **Confidence** | **High** |

---

## 2. Presentation / UI

### DUP-11 · Dead alias · Gold / GlassCard / NeoGlass stack

| Component | File | Call sites | Similarity |
|-----------|------|------------|------------|
| `GlassCard` / `SoftCard` | `components/GlassCard.kt` | **0 external** | Dead |
| `GoldComponents` | `GoldComponents.kt` | **0** (@Deprecated) | Dead alias |
| `NeoGlassPrimaryButton` | `NeoGlassButton.kt` | **0** | 100% delegate to `TlButton` |
| `BentoGlassCard` | `theme/BentoGlass.kt` | **~35** | **Keep** |

**Plan:** Delete Gold/GlassCard/NeoGlass after one release deprecation. **Confidence: High**

---

### DUP-12 · Near-copy · AddDiesel ≈ AddPaycheck scaffold

| | |
|---|---|
| **Файлы** | `AddDieselScreen.kt` (375 LOC), `AddPaycheckScreen.kt` (275 LOC) |
| **Similarity** | **Near-copy** (~55%) — date/time pickers, save confirm dialog, week selector, Scaffold+TopAppBar |
| **Source of truth** | Shared composables: `JournalDateTimePickers`, `JournalSaveConfirmDialog`, `WeekSelectorRow`, `JournalEntryScaffold` |
| **Plan** | Extract ~120 LOC shared; migrate both to `OneUiLargeTitleHeader` (как AddLoad). |
| **Confidence** | **High** |

---

### DUP-13 · Conceptual · Add screen chrome divergence

| | |
|---|---|
| **Файлы** | `AddLoadScreen` (OneUiLargeTitleHeader) vs AddDiesel/Paycheck (Material TopAppBar) |
| **Similarity** | **Conceptual** — same feature class, different chrome |
| **Source of truth** | `OneUiLargeTitleHeader` + `OneUiBottomActionBar` + `BentoGlassTheme.ScreenBackground` |
| **Confidence** | **High** |

---

### DUP-14 · Dead alias · Typography triple

| | |
|---|---|
| **Файлы** | `AppTypography`, `ForestSectionTitle`, `BentoSectionTitle` (0 call sites), `NeoGlassTypography` (0 call sites) |
| **Similarity** | `ForestSectionTitle` ≈ `BentoSectionTitle` **identical** |
| **Source of truth** | `AppTypography` + `ForestSectionTitle` |
| **Plan** | Delete NeoGlassTypography, collapse BentoSectionTitle. |
| **Confidence** | **High** |

---

### DUP-15 · High · Dual widget pipelines (4 providers)

| | |
|---|---|
| **Файлы** | `TruckerLoadWidget.kt` (2 RemoteViews receivers), `glance/OneUiGlanceWidgets.kt` (2 Glance), `WidgetRefresh.kt` (updates both) |
| **Similarity** | **Near-copy** (~60%) — same stats, deep links, formatters |
| **Source of truth** | **Glance** long-term |
| **План** | Port classic features (quick actions, day strip) → Glance; deprecate RemoteViews; single update path. |
| **Confidence** | **High** |

---

### DUP-16 · Minor · Widget route constants

| | |
|---|---|
| **Файлы** | `WidgetDeepLink.kt`, `MainActivity` companion constants, `Routes.kt` |
| **Similarity** | **Conceptual** — string literals duplicated |
| **Source of truth** | `Routes` + `WidgetDeepLink.resolveNavRoute` |
| **Plan** | MainActivity references Routes/WidgetDeepLink only. |
| **Confidence** | **High** |

---

### DUP-17 · Conceptual · Voice phrase tables

| | |
|---|---|
| **Файлы** | `AppVoiceActions.kt`, `LocalSpokenAssistantParser.kt`, `VoiceIntentReader.kt`, `strings.xml` nav/drawer titles |
| **Similarity** | **Conceptual** — diesel/paycheck/gross/screen phrases duplicated RU+EN in Kotlin vs resources |
| **Source of truth** | `strings.xml` (with normalized phrase table for voice matching) |
| **Plan** | Shared `VoiceIntentKeywords` built from string resources. |
| **Confidence** | **Medium** (voice needs normalized forms) |

---

### DUP-18 · Minor · Screen scaffold variants

| | |
|---|---|
| **Файлы** | `SoftAppPageScaffold`, `OneUiScreen`, raw `Scaffold` in add flows |
| **Similarity** | **Conceptual** — ~50% overlap |
| **Source of truth** | `SoftAppPageScaffold` (tool screens); `OneUiFormScaffold` (forms) — new wrapper |
| **Confidence** | **Medium** |

---

## 3. Resources / backend

### DUP-19 · Near-full duplicate · Russian string catalogs

| | |
|---|---|
| **Файлы** | `res/values/strings.xml` (**1528** lines), `res/values-ru/strings.xml` (**1528**), `values-en` (**1526**) |
| **Similarity** | **Near-identical** — same string names; diff ~12 lines (restore confirm strings aligned) |
| **Source of truth** | One Russian catalog: either `values/` (default) **or** `values-ru/`, not both |
| **Plan** | Delete duplicate; keep `values-en/` for English. |
| **Confidence** | **High** |

---

### DUP-20 · Hardcoded strings bypass i18n

| | |
|---|---|
| **Файлы** | `TaxTrackerScreen.kt` (EN tax labels), voice modules (RU/EN phrases) |
| **Similarity** | Duplicates string resource concepts |
| **Source of truth** | `strings.xml` |
| **Confidence** | **High** (TaxTracker); **Medium** (voice) |

---

### DUP-21 · Backend — no duplicate endpoints found

Ktor routes in `Application.kt` are unique per path. Snapshot/media/telegram/device endpoints serve distinct purposes. **No consolidation needed.**

`SyncConflictResolver` is pass-through to `CloudSyncPolicy` — **dead abstraction layer** (DUP-22, low).

---

## 4. Сводная таблица: что оставить как source of truth

| Область | Source of truth | Удалить/слить |
|---------|-----------------|---------------|
| Load parsing | `LoadMessageParser` + `MessageParseService` | Duplicate detection in `MessageTypeDetector` |
| Import adapters | `domain/import/parser/*` (thin) | Inline logic in adapters |
| Cloud sync engine | Rename object → `AccountSnapshotSyncEngine`; Hilt wrapper = entry | Direct object calls |
| Room restore | `BackupService` + shared `BackupRoomApplier` | `CloudSyncEngine.applyFullHydration` loop |
| Snapshot payload | `BackupData` / `BackupDataCodec` | Ad-hoc assembly in 3 places |
| Telegram ingest | `TelegramMessageParser` + `TelegramLoadHandler` | `ServerTelegramMessageProcessor` paycheck/diesel block |
| Duplicate rules | New `LoadDuplicateRules` | Scattered criteria in 3 classes |
| Goal math | `shared/domain/goal/GoalMoneyMath` | Duplicate yield formula in app |
| Profile identity | AuthStore → UserProfileStore → Room profile | Ad-hoc cloud JSON in engine |
| Week assignment | `LoadRepository` choke point | Parser-level `withReportingWeek` scatter |
| Money display | `MoneyFormat` | 5× private formatters |
| UI cards/buttons | `BentoGlassCard`, `TlButton` | GlassCard, Gold*, NeoGlass* |
| Add journal forms | Shared `JournalEntryScaffold` | Diesel/Paycheck copy-paste |
| Typography | `AppTypography`, `ForestSectionTitle` | NeoGlassTypography, BentoSectionTitle |
| Widgets | Glance (`OneUiGlanceWidgets`) | RemoteViews providers (after parity) |
| Deep links | `Routes` + `WidgetDeepLink` | MainActivity route constants |
| i18n | Single RU catalog + `values-en` | Duplicate `values-ru` |

---

## 5. План объединения по приоритету

| P | ID | Action | Risk if not done |
|---|-----|--------|------------------|
| **P0** | DUP-01 | Shared `BackupRoomApplier` | Restore/cloud diverge; media wipe inconsistency |
| **P0** | DUP-03 | Unified Telegram inbound processor | Diesel dedup hash mismatch; divergent insert rules |
| **P1** | DUP-04 | `LoadDuplicateRules` kernel | Import vs live vs audit **orchestration** drift (logic aligned; kernel still absent) |
| **P1** | DUP-06 | Single CloudSyncEngine entry | Status tracker dead; grep confusion |
| **P1** | DUP-15 | Widget → Glance only | 2× maintenance, 4 providers |
| **P2** | DUP-02 | `BackupSnapshotFactory` | Snapshot schema drift |
| **P2** | DUP-10 | MoneyFormat everywhere | RPM display inconsistency |
| **P2** | DUP-12/13 | Journal form scaffold | ~200 LOC duplicate UI |
| **P2** | DUP-19 | One Russian strings file | 1500 lines duplicate XML |
| **P3** | DUP-11/14 | Delete dead UI components | Confusion for new contributors |
| **P3** | DUP-17 | Voice phrase centralization | App Actions drift from nav labels |
| **P3** | DUP-09 | Repository week choke | Wrong week on parser-only paths |

---

## 6. Что НЕ является проблемой дублирования

- **`shared/contract` vs backend DTOs** — intentional KMP boundary ✅
- **`VoiceCommandHandler` vs `AppVoiceActions`** — parser vs UI executor (complementary) ✅
- **`StatBox` vs `BentoGrid`** — thin wrappers over `BentoGlassMetricCell` ✅
- **`ChartEmptyState`** — single definition, shared import ✅
- **Import parser delegation to core** — correct layering ✅

---

## Статус этапа

**Этап 3 (Audit v2) завершён.** Удалений/рефакторинга не выполнялось — refresh inventory + consolidation plan на post-P0 `main`.

**Следующий шаг (после подтверждения):** Этап 4 — мёртвый код и неиспользуемые данные.
