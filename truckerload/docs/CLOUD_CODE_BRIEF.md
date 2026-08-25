# TruckoRig — бриф для Cloud Code / Claude Code

> **Как пользоваться:** вставь этот файл целиком в начало чата (или положи как `CLAUDE.md` в корень репозитория). Этого достаточно, чтобы понять продукт, стек, структуру и ограничения. Код лежит в папке `truckerload/`.

---

## 1. Что это за продукт

**TruckoRig** — нативное Android-приложение для дальнобойщиков, которые возят грузы **Amazon Relay**.

Водитель ведёт локальный журнал:

- **лоуды** (рейсы / loads) — ставка, мили, RPM, стопы PU/DEL, диспуты;
- **зарплаты** (paychecks) и **дизель**;
- **недельные цели** по гроссу и фактическая суточная выработка;
- **ТО / maintenance**, штрафы, фото и сканы документов;
- **аналитика** по неделям / штатам / маршрутам.

Данные живут **на телефоне** (Room, отдельный файл БД на аккаунт). Облако **опционально**.

Аудитория: owner-operators / company drivers на Amazon Relay в США. UI — **русский + английский** (`AppLanguage`), дизайн — Material 3, тема **Mindwell Forest / Soft UI** (DM Sans).

**Нет** iOS-клиента, **нет** веб/Expo/React Native. Telegram-бот — не отдельный сервис: либо foreground-сервис на телефоне, либо webhook на Ktor.

Версия приложения: `1.5.6` (`versionCode` 11), package `com.truckerload`, `minSdk` 24, `compileSdk`/`targetSdk` 35.

---

## 2. Репозиторий и модули

Корень git — монорепо. Весь продукт в `truckerload/`:

```
truckerload/
├── app/                      # Android-клиент (Kotlin + Jetpack Compose), модуль :app
├── shared/contract/          # KMP: JSON-контракты API (JVM всегда; iOS target только на macOS)
├── shared/domain/            # KMP: портабельная доменная математика (цели недели)
├── backend/server/           # Ktor API на JDK 21
├── deploy/digitalocean/      # App Platform + runbooks
├── docs/                     # архитектура, синки, качество
├── config/kotlin-file-size-baseline.txt
└── gradle/
```

Gradle-модули (`settings.gradle.kts`):

- `:app`
- `:shared:contract`
- `:shared:domain`
- `:backend:server`

---

## 3. Стек

| Слой | Технологии |
|------|------------|
| UI | Jetpack Compose, Navigation Compose, Material 3, Hilt ViewModels |
| DI | Dagger/Hilt 2.60.1 + KSP. `SingletonComponent` — только process-wide (auth, settings, push). Room/репозитории — **account-scoped**, пересоздаются при смене пользователя |
| Локальные данные | Room 2.7, схема v33, файл `truckerload_<userId>`. Paging 3 |
| Фон | WorkManager 2.10, foreground services (Telegram `dataSync`) |
| Сеть | Ktor client 3.5.1, OkHttp |
| Auth | Google Sign-In и/или email+пароль. Supabase Auth — JWT identity, **не** БД приложения. `LOCAL_ONLY_MODE=true` — полностью офлайн |
| OCR | ML Kit Document Scanner + Latin OCR; Tesseract `rus+eng` как запасной |
| Карты | Google Maps, Directions API, heatmap штатов (Crowd RPM) |
| Голос | Gemini / Google Assistant (команды на экраны приложения; `RECORD_AUDIO`) |
| Бэкап без сервера | файл JSON + опционально Google Drive |
| Backend | Ktor, PostgreSQL, DigitalOcean Spaces (S3), FCM wake-up (`type=sync`, без данных аккаунта) |
| Kotlin | 2.2.10, Compose BOM `2026.06.01` |

**Не мигрировать** на Expo / RN / Flutter. Нативный Android остаётся клиентом.

---

## 4. Архитектура (local-first)

