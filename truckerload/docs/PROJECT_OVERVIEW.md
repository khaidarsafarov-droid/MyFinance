# TruckerLoad — обзор проекта

Android-приложение для учёта лоудов (Amazon Relay), зарплат, дизеля и недельных целей.

## Структура

```
truckerload/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── gradlew / gradlew.bat
├── local.properties          # секреты (не в git)
├── local.properties.example
├── proguard-rules.pro
├── src/
└── docs/
```

## Запуск

1. Откройте папку **`truckerload`** в Android Studio.
2. Скопируйте `local.properties.example` → `local.properties`, заполните ключи.
3. **Run** (модуль по умолчанию — корень проекта).

## Стек

Kotlin, Jetpack Compose, Room v8, WorkManager, Telegram Bot API (long-poll на устройстве).

## Ключевые пакеты

- `sync/` — Telegram-бот, восстановление
- `data/` — Room, репозитории
- `domain/` — парсеры, goal-математика
- `presentation/` — UI
- `widget/` — виджет рабочего стола
