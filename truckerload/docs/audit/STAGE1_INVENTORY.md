# Этап 1 — Инвентаризация TruckerLoad (Audit v2)

**Дата:** 2026-08-23  
**Ветка:** `cursor/full-audit-v2-9ae7`  
**База:** `main` @ `8f414b9` (после merge P0-фиксов и docs audit v1)  
**Промпт:** `cursor-audit-prompt (2).pdf` — этапы 1–8 (добавлен Этап 8: UI)  
**Метод:** чтение исходников (filesystem). Без правок кода.  
**Ограничение:** нет доступа к прод-БД / реальному трафику использования — помечено явно.

**Дельта к audit v1:** структура модулей/экранов/API не изменилась существено.  
На `main` уже влиты P0-фиксы: cloud delete sync (`localSnapshotForPush` / orphans), Google `accountId`/`voiceIdentity`, `DuplicateChecker`, предупреждение wipe медиа при restore.  
Этап 8 (облегчение/модернизация UI) — новый относительно v1.

Связанный (устаревший) бэклог: `docs/QUALITY_1000_AUDIT_INVENTORY.md` (2026-07-22).

---

## 1. Карта кодовой базы

### 1.1 Gradle-модули

```
TruckerLoad (root)
├── :app                 # Android client (Compose) — основной продукт
├── :shared:contract     # KMP API-контракты (JVM; iOS на macOS)
├── :shared:domain       # KMP portable domain (goal math, auth enum, …)
└── :backend:server      # Ktor API (PostgreSQL snapshots, Telegram inbox, media, FCM)
```

Прочее в репозитории (не Gradle-модули): `docs/`, `deploy/digitalocean/`, `supabase/` (SQL/конфиг), `scripts/`, `config/`, Docker Compose.

### 1.2 Дерево пакетов `:app` (`com.truckerload`)

```
app/src/main/java/com/truckerload/
├── TruckerLoadApp.kt, MainActivity (entry)
├── data/
│   ├── assistant/          # STT / assistant data helpers
│   ├── auth/               # PasswordPolicy, SilentAuthRestorer
│   ├── backup/             # JSON backup, Google Drive sync
│   ├── local/              # Room AppDatabase, dao/, entities/, migrations/
│   ├── paging/             # FilteredLoadsPagingSource
│   ├── preferences/        # AuthStore, SettingsDataStore, Telegram*, goals, …
│   ├── privacy/            # Privacy-related stores
│   ├── remote/ (+ ktor/)   # Supabase, Google, Telegram API, Ktor clients
│   ├── repository/         # Load/Week/Diesel/Paycheck/Photo/Scan/…
│   │   ├── account/, auth/, crowd/, social/
│   ├── social/             # Social data helpers
│   └── sync/ (+ cloud/)    # CloudSyncEngine, CloudSyncPolicy, media queue
├── di/                     # Hilt modules + UserComponent (per-login graph)
├── domain/                 # Parsers, goal/filter math, models, usecases
├── presentation/
│   ├── auth/, components/, connectivity/, di/, navigation/, privacy/
│   ├── screens/            # UI screens (see §2)
│   ├── theme/, utils/, voice/
├── sync/                   # Telegram FGS/workers, alarms, notifications
│   ├── import/, telegram/
├── utils/ (+ ocr/)
├── voice/                  # App Actions / spoken intents
└── widget/ (+ glance/)     # Home-screen widgets
```

**Объём (примерно, re-count на текущем main):**  
всего `.kt` ≈ **878**; `app` prod ≈ **595**; unit tests ≈ **194**; `shared` ≈ **17**; `backend` ≈ **33**.

### 1.3 Экраны (Compose) и навигация

| Route | Экран | Назначение |
|-------|--------|------------|
| `home` | HomeScreen | Журнал лоудов, неделя, totals |
| `stats` | WeeklyGoalScreen | Недельная цель gross / pace |
| `analytics` | AnalyticsScreen | Аналитика периода |
| `map` | MapScreen | US heatmap / state RPM |
| `load_detail/{id}` | LoadDetailScreen | Карточка лоуда |
| `add_load` | AddLoadScreen | Добавить (paste Relay / manual) |
| `edit_load/{id}` | EditLoadScreen | Редактирование |
| `add_paycheck` / `add_diesel` | Add*Screen | Зарплата / дизель |
| `maintenance` | MaintenanceScreen | ТО / архив чеков |
| `financial_advisor` | FinancialAdvisorScreen | Советник по журналу |
| `voice_assistant` | VoiceAssistantScreen | Голосовой ассистент |
| `settings` / `privacy_settings` / `about` | Settings* / About | Настройки, приватность, about |
| `camera*` / `scanner*` / `attach_pick` | Camera / Scanner / Attach | Медиа + привязка к лоуду |
| `photo_gallery` / `photo_detail` / `scan_gallery` | Gallery* | Галереи |
| `profile` / `profile_edit` | Profile* | Собственный профиль |
| `auth_login` / `auth_signup` | Login / SignUp | Auth (AuthNavHost) |

