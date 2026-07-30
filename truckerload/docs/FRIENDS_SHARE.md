# TruckerLoad для друзей (без сервера)

Данные живут **только на телефоне**. Сервер не нужен.
По желанию — бэкап в файл или Google Drive и восстановление оттуда.
Токены и ключи **не** кладём в git и не обязаны быть в APK.

## Что отдать друзьям

1. Файл APK: `dist/TruckerLoad-1.5.3-friends.apk` (после сборки)
2. Короткую инструкцию ниже (можно переслать текстом в Telegram)

## Установка на Android

1. Скачайте APK на телефон.
2. Откройте файл → **Установить**.
3. Если система блокирует: **Настройки → Безопасность → Установка неизвестных приложений** → разрешите для Chrome / Telegram / Файлы.
4. Откройте **Truck Log / TruckerLoad** — вход не требуется (локальный режим).

Обновления: ставьте новый APK поверх старого (тот же ключ подписи). Данные Room сохраняются.

## Бэкап и восстановление

### Файл на телефоне
**Настройки → Резервная копия**
- **Создать бэкап** — файл в Загрузки / можно поделиться
- **Восстановить из файла** — выбрать сохранённый `Loads_…` / бэкап

### Google Drive (по желанию)
**Настройки → Google Drive**
- Подключить свой Google-аккаунт
- **Сохранить в Drive** / **Восстановить из Drive**

У каждого друга свой телефон и свой Drive — данные не смешиваются.

## Сборка APK у себя

```bash
cd truckerload
cp keystore.properties.example keystore.properties
# один раз создайте keystore (см. комментарии в example) и сохраните пароли
chmod +x scripts/build-friends-apk.sh
./scripts/build-friends-apk.sh
```

APK path after build: `dist/TruckerLoad-1.5.4-friends.apk`.

В `local.properties` для дружеской сборки:

```
LOCAL_ONLY_MODE=true
SYNC_BACKEND_URL=
CLOUD_MEDIA_ENABLED=false
TELEGRAM_SYNC_MODE=device
```

Не вписывайте свой `TELEGRAM_BOT_TOKEN` / API-ключи в APK для друзей, если не хотите
отдавать им доступ к вашим сервисам. Друг может позже добавить **свой** бот в настройках
на устройстве (токен хранится локально).

## Безопасность

| Что | Где |
| --- | --- |
| Журнал грузов | Только Room на устройстве |
| Бэкап | Файл на телефоне или личный Google Drive пользователя |
| Keystore / пароли подписи | `signing/` + `keystore.properties` (gitignore) — храните у себя |
| Сервер DigitalOcean | Не нужен для этой раздачи |

## Версия

Friends APK: **1.5.4** (`versionCode` 9) — phone ABIs only (`arm64-v8a` + `armeabi-v7a`), ~half the size of a universal APK.
