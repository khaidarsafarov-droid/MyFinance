# Политика конфиденциальности TruckoRig

**Privacy Policy · TruckoRig**  
Android package: `com.truckorig`  
Разработчик / Developer: Khaidar Safarov  
Контакт / Contact: [truckorig@gmail.com](mailto:truckorig@gmail.com)  
Последнее обновление / Last updated: 25 ноября 2026 / 25 November 2026

Почтовый адрес в этом репозитории не указан. **TODO:** добавьте настоящий адрес только если хотите его опубликовать. Не выдумывайте адрес.

Postal address: none is stored in this repository. **TODO:** add a real mailing address only if you want it published.

Опубликованная версия: https://khaidarsafarov-droid.github.io/MyFinance/privacy/ (GitHub Pages). Удаление данных: https://khaidarsafarov-droid.github.io/MyFinance/delete-data/. Старую страницу rentry больше не используйте: там описан Google Drive.

---

## Русский

### Кратко

TruckoRig на Android — локальный журнал водителя. Грузы, зарплаты, дизель и фото хранятся на телефоне. В приложении **нет аккаунтов и входа** (ни по почте, ни через Google), а с версии 1.6.2 **нет синхронизации с Google Drive**. Разработчик не поднимает сервер, который принимает журнал. Рекламы нет, данные не продаются.

Некоторые необязательные функции и библиотеки Google, которые входят в приложение, отправляют данные с телефона. Ниже каждый такой случай и ссылка на политику этой компании.

Отдельное приложение TruckoRig для iPhone описано в разделе «iOS» ниже.

### Что остаётся на телефоне

| Категория | Примеры | Где |
| --- | --- | --- |
| Профиль | Имя при первом запуске, по желанию | Этот телефон |
| Журнал | Грузы, остановки, зарплаты, дизель, цели, ТО, налоговые заметки | База Room на устройстве, SQLCipher |
| Фото и сканы | Файлы и распознанный текст | Память приложения |
| Штамп на фото | Место, дата и время, которые водитель сам ставит на снимок | На снимке и в записи фото, на этом телефоне |
| Telegram | Токен бота и chat id, которые вы вводите | Зашифрованные настройки телефона |
| Подписка | Локальный флаг Premium | Этот телефон |

Системная автокопия Android для приложения выключена. Биометрия, если включите, работает только на телефоне. Экран «Аналитика» — ваши графики, а не отправка статистики разработчику.

### Штамп на фото

Место, дата и время на снимке — основная функция. Водитель ставит их, чтобы показать компании, где и когда сделано фото. Это происходит только когда вы снимаете. В фоне приложение местоположение не собирает.

Штамп рисуется на снимке и хранится вместе с фото на этом телефоне. Разработчик его не получает. Снимок со штампом уходит с телефона, только когда вы сами им делитесь, экспортируете его или сохраняете файл бэкапа. Это ваше действие.

### Когда данные уходят с телефона

Приложение запрещает незашифрованный http. Ниже только HTTPS.

**Google Maps.** Карта необязательна. SDK Google Maps отправляет Google сведения об устройстве и запросе, псевдонимный идентификатор Maps, IP-адрес, жесты карты (сдвиг, масштаб) и метрики сбоев внутри SDK. Разработчик это не получает. Приложение не вызывает Directions API.

- Политика Google: https://policies.google.com/privacy
- Что указывает SDK: https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure

**Город на штампе и место заправки.** Геолокация необязательна и только на переднем плане. Фоновое местоположение не запрашивается. Открытие карты не отправляет ваш GPS.

Если вы разрешили геолокацию, приложение может спросить геокодер Android в двух случаях: когда вы снимаете фото, чтобы написать на штампе город, штат и индекс; и когда вы добавляете заправку, чтобы подставить станцию или адрес. На телефоне с сервисами Google на эти запросы отвечает Google. Копии для TruckoRig не остаётся. Геолокацию можно запретить и всё равно снимать или вносить дизель: на штампе будет время без места, поле заправки останется пустым, его можно ввести вручную.

