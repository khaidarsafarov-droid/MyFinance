# Play Console answers — TruckoRig 1.6.0

Fill these in Play Console for package `com.truckorig`. This is a worksheet, not a legal opinion. Confirm every **TODO for K S** before submitting.

Source of truth in code (Android `main` at `7f98db9a`): `versionCode` **18**, `versionName` **1.6.0**, `targetSdk` **36**, `compileSdk` **37**, `applicationId` `com.truckorig`. Existing drafts: `truckerload/store/google-play/DATA_SAFETY.md` and `PLAY_CONSOLE_RELEASE.md` (the runbook still says 1.5.11 / versionCode 16).

**Do not publish, pay, or submit from this document.** Host the privacy policy and terms first.

---

## 0) Privacy policy and terms URLs

| Field | Value |
| --- | --- |
| Privacy policy URL | https://khaidarsafarov-droid.github.io/MyFinance/privacy/ (GitHub Pages, `docs/privacy/index.html`). The old rentry page is outdated (it still describes Google Drive). |
| Terms of use URL (if asked) | https://khaidarsafarov-droid.github.io/MyFinance/release/terms/ (`docs/release/terms/index.html`). Governing law is the Commonwealth of Pennsylvania, USA. |

Support email on the policy and this worksheet: `truckorig@gmail.com`.

---

## 1) Data safety

### Recommended overview (current release posture)

The field-by-field sheet is `truckerload/store/google-play/DATA_SAFETY.md`. The overview answer is **Yes**. Do not answer No.

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** |
| Privacy policy URL | https://khaidarsafarov-droid.github.io/MyFinance/privacy/ (GitHub Pages). Do not use the old rentry page. |
| Delete data URL | https://khaidarsafarov-droid.github.io/MyFinance/delete-data/ |
| Account creation methods | **My app does not allow users to create an account** |
| Can users log in with accounts created outside the app? | **No** (Google Sign-In removed in 1.6.2) |

Why Yes:

- The journal stays on the device (SQLCipher / Room). Android auto-backup is off. There is no TruckoRig backend.
- Google Maps SDK and ML Kit send diagnostics, identifiers, and (for Maps) SDK crash metrics to Google. That is collection under Play’s SDK rule. Directions API is not called.
- Precise and approximate location are collected as ephemeral fused-location plus geocoder lookups when the user allows location: for the photo stamp, and to prefill the diesel station or address. Photo GPS, date, and time stay on the device on purpose. The map does not use the driver’s GPS. 
- Telegram, the share sheet, backup files, and Play card data are not ticked. Google Drive sync and Google Sign-In were removed in 1.6.2, so Email address is not ticked. The exemptions and the “if a review rejects this” fallback are in `DATA_SAFETY.md`.
- Firebase Crashlytics is not in debug or release, even if `google-services.json` is present. The Crash logs row is still Yes because of the Maps SDK, not because of Firebase.

### Other declarations

- Is all collected user data encrypted in transit? **Yes** (HTTPS). Cleartext is blocked.
- Users can request that data be deleted? **Yes** — in-app “Delete all data”, clear storage, uninstall, or `truckorig@gmail.com`. Steps: https://khaidarsafarov-droid.github.io/MyFinance/delete-data/
- Independent security review? **No**.
- Play Families / designed for children? **No**.

### Sensitive permissions / photo and location forms

Declare as required by the current Play forms, purpose **App functionality**:

| Permission / form | Answer |
| --- | --- |
| Camera | Yes, used to capture load photos and document scans, only when the user takes or picks a picture. Not required for the core journal. |
| Photos / videos | On-device. GPS, date, and time are kept on purpose (watermark and photo record). Backup files include them only when the user saves a backup. They leave the device only if the user shares or exports that file. |
| Location | **Foreground only.** Photo geotag and prefilling the diesel station or address. The map does **not** use the driver’s GPS. The manifest does **not** request `ACCESS_BACKGROUND_LOCATION`. Approximate and precise location may both be requested. Optional; denying it does not block the journal. |
| Notifications | Reminders (loads, maintenance, trial) and optional Telegram status. |
| Foreground service | **Do not declare a data-sync FGS.** The current manifest removes `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC`. Telegram import no longer uses a persistent notification (see 1.5.11 notes). |
| Internet / Billing | Used for optional Telegram, maps, ML Kit, and Play subscription checks. |

