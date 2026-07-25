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

## Канон в коде (v1.1)

| Слой | Файл / API |
|------|------------|
| Цвета / радиусы / elevation | `SoftUiTheme.kt` |
| Material 3 + system bars | `Theme.kt` → `TruckerLoadTheme` |
| Типографика | **DM Sans** (`Type.kt` / `FontFamilies.kt`) |
| Карточки / bento | `BentoGlassCard`, `SoftCard` |
| Кнопки | `TlButton`, `TlOutlinedButton`, `TlChipButton` |
| Заголовки | `ForestScreenTitle`, `ForestSectionTitle` |
| Поля ввода | `AppTextFieldDefaults.outlined()` |
| Виджет | `res/values{,-night}/widget_colors.xml` (Forest, не purple) |

Legacy-имена `DarkGlass*`, `NeoGlass*`, `Gold*`, `PurpleStart` — deprecated aliases на Forest.

## Навигация (phone)

Logbook · Weekly Goal · Community · Profile

## Связь с кодом

После правок в canvas перенесите значения в:

- `app/.../presentation/theme/SoftUiTheme.kt` — цвета и радиусы
- `app/.../res/values/widget_colors.xml` (+ night) — виджет
- строки UI — `res/values/strings.xml` / Compose screens
