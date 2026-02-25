# Снимок состояния приложения TruckerLoad

**Дата:** 18 февраля 2026

Этот документ фиксирует полную функциональность приложения на текущий момент. При запросе «верни приложение к этой функциональности» — восстанови всё, что описано ниже.

---

## Основные модули

| Модуль | Назначение |
|--------|------------|
| `truckerload/` | Android-приложение (Jetpack Compose, Room, WorkManager) |
| `app/` | Обёртка приложения |
| `sync-server/` | Ktor backend с webhook `/api/webhook/sync` |

---

## Функциональность TruckerLoad

### 1. Главный экран (Лоуды)
- **Архив по годам** — иерархия Год > Месяц > День с итогами по году
- **Календарь** — выбор даты → автоматический показ недели
- **Фильтры:** вчера, эта неделя, прошлая неделя, этот месяц
- **Поиск** по Trip ID, городу, дате (25.10.2023 или 2023-10-25)
- **Умные метки времени:** creation_date (parsedAt), load_date (date), last_modified (updatedAt)
- **Редактирование load_date** в EditLoadScreen

### 2. Синхронизация с Telegram
- Polling getUpdates (не webhook)
- Мгновенный ответ «⏳ Обрабатываю...» при получении сообщения
- CDC: проверка Trip ID, batch insert, без дубликатов и пустых записей
- Парсинг через Gemini: loads, paycheck, diesel
- Ответы: «✅ Добавлено X грузов», «🆗 Все данные уже заполнены», «🔄 Новых данных не обнаружено»

### 3. База данных (Room)
- **LoadEntity:** уникальный индекс на trip_id, индекс на date
- **LoadDao:** getExistingTripIds, getLoadsByDate, getLoadsByDateRange
- **LoadRepository.syncLoadsCdc()** — CDC-синхронизация

### 4. Финансы
- **WeekCalendarPicker** — выбор месяца/года, недели с мини-сводкой (грузов • гросс)
- Клик по неделе → пересчёт зарплаты, дизеля, чистой прибыли
- Режимы: Неделя, Месяц, Год

### 5. Статистика
- Календарь с неделями
- Показатели за выбранную неделю: зарплата, мили, рейсы, дизель, гросс, чистая прибыль

### 6. Тема
- TruckLightColors, TruckDarkColors
- Следование системной теме

### 7. Иконка приложения
- Кастомная иконка (грузовик, TRUCKLOADS, 123+=)
- mipmap-* / ic_launcher.png, ic_launcher_round.png

---

## Ключевые файлы

```
truckerload/
├── sync/TelegramSyncWorker.kt      # Синхронизация Telegram
├── data/remote/GeminiService.kt    # Парсинг через Gemini
├── data/repository/LoadRepository.kt  # syncLoadsCdc
├── data/local/dao/LoadDao.kt       # getExistingTripIds, getLoadsByDateRange
├── data/local/entities/LoadEntity.kt  # Index на tripId, date
├── presentation/screens/home/HomeScreen.kt    # Календарь, фильтры, архив
├── presentation/screens/home/HomeViewModel.kt  # selectDateFromCalendar, LoadFilter.CALENDAR_WEEK
├── presentation/screens/finance/FinanceScreen.kt  # WeekCalendarPicker
├── presentation/screens/stats/StatsScreen.kt  # Календарь + статистика по неделе
├── presentation/components/WeekCalendarPicker.kt
└── utils/WeekUtils.kt              # getWeeksInMonth, getWeekLabelShort, parseDateFromQuery
```

---

## Sync-server (webhook)

- `POST /api/webhook/sync` — принимает JSON с loads
- CDC: фильтрация по Trip ID, INSERT OR IGNORE
- Ответы: success / duplicate / empty
- SQLite: sync_loads.db

---

## Конфигурация

- `local.properties`: TELEGRAM_BOT_TOKEN, GEMINI_API_KEY
- AppDatabase version: 5
- LoadEntity: indices на tripId (unique), date

---

## Восстановление

Чтобы вернуть приложение к этому состоянию:
1. Используй `git checkout` на коммит с тегом `app-snapshot-2026-02-18` (если создан)
2. Или опиши в сообщении: «Верни приложение к состоянию из APP_STATE_SNAPSHOT.md» — и я восстановлю функциональность по этому документу
