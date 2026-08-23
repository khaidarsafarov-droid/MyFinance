# Этап 4 — Мёртвый код и неиспользуемые данные (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этапы 1–3 v2  
**Метод:** reference grep (main + test), NavGraph/deep-link cross-check, lint-baseline, build.gradle vs imports. **Ничего не удалялось.**

---

## Резюме

| Категория | Количество | Confidence |
|-----------|------------|------------|
| Мёртвые composables/classes (0 prod call sites) | **~35** | certain/probable |
| Public fun без references | **~142** | order-of-magnitude |
| Unreachable nav / shortcuts | **4** | certain |
| Unused Gradle dependencies | **4 certain** + 1 manual check | |
| Unused BuildConfig fields | **7** | certain |
| Lint UnusedResources (baselined) | **67** | certain |
| Commented-out code blocks | **0** multi-line | certain |
| **New v2:** DI-wired but uncalled sync wrapper | **2 classes** | certain |

**Дельта v2:** P0-фиксы не добавили мёртвого prod-кода; добавлены **активные** тесты `DuplicateCheckerTest`, `AuthenticatedUserAccountIdTest`. Injectable `cloud.CloudSyncEngine` по-прежнему **не вызывается** из workers/MainActivity (используется legacy object).

⚠ **Ничего не удалять автоматически** — часть — intentional stubs (tax, community, TURN/Cerebras).

---

## 1. Мёртвые UI composables / экраны

### 1.1 Deprecated / zero call sites (certain)

| File | Symbol | Почему мёртвый |
|------|--------|----------------|
| `components/GoldComponents.kt` | GoldCard, GoldButton, GoldDivider, GoldIcon, GoldAnimatedNumber | @Deprecated; 0 imports |
| `components/NeoGlassButton.kt` | NeoGlassPrimaryButton | 0 call sites |
| `theme/NeoGlassTypography.kt` | NeoGlassTypography (+ members) | 0 references |
| `components/BentoGrid.kt` | BentoSectionTitle | 0 call sites |
| `theme/BentoGlass.kt` | BentoGlassDarkPanel | 0 call sites |
| `theme/DarkGlassTypography.kt` | DarkGlassScreenTitle, DarkGlassSectionTitle | Deprecated; 0 external |
| `theme/DarkGlassTypography.kt` | ForestScreenTitle | Only from dead DarkGlass wrapper |
| `theme/Animations.kt` | rememberSystemReduceMotion, screenEnterAnimation, StaggeredAnimatedItem | 0 call sites |
| `theme/AppElevation.kt` | heroShadow, buttonShadow, inputShadow, navShadow | 0 call sites |
| `components/OneUiScreen.kt` | oneUiContentPadding | 0 call sites |
| `components/SoftUiButtons.kt` | TlChipButton | 0 call sites |
| `components/TabletSoftChrome.kt` | SoftSurfaceCard, SoftSectionSpacer | 0 call sites |
| `components/AdaptiveDialog.kt` | AdaptiveAlertDialog | 0 call sites |

### 1.2 Feature stubs — код есть, UI не подключён (certain)

| File | Symbol | Почему мёртвый |
|------|--------|----------------|
| `screens/tax/TaxTrackerScreen.kt` | TaxTrackerScreen | **Not in NavGraph** |
| `screens/tax/TaxTrackerViewModel.kt` | TaxTrackerViewModel | Only unwired screen + unit test |
| `domain/usecase/ForecastService.kt` | ForecastService | Never constructed in prod |
| `domain/usecase/FuelAnalyticsService.kt` | FuelAnalyticsService | Test-only |
| `utils/ocr/HybridOCRService.kt` | HybridOCRService | Live path: OCRService/Tesseract |
| `components/ForecastCard.kt` | ForecastCard | 0 call sites |
| `components/FuelAnalyticsCard.kt` | FuelAnalyticsCard | 0 call sites |
| `components/CameraButton.kt` | CameraButton | 0 call sites |
| `components/ScannerButton.kt` | ScannerButton | 0 call sites |
| `components/HomeWeekHeroCard.kt` | HomeWeekHeroCard | 0 call sites |
| `components/LossAversionBanner.kt` | LossAversionBanner | 0 call sites |
| `components/GoalLinearProgress.kt` | GoalLinearProgress | 0 call sites |
| `components/PenaltyItem.kt` | PenaltyItem | 0 call sites |
| `components/WeekCalendarPicker.kt` | WeekCalendarPicker | 0 call sites |
| `screens/auth/LoginEmailScreen.kt` | LoginEmailScreen | AuthNavHost uses Login/SignUp only |
| `screens/camera/PhotoPreviewScreen.kt` | PhotoPreviewScreen | No nav callers |