**ML Kit.** Сканер документов и распознавание латиницы необязательны. Google пишет, что снимок обрабатывается на устройстве, а в Google уходят служебные сведения (устройство, приложение, ошибки), а не копия документа разработчику.

- Политика Google: https://policies.google.com/privacy
- Раскрытие ML Kit: https://developers.google.com/ml-kit/android-data-disclosure

При первом офлайн-распознавании приложение может скачать языковые файлы английского и русского с GitHub. Журнал при этом не загружается.

- Политика GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement

**Telegram.** По желанию. Если вы вставляете свой токен, приложение вызывает Bot API `https://api.telegram.org`. Пока токен сохранён, приложение может опрашивать этот бот в фоне (это не foreground service Android). Команда статуса может отправить в ваш чат количество записей, доход за неделю и мили. Файл, который вы экспортируете, может уйти документом. Входящие сообщения можно импортировать в журнал на этом телефоне. Telegram читает сообщения Bot API. Это не сквозное шифрование. Токен остаётся на телефоне. Бота ведёте вы, не разработчик.

- Политика Telegram: https://telegram.org/privacy

**Экспорт и письмо.** CSV, PDF, файл копии или отзыв уходят, когда вы сами сохраняете их или выбираете приложение в меню «Поделиться» или в почте.

**Оплата Google Play.** Установка бесплатна. Подписка TruckoRig Premium (`premium_monthly_subscription`) продаётся Google Play. Первый месяц может быть бесплатным (`free-month`). Дальше Play списывает месячную цену — **2,99 $ в США**, в других странах свою. Продлевается, пока не отмените в Google Play → Платежи и подписки. Удаление приложения подписку не отменяет.

Номер карты приложение не получает и токен покупки на сервер разработчика не отправляет.

- Политика Google: https://policies.google.com/privacy

### Чего мы не делаем

- Нет аккаунтов, входа и облачной синхронизации. Приложение не входит в Google и не читает ваш адрес почты.
- Не продаём данные и не показываем рекламу. Разрешение рекламного идентификатора не запрашивается.
- Не отправляем Firebase Crashlytics, сессии Firebase и другие отчёты о сбоях Firebase. Файл `google-services.json` на компьютере сборки это не включает.
- Не запускаем foreground service. Эти разрешения из манифеста удалены. Опрос Telegram — не служба Android с постоянным уведомлением.
- Не загружаем «народные» ставки. Карта строится по вашим грузам на телефоне.
- Не запрашиваем микрофон.

### Разрешения Android

- Камера — фото и сканы, когда вы снимаете.
- Геолокация (передний план, точная и приблизительная) — когда вы снимаете фото, чтобы на штампе было место, или когда добавляете заправку, чтобы подставить станцию или адрес. Карта ваш GPS не использует. Фона нет.
- Уведомления — местные напоминания.
- Интернет — функции выше и проверка подписки.
- Точные будильники и загрузка устройства — местные напоминания.
- Биометрия — по желанию, только на этом телефоне.
- Память на старых версиях Android — сохранение экспорта, который вы запросили.

Камеру, геолокацию и уведомления можно запретить. Журнал откроется.

### Сколько хранятся данные

- На телефоне — пока вы не нажмёте «Удалить все данные», не очистите данные приложения или не удалите приложение.
- Файлы бэкапа, которые вы сохранили, — пока вы их не удалите. У разработчика копий нет.
- Старая копия в Google Drive (только из версий до 1.6.2) — скрытый файл в вашем Google Drive, пока вы его не удалите.
- Telegram — по правилам Telegram.
- Диагностика Maps и ML Kit — по правилам Google. Разработчик её не получает.
- Серверы разработчика — журнала там нет.

### Как удалить

- В приложении: Настройки → Удалить все данные. Сотрутся журнал, фото, сканы и токен Telegram на телефоне.
- Или очистите данные приложения, или удалите приложение. Это не отменяет Premium.
- Файлы бэкапа удалите там, куда их сохраняли.
- Только если версия до 1.6.2 делала копию в Google Drive: в Google Drive в браузере откройте Настройки → Управление приложениями → TruckoRig → удалить скрытые данные. Инструкция Google: https://support.google.com/drive/answer/2523073 . Подключённые приложения: https://myaccount.google.com/connections
- Telegram: удалите токен в Настройках и, если нужно, чат в Telegram.
- Подписку отмените в Google Play.
- Запросы: [truckorig@gmail.com](mailto:truckorig@gmail.com) или обратная связь в Настройках. Разработчик не может удалить файл на вашем устройстве, в вашем облаке или сообщение на серверах Telegram.

