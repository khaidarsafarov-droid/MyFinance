# Настройка входа через Google

SHA-1 **нельзя задать в коде**. Это отпечаток ключа, которым подписан APK.
Google сравнивает: пакет `com.truckerload` + SHA-1 **установленного** файла.
Несовпадение → `DEVELOPER_ERROR` / status **10**.

## Одна правда: friends/release keystore

Канонический ключ проекта — `signing/truckerload-friends.keystore`
(см. `keystore.properties`). Его SHA-1:

```
66:46:40:1E:70:B7:3A:9C:28:D6:7E:4B:68:19:76:AD:46:C6:27:2C
```

Именно его держите в проекте **Track Load** как Android OAuth client.

| Что | SHA-1 | Нужен в Track Load? |
|-----|-------|---------------------|
| Friends / release APK | `66:46:40:1E:…:27:2C` | **Да — основной** |
| Debug с чужого компьютера (часто `~/.android/debug.keystore`) | например `F3:85:03:A5:…:BB:05` | Нет, если debug подписываете тем же friends-ключом |
| Облачный/новый debug.keystore | другой каждый раз | Нет |

`F3:85:03:…` — это **не** ключ Track Load, а отпечаток другого keystore
(обычно debug Android Studio). Его не нужно делать «главным».

## Как сделать SHA одинаковым везде (sideload)

1. На компьютере, где лежит `signing/truckerload-friends.keystore`, выполните:

   ```bash
   cd truckerload
   keytool -list -v -keystore signing/truckerload-friends.keystore -alias truckerload
   ```

   В выводе строка `SHA1:` должна быть `66:46:40:1E:70:B7:3A:9C:28:D6:7E:4B:68:19:76:AD:46:C6:27:2C`.

2. В Track Load оставьте **один** Android-клиент:
   - package: `com.truckerload`
   - SHA-1: `66:46:40:1E:70:B7:3A:9C:28:D6:7E:4B:68:19:76:AD:46:C6:27:2C`  
   Клиент с `F3:85:03:…` можно удалить.

3. Чтобы **Run из Android Studio** давал тот же SHA-1, подпишите debug тем же ключом.
   Файл `debug-keystore.properties` (не в git):

   ```
   storeFile=signing/truckerload-friends.keystore
   storePassword=<пароль из keystore.properties>
   keyAlias=truckerload
   keyPassword=<тот же пароль>
   ```

   Alias должен быть `truckerload`, не `androiddebugkey`.

4. Пересоберите, удалите старое приложение с телефона, поставьте новый APK.
   Проверка файла:

   ```bash
   keytool -printcert -jarfile dist/TruckerLoad-1.5.6-friends.apk
   # или
   sh ./gradlew :app:signingReport
   ```

   SHA-1 APK = SHA-1 в Track Load.

Play Store — отдельный случай: в Play Console возьмите
**App signing key certificate → SHA-1** и добавьте **второй** Android-клиент.
Upload-ключ и ключ Google Play часто разные.

## Console

- Клиенты: https://console.cloud.google.com/apis/credentials?project=842861516910
- Новый Android-клиент: https://console.cloud.google.com/apis/credentials/oauthclient?project=842861516910

Web client ID в `local.properties`:

```
GOOGLE_WEB_CLIENT_ID=842861516910-gkhu4dh9tu5rc8re40rpe4583hvs4uhv.apps.googleusercontent.com
```

Включите **Google Drive API**. Если consent **Testing** — добавьте Gmail в Test users.

## Данные от Google

Email, имя, фамилия, фото. Дата рождения не приходит.
