# Политика конфиденциальности TruckoRig

**Privacy Policy · TruckoRig**  
Android package: `com.truckorig`  
Разработчик / Developer: Khaidar Safarov  
Контакт / Contact: [truckorig@gmail.com](mailto:truckorig@gmail.com)  
Последнее обновление / Last updated: 23 ноября 2026 / 23 November 2026

Почтовый адрес в этом репозитории не указан. **TODO:** добавьте настоящий адрес только если хотите его опубликовать. Не выдумывайте адрес.

Postal address: none is stored in this repository. **TODO:** add a real mailing address only if you want it published.

Скопируйте эту страницу на https://rentry.co/truckorig-privacy-policy перед отправкой формы Data safety. Репозиторий не может обновить rentry.

---

## Русский

### Кратко

TruckoRig на Android — локальный журнал водителя. Грузы, зарплаты, дизель и фото хранятся на телефоне. Разработчик не поднимает сервер, который принимает журнал. Рекламы нет, данные не продаются.

Некоторые необязательные функции и библиотеки Google, которые входят в приложение, отправляют данные с телефона. Ниже каждый такой случай и ссылка на политику этой компании.

Код iOS в этом репозитории — экран недельной цели. Он не хранит журнал, не использует iCloud и не подключается к Telegram. Раздел про отдельное приложение для iPhone ниже перенесён из черновика от 18 ноября 2026 и в этот раз по коду Apple не проверялся.

### Что остаётся на телефоне

| Категория | Примеры | Где |
| --- | --- | --- |
| Профиль | Имя при первом запуске, по желанию | Этот телефон |
| Журнал | Грузы, остановки, зарплаты, дизель, цели, ТО, налоговые заметки | База Room на устройстве, SQLCipher |
| Фото и сканы | Файлы и распознанный текст | Память приложения |
| Место, дата и время фото | Координаты, город, штат, индекс, время съёмки | На снимке и в записи фото. Так и задумано. |
| Telegram | Токен бота и chat id, которые вы вводите | Зашифрованные настройки телефона |
| Подписка | Локальный флаг Premium | Этот телефон |

Системная автокопия Android для приложения выключена. Биометрия, если включите, работает только на телефоне. Экран «Аналитика» — ваши графики, а не отправка статистики разработчику.

### Фото

При съёмке приложение сохраняет время. Если вы разрешили геолокацию, сохраняет и координаты. Дата, время, место и координаты рисуются на снимке и пишутся в запись фото. Файл — новый JPEG в памяти приложения. Приложение не стирает место, дату и время.

Файлы фото не входят в копию Google Drive. Туда уходит JSON журнала. Фото покидает телефон, только если вы сами им делитесь или экспортируете его. Водяной знак остаётся на картинке.

### Когда данные уходят с телефона

Приложение запрещает незашифрованный http. Ниже только HTTPS.

**Google Maps.** Карта необязательна. SDK Google Maps отправляет Google сведения об устройстве и запросе, псевдонимный идентификатор Maps, IP-адрес, жесты карты (сдвиг, масштаб) и метрики сбоев внутри SDK. Разработчик это не получает. Приложение не вызывает Directions API.

- Политика Google: https://policies.google.com/privacy
- Что указывает SDK: https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure

**Геолокация.** Только на переднем плане, по разрешению. Фоновое местоположение не запрашивается. Если вы разрешили его и снимаете фото, или приложение ищет адрес, координата или адрес уходят в геокодер Android, чтобы получить город, штат и индекс. На телефоне с сервисами Google это делает Google. Отдельной копии у разработчика нет. Координаты остаются на телефоне вместе с фото.

**ML Kit.** Сканер документов и распознавание латиницы необязательны. Google пишет, что снимок обрабатывается на устройстве, а в Google уходят служебные сведения (устройство, приложение, ошибки), а не копия документа разработчику.

- Политика Google: https://policies.google.com/privacy
- Раскрытие ML Kit: https://developers.google.com/ml-kit/android-data-disclosure

При первом офлайн-распознавании приложение может скачать языковые файлы английского и русского с GitHub. Журнал при этом не загружается.

- Политика GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement

**Google Drive.** По желанию, в Premium. Вход через Google только к скрытой папке данных приложения. Почта Google нужна, чтобы показать, какой аккаунт привязан. Это не вход в TruckoRig.

В эту скрытую папку вашего аккаунта уходит JSON: грузы, зарплаты, дизель, ТО и настройки. Файла не видно в «Моём диске». Фото в нём нет. Google может прочитать JSON. Это не сквозное шифрование. На сервер разработчика файл не приходит.

После включения копии следующее изменение может отправиться снова. «Отключить» в Настройках останавливает новые отправки и выходит из аккаунта на телефоне. Уже лежащий скрытый файл это не удаляет.

- Политика Google: https://policies.google.com/privacy
- Папка app data: https://developers.google.com/drive/api/guides/appdata

**Telegram.** По желанию. Если вы вставляете свой токен, приложение вызывает Bot API `https://api.telegram.org`. Команда статуса может отправить в ваш чат количество записей, доход за неделю и мили. Файл, который вы экспортируете, может уйти документом. Telegram читает сообщения Bot API. Это не сквозное шифрование. Токен остаётся на телефоне. Бота ведёте вы, не разработчик.

- Политика Telegram: https://telegram.org/privacy

**Экспорт и письмо.** CSV, PDF, файл копии или отзыв уходят, когда вы сами сохраняете их или выбираете приложение в меню «Поделиться» или в почте.

**Оплата Google Play.** Установка бесплатна. Подписка TruckoRig Premium (`premium_monthly_subscription`) продаётся Google Play. Первый месяц может быть бесплатным (`free-month`). Дальше Play списывает месячную цену — **2,99 $ в США**, в других странах свою. Продлевается, пока не отмените в Google Play → Платежи и подписки. Удаление приложения подписку не отменяет.

Номер карты приложение не получает и токен покупки на сервер разработчика не отправляет.

- Политика Google: https://policies.google.com/privacy

### Чего мы не делаем

- Не продаём данные и не показываем рекламу. Разрешение рекламного идентификатора не запрашивается.
- Не отправляем Firebase Crashlytics, сессии Firebase и другие отчёты о сбоях Firebase. Файл `google-services.json` на компьютере сборки это не включает.
- Не запускаем foreground service. Эти разрешения из манифеста удалены. Опрос Telegram — не служба Android с постоянным уведомлением.
- Не загружаем «народные» ставки. Карта строится по вашим грузам на телефоне.
- Не запрашиваем микрофон.

### Разрешения Android

- Камера — фото и сканы, когда вы снимаете.
- Геолокация (передний план, точная и приблизительная) — место на фото и подписи адресов.
- Уведомления — местные напоминания.
- Интернет — функции выше и проверка подписки.
- Точные будильники и загрузка устройства — местные напоминания.
- Биометрия — по желанию, только на этом телефоне.
- Память на старых версиях Android — сохранение экспорта, который вы запросили.

Камеру, геолокацию и уведомления можно запретить. Журнал откроется.

### Сколько хранятся данные

- На телефоне — пока вы не удалите аккаунт в приложении, не очистите данные приложения или не удалите приложение.
- Скрытый файл Drive — пока вы не удалите скрытые данные приложения в Google Drive. У разработчика копий нет.
- Telegram — по правилам Telegram.
- Диагностика Maps и ML Kit — по правилам Google. Разработчик её не получает.
- Серверы разработчика — журнала там нет.

### Как удалить

- Удалите аккаунт в приложении. Сотрутся журнал, фото, сканы и токен Telegram на телефоне.
- Или очистите данные приложения, или удалите приложение. Это не отменяет Premium и не удаляет скрытый файл Drive.
- Drive: Настройки → Google Drive → отключить. Затем в Google Drive в браузере: Настройки → Управление приложениями → удалить скрытые данные. Инструкция Google: https://support.google.com/drive/answer/2523073 . Подключённые приложения: https://myaccount.google.com/connections
- Telegram: удалите токен в Настройках и, если нужно, чат в Telegram.
- Подписку отмените в Google Play.
- Запросы: [truckorig@gmail.com](mailto:truckorig@gmail.com) или обратная связь в Настройках. Разработчик не может удалить файл в вашем Google-аккаунте или сообщение на серверах Telegram.