---

## 2) Ads declaration

| Question | Answer |
| --- | --- |
| Does your app contain ads? | **No** |

There is no ads SDK. Monetization is an optional Play subscription only.

---

## 3) Target audience and content

| Question | Answer |
| --- | --- |
| Target age group | **18 and over only** |
| Does the app appeal to children? | **No** |
| Designed for children / Play Families? | **No** |
| Appeal to other age groups below 18? | **No** — it is a professional trucking journal |

**TODO for K S:** if Play still shows a “Store presence” age-group picker, select 18+ only and do not add 13–17.

---

## 4) Content rating (IARC questionnaire)

Start the IARC questionnaire. Category type: **All other app types** (this is not a social or chat app).

IARC correspondence email (decided): `truckorig@gmail.com`. **TODO for K S:** Play app category is not fixed in the repo; suggested **Business** or **Auto & Vehicles** (productivity / driver journal, not a bank).

Answer about what users can see or do **in the app**, including optional features. Recommended answers:

| Topic | Answer | Notes |
| --- | --- | --- |
| Violence (cartoon, realistic, graphic, or user-on-user) | **No** | |
| Sexual content, nudity, or dating | **No** | Users may photograph work documents; the app does not include sexual content. |
| Profanity or crude humor | **No** | The app does not ship profanity. |
| Illegal drugs | **No** | |
| Alcohol or tobacco | **No** | Diesel / fuel receipts are not alcohol or tobacco content. |
| Gambling, betting, or simulated gambling | **No** | |
| Horror / fear themes | **No** | |
| Can users interact or exchange messages **with other TruckoRig users** inside the app? | **No** | There is no in-app chat, comments, friends list, or live map of other drivers. `SocialNavGraph` is the local Profile tab only. Crowd RPM publish is hard-denied. |
| Can users share their location with other users? | **No** | Location is for the photo stamp and for prefilling the diesel place field, not for sharing with other users. The map does not use the driver’s GPS. |
| Unrestricted internet / open web browser? | **No** | No in-app open web browser. Maps, Telegram, and Play billing are specific features. |
| Digital goods or in-app purchases? | **Yes** | Auto-renewing subscription `premium_monthly_subscription`. First month free, then the store price ($2.99/month in the US). |
| Users can purchase physical goods? | **No** | |
| Age-restricted products (alcohol, tobacco, etc.)? | **No** | |

Expected outcome: a low rating such as **Everyone / PEGI 3 / IARC 3**, usually with an **In-app purchases** notice. If IARC wording in Console differs slightly, keep the same facts: no violence, no sex, no drugs, no user-to-user social, yes subscription.

---

## 5) App access

| Question | Answer |
| --- | --- |
| Are all or some features restricted (login, password, or other access control)? | **No** |
| Do reviewers need a demo account? | **No** |

First launch can take an optional name or skip it; the journal then opens a local on-device session. There is no required cloud login. Premium features sit behind the Play purchase sheet; the core journal (loads, diesel, paychecks, deadhead) stays free. Reviewers do not need Telegram, Drive, or a paid subscription to smoke-test the journal.

---

## 6) Other policy forms