---

## 1b. Sync layer — DI wired, prod uncalled (certain, v2)

| File | Symbol | Почему мёртвый |
|------|--------|----------------|
| `data/sync/cloud/CloudSyncEngine.kt` | `@Singleton CloudSyncEngine` | Hilt-provided; **0 prod call sites**. Workers/MainActivity call `data.sync.CloudSyncEngine` object directly. |
| `data/sync/cloud/SyncStatusTracker.kt` | SyncStatusTracker | Updated only by unused injectable wrapper → UI never sees sync status. |
| `data/sync/cloud/SyncConflictResolver.kt` | SyncConflictResolver | Pass-through to `CloudSyncPolicy`; **test-only** references. |

**Confidence:** **certain** (grep `CloudSyncEngine.onSessionReady` → Worker/MainActivity use legacy object package).

---

## 2. Domain / data — unused public API

### 2.1 Repository methods (certain — 0 call sites)

| File | Method |
|------|--------|
| `DieselRepository.kt:46` | `deleteAllDiesel` |
| `PaycheckRepository.kt:38` | `deleteAllPaychecks` |
| `LoadRepository.kt:194` | `getLoadsByDateRangeOnce` |
| `LoadRepository.kt:220` | `importLoadsIfNotDuplicate` |
| `LoadRepository.kt:307` | `getChangeHistory` |
| `LoadRepository.kt:462` | `backfillRouteMetricsFromStops` |
| `PhotoRepository.kt:26` | `watchPhotosFiltered` |
| `PhotoRepository.kt:96` | `deletePhotosForLoad` |
| `ScanRepository.kt:55` | `linkScanToLoad` |
| `ScanRepository.kt:95` | `deleteScansForLoad` |
| `WeekRepository.kt:21` | `getWeekSummary` (Flow) |
| `WeekRepository.kt:80` | `getWeeksInMonthSummaries` |
| `WeekRepository.kt:113` | `getPeriodLoadsOnce` |
| `AiRepository.kt:35` | `extractTextFromImage` |
| `AiRepository.kt:66` | `generateRealTimeLogisticsInsight` |
| `SettingsDataStore.kt:255` | `saveParserPriceThreshold` |
| `SettingsDataStore.kt:278` | `getDynamicColorOnce` |

### 2.2 DAO / Ktor scaffolding (probable — needs manual check)

- CrowdRateDao, LoadDao mile sums, PhotoDao unlinked, TelegramInboxDao count — 0 repository callers.
- `KtorJournalApi` / `KtorLoadApi` / `MediaPresignApi` — some methods 0 call sites (contract scaffolding).

### 2.3 Prefs API leftovers (probable)

`AuthCredentialsStore.getPassword`, `AuthStore.requireUserId`, `TelegramTokenStore.clearToken`, etc. — zero refs; may be multi-account leftovers.

**Pool:** ~142 public funs with 0 references (supports QUALITY_1000 claim).

---

## 3. Unreachable routes / deep links

