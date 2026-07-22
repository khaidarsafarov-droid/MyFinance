# TruckerLoad QUALITY_1000+ — Harsh audit inventory

**Package:** `com.truckerload` (NOT trucklog)  
**Tree:** `/workspace/truckerload`  
**Evidence date:** 2026-07-22  
**Baseline:** `main` with QUALITY_50/100/150 merged (`c3ef59c`)  
**Method:** filesystem-only (grep/LOC/lint-baseline/XML). No assumed fixes.

Legend: each **instance** below is a backlog-seedable task unless noted as a single epic.

---

## Executive summary

1. **363** Kotlin files under `app/src/main/java` (~45.4k LOC); god-files still dominate (`SocialRepository` 1196, `SettingsScreen` 1055, `StatsScreen` 954, `TelegramBotSyncEngine` 909).
2. QUALITY_150 closed many P0 correctness holes (Telegram offset, cascade delete, swipe confirm, LogRedactor on some paths) — **verified on disk** — but left explicit follow-ups: Home still full-hydrates all loads; true Room paging unused by UI.
3. `app/lint-baseline.xml` still hides **453** issues (**230 UnusedResources** alone). Lint is “green” only because of the baseline.
4. **374** Cyrillic string literals remain outside `strings.xml`; EN heuristic UI-ish literals ~**251** more. i18n is unfinished despite 993 strings × 3 locales.
5. Security posture is still weak for a finance/Telegram app: `allowBackup=true`, BuildConfig secrets, reversible password storage, EncryptedSharedPreferences **plaintext fallback with zero UI**, release **minify=false**.
6. Room schema: `exportSchema=false`, destructive fallback for v1–5, and multiple entities queried by non-PK columns **without indexes** (diesel/paycheck/voice_rooms + inviteCode/archived/firstPuCityState/parsedAt).
7. Presentation debt: **17/20** ViewModel files lack loading and/or error modeling; **66** `collectAsState` vs **0** `collectAsStateWithLifecycle`; **2** Lazy `items` without keys.
8. Perf debt: `watchLoads()` → full `SELECT *` + `hydrateLoads` still feeds Home; **84** data-layer `Flow` APIs have **no** `flowOn` in their files; only HomeViewModel uses `flowOn`.
9. Tests: **70** unit test files vs **363** prod files; **36** packages have **zero** exact-package tests (DAOs, entities, remotes, voice, most screens).
10. Dead-code signal is loud: **~106** never-referenced public funs, **~11** unused classes (`ForecastService`, `FuelAnalyticsService`, `HybridOCRService`, Gold/Bento leftovers). Opportunity pool **>1600** task-instances before splitting god-files.

---

## Category counts (sum toward 1000+)

| Category | Count | Notes |
|---|---:|---|
| Lint UnusedResources | **230** | each instance = task |
| Lint UseTomlInstead | **44** | migrate hard-coded deps → toml |
| Lint GradleDependency | **37** | outdated declared deps |
| Lint PluralsCandidate | **36** | |
| Lint DefaultLocale | **24** | `String.format` without Locale |
| Lint NewerVersionAvailable | **23** | |
| Lint TypographyEllipsis | **12** | |
| Lint IconLauncherShape | **11** | |
| Lint UnusedAttribute | **7** | |
| Lint SmallSp | **7** | |
| Lint UseKtx | **6** | |
| Lint IconDuplicates | **5** | |
| Lint misc (UselessParent 3, TypographyDashes 2, MonochromeLauncherIcon 2, OldTargetApi 1, CredentialManagerMisuse 1, AndroidGradlePluginVersion 1, ConstantLocale 1) | **11** | |
| **Lint baseline total** | **453** | unique IssueIds: 19 |
| Cyrillic hardcoded string literals (`.kt`) | **374** | 37 files |
| EN heuristic hardcoded UI-ish literals | **251** | 59 files (noisy; still seeds) |
| `catch (_:` sites | **27** | silent / literal-return swallows |
| `!!` bangs (all main) | **8** | 6 in presentation/data |
| Never-referenced public fun candidates | **106** | quick whole-app `\bname\b` ≤ decls |
| Never-referenced class candidates | **11** | |
| Room index gaps (entities/fields) | **~10** | see §7 |
| ViewModels missing loading and/or error | **17** | of 20 VM files |
| Screens no VM / weak error UI | **14** | |
| Packages with zero exact-package tests | **36** | |
| Packages with thin tests | **6** | |
| Prod `.kt` files in zero-test packages | **125** | one test-file task each |
| Lazy `items`/`itemsIndexed` without `key` | **2** | |
| `collectAsState` without lifecycle | **66** | 0 WithLifecycle |
| `getSharedPreferences` call sites | **42** | MODE_PRIVATE ecosystem |
| data/ `Flow`-returning APIs w/o `flowOn` in file | **84** | |
| Files with `getAllLoads` / hydrate / getAll call sites | **17** | |
| Hard security/config epics | **7+** | backup, BuildConfig, fallback, minify, schema… |
| Top-30 god-file split epics | **30** | |
| **Rough sum of seedable instances** | **~1690** | before further splits |