```
[Водитель] → Android UI (Compose)
                 ↓ читает/пишет
              Room (per-account)  ← source of truth для UI
                 ↓ outbox
         CloudSyncEngine (если SYNC_BACKEND_URL задан)
                 ↓ JWT (Supabase)
         Ktor  →  PostgreSQL snapshot (LWW по updatedAt)
               →  Spaces/S3 (байты медиа, presigned PUT)
               →  FCM (только сигнал sync)
         Telegram: device long-poll  ИЛИ  server webhook
```

Правила:

- UI **всегда** читает Room. Сеть не блокирует журнал и виджеты.
- Мутация: сначала Room + `sync_outbox`, потом best-effort облако.
- Если `SYNC_BACKEND_URL` пустой или `LOCAL_ONLY_MODE=true` — тот же пайплайн, но только локальное зеркало `files/cloud_account_mirror/<accountId>.json`.
- Медиа в снапшот **не** входят. `CLOUD_MEDIA_ENABLED` по умолчанию `false`.
- Клиент **никогда** не получает: Telegram bot token сервера, секреты БД, Spaces secret, metrics token, Firebase service account.

Hilt:

- Singleton: `AuthStore`, credentials, settings, push-token store.
- После логина `MainActivity` собирает user-граф (Room + репозитории) и отдаёт его в Compose через `CompositionLocal`.
- Logout закрывает БД. Чужой файл `truckerload_<otherUser>` не должен остаться открытым.
- WorkManager workers пока **не** HiltWorker.

---

## 5. Главные пользовательские сценарии

### 5.1 Добавить груз (ядро продукта)

Экран **«Добавить груз»** (`Routes.ADD_LOAD`). Пользователь вставляет текст Amazon Relay (или бот присылает его сам).

Парсер принимает лоуд, если есть:

1. `Trip ID`
2. `Total Rate` > 0
3. хотя бы одна отдельная строка `Pu-address:` или `Del-address:`

Пример:

```text
Trip ID: T-116KYL6KW
Total Rate: 2500.00
Total Loaded Miles: 850 mi
Pu-address: SWF2, Garner, NC
Del-address: TOL3, Perrysburg, OH
```

Адрес-однострочник: `FACILITY, City, ST`. Facility-код хранится отдельно, в маршруте — `City, ST`.

После Save журнал показывает count / miles / RPM. Дедуп по уникальному `tripId`.

Парсеры: `domain/parser/LoadMessageParser`, `AmazonRelayParser`, `domain/import/parser/RelayMessageParser`, плюс CSV/HTML/Telegram-export.

### 5.2 Telegram

- **device** (`TELEGRAM_SYNC_MODE=device`, дефолт): `TelegramBotForegroundService` long-poll на телефоне. Токен **не** зашивается в APK (только `-PallowDebugSecrets=true`).
- **server**: webhook на Ktor, инбокс в PostgreSQL по `update_id`, Android забирает и ack'ает.
- Повтор того же Trip ID → не дублировать, ответить «уже есть».

### 5.3 Недельная цель

Вкладка Stats на телефоне = `WeeklyGoalScreen`. Гросс недели vs цель. Pace: AHEAD / ON_TRACK / BEHIND / GOAL_MET.

`WeeklyGoalCalculator` + `LoadYieldCalculator`: активные дни = длительность PU→DEL (ceil, минимум 1 день на лоуд). Для прошедших недель `daysRemaining` не должен превращать «нужно в день» в весь остаток цели.

Портабельная математика дублируется в `:shared:domain` (`GoalMoneyMath`, `WeeklyGoalProgress`).

### 5.4 Остальное

- Правка лоуда, фактическая дата окончания, диспут.
- Paycheck / diesel — руками или OCR чека.
- Камера с геотегом, галерея, ML Kit scanner, привязка фото/скана к лоуду.
- Карта / heatmap штатов, crowd RPM (локально, без social peers).
- Maintenance-задачи и архив чеков.
- Собственный профиль водителя.
- Виджет рабочего стола, будильники PU.
- Бэкап: Настройки → файл / Google Drive.

