# TruckoRig — обзор проекта

Android-приложение для учёта лоудов (Amazon Relay), зарплат, дизеля и недельных целей.

Полный бриф для другой нейросети (Cloud Code / Claude Code): **[CLOUD_CODE_BRIEF.md](CLOUD_CODE_BRIEF.md)** — один файл, его можно отправить целиком.

## Структура

```
truckerload/
├── app/                      # Android client
├── shared/contract/          # KMP API contracts
├── shared/domain/            # KMP portable domain
├── backend/server/           # Ktor API
├── docs/
└── gradle/
```

## Запуск

1. Откройте папку **`truckerload`** в Android Studio.
2. Скопируйте `local.properties.example` → `local.properties`, заполните ключи.
3. **Run** (модуль по умолчанию — корень проекта).

## Стек

Kotlin, Jetpack Compose, Room v8, WorkManager, Telegram Bot API (long-poll на устройстве).
Общий код для будущего iOS — Kotlin Multiplatform (`shared/contract`, `shared/domain`).

## Ключевые пакеты

- `sync/` — Telegram-бот, восстановление
- `data/` — Room, репозитории
- `domain/` — парсеры, goal-математика
- `presentation/` — UI
- `widget/` — виджет рабочего стола