---

## 1. Kotlin files + top 30 by LOC

- **Count:** 363 files under `app/src/main/java`
- **Total LOC:** 45,414

| LOC | Path |
|---:|---|
| 1196 | `app/src/main/java/com/truckerload/data/repository/SocialRepository.kt` |
| 1055 | `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt` |
| 954 | `app/src/main/java/com/truckerload/presentation/screens/stats/StatsScreen.kt` |
| 909 | `app/src/main/java/com/truckerload/sync/TelegramBotSyncEngine.kt` |
| 768 | `app/src/main/java/com/truckerload/presentation/screens/social/SocialViewModels.kt` |
| 687 | `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt` |
| 630 | `app/src/main/java/com/truckerload/presentation/navigation/NavGraph.kt` |
| 572 | `app/src/main/java/com/truckerload/presentation/screens/stats/StatsViewModel.kt` |
| 553 | `app/src/main/java/com/truckerload/utils/WeekUtils.kt` |
| 533 | `app/src/main/java/com/truckerload/presentation/screens/login/LoginScreen.kt` |
| 531 | `app/src/main/java/com/truckerload/presentation/screens/social/CommunityScreen.kt` |
| 515 | `app/src/main/java/com/truckerload/presentation/screens/home/HomeViewModel.kt` |
| 489 | `app/src/main/java/com/truckerload/data/local/DatabaseMigrations.kt` |
| 475 | `app/src/main/java/com/truckerload/widget/WidgetRemoteViewsFactory.kt` |
| 466 | `app/src/main/java/com/truckerload/presentation/screens/detail/LoadDetailScreen.kt` |
| 462 | `app/src/main/java/com/truckerload/presentation/screens/social/ProfileScreen.kt` |
| 455 | `app/src/main/java/com/truckerload/presentation/screens/goal/WeeklyGoalScreen.kt` |
| 431 | `app/src/main/java/com/truckerload/data/repository/LoadRepository.kt` |
| 428 | `app/src/main/java/com/truckerload/presentation/screens/voice/VoiceScreens.kt` |
| 406 | `app/src/main/java/com/truckerload/presentation/screens/social/SocialChatScreen.kt` |
| 399 | `app/src/main/java/com/truckerload/presentation/screens/advisor/FinancialAdvisorScreen.kt` |
| 394 | `app/src/main/java/com/truckerload/presentation/screens/analytics/AnalyticsScreen.kt` |
| 382 | `app/src/main/java/com/truckerload/data/remote/SupabaseAuthService.kt` |
| 372 | `app/src/main/java/com/truckerload/presentation/screens/camera/CameraViewModel.kt` |
| 364 | `app/src/main/java/com/truckerload/utils/BackupService.kt` |
| 351 | `app/src/main/java/com/truckerload/presentation/screens/auth/SignUpScreen.kt` |
| 347 | `app/src/main/java/com/truckerload/presentation/components/GoogleMapsHeatmapCard.kt` |
| 344 | `app/src/main/java/com/truckerload/presentation/screens/camera/CameraScreen.kt` |
| 342 | `app/src/main/java/com/truckerload/presentation/components/AdaptiveScaffold.kt` |
| 328 | `app/src/main/java/com/truckerload/presentation/auth/GoogleSignInLauncher.kt` |

