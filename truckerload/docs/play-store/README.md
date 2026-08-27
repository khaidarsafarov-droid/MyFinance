# Материалы для карточки Google Play

Готовые ассеты для загрузки в Play Console → **Основная информация о Store**.

## Что здесь лежит

| Файл | Размер | Куда в Play Console |
|------|--------|---------------------|
| `icon-512.png` | 512×512 PNG | Значок приложения |
| `feature-graphic-1024x500.png` | 1024×500 PNG | Многофункциональное изображение |
| `screenshots/phone-framed/*.png` | 8 × 1080×1920 PNG | Скриншоты телефона (**рекомендуется загружать эти**) |
| `screenshots/phone/*.png` | 8 × 1080×1920 PNG | Исходные снимки экрана без подписей |

`phone-framed` — те же снимки на фирменном фоне с русскими подписями: в поиске Play
подпись видна сразу и продаёт функцию лучше, чем «голый» экран. `phone` оставлены
как исходники — из них пересобираются подписанные версии, и их же можно залить,
если нужны скриншоты без маркетингового оформления.

## Соответствие требованиям Google Play

- Формат: PNG без прозрачности (значок и feature graphic — обязательно без альфа-канала).
- Скриншоты телефона: 1080×1920, соотношение **9:16**, каждая сторона в диапазоне
  320–3840 px, до 8 МБ. Google требует минимум 2 скриншота, максимум 8 — здесь ровно 8.
- Значок: ровно 512×512, скругление Play добавляет сам. Кадр повторяет то, что
  видно на лаунчере (adaptive-icon с маской 72/108), поэтому иконка в сторе и на
  телефоне выглядят одинаково.
- Feature graphic: ровно 1024×500, важное — в левой части, справа только логотип
  (Play обрезает края на некоторых раскладках).

## Подписи к скриншотам

| # | Экран | Заголовок |
|---|-------|-----------|
| 01 | Главная / журнал недели | Вся неделя на одном экране |
| 02 | Добавить груз | Вставил сообщение — груз разобран |
| 03 | Цель недели | Цель недели и ваш темп |
| 04 | Мои цифры | Мои цифры за 12 недель |
| 05 | Деньги по неделям | График заработка по неделям |
| 06 | Финансы периода | Зарплата и дизель рядом |
| 07 | Журнал рейсов | Журнал рейсов всегда под рукой |
| 08 | Профиль | Профиль водителя со статистикой |

Тексты подписей лежат в словаре `CAPTIONS` в `scripts/build_play_store_assets.py` —
правьте там и пересобирайте.

## Как пересобрать подписанные версии

```bash
pip install Pillow
python3 scripts/build_play_store_assets.py
```

Скрипт читает `docs/play-store/screenshots/phone/*.png`, кладёт результат в
`screenshots/phone-framed/`, а также заново собирает значок и feature graphic из
`app/src/main/res/drawable-nodpi/`. Нужны шрифты Inter (`Inter-Bold/Medium/Regular.ttf`);
если их нет в системе, укажите каталог через `--font-dir`.

## Как сняты исходные скриншоты

Снимались на эмуляторе `android-34;default;x86_64` (AOSP-образ — он легче и не
показывает системные диалоги Google Play Services), AVD 1080×1920 @ 420 dpi,
`adb exec-out screencap -p`.

Подготовка устройства перед съёмкой:

```bash
# анимации выключены, системные ANR-диалоги скрыты, жестовая навигация вместо трёх кнопок
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put global hide_error_dialogs 1
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural

# «чистый» статус-бар: фиксированное время, полная батарея, без иконок уведомлений
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
```

Данные на скриншотах — демонстрационные: 12 отчётных недель (48 грузов, зарплаты,
дизель и разные расходы) залиты напрямую в Room-базу офлайн-аккаунта
`/data/data/com.truckerload/databases/truckerload_local_dev`. Генератор SQL —
`scripts/seed_play_store_demo_data.py`:

```bash
python3 scripts/seed_play_store_demo_data.py > demo.sql
adb shell am force-stop com.truckerload
adb push demo.sql /data/local/tmp/demo.sql
adb shell "cat /data/local/tmp/demo.sql | run-as com.truckerload sqlite3 databases/truckerload_local_dev"
```

Даты в генераторе привязаны к неделе съёмки (23–29 августа 2026). Если скриншоты
снимаются заново, поменяйте `WEEK_START` / `LAST_WEEK_START` / `TODAY` в скрипте,
иначе фильтр «Эта неделя» окажется пустым.
