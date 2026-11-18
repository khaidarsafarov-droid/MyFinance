# Политика конфиденциальности TruckoRig

**Privacy Policy · TruckoRig**  
Android package / iOS bundle ID: `com.truckorig`  
Разработчик / Developer: Khaidar Safarov  
Контакт / Contact: [khaidar.safarov@gmail.com](mailto:khaidar.safarov@gmail.com)  
Дата вступления в силу / Effective date: 18 ноября 2026 / 18 November 2026  
Последнее обновление / Last updated: 18 ноября 2026 / 18 November 2026

Эта политика описывает, как приложение TruckoRig на Android и iOS обрабатывает данные. Она заменяет версию от 3 сентября 2026 года.

---

## Русский

### Кратко

TruckoRig — локальный журнал для водителей. Разработчик **не собирает** персональные данные на своих серверах и **не передаёт** их третьим лицам в рекламных или коммерческих целях. Журнал грузов, зарплат и дизеля хранится на вашем устройстве.

Рекламы в приложении нет. Облачного аккаунта TruckoRig нет. Вход через Google или email для работы журнала не требуется.

Подписка TruckoRig Premium оформляется через Google Play (Android) или App Store (iOS). Платёжные данные обрабатывает магазин, а не разработчик.

### Кто мы

Приложение TruckoRig (`com.truckorig`) публикуется разработчиком Khaidar Safarov.  
Контакт: [khaidar.safarov@gmail.com](mailto:khaidar.safarov@gmail.com), либо обратная связь в Настройках приложения.

### Какие данные обрабатываются на устройстве

| Категория | Примеры | Где хранится |
| --- | --- | --- |
| Профиль | Имя, которое вы можете указать при первом запуске на Android. На iOS вход в аккаунт не нужен. | Только на устройстве |
| Журнал | Грузы, остановки, зарплаты, дизель, цели, напоминания ТО, налоговые заметки | Локальная база (Room на Android, GRDB на iOS), шифрование SQLCipher |
| Медиа | Фото и сканы документов, распознанный текст | Хранилище устройства |
| Геолокация | Метка на фото или карта | На устройстве, только с разрешения |
| Telegram (по желанию) | Токен бота и chat ID, которые вы сами вводите | Настройки устройства (на Android — в Android Keystore, на iOS — в Keychain) |
| Подписка | Локальный флаг, что Premium активен, чтобы доступ сохранялся без сети короткое время | Настройки устройства |
| Виджет (iOS) | Снимок цели и темы для виджета | Общая группа приложений `group.com.truckorig` на этом iPhone |

На Android системная автокопия приложения отключена (`allowBackup="false"`). Разблокировка по биометрии, если вы её включите, работает только на устройстве.

Раздел «Аналитика» в приложении — это ваши графики по грузам и дизелю на телефоне, а не отправка статистики разработчику.

### Чего мы не делаем

- Не ведём облачный аккаунт TruckoRig и не храним ваш журнал на серверах разработчика.
- Не продаём данные и не показываем рекламу.
- Не требуем вход через Google, Apple или email для работы журнала.
- Не отправляем отчёты о сбоях (Firebase Crashlytics), пока в выпускную сборку не добавлен файл `google-services.json`. В обычной сборке из репозитория этого файла нет.
- Не публикуем и не загружаем «народные» ставки (Crowd RPM): карта эффективности строится только по вашим локальным грузам.

### Когда данные могут покинуть устройство — только по вашему действию

Эти функции необязательны. Основной журнал (грузы, дизель, зарплаты) работает без интернета.

1. **Резервная копия Google Drive (Android, в Premium)** — если вы сами подключите Диск в Настройках. Копия уходит в *ваш* Google Drive, не к разработчику. Для входа Drive приложение запрашивает доступ у Google; адрес вашей учётки Google нужен только для этой привязки и не становится входом в TruckoRig.
2. **Резервная копия iCloud Drive (iOS)** — если вы включите синхронизацию. Журнал и, при включённой копии медиа, фото и сканы сохраняются в контейнер `iCloud.com.truckorig` в *вашем* iCloud. Файлы можно увидеть в приложении «Файлы». Разработчик не имеет доступа к вашему iCloud.
3. **Telegram-бот** — если вы укажете свой токен бота. Сообщения идут в API Telegram, который вы настроили. Токен хранится только на устройстве.
4. **Экспорт и отправка** — CSV, PDF, файл бэкапа или письмо с обратной связью, которое вы сами сохраняете или отправляете.
5. **Карта** — при открытии карты Google Maps может загрузить тайлы. Построение маршрута может отправить точки маршрута в сервис Google. Это сервис Google, не сервер TruckoRig.
6. **Оплата подписки** — Google Play или App Store обрабатывают платёж. TruckoRig не получает номер вашей карты и не отправляет токен покупки на сервер разработчика.
7. **Сканер документов (Android)** — распознавание может использовать сервисы Google Play на устройстве.