---

## 2. lint-baseline.xml — top IssueId frequencies

Source: `app/lint-baseline.xml` (453 instances, 19 IDs).

| # | IssueId | Count |
|---|---|---:|
| 1 | UnusedResources | 230 |
| 2 | UseTomlInstead | 44 |
| 3 | GradleDependency | 37 |
| 4 | PluralsCandidate | 36 |
| 5 | DefaultLocale | 24 |
| 6 | NewerVersionAvailable | 23 |
| 7 | TypographyEllipsis | 12 |
| 8 | IconLauncherShape | 11 |
| 9 | UnusedAttribute | 7 |
| 10 | SmallSp | 7 |
| 11 | UseKtx | 6 |
| 12 | IconDuplicates | 5 |
| 13 | UselessParent | 3 |
| 14 | TypographyDashes | 2 |
| 15 | MonochromeLauncherIcon | 2 |
| 16 | OldTargetApi | 1 |
| 17 | CredentialManagerMisuse | 1 |
| 18 | AndroidGradlePluginVersion | 1 |
| 19 | ConstantLocale | 1 |

---

## 3. Hardcoded RU/EN user strings in `.kt`

### Cyrillic in string literals (comments stripped): **374** matches / **37** files

Highest:

| Count | File |
|---:|---|
| 58 | `domain/geo/CountryCatalog.kt` |
| 49 | `presentation/components/UsStatePaths.kt` |
| 42 | `data/remote/TelegramBotFeatures.kt` |
| 34 | `domain/advisor/DeterministicAdvisorService.kt` |
| 27 | `data/repository/SocialRepository.kt` |
| 27 | `domain/social/EnhancedSocialModels.kt` |
| 23 | `data/social/SocialSeedData.kt` |
| 11 | `data/voice/VoiceSeedData.kt` |
| 10 | `utils/LoadExporter.kt` |
| 9 | `domain/social/SocialModels.kt` |
| … | (+27 more files; UI still has `"Водитель"`, `"/день"`, Telegram RU logs) |

Resources already exist: 993 strings in `values/`, `values-en/`, `values-ru/` — so these are **regression / incomplete extraction**, not missing infrastructure.

### EN heuristic (presentation/sync/advisor/telegram/widget): ~251

Worst: `ChatViewModel.kt` (28), `TelegramApi.kt` (21), `AppProvider.kt` (17), `TelegramBotSyncEngine.kt` (17).

---

## 4. Empty / silent catch blocks

- Truly empty `{ }` bodies: **0** (QUALITY likely cleaned pure empties).
- `catch (_:` sites still present: **27** — many return `null` / `false` / `""` / fallbacks with **no log**.
- Notable: `ScannerViewModel.kt` (3× `_`), `SupabaseAuthService.kt`, `OCRService`/`TesseractOCRService`/`TessDataManager`, `BatteryOptimizationHelper`, `TelegramServiceRestarter`, `ParseUtils`, `ForecastService`, `FuelAnalyticsService`, `MainActivity` (`Throwable`), `CameraScreen`, `GoogleMapsHeatmapCard`.

Task seed: **27** (add logging + typed errors) + review ~80 other `catch (e:` for drop-on-floor.

---

## 5. `!!` bangs in `presentation/` and `data/`