### iOS

Приложение TruckoRig для iPhone хранит журнал на устройстве. В нём нет аккаунтов и входа по почте или через Google. Копия iCloud, если вы её включили, сохраняется в iCloud Drive того Apple ID, который уже вошёл на iPhone, и разработчик её не читает. Камера, медиатека и геолокация «при использовании» работают только с вашего разрешения. Удаление фото на телефоне само не удаляет копию в iCloud.

### Дети

Приложение не предназначено для детей младше 13 лет.

### Изменения

Дата в начале страницы меняется вместе с текстом. Продолжая пользоваться приложением после обновления, вы принимаете новую редакцию.

### Контакты

Khaidar Safarov — [truckorig@gmail.com](mailto:truckorig@gmail.com)

---

## English

### Summary

TruckoRig on Android is a local-first trucking journal. Loads, paychecks, diesel, and photos stay on your phone. The app has **no accounts and no sign-in** (no email login, no Google sign-in), and since version 1.6.2 it has **no Google Drive sync**. The developer does not run a server that receives your journal. The app does not show ads and does not sell personal data.

Some optional features, and Google libraries inside the app, send data off the phone. Each case is listed below with that company’s policy.

The separate TruckoRig iPhone app is described under “iOS” below.

### Data that stays on the phone

| Category | Examples | Where |
| --- | --- | --- |
| Profile | Optional name from the first launch | This phone |
| Journal | Loads, stops, paychecks, diesel, goals, maintenance, tax notes | On-device Room database, encrypted with SQLCipher |
| Photos and scans | Image files, recognized text | App storage on this phone |
| Photo stamp | GPS, date, and time the driver asked to put on a photo | On the photo and in the photo record, on this phone |
| Telegram | Bot token and chat id you type | Encrypted preferences on this phone |
| Subscription | A local flag that Premium is active | This phone |

Android system backup of the app is off. Biometric unlock, if you turn it on, stays on the phone. The in-app Analytics screens are your own charts. They are not sent to the developer.

### Photo stamp

Putting location, date, and time on a photo is a core feature. Drivers use it so they can show their trucking company where and when a photo was taken. It happens only when you take the photo. It does not run in the background.

The stamp is drawn onto the picture and saved with the photo on this phone. The developer does not receive it. The stamped photo leaves the phone only when you share or export it, or save a backup file, yourself. That share is your action.

### When data leaves the phone

The app blocks unencrypted http. The transfers below use HTTPS.

**Google Maps.** Opening the map is optional. Google’s Maps SDK then sends Google device and request details, a pseudonymous Maps identifier, your IP address, map gestures such as pan and zoom, and crash metrics from inside the Maps SDK. The developer does not receive those. The map does not call Google’s Directions API.

- Google privacy policy: https://policies.google.com/privacy
- What the Maps SDK lists: https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure

**City line on the stamp, and the diesel place field.** Location is optional and foreground-only. The app does not ask for background location, and opening the map does not send your GPS.

If you allow location, the app can ask Android’s geocoder in two cases: when you take a photo, so the stamp can include city, state, and ZIP; and when you add a diesel fill, so the station or address field can be prefilled. On a phone with Google Play services, Google answers those lookups and does not keep a copy for TruckoRig. You can refuse location and still take photos or add diesel: the stamp then has the time without a place, and the diesel field stays empty for you to type.

**ML Kit.** The document scanner and Latin text recognition are optional. Google’s ML Kit processes the image on the device and sends Google diagnostic details, not a TruckoRig copy of the document.

- Google privacy policy: https://policies.google.com/privacy
- ML Kit disclosure: https://developers.google.com/ml-kit/android-data-disclosure

