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

Проект `253720408597`. Android-клиент TruckoRig:

`253720408597-8pp7kbk23mblnfuj2nk687e20n88ftl7.apps.googleusercontent.com`

Пакет в клиенте: `com.truckorig`. SHA-1 в клиенте — отпечаток **установленного** APK/AAB.
Этот Client ID в код входа не подставляется: Google сам находит Android-клиент по пакету и SHA-1.
`requestIdToken` с этим ID ломает вход (ошибка 10), потому что это не Web client.

- Клиенты: https://console.cloud.google.com/apis/credentials?project=253720408597
- Drive API: https://console.cloud.google.com/apis/library/drive.googleapis.com?project=253720408597

Включите **Google Drive API**. Если consent **Testing** — добавьте Gmail в Test users.

Вход в приложение **не** использует Google Sign-In. Android OAuth client нужен
только для резервной копии Drive в Настройках.

После смены ключа: удалите приложение с телефона и поставьте сборку, подписанную
этим keystore.

Play Store — отдельно: SHA-1 **App signing key certificate** из Play Console
добавляют вторым Android-клиентом.

## Данные от Google

Email для привязки Drive. Дата рождения не приходит.
