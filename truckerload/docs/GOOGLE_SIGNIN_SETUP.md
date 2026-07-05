# Настройка входа через Google

Для работы кнопки «Войти через Google» нужно создать OAuth-клиент в Google Cloud Console.

## Шаги

1. Откройте [Google Cloud Console](https://console.cloud.google.com/)
2. Выберите или создайте проект
3. **APIs & Services** → **Credentials** → **Create Credentials** → **OAuth client ID**
4. Application type: **Android**
5. Name: `TruckerLoad` (или любое)
6. Package name: `com.truckerload`
7. SHA-1: получите из keystore:

   **Debug (для разработки):**
   ```bash
   keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android
   ```

   **Release:** подставьте путь к вашему release keystore.

8. Нажмите **Create**

После создания клиента пересоберите приложение — Google Sign-In будет работать.

## Данные, которые получает приложение

- **Email** — адрес почты Google
- **Имя (givenName)** — имя из профиля
- **Фамилия (familyName)** — фамилия
- **Фото (photoUrl)** — ссылка на аватар

> **Важно:** Дата рождения **не** передаётся через стандартный Google Sign-In API. Для её получения нужен People API с доп. разрешениями. Сейчас приложение получает только имя, фамилию и email.