**Гейты до NavHost (не routes):** EmailVerification → ProfileSetup → TelegramOnboarding.

**Сироты / рассинхрон (для Этапов 3–4):**  
- `TaxTrackerScreen` — UI есть, **нет** регистрации в NavGraph.  
- `Routes.PROFILE_SETUP` — константа есть, экран показывается как gate.  
- App Actions shortcuts: `community` / `friends_live` — **нет** Compose-роутов.

### 1.4 Backend API (Ktor)

| Method | Path | Auth | Назначение |
|--------|------|------|------------|
| GET | `/health/live`, `/health/ready` | — | Liveness / readiness |
| GET | `/openapi.yaml`, `/docs` | — | OpenAPI |
| GET | `/metrics` | metrics bearer | Prometheus |
| POST | `/v1/telegram/webhook` | webhook secret | Inbox / link |
| GET/PUT | `/v1/sync/snapshot` | JWT | LWW account snapshot |
| GET/PUT | `/v1/sync/cursor` | JWT | Device cursor |
| POST/DELETE | `/v1/devices/register` | JWT | Phone/tablet slot |
| PUT/DELETE | `/v1/devices/push-token` | JWT | FCM/APNs |
| GET/POST/DELETE | `/v1/media…` | JWT / signed | Media list, presign, complete, local up/down |
| POST/GET/DELETE | `/v1/telegram/…` | JWT | link-token, inbox, ack, unlink |

Flyway: `V1`…`V5` (users, snapshots, cursors, telegram, media, push, account_devices).

### 1.5 Shared KMP

**contract:** `AccountCloudSnapshot`, `SyncCursor`, media DTOs, Telegram inbox DTOs, device register, `ApiError`, `ContractJson`.  
**domain:** `GoalMoneyMath`, `WeeklyGoalProgress`, `WeekYieldSnapshot`, `EquipmentType`, `AuthProvider`, `PlatformTime`.

---

## 2. Пользовательские функции (features)

С точки зрения водителя приложение умеет:

1. Вести **локальный журнал Amazon Relay лоудов** (parse paste / manual).
2. **Редактировать** лоуд, finish date, **disputes / penalties**.
3. Учитывать **paychecks** и **diesel**.
4. Ставить **недельную цель gross** и видеть pace (ahead / on track / behind / met).
5. Смотреть **аналитику** (RPM, miles, revenue).
6. Смотреть **карту / heatmap** по штатам (crowd RPM — локальная агрегация).
7. Принимать лоуды/чеки из **Telegram** (device long-poll FGS **или** server webhook inbox).
8. **Фото** (geotag) и **скан документов** (ML Kit + OCR), галереи, привязка к лоуду.
9. **Maintenance** задачи и архив чеков.
10. **Профиль** водителя (edit, avatar crop), onboarding, soft email verify, Telegram link prompt.
11. **Financial advisor** чат по данным журнала.
12. **Voice assistant** в приложении + **Google Assistant / Gemini App Actions**.
13. **Виджеты** домашнего экрана (2×2 / 4×2) с deep links.
14. **Backup** (файл / Google Drive), **CSV export**, privacy settings.
15. Опциональный **cloud sync** (Ktor snapshots, device slots, FCM wake) при `SYNC_BACKEND_URL`.
16. Опциональный **cloud media** при `CLOUD_MEDIA_ENABLED=true`.
17. UI **RU + EN**, Material 3 Soft / Forest.

**Недоступно из текущей навигации (код есть):** Tax Tracker.  
**Заявлены shortcuts, UI удалён/отсутствует:** Community, Friends map.

---

## 3. Технические функции / модули (по слоям)

Полный enumeration всех методов (~тысячи) непрактичен в одном отчёте; ниже — **публичные единицы ответственности по файлам/классам**. Детальный method-level dig — по запросу на пакет в Этапе 2+.

### 3.1 Room (`data/local`)

