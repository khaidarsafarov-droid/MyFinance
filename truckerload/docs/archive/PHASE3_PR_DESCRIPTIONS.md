# Phase 3 — PR descriptions (шаги 3.2–3.5)

Готовые описания для копирования в PR. Шаблон-заготовка: [`.github/PULL_REQUEST_TEMPLATE/phase3_god_file_split.md`](../../.github/PULL_REQUEST_TEMPLATE/phase3_god_file_split.md).

**Уже в main:** 3.1 (`SocialRepository` split, PR #111), 3.6 (lint-gate).

---

## Phase 3.2 — FriendsLiveMapScreen split

**Цель:** Разбить `FriendsLiveMapScreen.kt` (976 строк) на `ui/social/friends/map/*`.

### Что сделано

| До | После | Строки |
|---|---|---|
| `presentation/screens/map/FriendsLiveMapScreen.kt` | `presentation/screens/social/friends/map/FriendsLiveMapScreen.kt` | 976→~180 |
| | `friends/map/FriendsMapViewModel.kt` | →~220 |
| | `friends/map/FriendsMapUiState.kt` | →~45 |
| | `friends/map/MapOverlays.kt` | →~190 |
| | `friends/map/LocationPermissionHandler.kt` | →~120 |
| | `friends/map/FriendsMapBottomSheet.kt` | →~160 |

### Архитектура

- `FriendsMapViewModel` — `@HiltViewModel`, инжектит `StatusRepository` (из 3.1).
- `LocationPermissionHandler` — чистый Compose, без VM-зависимостей.
- `FriendsLiveMapScreen` — только композиция (`hiltViewModel()`, `Scaffold`, map + overlays + bottom sheet).

### Проверки

- [ ] `./gradlew :app:checkKotlinFileSize` — green
- [ ] `./gradlew :app:testDebugUnitTest` — все проходят
- [ ] `./gradlew :app:assembleDebug` — ok
- [ ] `hiltJavaCompileDebug` — ok

### Тесты

- [ ] `FriendsMapViewModelTest` — state transitions (Loading → Success / Error)
- [ ] `FriendsMapScreenTest` (instrumented, опционально) — bottom sheet opens

### Риски / Rollback

- **Риск:** Карта может потерять состояние камеры при переносе логики в ViewModel.
- **Rollback:** `git revert` PR; старый 976-строчный файл восстанавливается.

### Связанные шаги

- Blocked by: **3.1** (использует `StatusRepository` / `SocialRepository` facade)
- Blocks: **3.4** (`FriendsMapViewModel` выносится из `SocialViewModels.kt`)

---

## Phase 3.3 — TelegramBotSyncEngine split

**Цель:** Разбить `TelegramBotSyncEngine.kt` (871 строк) на `data/sync/telegram/*`.

### Что сделано

| До | После | Строки |
|---|---|---|
| `sync/TelegramBotSyncEngine.kt` | `sync/telegram/TelegramBotSyncEngine.kt` | 871→~280 |
| | `sync/telegram/TelegramApiClient.kt` | →~250 |
| | `sync/telegram/TelegramMessageParser.kt` | →~180 |
| | `sync/telegram/TelegramSyncScheduler.kt` | →~120 |
| | `sync/telegram/TelegramStateMachine.kt` | →~80 |

### Архитектура

- `TelegramBotSyncEngine` — coordinator/facade, инжектит 4 зависимости.
- `TelegramApiClient` — `@Singleton`, только сетевой слой (retry, rate limiting).
- `TelegramMessageParser` — чистые функции, без side effects.
- `TelegramSyncScheduler` — WorkManager scheduling, backoff.
- `TelegramStateMachine` — `SyncState`: Idle, Polling, Syncing, Error.
- **Только move** — логика синхронизации не меняется.

### Проверки

- [ ] `./gradlew :app:checkKotlinFileSize` — green
- [ ] `./gradlew :app:testDebugUnitTest` — все проходят (парсеры Relay/Telegram)
- [ ] `./gradlew :app:assembleDebug` — ok
- [ ] `hiltJavaCompileDebug` — ok

### Тесты

- [ ] `TelegramMessageParserTest` — parse incoming → domain models (если уже есть — не ломаются)
- [ ] `TelegramStateMachineTest` — переходы Idle → Polling → Syncing → Error

### Риски / Rollback

- **Риск:** WorkManager jobs могут задвоиться при смене class name worker/engine.
- **Rollback:** `git revert` PR; один файл `TelegramBotSyncEngine.kt` восстанавливается.

### Связанные шаги

- Blocked by: нет (независим от 3.1)
- Blocks: нет

---

## Phase 3.4 — SocialViewModels split

**Цель:** Разбить `SocialViewModels.kt` (770 строк) — 1 ViewModel = 1 файл.

### Что сделано

| До | После | Строки |
|---|---|---|
| `presentation/screens/social/SocialViewModels.kt` | `social/ProfileViewModel.kt` | 770→~[X] |
| | `social/ChatListViewModel.kt` | →~[Y] |
| | `social/ChatDetailViewModel.kt` | →~[Y] |
| | `social/GroupListViewModel.kt` | →~[Y] |
| | `social/GroupDetailViewModel.kt` | →~[Y] |
| | `social/FriendsMapViewModel.kt` | →~[Y] *(если не вынесен в 3.2)* |
| | `social/StatusViewModel.kt` | →~[Y] *(если есть)* |

### Архитектура

- Каждый `@HiltViewModel` с `@Inject constructor`.
- Каждый файл ≤350 строк (ideal). Если VM >350 — вынести `*UiState` / `*Event` в отдельный файл.
- ViewModels пока инжектят `SocialRepository` facade (миграция на конкретные repos — отдельный PR после 3.5).

### Проверки

- [ ] `./gradlew :app:checkKotlinFileSize` — green
- [ ] `./gradlew :app:testDebugUnitTest` — все проходят
- [ ] `./gradlew :app:assembleDebug` — ok
- [ ] `hiltJavaCompileDebug` — ok

### Тесты

- [ ] `HiltArchitectureTest` — не сломан bridge `UserAccountModule`
- [ ] Unit-тесты VM (если добавлены) — базовые state transitions

### Риски / Rollback

- **Риск:** KSP/Hilt может не найти VM после переименования package — проверить `@HiltViewModel` + imports в NavGraph.
- **Rollback:** `git revert` PR.

### Связанные шаги

- Blocked by: **3.2** (если `FriendsMapViewModel` уже вынесен на карте)
- Blocks: нет

---

## Phase 3.5 — LoginScreen + AuthRepository split

**Цель:** Разбить `LoginScreen.kt` (587 строк) + выделить `AuthRepository`.

### Что сделано

| До | После | Строки |
|---|---|---|
| `presentation/screens/login/LoginScreen.kt` | `ui/auth/LoginScreen.kt` | 587→~220 |
| *(логика в Screen)* | `ui/auth/AuthViewModel.kt` | →~180 |
| | `data/repository/auth/AuthRepository.kt` | →~40 |
| | `data/repository/auth/AuthRepositoryImpl.kt` | →~280 |

### Архитектура

- `AuthRepository` — unified interface: Google, email, anonymous, `observeAuthState()`, `signOut()`.
- `AuthViewModel` — `AuthUiState`, вызывает `AuthRepository`.
- `LoginScreen` — только Compose UI; **никакого** `SupabaseClient.create()` в Composable.
- Wiring через `UserComponent` / `ApplicationStoreModule` (auth stores уже в Singleton).

### Проверки

- [ ] `./gradlew :app:checkKotlinFileSize` — green
- [ ] `./gradlew :app:testDebugUnitTest` — все проходят
- [ ] `./gradlew :app:assembleDebug` — ok
- [ ] `hiltJavaCompileDebug` — ok

### Тесты

- [ ] `AuthViewModelTest` — sign-in error / loading states
- [ ] `AuthRepositoryTest` (mock Supabase) — `observeAuthState` emits correctly

### Риски / Rollback

- **Риск:** Google Credential Manager / Supabase flow может сломаться при переносе из Composable.
- **Rollback:** `git revert` PR; `LOCAL_ONLY_MODE=true` остаётся рабочим fallback.

### Связанные шаги

- Blocked by: нет
- Blocks: Phase 5 (cloud maturity) — чистый auth boundary

---

## Rebase старых PR (#100–#104)

| Сценарий | Действие |
|---|---|
| Мало конфликтов (<10 файлов) | `git rebase main` → resolve → force push |
| Много конфликтов или pre-Hilt фабрики | **Redo проще.** Cherry-pick отдельные коммиты или скопировать `.kt` в новую ветку от `main` |
| В PR есть уникальная логика (не просто split) | Скопировать файлы из старого PR, адаптировать под Hilt/`UserComponent` |

**Рекомендация:** закрыть #100–#104 комментарием «Superseded by Phase 3 rework with Hilt (#111+)». Не тратить время на rebase pre-Hilt фабрик — redo с чистого `main` быстрее.