### Платежи и подписка

TruckoRig бесплатен в установке. Грузы, дизель, зарплаты и deadhead остаются бесплатными.

По желанию можно оформить автопродлеваемую подписку **TruckoRig Premium** (идентификатор продукта `premium_monthly_subscription`):

- Первый месяц бесплатный (оффер / introductory offer `free-month`). Бесплатный месяц начинается только после оформления подписки, а не при открытии приложения.
- Далее магазин списывает ежемесячную плату. В США это **2,99 $ в месяц**; в других странах цена задаётся Google Play или App Store.
- Подписка продлевается каждый месяц, пока вы её не отмените.
- На Android отмена: Google Play → Платежи и подписки.
- На iOS отмена: Настройки iPhone → [Ваше имя] → Подписки, либо подписка в App Store.
- Удаление приложения само по себе подписку не отменяет.

В Premium на Android входят: импорт из Telegram, аналитика, недельная цель, карта, налоги, сканер, камера, экспорт и копия в Google Drive. На iOS набор тот же, но вместо Google Drive — файловая резервная копия и, если функция включена, iCloud.

### Разрешения

**Android**

- Камера — фото груза или скана, только когда вы снимаете.
- Геолокация (передний план) — геотег фото и карта, только с вашего разрешения. Фоновая геолокация не запрашивается.
- Уведомления — напоминания (грузы, ТО, окончание пробного месяца) и статус бота.
- Интернет — Drive, Telegram, карта и проверка подписки в Google Play, если вы ими пользуетесь.
- Биометрия — по желанию, только для разблокировки на этом устройстве.
- Точные будильники — локальные напоминания.

Любое разрешение можно отозвать в настройках Android. Журнал при этом остаётся доступен.

**iOS**

- Камера — фото документов рейса и сканирование бумаг.
- Медиатека — если вы берёте фото из галереи.
- Геолокация «при использовании» — место съёмки на фото рейса.
- Фоновые задачи — только если вы включили Telegram-синхронизацию или копию в iCloud.

Разрешения можно отозвать в Настройках iOS.

### Дети

Приложение не предназначено для детей младше 13 лет. Мы сознательно не собираем данные детей.

### Как удалить данные

- Удалите данные приложения в настройках системы или удалите само приложение. Локальный журнал, фото и токен Telegram при этом стираются с устройства.
- Если вы делали копию в Google Drive — удалите файл бэкапа в своём Диске. Чтобы отозвать доступ: Настройки приложения → Google Drive → отключить.
- Если вы делали копию в iCloud — удалите файлы TruckoRig в приложении «Файлы» / iCloud Drive. Удаление фото на телефоне само по себе не удаляет копию в iCloud; копия уходит из iCloud, только когда вы удалите её сами.
- Чтобы отключить Telegram: удалите токен в Настройках приложения.
- Подписку отменяйте в Google Play или App Store, как указано выше.

Поскольку разработчик не хранит ваш журнал на своих серверах, отдельного «аккаунта TruckoRig» на стороне разработчика удалять не нужно.

### Изменения

Мы можем обновить эту политику. Дата в начале страницы изменится. Актуальная версия всегда доступна по этому адресу. Если изменения существенные, мы обновим дату и текст здесь; рекомендуем периодически просматривать политику.

### Контакты

Вопросы по конфиденциальности: [khaidar.safarov@gmail.com](mailto:khaidar.safarov@gmail.com) или обратная связь в Настройках приложения.

---

## English

### Summary

TruckoRig is a **local-first** trucking journal for Android and iOS. The developer **does not collect** personal data on developer-operated servers and **does not share** it with third parties for advertising or sale. Loads, paychecks, and diesel records stay on your device.

The app shows no ads. There is no TruckoRig cloud account. Google, Apple, or email sign-in is not required to use the journal.

Optional TruckoRig Premium is billed by Google Play (Android) or the App Store (iOS). The store processes the payment; the developer does not receive your card number.

### Who we are

TruckoRig (package and bundle ID `com.truckorig`) is published by Khaidar Safarov.  
Contact: [khaidar.safarov@gmail.com](mailto:khaidar.safarov@gmail.com), or in-app Settings feedback.

### Data processed on the device

| Category | Examples | Stored |
| --- | --- | --- |
| Profile | Optional first-run name on Android. iOS does not require an account. | On device only |
| Journal | Loads, stops, paychecks, diesel, goals, maintenance reminders, tax notes | On-device database (Room on Android, GRDB on iOS), encrypted with SQLCipher |
| Media | Photos, document scans, recognized text | Device storage |
| Location | Photo geotag or map | On device, only with permission |
| Telegram (optional) | Bot token and chat ID you enter | Device preferences (Android Keystore / iOS Keychain) |
| Subscription | A local flag that Premium is active, so access can continue offline for a short time | Device preferences |
| Widget (iOS) | A snapshot of your goal and theme for the home-screen widget | App Group `group.com.truckorig` on this iPhone |

