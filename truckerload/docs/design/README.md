# Truck Log — Design Canvas

Редактируемый design file приложения **Truck Log** (Mindwell Forest).

## Как открыть

Откройте в браузере:

```text
truckerload/docs/design/truck-log-canvas.html
```

Или через локальный сервер из корня модуля:

```bash
cd truckerload/docs/design
python3 -m http.server 8765
# → http://localhost:8765/truck-log-canvas.html
```

## Что можно редактировать

| Область | Как |
|--------|-----|
| Цвета / радиусы | Панель **Tokens** слева (color pickers + number inputs) |
| Тексты на экранах | Клик по тексту (contenteditable) |
| Активный экран | Клик по телефону / вкладки внизу / список Screens |
| Сохранение токенов | **Export JSON** → файл; **Import JSON** — обратно |
| Сброс | **Reset defaults** |
| Автосохранение | Токены пишутся в `localStorage` браузера |

Файл `tokens.json` — исходный набор токенов (синхронизирован с `SoftUiColors` в `SoftUiTheme.kt`).

## Связь с кодом

После правок в canvas перенесите значения в:

- `app/.../presentation/theme/SoftUiTheme.kt` — цвета и радиусы
- строки UI — `res/values/strings.xml` / Compose screens

## Figma

В этой среде Figma MCP не подключён. Если нужен именно `.fig` файл в Figma:

1. Подключите Figma MCP в Cursor
2. Попросите агента создать файл и перенести экраны из этого canvas