---

## 6. Навигация (Compose routes)

Файл: `app/.../presentation/navigation/Routes.kt`.

Нижние вкладки телефона: **Journal / Weekly goal / Profile**.

| Route | Экран |
|-------|--------|
| `home` | Журнал лоудов, календарь недели |
| `stats` | Недельная цель |
| `analytics` / `advanced_stats` | Аналитика и расширенная статистика |
| `map` | Карта / heatmap |
| `profile` / `profile_edit` / `profile_setup` | Собственный профиль |
| `add_load` / `edit_load/{id}` / `load_detail/{id}` | CRUD лоуда |
| `add_paycheck` / `add_diesel` | Зарплата / дизель |
| `maintenance` | ТО |
| `financial_advisor` | Детерминированный фин. советник (не LLM) |
| `settings` / `about` | Настройки |
| `camera` / `scanner` / галереи | Медиа |

На планшете — navigation rail + two-pane (`isTablet()`, `useNavigationRail()`). Иммерсивные экраны (камера/сканер) прячут bottom bar.

Графы: `NavGraph.kt`, `LoadsNavGraph.kt`, `ToolsNavGraph.kt`, `SocialNavGraph.kt`, `AuthNavHost.kt`.

---

## 7. Пакеты Android (`com.truckerload`)

```
app/src/main/java/com/truckerload/
├── TruckerLoadApp.kt              # Hilt @Application
├── presentation/                  # Compose UI
│   ├── MainActivity.kt
│   ├── navigation/
│   ├── screens/                   # home, add, edit, detail, stats, settings,
│   │                              # social (own profile), map, camera, scanner, gallery,
│   │                              # maintenance, goal, login, tax, advisor
│   ├── components/
│   ├── theme/                     # SoftUiTheme, Type, цвета Mindwell Forest
│   └── di/                        # CompositionLocals
├── domain/                        # чистая логика, без Android где возможно
│   ├── model/                     # Load, Stop, Penalty, Diesel, Paycheck, Maintenance
│   ├── parser/                    # Relay / Telegram / receipts
│   ├── import/parser/             # RelayMessageParser, CSV, HTML, Telegram export
│   ├── goal/                      # WeeklyGoalCalculator, LoadYieldCalculator
│   ├── filter/                    # LoadFilterUseCase
│   ├── social/, advisor/, crowd/
├── data/
│   ├── local/                     # AppDatabase, entities, dao, migrations
│   ├── repository/                # LoadRepository, *Repository, social/profile, auth/
│   ├── preferences/               # AuthStore, SettingsDataStore, token stores
│   ├── backup/                    # JSON + Google Drive
│   ├── sync/cloud/                # CloudSyncEngine, LWW, SyncMode
│   └── remote/ktor/               # HTTP к своему backend
├── sync/                          # Telegram FGS, workers, FCM, alarms, media queue
├── widget/
├── di/                            # Hilt modules, UserComponent
└── utils/                         # недели, OCR, даты
```

Backend: `backend/server/src/main/kotlin/com/truckerload/backend/` — `Application.kt` (роуты), `JdbcRepositories.kt`, `Security.kt`.

Контракты: `shared/contract/.../Contracts.kt` — `AccountCloudSnapshot`, media, cursor, push platform.

---

## 8. Модель данных (ядро журнала)

Room `AppDatabase` version **30**, `exportSchema = true` (JSON в `app/schemas/`).

Один аккаунт = один файл `truckerload_<userId>`. Legacy `truckerload_db` не копируется молча — `LegacyDatabaseAbsorb` спрашивает.

### Load (таблица `loads`)

Уникальный индекс по `tripId`. Ключевые поля:

