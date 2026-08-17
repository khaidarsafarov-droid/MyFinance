# Настройка входа через Google

Кнопка «Войти через Google» и Google Drive работают только если в **том же**
проекте Google Cloud есть:

1. **Android** OAuth client — package + SHA-1 подписи APK
2. **Web** OAuth client — его ID кладётся в `local.properties` как `GOOGLE_WEB_CLIENT_ID`

Приложение само не регистрирует SHA-1. Несовпадение SHA-1 даёт
`DEVELOPER_ERROR` / status **10**.

## Значения для Console

| Поле | Значение |
|------|----------|
| Package name | `com.truckerload` |
| SHA-1 (этот билд / текущий keystore) | `F3:85:03:A5:AB:66:25:1F:36:2E:81:65:20:A9:86:2F:0D:22:BB:05` |
| SHA-1 (friends/release keystore) | `66:46:40:1E:70:B7:3A:9C:28:D6:7E:4B:68:19:76:AD:46:C6:27:2C` |
| Web client ID (`GOOGLE_WEB_CLIENT_ID`) | `842861516910-gkhu4dh9tu5rc8re40rpe4583hvs4uhv.apps.googleusercontent.com` |

Нужен **отдельный Android-клиент на каждый SHA-1** (один клиент = один отпечаток).
Оба отпечатка должны быть в проекте, иначе debug-сборка и friends-APK не могут
войти одним и тем же OAuth-проектом.

Если приложение ставится из Play Store — добавьте ещё SHA-1
**App signing key certificate** из Play Console (он часто другой, не upload-ключ).

## Шаги

1. Откройте [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials)
2. Выберите проект OAuth (тот, где уже есть Web client `842861516910-…`)
3. **Create Credentials** → **OAuth client ID** → Application type: **Android**
4. Package name: `com.truckerload`
5. SHA-1: `F3:85:03:A5:AB:66:25:1F:36:2E:81:65:20:A9:86:2F:0D:22:BB:05`
6. **Create**. Повторите шаг 3–5 для friends SHA-1, если такого клиента ещё нет.
7. Включите **Google Drive API** в том же проекте (для «Сохранить в Drive»).
8. OAuth consent screen: если статус **Testing**, добавьте Gmail testers в **Test users**,
   иначе будет `access_denied` / 403.
9. В `truckerload/local.properties` (не в git):

   ```
   GOOGLE_WEB_CLIENT_ID=842861516910-gkhu4dh9tu5rc8re40rpe4583hvs4uhv.apps.googleusercontent.com
   ```

10. Пересоберите и переустановите APK. Google кэширует клиент ~минуты; если ошибка 10
    остаётся — подождите и переустановите приложение.

Проверить SHA-1 установленного APK:

```bash
keytool -printcert -jarfile app-debug.apk
# или в Android Studio: Gradle → :app → signingReport
```

Отпечаток в Console должен **байт-в-байт** совпасть с подписью того APK, который
стоит на телефоне.

## Данные, которые получает приложение

- **Email** — адрес почты Google
- **Имя (givenName)** — имя из профиля
- **Фамилия (familyName)** — фамилия
- **Фото (photoUrl)** — ссылка на аватар

> Дата рождения **не** передаётся через стандартный Google Sign-In API.
