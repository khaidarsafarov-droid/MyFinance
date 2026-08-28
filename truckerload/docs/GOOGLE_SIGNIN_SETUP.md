# Настройка Google Drive OAuth (Play Store)

SHA-1 **нельзя задать в коде**. Это отпечаток ключа, которым подписан AAB/APK.
Google сравнивает: пакет `com.truckorig` + SHA-1 **установленного** файла.
Несовпадение → `DEVELOPER_ERROR` / status **10**.

## Один ключ на debug и Play upload

Положите upload-keystore и `keystore.properties` в `truckerload/`.
Если нет отдельного `debug-keystore.properties`, Gradle подписывает **debug тем же
ключом**, что и release — SHA-1 совпадает.

Узнать отпечаток:

```bash
cd truckerload
sh ./scripts/print-signing-sha1.sh
```

Строку `SHA1:` из upload-keystore внесите в Android OAuth client,
package `com.truckorig`. Старый debug-отпечаток вроде `F3:85:03:…` удалите.

Если у вас ещё есть прежний keystore со SHA-1
`66:46:40:1E:70:B7:3A:9C:28:D6:7E:4B:68:19:76:AD:46:C6:27:2C` — используйте
**его**, не создавайте второй ключ.

## Console

- Клиенты: https://console.cloud.google.com/apis/credentials?project=842861516910
- Новый Android-клиент: https://console.cloud.google.com/apis/credentials/oauthclient?project=842861516910

Web client ID в `local.properties`:

```
GOOGLE_WEB_CLIENT_ID=842861516910-gkhu4dh9tu5rc8re40rpe4583hvs4uhv.apps.googleusercontent.com
```

Включите **Google Drive API**. Если consent **Testing** — добавьте Gmail в Test users.

После смены ключа: удалите приложение с телефона и поставьте сборку, подписанную
этим keystore.

Play Store — отдельно: SHA-1 **App signing key certificate** из Play Console
добавляют вторым Android-клиентом.

## Данные от Google

Email для привязки Drive. Дата рождения не приходит.