| Form | Answer |
| --- | --- |
| News app? | **No** |
| COVID-19? | **No** |
| Financial features | This is a **personal work journal**, not banking, money transfer, lending, credit, crypto, or a brokerage. Declare the subscription under **Monetize → Subscriptions**, not as a financial product. If Console asks about “personal loan / banking / crypto,” answer **No**. If it asks whether users can record income and expenses, describe it as an on-device logbook only. |
| Data safety / account deletion | No developer-held account. Deletion is “Delete all data”, uninstall, or clear storage, plus any backup files the user saved. |
| Trader status (EU/UK) | **TODO for K S.** Complete this in Console before 1 February 2027. You are publishing as an individual (Khaidar Safarov), not an organization, unless you later enroll as an LLC. |
| Closed testing | Internal testing does **not** satisfy the 14-day / 12-tester rule for new personal accounts. Start **Closed testing** when you are ready; this worksheet does not start it for you. |

---

## 7) Subscription product (create in Console, do not skip)

Play Console → Monetize → Products → Subscriptions. The app cannot set the price itself.

| Field | Value |
| --- | --- |
| Product ID | `premium_monthly_subscription` (cannot be changed later) |
| Name | TruckoRig Premium |
| Description | First month free, then monthly Premium: Telegram, analytics, weekly goal, map, taxes, scanner, camera, export. Loads, diesel, paychecks, and deadhead stay free. |
| Base plan ID | `monthly` |
| Type | Auto-renewing, billing period 1 month |
| Price | **USD 2.99**; let Play convert other countries, or set them yourself |
| Offer ID | `free-month` |
| Offer | Free trial, 1 month, tag `free-month`, new subscribers, same regions as the base plan |

Activate the offer, the base plan, and the subscription. License testers: add your Gmail under License testing; install the build **from Play**, not as a sideloaded APK.

---

## 8) Store listing — use these, not the outdated files as-is

Repo files live in `truckerload/store/google-play/listing/`. Fix these when you paste into Console:

1. **Title length.** Play’s limit is **30**. The en-US title is **decided:** `TruckoRig: Driver Logbook` (25 characters). The RU title is `TruckoRig: журнал водителя` (26 characters).
2. **Getting started** is fixed in the repo listing files: no sign-in, local first-run name, backup to a file. Google Drive sync was removed in 1.6.2.

### Title (max 30)

| Locale | Paste this | Length | Status |
| --- | --- | --- | --- |
| en-US | `TruckoRig: Driver Logbook` | 25 | Decided |
| ru-RU | `TruckoRig: журнал водителя` | 26 | Decided |

### Short description (max 80) — existing text is within the limit

**EN (79):** Loads, paychecks, diesel & weekly goals — local-first logbook for truck drivers

**RU (78):** Лоуды, зарплаты, дизель и недельные цели — локальный журнал для дальнобойщиков

### Full description — EN (corrected)

```
TruckoRig is a local logbook for truck drivers. Loads, paychecks, diesel, and weekly goals are stored only on your phone. No account, no sign-in.

WHAT YOU CAN DO
• Paste or forward Amazon Relay / broker load messages into your journal
• Track paychecks and diesel receipts in one place
• Set a weekly profit goal and see your pace at a glance
• Scan documents and attach photos to loads, stamped with place, date, and time
• Home-screen widgets: quick diesel / camera / scan
• Optional Telegram bot on the device (import chat history)
• Backup to a file that you keep yourself

SUBSCRIPTION
The app is free to install. Loads, diesel, and paychecks stay free.
TruckoRig Premium is optional: the first month is free, then Google Play charges $2.99 per month in the United States (the price can differ by country). Premium includes Telegram import, analytics, the weekly goal, the map, taxes, scanner, camera, and export. It renews every month until you cancel in Google Play → Payments and subscriptions.

PRIVACY
• Your data is only yours: the journal stays on your phone, we never see it and never share it
• No developer cloud: you keep the backup file yourself
• Camera and location are used only for the photo stamp (place, date, time) and to prefill the place of a diesel fill. The map does not use your GPS
• No ads. We don't sell data

GETTING STARTED
1. Add your first load (paste a Relay message or enter it manually)
2. Optionally connect Telegram in Settings

Support and suggestions: truckorig@gmail.com
```