| File | Lines |
|---|---|
| `presentation/MainActivity.kt` | 134 (`userId!!`), 189 (`dependencies!!`) |
| `presentation/screens/settings/SettingsScreen.kt` | 610 (`exportedFile!!`), 878 (`linkedEmail!!`) |
| `presentation/screens/detail/LoadDetailScreen.kt` | 177 (`uiState.load!!`) |
| `data/local/AppDatabase.kt` | 140 (`INSTANCE!!`) |

Also outside those packages: `sync/TelegramBotSyncEngine.kt:388` (`documentFileId!!`), `domain/import/usecase/ImportLoadsUseCase.kt:180` (`loadProcessor!!`). **Total main `!!` = 8.**

---

## 6. TODO / FIXME / HACK

**0** matches in `app/src/main/java` for `TODO|FIXME|HACK|XXX`.  
Debt is undocumented in-code; lives in QUALITY docs + silent catches + dead APIs instead.

---

## 7. Room `@Entity` missing indexes (queried by non-PK)

Entities with **no `indices=`** that DAOs still filter:

| Entity | File | Queried columns (DAO evidence) |
|---|---|---|
| `DieselEntity` | `data/local/entities/DieselEntity.kt` | `weekNumber`,`year` — `DieselDao` |
| `PaycheckEntity` | `data/local/entities/PaycheckEntity.kt` | `weekNumber`,`year` — `PaycheckDao` |
| `VoiceRoomEntity` | `data/local/entities/VoiceEntities.kt` | `isActive` — `VoiceDaos` |
| `SocialPeerEntity` | `data/local/entities/SocialEntities.kt` | `getAll()` full scans |
| `DriverProfileEntity` | same | PK-only (lower risk) |
| `BlockedUserEntity` / `DriverFollowEntity` / `ChallengeParticipationEntity` | same | composite PK only |

Partial indexes but **missing fields used in WHERE**:

| Table | Missing index | Query |
|---|---|---|
| `social_chats` | `inviteCode`, `archived` | `SocialDaos.kt` `WHERE inviteCode = :code`, `WHERE archived = 0` |
| `loads` | `firstPuCityState`, `parsedAt`, `updatedAt` | `LoadDao` origin filter + `ORDER BY parsedAt/updatedAt` |

Also: `AppDatabase` `exportSchema = false`; `.fallbackToDestructiveMigrationFrom(..., 1..5)`.

---

## 8. ViewModels / Screens without error/loading state

Heuristic scan of `*ViewModel*.kt`:

| Status | Files |
|---|---|
| Missing loading | AddDiesel/AddLoad/AddPaycheck, Camera, SocialViewModels (8 VMs), … |
| Missing error | LoadDetail, EditLoad, Goal, Map, Stats, VoiceViewModels, … |
| Missing both | PhotoGallery, Home (no `error` field; has `deleteError` only), Scanner, TaxTracker |
| OK-ish | Analytics, Chat, Settings |

Screens without dedicated VM: auth (`LoginEmailScreen`, `SignUpScreen`, `ProfileSetupScreen`), `LoginScreen`, `PhotoBatchReviewScreen`, `PhotoDetailScreen`, `ScanGalleryScreen`, `ScanResultScreen`, `AvatarCropScreen`.

Screens with weak/no error UI: `PhotoGalleryScreen`, `GroupDetailScreen`, `TaxTrackerScreen`, `CallScreens`, `VoiceScreens`, `ScanResultScreen`, `AvatarCropScreen`.

---

## 9. Unused public APIs / dead code candidates

**~106** public funs with no external reference; **~11** classes.

High-confidence dead:

- `domain/usecase/ForecastService.kt`, `FuelAnalyticsService.kt`
- `presentation/components/ForecastCard.kt`, `FuelAnalyticsCard.kt`
- `utils/ocr/HybridOCRService.kt`
- Gold/Bento leftovers: `GoldComponents.kt` funs, `BentoGrid.kt`, `BentoGlassDarkPanel`, …
- `AuthStore.accessTokenOrNull()` never called
- `logging-interceptor` on classpath but **zero** `HttpLoggingInterceptor` usages in source