### iOS

Эти предложения перенесены из черновика от 18 ноября 2026 для отдельного приложения Apple. В этом репозитории iOS их не делает. Перед публикацией сверьте их с тем приложением.

Если вы пользуетесь приложением TruckoRig для iPhone из другого репозитория: журнал, если он там есть, хранится на устройстве. Копия iCloud, если вы её включили, лежит в вашем iCloud, и разработчик её не читает. Камера, медиатека и геолокация «при использовании» работают только с вашего разрешения. Удаление фото на телефоне само не удаляет копию в iCloud.

### Дети

Приложение не предназначено для детей младше 13 лет.

### Изменения

Дата в начале страницы меняется вместе с текстом. Продолжая пользоваться приложением после обновления, вы принимаете новую редакцию.

### Контакты

Khaidar Safarov — [truckorig@gmail.com](mailto:truckorig@gmail.com)

---

## English

### Summary

TruckoRig on Android is a local-first trucking journal. Loads, paychecks, diesel, and photos stay on your phone. The developer does not run a server that receives your journal. The app does not show ads and does not sell personal data.

Some optional features, and Google libraries inside the app, send data off the phone. Each case is listed below with that company’s policy.

The iOS code in this repository is a weekly-goal screen. It does not store a journal, use iCloud, or connect to Telegram. The iPhone sentences further down were carried forward from the 18 November 2026 draft and were not re-checked against the Apple app in this change.

### Data that stays on the phone

| Category | Examples | Where |
| --- | --- | --- |
| Profile | Optional name from the first launch | This phone |
| Journal | Loads, stops, paychecks, diesel, goals, maintenance, tax notes | On-device Room database, encrypted with SQLCipher |
| Photos and scans | Image files, recognized text | App storage on this phone |
| Photo place, date, and time | GPS coordinates, city, state, ZIP, capture time | On the photo and in the photo record. Kept on purpose. |
| Telegram | Bot token and chat id you type | Encrypted preferences on this phone |
| Subscription | A local flag that Premium is active | This phone |

Android system backup of the app is off. Biometric unlock, if you turn it on, stays on the phone. The in-app Analytics screens are your own charts. They are not sent to the developer.

### Photos

When you take a photo, the app keeps the capture time. If you allowed location, it also keeps the GPS coordinates. Date, time, place, and coordinates are drawn onto the picture and stored with the photo record. The saved file is a new JPEG in app storage. The app does not remove that place, date, or time.

Photo files are not part of the Google Drive backup. That backup is a JSON file of the journal. A photo leaves the phone only if you share or export it yourself. The watermark stays on the image.

### When data leaves the phone

The app blocks unencrypted http. The transfers below use HTTPS.

**Google Maps.** Opening the map is optional. Google’s Maps SDK then sends Google device and request details, a pseudonymous Maps identifier, your IP address, map gestures such as pan and zoom, and crash metrics from inside the Maps SDK. The developer does not receive those. The map does not call Google’s Directions API.

- Google privacy policy: https://policies.google.com/privacy
- What the Maps SDK lists: https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure

**Location lookup.** Location is optional and foreground-only. The app does not ask for background location. If you allow it and take a photo, or the app resolves an address, the coordinate or address is sent to Android’s geocoder to get a city, state, and ZIP. On a phone with Google Play services, Google handles that lookup. The developer does not keep a separate copy. The coordinates stay on the phone with the photo.

**ML Kit.** The document scanner and Latin text recognition are optional. Google’s ML Kit processes the image on the device and sends Google diagnostic details, not a TruckoRig copy of the document.

- Google privacy policy: https://policies.google.com/privacy
- ML Kit disclosure: https://developers.google.com/ml-kit/android-data-disclosure

The first time offline recognition needs language files, the app may download English and Russian data from GitHub. That download does not upload your journal.

- GitHub privacy statement: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement

**Google Drive.** Optional. If you connect Drive in Settings, the app signs in with Google and asks only for Drive’s hidden app-data folder. Your Google email shows which account is linked. It is not a TruckoRig login.