| Поле | Смысл |
|------|--------|
| `id`, `tripId` | стабильный ID рейса |
| `date` | рабочая дата `YYYY-MM-DD`, редактируется |
| `totalRate`, `totalMiles` | ставка и loaded miles |
| `pointA` / `pointB` | первая PU / последняя DEL |
| `puCount`, `delCount`, `stopCount` | стопы |
| `weekNumber`, `year` | ISO-неделя журнала |
| `rawMessage` | исходный текст Relay |
| `parsedAt` | время создания, **никогда не перезаписывать** |
| `updatedAt` | last-write-wins |
| `firstPuMillis` / `lastDelMillis` | денорм. для SQL yield |
| `isDispute`, `disputeCompleted`, `disputeResponseDate` | диспут |
| `actualFinishDate` | факт. конец, иначе из последнего DEL |

Стопы: `Stop` (`PU` | `DEL`) — facility, city, state, zip, scheduledTime, timezone.  
Штрафы: `Penalty(loadId, description, amount)`.

Другие сущности: `Paycheck`, `Diesel`, `MaintenanceTask` / `MaintenanceArchive`, `Photo`, `Scan`, `TelegramInbox`, `SyncOutbox`, `MediaSyncQueue`, `DriverProfile`, `CrowdRate`.

### Правила дат

- Иерархия журнала: год → месяц → день.
- Смена `date` переносит лоуд в другую неделю/год.
- `parsedAt` иммутабелен.

---

## 9. Backend API (когда облако включено)

База: `/v1`, JWT Supabase (`authenticate("supabase")`).

| Метод | Путь | Назначение |
|-------|------|------------|
| GET/PUT | `/v1/sync/snapshot` | аккаунт-снапшот |
| GET/PUT | `/v1/sync/cursor` | курсор устройства |
| POST/DELETE | `/v1/devices/register` | устройство |
| PUT/DELETE | `/v1/devices/push-token` | FCM (`platform=ios` хранится, но не шлётся) |
| POST | `/v1/media/upload-url` | presigned upload |
| POST | `/v1/media/complete` | подтверждение |
| GET/DELETE | `/v1/media/{id}` | метаданные / tombstone |
| POST/GET/ACK | `/v1/telegram/link-token`, `/inbox` | серверный бот |
| POST | `/v1/telegram/webhook` | секретный webhook (не JWT пользователя) |
| GET | `/health/live`, `/health/ready`, `/metrics`, `/openapi.yaml` | ops |

Метрики закрыты `METRICS_BEARER_TOKEN`. Ownership всегда из JWT subject, клиентский `accountId` не авторизует.

---

## 10. Конфигурация (`local.properties`, gitignored)

