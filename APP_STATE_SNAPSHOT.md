# Снимок состояния приложения TruckerLoad

**Дата:** 18 февраля 2026 (обновлено с TruckerLoad_Analytics_Prompts)

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
- **ForecastCard** — прогноз недели на основе последних 8 недель (ABOVE/ON_TRACK/BELOW)
- **FuelAnalyticsCard** — MPG, $/гал, потрачено, на 100 миль

### 5. Статистика
- Календарь с неделями
- Показатели за выбранную неделю: зарплата, мили, рейсы, дизель, гросс, чистая прибыль
- **ComparisonIndicator** — сравнение с предыдущей неделей (↑/↓, %)
- **RouteStats** — анализ маршрутов (pointA → pointB), сортировка по $/mi, всего $, рейсов
- **ActivityHeatmapCard** — тепловая карта активности по дням года
- Кнопки: Экспорт (CSV), Финансовый советник, Налоги

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
├── presentation/components/ComparisonIndicator.kt
├── presentation/components/ForecastCard.kt
├── presentation/components/RouteStatsCard.kt
├── presentation/components/ActivityHeatmapCard.kt
├── domain/usecase/ForecastService.kt
├── domain/model/RouteStats.kt
├── domain/usecase/FuelAnalyticsService.kt
├── presentation/screens/tax/TaxTrackerScreen.kt
├── presentation/screens/tax/TaxTrackerViewModel.kt
├── presentation/screens/advisor/FinancialAdvisorScreen.kt
├── presentation/components/ExportBottomSheet.kt
├── presentation/components/FuelAnalyticsCard.kt
├── utils/ExportService.kt
├── sync/SmartNotificationWorker.kt
└── utils/WeekUtils.kt              # getWeeksInMonth, getWeekLabelShort, parseDateFromQuery
```

---

### 8. Налоги (TaxTrackerScreen)
- Доход, вычеты (дизель, per diem), налогооблагаемый доход
- SE Tax, Federal Tax, итого к уплате
- Навигация из StatsScreen (иконка AccountBalance)

### 9. Финансовый советник (FinancialAdvisorScreen)
- Чат с Gemini с контекстом приложения
- Подсказки: «Как улучшить доход?», «Какие маршруты выгодные?» и др.
- Навигация из StatsScreen (иконка Psychology)

### 10. Экспорт
- **ExportService** — экспорт в CSV по году
- **ExportBottomSheet** — выбор года, кнопка экспорта
- Кнопка Share в StatsScreen

### 11. Умные уведомления (SmartNotificationWorker)
- WorkManager, раз в 24 часа
- Проверка: нет зарплаты за прошлую неделю, нет дизеля за прошлую неделю

---

## Sync-server (webhook)

- `POST /api/webhook/sync` — принимает JSON с loads
- CDC: фильтрация по Trip ID, INSERT OR IGNORE
- Ответы: success / duplicate / empty
- SQLite: sync_loads.db

---

## Конфигурация

- `local.properties`: TELEGRAM_BOT_TOKEN, GEMINI_API_KEY, CEREBRAS_API_KEY (optional)
- `local.properties.example` — шаблон для настройки
- **Чат:** Cerebras (llama3.1-8b) первым, при 429 rate limit — fallback на Gemini. История сохраняется.
- AppDatabase version: 5
- LoadEntity: indices на tripId (unique), date

---

## Восстановление

Чтобы вернуть приложение к этому состоянию:
1. Используй `git checkout` на коммит с тегом `app-snapshot-2026-02-18` (если создан)
2. Или опиши в сообщении: «Верни приложение к состоянию из APP_STATE_SNAPSHOT.md» — и я восстановлю функциональность по этому документу
