# Этап 8 — Облегчение и модернизация UI (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` + Этапы 1–7 v2  
**Стек (определено по коду):** Native **Jetpack Compose** + Material3 (BOM 2026.06.01), Navigation Compose, Hilt, Room, Paging 3, Coil, Glance widgets. **Не web** — Core Web Vitals / JS bundle analyzer не применимы; аналоги — APK size, Compose recomposition, main-thread jank.

**Метод:** static analysis `build.gradle.kts`, Home/Goal screens, theme/animation components. **Код не менялся.**

---

## Резюме

UI построен на **едином BentoGlass / Material3** стеке (~35 экранов используют `BentoGlassCard`). Есть **migration debt**: dead Gold/NeoGlass, dual widget pipelines, 5× private money formatters. Главные проблемы производительности — **Home data path** (full hydrate на ALL filter), **множественные collectAsState** на Home, **animation/shimmer** на software GPU, **тяжёлые native deps** (Maps, ML Kit, Tesseract, material-icons-extended).

---

## 1. Вес приложения (APK / dependencies)

| Dependency / asset | Role | Weight impact | Recommendation |
|--------------------|------|---------------|----------------|
| `material-icons-extended` | Icons | **High** — entire extended set | Replace with `material-icons-core` + explicit icons |
| `play-services-maps` + `maps-compose` + `maps-utils` | MapScreen | **High** | maps-utils **unused** (Stage 4) — remove; lazy-init Maps only on map route |
| `tesseract4android` + runtime tessdata download | OCR rus+eng | **Medium** (~4 MB/lang on first use) | Keep for offline; document download UX; consider ML Kit-only path |
| `play-services-mlkit-document-scanner` + `text-recognition` | Scanner/OCR | **Medium** | Required for scan feature; no duplicate init on Home |
| `itextpdf` (3 modules) | PDF from JPEG | **Medium** | Lazy class-load; only scanner path |
| `firebase-bom` + messaging + crashlytics | Push/crash | **Medium** | Optional via `google-services.json` ✅ |
| `camera-camera2` ×3 | CameraX | **Medium** | Route-scoped; OK |
| `vico` compose charts | Analytics | **Low–Medium** | Load only on Analytics route ✅ |
| **Retrofit + gson + logging-interceptor** | Unused | **Low** | **Remove** (0 imports) |
| `abiFilters` | APK ABI filter | Optional for local debug | Play Store ships AAB; no sideload APK ✅ |
| Release `isMinifyEnabled` + `shrinkResources` | R8 | ✅ | Good |

**APK size measurement:** not run in VM (Gradle JDK path). Recommend `:app:assembleRelease` + `bundletool` / Android Studio APK Analyzer on dev machine.

---

## 2. Изображения / шрифты / иконки

| Item | Current | Issue | Fix |
|------|---------|-------|-----|
| Photos/scans | Local files + **Coil** (`AsyncImage` / `SubcomposeAsyncImage`) | Limited usage (~5 screens); no global size cap documented | Add `size()` / crossfade; thumbnail DAO field for grids |
| Avatars | Coil `SubcomposeAsyncImage` with loading/error | ✅ Subcompose pattern | — |
| Tessdata | Downloaded to `filesDir/tessdata` | First OCR delay + disk | Progress indicator exists in flow; cache OK |
| Fonts | Material3 default (system) | No custom font bloat | ✅ |
| Widget drawables | 67 UnusedResources in baseline | Dead asset weight | Prune with Glance migration |

---

## 3. Recomposition и вычисления на рендер

| Hotspot | File / pattern | Issue | Fix | Effort |
|---------|----------------|-------|-----|--------|
| **HomeScreen** | 10+ `collectAsStateWithLifecycle()` | Any flow emission recomposes whole tree | Split sub-composables; `derivedStateOf`; hoist stable params | M |
| **filteredLoadsAndTotals** | `HomeViewModel` combine + `flowOn(Default)` | Re-filters full in-memory list on every load emission | Prefer Room paging for ALL+year; push filter to SQL | H |
| **loadsFromDb** | `watchLoads()` for non-week filters | Full journal Flow | SQL-scoped flows per filter (Stage 5 SEC-01) | H |
| **AnimatedCircularProgress** | Dual `animateFloatAsState` + Canvas | Runs on Home hero; respects `LocalReduceMotion` ✅ | Default ring static when reduce-motion | L |
| **shimmerPulse** | `rememberInfiniteTransition` | Infinite animation on skeletons | Already skips when reduce-motion ✅ | — |
| **FinancialAdvisorScreen** | `rememberInfiniteTransition` (typing indicator) | Continuous recomposition while chat open | Gate on visible + reduce-motion | L |
| **@Stable / @Immutable** | Domain models | **Rare** — mostly default | Mark `Load`, list item UI models `@Immutable` where stable | M |