| Ключ | Смысл |
|------|--------|
| `LOCAL_ONLY_MODE` | `true` = без логина в облако, всё на Room |
| `SYNC_BACKEND_URL` | HTTPS Ktor; пусто = только локальное зеркало |
| `CLOUD_MEDIA_ENABLED` | дефолт `false` |
| `TELEGRAM_SYNC_MODE` | `device` \| `server` |
| `GOOGLE_WEB_CLIENT_ID` | Web OAuth для Google ID token |
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` | identity |
| `TELEGRAM_BOT_TOKEN` | **не** в APK, кроме `-PallowDebugSecrets=true` |

Друзьям собирают APK скриптом `scripts/build-friends-apk.sh` (без своего сервера, бэкап файл/Drive). См. `docs/FRIENDS_SHARE.md`.

---

## 11. Сборка и тесты

Из каталога `truckerload/`:

```bash
sh ./gradlew :app:assembleDebug
sh ./gradlew :app:testDebugUnitTest
sh ./gradlew :shared:contract:jvmTest :shared:domain:jvmTest :backend:server:test
sh ./gradlew :app:checkKotlinFileSize
```

- `gradlew` в checkout может быть не executable — всегда `sh ./gradlew`.
- **Не править** `truckerload/gradle.properties` `org.gradle.java.home=C:\...` — это Windows-путь автора; на Linux его перекрывает `~/.gradle/gradle.properties`.
- Гейт размера файла: прод `.kt` ≤ **600** строк (цель 350). Baseline: `config/kotlin-file-size-baseline.txt`. Не раздувать god-файлы — сплитовать.
- Lint (`:app:lintDebug`) исторически шумный; не считать «починить все lint» частью обычной задачи.
- Юнит-тесты ядра: Relay/Telegram-парсеры, weekly-goal math, CSV export, sync LWW, репозитории.

---

## 12. Инварианты — не ломать

1. **Local-first.** Журнал и виджеты не ждут сеть.
2. **Изоляция аккаунтов.** Своя Room-БД на userId; не синглтонить репозитории на процессе.
3. **Дедуп Trip ID.** Повтор сообщения бота / вставка того же Relay ≠ вторая запись.
4. **`parsedAt` не трогать** при edit; обновлять `updatedAt`.
5. **Парсер Relay:** недостаточно одной простыни текста — нужны `Total Rate` > 0 и отдельная строка адреса PU/DEL.
6. **Секреты бота/AI не в APK.**
7. **Медиа:** байты не гонять через Ktor в проде; только presigned URL. Не логировать JWT, пути, OCR, signed URL.
8. **FCM** — wake-up, не канал данных.
9. **Weekly goal:** для закрытой недели не считать «нужно в день» как весь remaining; SQL yield и in-memory week filter должны согласовываться (`weekNumber=0` и т.п.).
10. **Hilt workers** не внедрять частично — или все, или никакие.
11. **Нет RN/Expo.** Портабельное — только `:shared:contract` / `:shared:domain`.
12. Production `.kt` не растить за 600 строк.

---

## 13. Где что править (шпаргалка)

| Задача | Куда идти |
|--------|-----------|
| Парсинг Relay / Telegram текста | `domain/parser/*`, `domain/import/parser/*`, тесты в `app/src/test/.../parser` |
| Форма «Добавить груз» | `presentation/screens/add/` |
| Карточка / детали лоуда | `screens/detail/`, `screens/edit/`, `screens/home/` |
| Математика недели / RPM / yield | `domain/goal/`, `shared/domain/.../goal/` |
| Room / миграции | `data/local/AppDatabase.kt`, `migrations/Migrations{Early,Mid,Late}.kt` |
| Telegram на устройстве | `sync/TelegramBotForegroundService.kt`, `sync/telegram/*` |
| Облачный sync | `data/sync/cloud/*`, `data/remote/ktor/*`, `backend/server/.../Application.kt` |
| Тема / цвета | `presentation/theme/SoftUiTheme.kt`, `docs/design/` |
| Собственный профиль | `presentation/screens/social/`, `data/repository/social/` |
| Crowd RPM / карта | `presentation/screens/map/`, `domain/crowd/`, `docs/CROWD_RPM_PRIVACY.md` |
| Голосовой ассистент | `voice/`, `presentation/voice/`, `docs/VOICE_ASSISTANTS.md` |
| Виджет | `widget/` |
| Auth / Google | `data/repository/auth/`, `presentation/screens/login/`, `docs/GOOGLE_SIGNIN_SETUP.md` |

Документы рядом (не обязательны, если есть этот бриф):

- `README.md` — запуск
- `docs/TARGET_ARCHITECTURE.md` — целевая схема
- `docs/CLOUD_DATA_SYNC.md` — sync
- `docs/PROJECT_OVERVIEW.md` — короткий обзор
- `docs/RELAY_PARSE_EXAMPLES.md` — примеры сообщений
- `docs/FRIENDS_SHARE.md` — APK без сервера
- `docs/KMP_IOS_ROADMAP.md` — будущего iOS пока нет

---

## 14. Тон продукта

Это рабочий инструмент водителя, не соцсеть. Community / Friends-on-map / LiveKit-комнаты удалены. При сомнении приоритет:

1. корректность лоуда / денег / недели / дедупа;
2. офлайн и изоляция аккаунта;
3. UX журнала (Home, add/edit, цели);
4. остальное.

Язык UI: не вычищать русские строки парсера/OCR — часто нужен dual RU/EN accept.
