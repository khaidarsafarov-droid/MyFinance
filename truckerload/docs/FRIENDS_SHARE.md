# TruckerLoad для друзей (без своего сервера)

Данные живут **на телефоне** (Room). Свой backend / DigitalOcean **не** нужен.
Вход: **Google** или **email + пароль** (аккаунт на устройстве).
По желанию — бэкап в файл или Google Drive.
Секреты бота / API **не** обязаны быть в APK.

## Что отдать друзьям

1. Файл APK: `dist/TruckerLoad-1.5.5-friends.apk` (после сборки)
2. Короткую инструкцию ниже (можно переслать текстом в Telegram)

## Установка на Android

1. Скачайте APK на телефон.
2. Откройте файл → **Установить**.
3. Если система блокирует: **Настройки → Безопасность → Установка неизвестных приложений** → разрешите для Chrome / Telegram / Файлы.
4. Откройте **Truck Log / TruckerLoad** и войдите:
   - **Войти через Google**, или
   - **Создать аккаунт** (email + пароль) / войти с уже созданным email.

Обновления: ставьте новый APK поверх старого (тот же ключ подписи). Данные Room сохраняются
для того же аккаунта на устройстве.

### Важно про Google

1. В Google Cloud Console добавьте **Android** OAuth client:
   - package: `com.truckerload`
   - SHA-1 friends keystore (пример): `1C:C5:60:BB:81:EA:B7:9E:9D:38:AB:E7:99:AC:2D:A6:82:94:BA:DB`
2. Если OAuth consent в режиме **Testing** — добавьте email друзей в **Test users**,
   иначе Google покажет `access_denied` / 403.
3. В сборке должен быть `GOOGLE_WEB_CLIENT_ID` (Web client) — скрипт подставляет
   значение из `local.properties.example`, если поле пустое.

Без Supabase аккаунт Google/email хранится **только на этом телефоне**
(смена телефона = новый вход + восстановление из бэкапа).

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
Те же Test users / SHA-1 нужны и для Drive OAuth.

## Сборка APK у себя

```bash
cd truckerload
cp keystore.properties.example keystore.properties
# один раз создайте keystore (см. комментарии в example) и сохраните пароли
chmod +x scripts/build-friends-apk.sh
./scripts/build-friends-apk.sh
```

APK path after build: `dist/TruckerLoad-1.5.5-friends.apk`.

Скрипт выставляет для дружеской сборки:

```
LOCAL_ONLY_MODE=false
SYNC_BACKEND_URL=
CLOUD_MEDIA_ENABLED=false
GOOGLE_WEB_CLIENT_ID=<из local.properties или default Web client>
```

`LOCAL_ONLY_MODE=true` **нельзя** для друзей с логином — тогда приложение само
входит как `local_dev` и экран Google/email не показывается.

Не вписывайте свой `TELEGRAM_BOT_TOKEN` / API-ключи в APK для друзей, если не хотите
отдавать им доступ к вашим сервисам. Друг может позже добавить **свой** бот в настройках
на устройстве (токен хранится локально).

Опционально (общий аккаунт на нескольких устройствах): заполните `SUPABASE_URL` +
`SUPABASE_ANON_KEY` — тогда email/Google идут через Supabase Auth. Без них вход
работает локально на телефоне.

## Безопасность

| Что | Где |
| --- | --- |
| Журнал грузов | Room на устройстве (отдельная БД на аккаунт) |
| Пароль email | Хеш на устройстве (`AuthCredentialsStore`) |
| Бэкап | Файл на телефоне или личный Google Drive |
| Keystore / пароли подписи | `signing/` + `keystore.properties` (gitignore) |
| Сервер DigitalOcean | Не нужен для этой раздачи |

## Версия

Friends APK: **1.5.5** (`versionCode` 10) — phone ABIs only (`arm64-v8a` + `armeabi-v7a`),
вход Google / email+пароль включён.