| Класс | Роль |
|-------|------|
| `AppDatabase` | Единственная `@Database`, **v34**, per-user file `truckerload_<userId>` |
| `LoadDao` … `DriverProfessionalDao` (**15** DAO) | CRUD / observe / aggregates |
| Entities (**20** entity files / 17 tables) | loads, stops, penalties, paychecks, diesel, telegram_inbox, photos, scans, load_history, driver_profile, sync_outbox, media_sync_queue, maintenance_*, crowd_rates, user_accounts, driver_professional_profiles |
| Agg DTOs | WeekYieldAgg, LoadStatsAgg, analytics/*Agg — не таблицы |

### 3.2 Repositories

| Класс | Роль |
|-------|------|
| `LoadRepository` | CRUD/observe loads, week yield, paging, hydrate, outbox |
| `WeekRepository` | Period summary = loads + paycheck + diesel |
| `DieselRepository` / `PaycheckRepository` | Журналы + backup/outbox |
| `PhotoRepository` / `ScanRepository` | Медиа + media sync enqueue |
| `MaintenanceRepository` | Tasks + archive |
| `AnalyticsRepository` | Aggregates |
| `AiRepository` | Facade parse + local advisor |
| `LoadImportRepositoryImpl` | Import writes |
| `AuthRepository` / `Impl` | Google/email/anonymous |
| `ProfileRepository` / `Impl` | Own profile |
| `CrowdRpmRepository` | Local crowd RPM |
| `AccountIdentityRepository` / `DriverProfessionalRepository` | Identity / professional |

### 3.3 Domain — ключевые типы

| Область | Типы | Роль |
|---------|------|------|
| import/parser | `RelayMessageParser`, CSV/HTML/Telegram export, `ParserFactory`, `LoadValidator`, `ImportLoadsUseCase`, `ImportTripDedup` | Импорт лоудов |
| parser | `LoadMessageParser`, `FlexibleLoadParser`, `MessageParseService`, DuplicateChecker*, diesel/paycheck parsers | Текст/OCR |
| goal | `LoadYieldCalculator`, `WeeklyGoalCalculator` | Yield / weekly goal |
| filter | `LoadFilter`, `LoadFilterUseCase` | Фильтры/агрегаты |
| ingest | Receipt classifiers / extractors | Telegram/OCR receipts |
| advisor | `DeterministicAdvisorService` | Rule-based insights |
| usecase | `ForecastService`, `FuelAnalyticsService` | Forecast / fuel analytics |
| crowd | CrowdRpmMath/Mapper/ShareGate | Anonymized RPM |
| maintenance | Mileage / progress calculators | TO |
| account | Registration/consent/age gate models | Onboarding |

### 3.4 Sync / workers / services

| Класс | Роль |
|-------|------|
| `CloudSyncWorker`, `OutboundSyncWorker`, `MediaSyncWorker` | Cloud / outbox / media |
| `TelegramSyncWorker`, `ServerTelegramInboxWorker` | Telegram poll paths |
| `SmartNotificationWorker`, `PushTokenRegistrationWorker` | Reminders / FCM |
| `DriveSyncWorker` | Google Drive backup |
| `TelegramBotForegroundService` | Device long-poll FGS |
| `TruckerLoadFirebaseMessagingService` | FCM |
| `LoadAlarmReceiver`, Telegram boot/restart receivers | Alarms / restart |
| `TelegramBotSyncEngine`, PollCoordinator, LoadHandler, … | Core bot pipeline |
| `CloudSyncEngine` (object) + `cloud/CloudSyncEngine` (injectable) | **Два** sync-оркестратора |
| `CloudSyncPolicy` | Merge / push snapshot / orphan delete policy (**P0 fixed**) |

### 3.5 Preferences / remote / DI

- **Prefs:** `AuthStore`, `AuthCredentialsStore`, `SettingsDataStore`, `TelegramTokenStore`, `WeeklyProfitGoalStore`, `SecurePreferences`, consent/email/biometric/push stores, …
- **Remote:** `SupabaseAuthService`, Google sign-in clients, `TelegramApi`, Ktor `HttpClientProvider`, `KtorLoadApi`/`JournalApi`/`MediaPresignApi`
- **DI:** Hilt singleton stores + **hand-rolled `UserComponent`** (Room/repos per login; destroy on logout)

### 3.6 Presentation ViewModels (~22)

Home, Goal, Analytics, Map, LoadDetail, EditLoad, AddLoad/Paycheck/Diesel, Maintenance, VoiceAssistant, VoiceCommand, Settings, Privacy, Scanner, Camera, PhotoGallery, Profile, Auth, Registration, TaxTracker (**не в nav**), Chat (advisor).

### 3.7 Widgets / voice

- AppWidget + Glance (square/wide), `WidgetUpdateWorker` (30 min), deep links → Routes.
- `AppVoiceActions`, `VoiceCommandBus`, shortcuts.xml App Actions.

---

## 4. Стек и схема данных

### 4.1 Стек (версии из `gradle/libs.versions.toml` + app)

| Слой | Технология | Версия / заметка |
|------|------------|------------------|
| Language | Kotlin | **2.2.10** |
| Android | AGP | **9.3.1** |
| UI | Jetpack Compose + Material3 | BOM **2026.06.01** |
| Navigation | navigation-compose | **2.9.8** |
| Architecture | ViewModel / Lifecycle | **2.9.4** |
| DI | Hilt + KSP | Hilt **2.60.1**, KSP **2.3.6** |
| Local DB | Room | **2.7.0**, schema v**34**, per-user DB file |
| Async work | WorkManager | **2.10.5** |
| Prefs | DataStore | **1.1.7** |
| Paging | Paging 3 | **3.3.6** |
| Networking | Ktor client | **3.5.1** |
| Serialization | kotlinx.serialization | **1.11.0** |
| Backend | Ktor server + PostgreSQL + Flyway | JDK 21 |
| Auth | Google / email local + optional Supabase JWT | |
| OCR / scan | ML Kit Document Scanner + Tesseract rus+eng | |
| Maps | Google Maps SDK | |
| Push | FCM (optional `google-services.json`) | |
| State | Compose state + Flows; **нет** Redux/MVI framework | CompositionLocal + Hilt/UserComponent |
| App version | `versionName` **1.5.6**, `versionCode` **11** | minSdk 24, compile/targetSdk **35** |

**Для Этапа 8:** фронтенд = **native Android Jetpack Compose** (не web). Метрики Core Web Vitals / bundle analyzer браузера **не применимы**; аналоги — APK size / R8, Compose recomposition, WorkManager, main-thread I/O.

### 4.2 Схема Room (таблицы)

| Table | PK | Суть |
|-------|-----|------|
| `loads` | id | tripId unique, rate/miles, week/year, PU/DEL, dispute, equipment |
| `stops` | id | FK loadId, address/schedule |
| `penalties` | id | FK loadId, amount |
| `paychecks` / `diesel` | id | week/year, money, OCR |
| `telegram_inbox` | updateId | bot inbox |
| `photos` / `scans` | id | paths, optional loadId, cloud fields |
| `load_history` | id | field audit |
| `driver_profile` | id | social/profile |
| `sync_outbox` / `media_sync_queue` | id | outbound sync |
| `maintenance_tasks` / `maintenance_archive` | id | TO |
| `crowd_rates` | id | anonymized RPM samples |
| `user_accounts` | id | identity/consents |
| `driver_professional_profiles` | userId | encrypted CDL, role |

Backend PG: `app_users`, `account_snapshots`, `sync_cursors`, telegram link/inbox, `media_objects`, `device_push_tokens`, `account_devices` (+ stub journal projection tables in V1).

### 4.3 Конфиг / env (ключевые флаги)

**BuildConfig / local.properties:** `LOCAL_ONLY_MODE`, `SYNC_BACKEND_URL`, `CLOUD_MEDIA_ENABLED`, `TELEGRAM_SYNC_MODE`, Supabase/Google/Maps/Telegram/Cerebras keys, TURN (optional WebRTC), `FIREBASE_CONFIGURED`.

**Backend `.env`:** Postgres, S3/MinIO, Supabase JWT, Telegram webhook secret, metrics bearer, storage limits.

---

## 5. Наблюдения для следующих этапов (без действий)

Не фиксы — сигналы для Этапов 2–8:

1. Два `CloudSyncEngine` (legacy object + injectable) — дублирование / мёртвый путь.
2. Tax Tracker и community/friends shortcuts без навигации.
3. Финансовый advisor: docs «deterministic» vs `ChatViewModel` AI stream — проверить в Этапе 2.
4. P0 из audit v1 **закрыты на main**; в Этапе 2 перепроверить, что фиксы полные и нет регрессий.
5. Lint: по AGENTS.md `:app:lintDebug` падает с сотнями pre-existing issues.
6. **Этап 8 (новый):** Compose recomposition, APK weight (ML Kit/Maps/Tesseract), Home main-thread hydrate, animations на software GPU.

---

## Статус этапа

**Этап 1 (Audit v2) завершён.**  
Ожидается подтверждение перед **Этапом 2** (логика и корректность) или перед любыми правками кода.  
После этапов 2–7 — **Этап 8** (облегчение и модернизация UI под Compose).