The first time offline recognition needs language files, the app may download English and Russian data from GitHub. That download does not upload your journal.

- GitHub privacy statement: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement

**Telegram.** Optional. If you paste your own bot token, the app calls `https://api.telegram.org`. Once the token is saved, the app can poll that bot in the background (this is not an Android foreground service). A status command can send counts, weekly income, and miles back to your chat. A file you export can be sent as a document. Incoming chats can be imported into the journal on this phone. Telegram can read Bot API messages. They are not end-to-end encrypted. The token stays on the phone. The developer does not run the bot.

- Telegram privacy policy: https://telegram.org/privacy

**Export and feedback.** A CSV, PDF, backup file, or feedback email leaves the phone when you save it or pick an app in the share sheet or your mail app.

**Google Play billing.** Install is free. TruckoRig Premium (`premium_monthly_subscription`) is sold by Google Play. The first month can be free (`free-month`). After that, Play charges the monthly price — **$2.99 in the United States**; other countries use the local price. It renews until you cancel in Google Play → Payments and subscriptions. Uninstalling does not cancel it.

The app does not receive your card number and does not send the purchase token to a developer server.

- Google privacy policy: https://policies.google.com/privacy

### What we do not do

- We do not have accounts, a login, or cloud sync. The app does not sign in to Google and does not read your email address.
- We do not sell personal data and we do not show ads. The app does not request the advertising ID.
- We do not send Firebase Crashlytics, Firebase sessions, or any other Firebase crash log. A `google-services.json` file on a build machine does not turn that on.
- We do not run a foreground service. Those permissions are removed from the manifest. Telegram polling is not an Android foreground service.
- We do not upload “crowd” rates. The map uses your on-device loads only.
- We do not request the microphone.

### Android permissions

- Camera — photos and scans, when you take them.
- Location (foreground, fine and coarse) — when you take a photo, so the stamp can include the place, or when you add a diesel fill, to prefill the station or address. The map does not use your GPS. No background location.
- Notifications — local reminders.
- Internet — the features above, and the Play subscription check.
- Exact alarms and boot — local reminders.
- Biometrics — optional unlock on this phone.
- Storage on older Android versions — saving an export you asked for.

You can deny camera, location, and notifications. The journal still opens.

### How long data is kept

- On the phone: until you tap “Delete all data”, clear the app’s storage, or uninstall.
- Backup files you saved: until you delete them. The developer does not hold a copy.
- Old Google Drive copy (only versions before 1.6.2): a hidden file in your own Google Drive until you delete it.
- Telegram: under Telegram’s policy.
- Maps and ML Kit diagnostics: under Google’s policy. The developer does not receive them.
- Developer servers: no journal is stored there.

### How to delete data

- In the app, Settings → Delete all data. That erases the on-device journal, photos, scans, and saved Telegram token.
- Or clear the app’s storage, or uninstall. That does not cancel Premium.
- Delete backup files where you saved them.
- Only if a version before 1.6.2 backed up to Google Drive: in Google Drive on the web, open Settings → Manage apps → TruckoRig and delete hidden app data. Google’s steps: https://support.google.com/drive/answer/2523073 . Connected apps: https://myaccount.google.com/connections
- Telegram: remove the token in Settings, and delete the chat in Telegram if you want those messages gone.
- Cancel Premium in Google Play → Payments and subscriptions.
- Privacy requests: [truckorig@gmail.com](mailto:truckorig@gmail.com) or in-app Settings feedback. The developer cannot delete a file on your device, in your cloud storage, or a message on Telegram’s servers.

### iOS

The TruckoRig iPhone app keeps the journal on the device. It has no accounts and no email or Google sign-in. An iCloud copy, if you turned it on, goes to iCloud Drive under the Apple ID already signed in on the iPhone, and the developer cannot read it. Camera, photo library, and While Using location run only with your permission. Deleting a photo on the phone does not by itself delete an iCloud copy.

### Children

TruckoRig is not directed at children under 13.

### Changes

The date at the top changes with the text. Continued use after an update means you accept the revised policy.

### Contact

Khaidar Safarov — [truckorig@gmail.com](mailto:truckorig@gmail.com)