**Positive:** `filterState` uses `distinctUntilChanged`; filtering off main thread via `flowOn(Dispatchers.Default)`.

---

## 4. Main-thread / blocking work

| Path | Issue | Fix |
|------|-------|-----|
| `SettingsViewModel.exportCsv()` | `getAll()` on caller thread | `withContext(IO)` |
| `LoadRepository.hydrateLoadEntities` | Stop/penalty fetch per journal load | Batch join or Room `@Relation` |
| Home cold start | Room + multiple Flow subscriptions + animations | Defer non-visible stores; AOT compile (AGENTS.md) |
| MapViewModel | `getAllLoadsOnce()` on IO ✅ | OK; still heavy data |

---

## 5. Стратегия загрузки данных (waterfall)

| Screen | Pattern | Issue |
|--------|---------|-------|
| Home | Week filters: Room paging ✅; ALL: **full hydrate + in-memory filter** | Waterfall: DB → hydrate all stops → filter in VM |
| Home | Calendar: `allLoadsForCalendar` only when dialog open ✅ | Good lazy pattern |
| Analytics | `refresh()` loads full dashboard | No cache; race (V-01) |
| Map | One-shot all loads | Acceptable for heatmap; cache by period |
| Cloud sync | Session: hydrate → pull → push sequential | OK for correctness |

**Fix direction:** Make **Room paging default for ALL+year**; keep in-memory filter only for THIS_MONTH/YESTERDAY/CALENDAR_DATE.

---

## 6. Современность Compose / patterns

| Area | Current | Modern target |
|------|---------|---------------|
| State collection | `collectAsStateWithLifecycle` | ✅ Current best practice |
| Navigation | Navigation Compose 2.9.8 | ✅ |
| DI | Hilt + ViewModel | ✅ |
| Legacy UI | Gold/GlassCard/NeoGlass dead | Delete; **BentoGlass only** |
| Add flows | TopAppBar (Diesel/Paycheck) vs OneUi (AddLoad) | Unify `OneUiLargeTitleHeader` |
| Widgets | RemoteViews + Glance dual | **Glance-only** |
| Material | material + material3 both | Audit if `material` can shrink to m3-only |
| Paging | Implemented but not for ALL filter | Enable `usesRoomPaging` for archive year |

---

## 7. Плавность / отклик / offline

| Check | Result |
|-------|--------|
| Reduce motion setting | `AccessibilitySettingsSection` → `LocalReduceMotion` ✅ |
| Pull-to-refresh | Home `PullToRefreshBox` ✅ |
| Loading skeletons | `StatsCardSkeleton` + shimmer (respects reduce-motion) |
| Empty states | Home flow-specific strings ✅ |
| Offline banner | `ConnectivityStatus` on Home ✅ |
| Button disabled/loading | Partial — Add flows vary |
| Layout shift | Hero ring animates gross text — minor shift; goal amount steps |
| Sync failure visible | ❌ `SyncStatusTracker` not in UI |

---

## 8. Результат этапа — приоритетный список

Sorted by **effect / effort** (E=effort S/M/L):

| Rank | What makes UI heavy/slow now | Concrete fix | Expected effect | E |
|------|------------------------------|--------------|-----------------|:-:|
| 1 | ALL filter full-hydrate journal | Room paging + SQL year filter | Large fleet Home usable; lower memory | L |
| 2 | `material-icons-extended` | Switch to core icons set | **− hundreds KB–1MB+** DEX/resources | M |
| 3 | Dead deps (Retrofit, maps-utils) | Remove from Gradle | Faster builds; smaller APK | S |
| 4 | Home 10+ StateFlow collectors | Decompose + narrow recomposition scope | Smoother scroll/input | M |
| 5 | Dual widget pipelines | Glance-only + delete RemoteViews | Half widget maintenance; less wake work | M |
| 6 | Delete Gold/NeoGlass/GlassCard | Remove dead composables | Clarity; slight compile win | S |
| 7 | Unify MoneyFormat | Remove 5× `formatUsd` privates | Consistent RPM display | S |
| 8 | Maps lazy init | Maps SDK init only on Map route | Faster cold start when map unused | M |
| 9 | Wire SyncStatusTracker to banner | Use injectable sync engine OR expose status from worker | User sees sync failures | M |
| 10 | Journal form scaffold (Add Diesel/Paycheck) | Shared composables + OneUi chrome | ~200 LOC less; consistent UX | M |
| 11 | `@Immutable` on list models | Reduce LazyColumn recomposition | Smoother list scroll | S |
| 12 | Analytics refresh cancel | Generation guard | Correct period after fast switch | S |

---

## Статус этапа

**Этап 8 (Audit v2) завершён.**

**Следующий шаг (после подтверждения):** единый **AUDIT_FINAL_REPORT.md** v2 + PR update.