The app uploads a JSON backup (loads, paychecks, diesel, maintenance, settings) to that hidden folder in your Google account. The file does not appear in My Drive. Photo files are not in it. Google can read the JSON. This is not end-to-end encryption. The developer’s servers do not receive it.

After you turn backup on, a later edit can upload again. Disconnect in Settings stops new uploads and signs out on the phone. It does not delete the hidden file.

- Google privacy policy: https://policies.google.com/privacy
- Drive app-data folder: https://developers.google.com/drive/api/guides/appdata

**Telegram.** Optional. If you paste your own bot token, the app calls `https://api.telegram.org`. A status command can send counts, weekly income, and miles back to your chat. A file you export can be sent as a document. Telegram can read Bot API messages. They are not end-to-end encrypted. The token stays on the phone. The developer does not run the bot.

- Telegram privacy policy: https://telegram.org/privacy

**Export and feedback.** A CSV, PDF, backup file, or feedback email leaves the phone when you save it or pick an app in the share sheet or your mail app.

**Google Play billing.** Install is free. TruckoRig Premium (`premium_monthly_subscription`) is sold by Google Play. The first month can be free (`free-month`). After that, Play charges the monthly price — **$2.99 in the United States**; other countries use the local price. It renews until you cancel in Google Play → Payments and subscriptions. Uninstalling does not cancel it.

The app does not receive your card number and does not send the purchase token to a developer server.

- Google privacy policy: https://policies.google.com/privacy

### What we do not do

- We do not sell personal data and we do not show ads. The app does not request the advertising ID.
- We do not send Firebase Crashlytics, Firebase sessions, or any other Firebase crash log. A `google-services.json` file on a build machine does not turn that on.
- We do not run a foreground service. Those permissions are removed from the manifest. Telegram polling is not an Android foreground service.
- We do not upload “crowd” rates. The map uses your on-device loads only.
- We do not request the microphone.

### Android permissions

- Camera — photos and scans, when you take them.
- Location (foreground, fine and coarse) — the place stored with a photo, and address labels.
- Notifications — local reminders.
- Internet — the features above, and the Play subscription check.
- Exact alarms and boot — local reminders.
- Biometrics — optional unlock on this phone.
- Storage on older Android versions — saving an export you asked for.

You can deny camera, location, and notifications. The journal still opens.

### How long data is kept

- On the phone: until you delete the account in the app, clear the app’s storage, or uninstall.
- Hidden Drive file: until you delete hidden app data in Google Drive. The developer does not hold a copy.
- Telegram: under Telegram’s policy.
- Maps and ML Kit diagnostics: under Google’s policy. The developer does not receive them.
- Developer servers: no journal is stored there.

### How to delete data

- In the app, delete the account. That erases the on-device journal, photos, scans, and saved Telegram token.
- Or clear the app’s storage, or uninstall. That does not cancel Premium and does not delete the hidden Drive file.
- Drive: Settings → Google Drive → disconnect. Then in Google Drive on the web, open Settings → Manage apps and delete hidden app data. Google’s steps: https://support.google.com/drive/answer/2523073 . Connected apps: https://myaccount.google.com/connections
- Telegram: remove the token in Settings, and delete the chat in Telegram if you want those messages gone.
- Cancel Premium in Google Play → Payments and subscriptions.
- Privacy requests: [truckorig@gmail.com](mailto:truckorig@gmail.com) or in-app Settings feedback. The developer cannot delete a file in your Google account or a message on Telegram’s servers.

### iOS

These sentences were carried forward from the 18 November 2026 draft for a separate Apple app. The iOS target in this repository does not do them. Check that app before you publish this section.

If you use a TruckoRig iPhone app from another repository: a journal there, if that app has one, stays on the device. An iCloud copy, if you turned it on, stays in your iCloud, and the developer cannot read it. Camera, photo library, and While Using location run only with your permission. Deleting a photo on the phone does not by itself delete an iCloud copy.

### Children

TruckoRig is not directed at children under 13.

### Changes

The date at the top changes with the text. Continued use after an update means you accept the revised policy.

### Contact

Khaidar Safarov — [truckorig@gmail.com](mailto:truckorig@gmail.com)