On Android, system auto-backup of the app is turned off (`allowBackup="false"`). Biometric unlock, if you enable it, stays on the device.

The in-app “Analytics” screens are your own load and diesel charts on the phone. They are not telemetry sent to the developer.

### What we do not do

- We do not run a TruckoRig cloud account or store your journal on our servers.
- We do not sell data or show ads.
- Google, Apple, or email sign-in is not required to use the journal.
- Crash reports are not sent unless a release build includes `google-services.json`. The default repository build does not include that file.
- We do not publish or upload “crowd” rates (Crowd RPM). The efficiency map uses only your own on-device loads.

### When data may leave the device — only if you choose

These features are optional. The core journal (loads, diesel, paychecks) works offline.

1. **Google Drive backup (Android, Premium)** — if you connect Drive in Settings. The copy goes to *your* Google Drive, not to the developer. Drive sign-in uses Google OAuth. Your Google account email is used only to attach Drive; it does not become the TruckoRig login.
2. **iCloud Drive backup (iOS)** — if you enable sync. The journal and, when media backup is on, photos and scans are stored in the `iCloud.com.truckorig` container in *your* iCloud. You can see the files in the Files app. The developer cannot access your iCloud.
3. **Telegram bot** — if you enter your own bot token. Traffic goes to Telegram’s API that you configured. The token stays on the device.
4. **Export / share** — a CSV, PDF, backup file, or feedback email that you save or send yourself.
5. **Maps** — opening the map may load Google Maps tiles. Route directions may send route points to Google. That is Google’s service, not a TruckoRig server.
6. **Subscription payment** — Google Play or the App Store processes the payment. TruckoRig does not receive your card number and does not send the purchase token to a developer server.
7. **Document scanner (Android)** — recognition may use Google Play services on the device.

### Payments and subscriptions

TruckoRig is free to install. Loads, diesel, paychecks, and deadhead miles stay free.

An optional auto-renewing subscription, **TruckoRig Premium** (product ID `premium_monthly_subscription`), is sold by the store:

- The first month is free (offer / introductory offer `free-month`). Opening the app does not start the free month; subscribing does.
- After that, the store charges the monthly price — **$2.99 per month in the United States** (other countries use the local Play or App Store price).
- The plan renews every month until you cancel.
- Android: cancel in Google Play → Payments and subscriptions.
- iOS: cancel in iPhone Settings → [your name] → Subscriptions, or in the App Store.
- Uninstalling the app does not cancel the subscription by itself.

On Android, Premium includes Telegram import, analytics, the weekly goal, the map, taxes, scanner, camera, export, and Google Drive backup. On iOS the paid set is the same, except file backup and optional iCloud replace Google Drive.

### Permissions

**Android**

- Camera — load photos and document scans, only when you capture them.
- Location (foreground) — photo geotag and map, only with your permission. The app does not request background location.
- Notifications — reminders (loads, maintenance, trial ending) and bot status.
- Internet — Drive, Telegram, map tiles, and Play subscription checks if you use them.
- Biometrics — optional, only to unlock the app on this device.
- Exact alarms — local reminders.

You can revoke permissions in Android settings. The journal remains usable.

**iOS**

- Camera — trip document photos and paper scans.
- Photo library — if you pick an image from the library.
- Location When In Use — the capture location on a trip photo.
- Background processing — only if you enable Telegram sync or iCloud backup.

You can revoke permissions in iOS Settings.

### Children

TruckoRig is not directed at children under 13. We do not knowingly collect children’s data.

### Deleting your data

- Clear the app’s storage in system settings or uninstall the app. That removes the on-device journal, photos, and any saved Telegram token.
- If you made a Drive backup, delete that file in your own Google Drive. To revoke Drive access: Settings → Google Drive → disconnect.
- If you made an iCloud backup, delete the TruckoRig files in the Files app / iCloud Drive. Removing a photo on the phone does not remove the iCloud copy; the copy leaves iCloud only when you delete it there yourself.
- To disconnect Telegram, remove the token in app Settings.
- Cancel a subscription in Google Play or the App Store, as described above.

Because the developer does not store your journal on developer servers, there is no separate TruckoRig account on our side to delete.

### Changes

We may update this policy. The effective date at the top will change. The current version is always available at this URL. If we make material changes, we will update the date and the text here. Please review this page from time to time.

### Contact

Privacy questions: [khaidar.safarov@gmail.com](mailto:khaidar.safarov@gmail.com) or in-app Settings feedback.
