# TruckoRig — обзор проекта

Android-приложение для учёта лоудов (Amazon Relay), зарплат, дизеля и недельных целей.

Журнал, Telegram-бот и резервная копия Google Drive работают на устройстве.
Отдельного сервера и Supabase в проекте нет.

## Структура

```
truckerload/
├── app/                      # Android client
├── shared/contract/          # KMP identifiers shared with iOS

├── shared/domain/            # KMP portable domain
├── docs/
└── gradle/
```

## Запуск

1. Откройте папку **`truckerload`** в Android Studio.
2. Скопируйте `local.properties.example` → `local.properties`, заполните ключи.
3. **Run** (модуль по умолчанию — корень проекта).

## Стек

Kotlin, Jetpack Compose, Room, WorkManager, Telegram Bot API (long-poll на устройстве).
Общий код для будущего iOS — Kotlin Multiplatform (`shared/contract`, `shared/domain`).

## Ключевые пакеты

- `sync/` — Telegram-бот, восстановление
- `data/` — Room, репозитории, Google Drive
- `domain/` — парсеры, goal-математика
- `presentation/` — UI
- `widget/` — виджет рабочего стола