### Full description — RU (corrected)

```
TruckoRig — локальный журнал для водителей. Лоуды, зарплаты, дизель и недельные цели хранятся только на вашем телефоне.

ЧТО УМЕЕТ
• Вставка или пересылка сообщений Amazon Relay / брокера в журнал
• Учёт зарплат и чеков на дизель в одном месте
• Недельная цель по прибыли и наглядный темп
• Сканер документов и фото к лоудам со штампом места, даты и времени
• Виджеты: быстрый дизель / камера / скан
• Опциональный Telegram-бот на устройстве (импорт истории чата)
• Резервная копия в файл, которую вы храните сами

ПОДПИСКА
Приложение бесплатно в установке. Грузы, дизель и зарплаты остаются бесплатными.
TruckoRig Premium — по желанию: первый месяц бесплатно, затем Google Play списывает 2,99 $ в месяц в США (в других странах цена может отличаться). В Premium входят импорт из Telegram, аналитика, недельная цель, карта, налоги, сканер, камера и экспорт. Подписка продлевается каждый месяц, пока вы не отмените её в Google Play → Платежи и подписки.

ПРИВАТНОСТЬ
• Ваши данные — только ваши: журнал хранится на телефоне, мы его не видим и никому не передаём
• Без облака разработчика: копию в файл вы храните сами
• Камера и геолокация — только для штампа на фото и карты
• Без рекламы. Данные не продаём

СТАРТ
1. Добавьте первый лоуд (вставьте сообщение Relay или введите вручную)
2. При желании подключите Telegram в Настройках

Поддержка и предложения: truckorig@gmail.com.
```

Graphics: `play-store-assets/` and `truckerload/store/google-play/` (icon 512, feature graphic 1024×500, phone and tablet screenshots). Upload map is in `truckerload/store/google-play/README.md`.

---

## 9) Release notes 1.6.0 (What’s new)

User-facing notes for Play, covering work since 1.5.11 (versionCode 16) through versionCode 18 on `main`. Keep each locale under Play’s 500-character limit.

### EN

```
• Edit Load shows normal amounts (e.g. 2111.72) and does not overwrite untouched fields
• New Gallery for photos and scans, plus a clearer load-detail summary
• Maintenance reminders can ring on time
• Premium paywall lists each paid feature; first month free, then the Play price
• Trial-end reminder and an optional weekly prompt if you are not on Premium
• Miles/km stay selected after a language change; Canadian French added
• Workbook numbers work in Excel; text fields stay above the keyboard
```

### RU

```
• Редактирование груза: сумма видна нормально (2111.72), нетронутые поля не затираются
• Галерея фото и сканов и более компактная карточка груза
• Напоминания ТО могут сработать вовремя
• В Premium каждый инструмент указан отдельно; первый месяц бесплатно, дальше цена Play
• Напоминание о конце пробного периода и необязательный вопрос без Premium
• Мили/км сохраняются после смены языка; добавлен канадский французский
• Числа в таблице открываются в Excel; поля не прячутся под клавиатурой
```

---

## 10) TODO checklist before you click Submit

- [ ] Host privacy policy and terms; paste HTTPS URLs
- [x] Governing law: Commonwealth of Pennsylvania, USA
- [x] Support email: `truckorig@gmail.com`
- [ ] Confirm the release AAB has no `google-services.json`
- [ ] Review Maps / Billing / Auth / ML Kit / Location Data safety disclosures
- [x] EN listing title: `TruckoRig: Driver Logbook`
- [ ] Paste that title, the suggested RU title, and the corrected full descriptions
- [ ] Create and activate the subscription product and free-month offer
- [ ] Complete trader status when it applies to you
- [ ] Start Closed testing (12 testers / 14 days) if your account requires it; internal testing is not enough