Treat as delete-or-wire tasks (do not blindly delete Compose entrypoints referenced only from Nav by string — verify Navigation).

---

## 10. Test gaps

- Unit tests: **70** files; androidTest: **1** (`MultiUserIsolationInstrumentedTest`)
- Prod packages with **0** exact-package tests: **36** (includes all DAOs, entities, `data.remote`, `data.voice`, most presentation screens, `sync.import`, theme)
- Thin: `presentation.components` (41 files / 1 test), `presentation.screens.social` (12/1), `sync` (15/4), `widget` (19/3)

Largest untested surfaces: `SocialRepository`, `TelegramBotSyncEngine`, `SupabaseAuthService`, `TelegramApi`, Settings/Stats UI, Voice/WebRTC.

---

## 11. Compose

- Lazy `items`/`itemsIndexed` **without key:**  
  - `presentation/screens/advisor/FinancialAdvisorScreen.kt:235`  
  - `presentation/screens/analytics/AnalyticsScreen.kt:225` (`itemsIndexed`)
- `collectAsState(`: **66**; `collectAsStateWithLifecycle`: **0**
- `remember` around Flow factory collect: no strong hits; lifecycle gap dominates.

---

## 12. Security

| Finding | Evidence |
|---|---|
| `allowBackup="true"` | `AndroidManifest.xml:28` — backups can include prefs/DB |
| Cleartext | `network_security_config.xml` `cleartextTrafficPermitted="false"` — OK |
| Secrets in BuildConfig | `app/build.gradle.kts` `TELEGRAM_BOT_TOKEN`, `SUPABASE_*`, `CEREBRAS_API_KEY`, `GOOGLE_WEB_CLIENT_ID` |
| Token in URL | `TelegramApi.kt` / `TelegramBotHealth.kt` `api.telegram.org/bot$token/...` |
| LogRedactor | Present on some Telegram paths; **not** universal |
| Encrypted prefs fallback | `SecurePreferences.kt` → MODE_PRIVATE; `plaintextFallbackUsed` **never read by UI** |
| Reversible passwords | `AuthCredentialsStore.putString(pwdKey, password)` |
| MODE_PRIVATE ecosystem | **42** `getSharedPreferences` sites / **43** `MODE_PRIVATE` refs |
| Release minify off | `isMinifyEnabled = false` |
| Exported activities | `MainActivity`, `WidgetConfigureActivity` exported=true |

---

## 13. Performance

| Finding | Evidence |
|---|---|
| Home full hydrate | `HomeViewModel.loadsFromDb = loadRepository.watchLoads()` → `getAllLoads()` → `hydrateLoads` (stops+penalties for **all** rows) |
| Fake paging | `filteredLoadsPaging` pages **in-memory** list; comment admits HomeScreen still uses full `flattenedListItems` |
| Other full loads | `ChatViewModel.getAllLoads().first()`, `MapViewModel.getAllLoadsOnce()`, `SettingsViewModel.getAll()`, `CameraViewModel.getAllLoadsOnce()`, Backup/Drive/Telegram size checks |
| `flowOn` | **Only** `HomeViewModel.kt` in entire main; **84** data `Flow` APIs have none in-file |
| Main-thread Room | No `allowMainThreadQueries`; no `runBlocking`/`GlobalScope` found — OK-ish; Eagerly `stateIn` still does heavy work on collectors |
| God hydrate | `LoadRepository.hydrateLoads` batches IN(500) but still O(all loads) on every emission |

---

## 14. Manifest / Gradle freshness

