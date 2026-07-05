# Android Studio — открытие проекта

## 1. Открыть проект

1. **File → Open**
2. Выберите папку: `C:\Users\khaid\MyFinance`
3. Нажмите **OK**
4. Дождитесь завершения Gradle Sync

## 2. Синхронизация изменений

Если проект уже открыт и вы не видите последние правки:

- **File → Sync Project with Gradle Files** (или иконка слона с синей стрелкой)
- Либо **File → Invalidate Caches → Invalidate and Restart**

## 3. Сборка и запуск

- **Build → Make Project** (Ctrl+F9)
- Выберите модуль **app** в конфигурации запуска
- **Run** (Shift+F10) — запуск на эмуляторе или устройстве

## 4. Модули проекта

| Модуль      | Описание                                      |
|-------------|-----------------------------------------------|
| **app**     | MyFinance — Loads, Add Trip, календарь        |
| **truckerload** | TruckerLoad — отдельное приложение        |
| **sync-server** | Ktor backend                             |

## 5. Изменения в app (календарь)

- `app/calendar/CalendarHelper.kt` — работа с календарём
- `app/data/Trip.kt` — поле `calendarEventId`
- `app/data/AppRepository.kt` — синхронизация с календарём
- `app/ui/TripCard.kt` — кнопка «Добавить в календарь»
- `app/ui/EditTripScreen.kt` — кнопка «Добавить в календарь»
- `app/ui/MainNav.kt` — кнопка «Sync all to calendar» на экране Loads
