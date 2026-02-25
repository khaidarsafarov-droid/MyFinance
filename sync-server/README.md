# Sync Server — Webhook API

Event-Driven синхронизация с CDC (Change Data Capture). Принимает POST-запросы от бота или внешней системы.

## Запуск

```bash
./gradlew :sync-server:run
```

Сервер слушает порт 8080.

## Эндпоинт

### POST /api/webhook/sync

Принимает JSON с массивом грузов. CDC: сравнивает Trip ID с базой, добавляет только новые записи.

**Тело запроса:**

```json
{
  "loads": [
    {
      "trip_id": "T-12345",
      "date": "2025-02-18",
      "total_rate": 2500.00,
      "total_miles": 1200,
      "point_a": "Chicago, IL",
      "point_b": "Dallas, TX"
    }
  ],
  "message_date_seconds": 1739817600
}
```

Поддерживаются snake_case (`trip_id`) и camelCase (`tripId`).

**Ответы:**

| Сценарий | status | message |
|----------|--------|---------|
| Успех | `success` | «Добавлено [N] новых грузов. Последний: [текст]» |
| Дубликат | `duplicate` | «Все данные уже заполнены. Отправьте новые данные.» |
| Пусто | `empty` | «Новых данных не обнаружено» |

**Пример ответа (успех):**

```json
{
  "status": "success",
  "message": "Добавлено 2 новых грузов. Последний: T-12345 — Chicago, IL → Dallas, TX, $2,500.00",
  "added_count": 2,
  "last_added_text": "T-12345 — Chicago, IL → Dallas, TX, $2,500.00"
}
```

## CDC-логика

1. Фильтрация: пропуск пустых Trip ID, T-UNKNOWN, записей без маршрута или с total_rate ≤ 0.
2. Один запрос к БД: получение существующих Trip ID.
3. Фильтрация в памяти: только новые Trip ID.
4. Batch insert с `INSERT OR IGNORE` (уникальный индекс на trip_id).
5. Логируются только успешные вставки.

## База данных

SQLite (`sync_loads.db` в текущей директории). Уникальный индекс на `trip_id` предотвращает дубликаты.