| Item | Current | Note |
|---|---|---|
| `compileSdk`/`targetSdk` | 34 | Lint `OldTargetApi` baselined |
| `minSdk` | 24 | |
| `versionName` | 1.5.2 (code 7) | |
| AGP | 9.3.0 | |
| Kotlin | 2.2.10 | |
| Compose BOM | **2024.02.00** | very stale vs AGP/Kotlin |
| Room | 2.7.0 | |
| Paging | 3.3.5 | |
| DataStore | **1.0.0** | ancient |
| lifecycle | 2.7.0 | |
| navigation | 2.7.7 | |
| security-crypto | **1.1.0-alpha06** | alpha in prod |
| WorkManager | 2.9.0 | |
| OkHttp | 4.12.0 | logging-interceptor unused |
| Retrofit | 2.9.0 | |
| Many deps not in toml | Lint UseTomlInstead ×44 | |
| Hilt in toml | declared but app module may not apply (verify separately) | |

---

## Top 50 highest-severity **NEW** bugs still present after QUALITY_150

Verified still on disk (not assumed fixed). Severity = user data loss / security / ANR-scale perf / correctness.

1. **Home still full-table hydrate** — `HomeViewModel.kt` `watchLoads()`; QUALITY_150 itself marks true Room paging as follow-up.
2. **`filteredLoadsPaging` unused by Home UI** — dead paging path; memory claim false.
3. **`allowBackup=true`** with auth/token/DB — `AndroidManifest.xml`.
4. **`SecurePreferences` plaintext fallback** with **no Settings/Login warning** — flag write-only.
5. **Passwords stored reversible** in prefs — `AuthCredentialsStore.kt`.
6. **Secrets baked into BuildConfig** — `app/build.gradle.kts` (bot token, Supabase, Cerebras, Google client id).
7. **`fallbackToDestructiveMigrationFrom(1..5)`** — wipe risk — `AppDatabase.kt:172`.
8. **`exportSchema=false`** — no migration CI safety net — `AppDatabase.kt:84`.
9. **Release `isMinifyEnabled = false`** — full symbols + unused code ship.
10. **`ChatViewModel` loads entire journal** via `getAllLoads().first()` for AI context.
11. **`MapViewModel` / `SettingsViewModel` / backups** call `getAllLoadsOnce`/`getAll` on hot paths.
12. **`!!` on `dependencies` / `userId` in `MainActivity`** — crash if race.
13. **`LoadDetailScreen` `load!!`** after null check is fragile under recomposition.
14. **`SettingsScreen` `exportedFile!!` / `linkedEmail!!`**.
15. **`TelegramBotSyncEngine` `documentFileId!!`**.
16. **Diesel/Paycheck tables unindexed** on week queries.
17. **`social_chats.inviteCode` / `archived` unindexed** but queried.
18. **`voice_rooms.isActive` unindexed**.
19. **`loads.firstPuCityState` / `parsedAt` unindexed** despite filters/sorts.
20. **27× `catch (_:` silent** — Scanner OCR/auth paths hide failures.
21. **Telegram bot UX strings hardcoded RU** — `TelegramBotFeatures.kt` (42 Cyrillic).
22. **Advisor copy hardcoded RU** — `DeterministicAdvisorService.kt`.
23. **Social/voice seed demo data** shipped in main — `SocialSeedData`, `VoiceSeedData`.
24. **WebRTC only Google STUN, no TURN** — `WebRtcCallManager.kt` — calls fail on many NATs.
25. **Voice/social features look live but are local-demo** — misleading product surface.
26. **`collectAsState` ×66 without lifecycle** — leaks work when backgrounded.
27. **DefaultLocale ×24 baselined** — RPM/money formatting bugs by device locale.
28. **UnusedResources ×230** — APK bloat; baseline hides rot.
29. **God-class `SocialRepository` 1196 LOC** — untestable; single point of social bugs.
30. **God-screen `SettingsScreen` 1055 LOC**.
31. **God-engine `TelegramBotSyncEngine` 909 LOC**.
32. **No tests for `TelegramBotSyncEngine` / `TelegramApi` / `SupabaseAuthService`**.
33. **No DAO/entity unit tests** (11+13 files).
34. **TaxTracker / Voice / Gallery screens lack error UI**.
35. **Auth screens have no ViewModel** — logic in Composables (`LoginScreen` 533 LOC).
36. **`plaintextFallbackUsed` never gated** — QUALITY_150 #27 incomplete vs “surface warning”.
37. **OkHttp logging-interceptor dependency unused** — dead/attack surface confusion.
38. **Compose BOM Feb 2024** under AGP 9.3 / Kotlin 2.2 — compatibility footgun.
39. **DataStore 1.0.0** still declared.
40. **security-crypto alpha** in production path for tokens/passwords.
41. **WidgetConfigureActivity exported** — ensure intent validation.
42. **Broad permissions** (exact alarm, ignore battery, mic FGS) for partially-demo voice.
43. **Import timeout catches** swallow `TimeoutCancellationException` silently — `ImportLoadsUseCase.kt`.
44. **`LoadRepository.backfillRouteMetricsFromStops` never referenced** — dead maintenance path.
45. **`ForecastService`/`FuelAnalyticsService`/`HybridOCRService` dead** — feature rot / fake roadmap.
46. **Lazy lists without keys** — Advisor + Analytics reorder glitches.
47. **Cyrillic in DB migration defaults** — `DatabaseMigrations.kt` `'Русский,Английский'`.
48. **Multi-account prefs sprawl** (42 SharedPreferences opens) — leak/migration risk remains.
49. **Lint baseline as quality theater** — 453 issues never force CI failure.
50. **Target/compile SDK 34** with `OldTargetApi` baselined — Play policy clock ticking.