| Location | Target | Why unreachable | Confidence |
|----------|--------|-----------------|------------|
| `res/xml/shortcuts.xml:208` | `truckerload://app/community` | No Compose route; WidgetDeepLink returns null | **certain** |
| `res/xml/shortcuts.xml:223` | `truckerload://app/friends_live` | Same | **certain** |
| `Routes.kt:15` | `PROFILE_SETUP` constant | Setup is pre-NavHost gate, not composable route | **certain** |
| (none) | TaxTracker | Screen exists, no route constant or NavHost | **certain** |

Community/friends **strings** (~20 keys × 3 locales) and **shortcuts** remain — feature removed from NavGraph but i18n/shortcuts not cleaned up (**probable dead resources**, not in lint baseline count).

---

## 4. Commented-out code

**No** multi-line commented-out `fun`/`class`/`@Composable` blocks in `app/src/main/java`. Only explanatory `//` comments.

---

## 5. Unused Gradle dependencies (`app/build.gradle.kts`)

| Dependency | Evidence | Confidence |
|------------|----------|------------|
| `retrofit2:retrofit:2.9.0` | 0 imports; networking = Ktor | **certain** |
| `retrofit2:converter-gson:2.9.0` | 0 imports | **certain** |
| `okhttp3:logging-interceptor:4.12.0` | 0 HttpLoggingInterceptor | **certain** |
| `android-maps-utils:3.8.2` | 0 heatmap/cluster usage | **certain** |
| `okhttp:4.12.0` (direct) | No direct OkHttp API | **manual check** (may be transitive) |

---

## 6. Unused BuildConfig / env flags

**0** `BuildConfig.*` references in Kotlin for:

| Field | Notes |
|-------|-------|
| `CEREBRAS_API_KEY` / `CEREBRAS_MODEL` | No AI client |
| `GOOGLE_DIRECTIONS_API_KEY` | No Directions client |
| `TURN_URI` / `TURN_USERNAME` / `TURN_CREDENTIAL` | No WebRTC |
| `MAX_GROUP_CALL_PARTICIPANTS` | No group call |
| `GOOGLE_MAPS_API_KEY` (BuildConfig) | Manifest uses local.properties; BuildConfig field unused |

**Still used:** TELEGRAM_*, GOOGLE_WEB_CLIENT_ID, SUPABASE_*, SYNC_BACKEND_URL, LOCAL_ONLY_MODE, CLOUD_MEDIA_ENABLED, FIREBASE_CONFIGURED.

---

## 7. Lint baseline — UnusedResources

`app/lint-baseline.xml`: **67** UnusedResources (inventory doc claimed **230** — outdated).

Hotspots: `widget_colors.xml`, `widget_dimensions.xml`, unused widget drawables, ~7 strings.

Run `:app:lintDebug` still fails on non-baselined issues (~663 per AGENTS.md) — Stage 5.

---

## 8. Orphaned tests

| Test | Status |
|------|--------|
| `TaxTrackerViewModelYearSwitchTest` | Tests unwired screen — **soft orphan** |
| `FuelAnalyticsServiceTest` | Tests prod-dead service — **soft orphan** |
| Community/friends/call screen tests | **None** — features removed without test leftovers ✅ |

---

## 9. Кластеры для cleanup (при подтверждении)

```
Dead UI:     GoldComponents, NeoGlassTypography, TaxTracker*, Forecast/Fuel cards
Dead sync:   cloud.CloudSyncEngine (injectable), SyncStatusTracker, SyncConflictResolver
Dead domain: ForecastService, FuelAnalyticsService, HybridOCRService
Dead nav:    shortcuts community/friends_live, Routes.PROFILE_SETUP
Dead deps:   Retrofit, maps-utils, logging-interceptor
Dead config: Cerebras, TURN, Directions, MAX_GROUP_CALL
Dead i18n:   community/friends string bulk (shortcuts only)
```

**Manual check before delete:** Tax tracker, community shortcuts, TURN/Cerebras may be intentional backlog.

---

## Статус этапа

**Этап 4 (Audit v2) завершён.** Refresh на post-P0 `main`; добавлен блок sync DI dead code.

**Следующий шаг (после подтверждения):** Этап 5 — lint/compiler errors, security, performance.