---

## Suggested task ID ranges

| Range | Theme | Seed from |
|---|---|---|
| **1–400** | Code quality / correctness / i18n / dead code / god-file splits / bangs / catches / lint code issues | §§1,3–6,8–9 + DefaultLocale/Plurals/Typography |
| **401–700** | Performance / Room indexes / hydrate / paging / flowOn / Eagerly stateIn / Compose keys/lifecycle | §§7,11,13 |
| **701–900** | UX / error+loading states / screen VM extraction / advisor-social-voice honesty / a11y / unused UI | §§8,3,11 + screen gaps |
| **901–1000+** | Security / tests / manifest / gradle freshness / lint dependency & UnusedResources purge | §§2,10,12,14 |

### Mapping cheat-sheet

- **1–50:** Top-50 bugs above (fix first).
- **51–200:** Cyrillic string extraction (374 → batch by file).
- **201–280:** Silent catches + `!!` + dead API delete/wire.
- **281–400:** God-file splits (SocialRepository, Settings, Stats, SyncEngine, SocialViewModels…).
- **401–500:** Room indexes + `exportSchema` + migration hardening.
- **501–600:** Replace Home hydrate with SQL `PagingSource`; kill fake pager or wire it.
- **601–700:** `flowOn` / dispatcher audit; Eagerly→WhileSubscribed; Lazy keys; lifecycle collect.
- **701–800:** ViewModel error/loading matrix (17 files + social/voice VMs).
- **801–900:** Auth/settings/tax/voice UX honesty; demo-seed gating.
- **901–950:** Security (backup, BuildConfig, plaintext fallback UI, minify, password hashing).
- **951–1000+:** Test packages (36 zero-test packages → ≥1 test each) + lint baseline burn-down (453) + dep bumps.

---

## QUALITY_150 verification notes (do not re-open blindly)

Confirmed still fixed on disk:

- Telegram `stoppedOnFailure` does **not** advance to `result.nextOffset`.
- Home swipe → confirm dialog → `confirmDeleteLoad`.
- Photo/Scan delete removes files; `deleteLoad` uses `withTransaction`.
- LogRedactor on selected Telegram error paths.
- Per-user DB naming / Drive+widget scoping (from merge history + code structure).

Still open (called out by QUALITY_150 itself or incomplete):

- True Room SQL paging for Home.
- `plaintextFallbackUsed` user warning not wired.
- Lint baseline not burned down.
- Large untested surfaces.

---

*End of inventory. Regenerated from filesystem evidence only.*
