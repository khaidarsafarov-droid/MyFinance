# Play Console answers — TruckoRig 1.6.0

Fill these in Play Console for package `com.truckorig`. This is a worksheet, not a legal opinion. Confirm every **TODO for K S** before submitting.

Source of truth in code (Android `main` at `b431f9b`): `versionCode` **18**, `versionName` **1.6.0**, `targetSdk` **36**, `applicationId` `com.truckorig`. Existing drafts: `truckerload/store/google-play/DATA_SAFETY.md` and `PLAY_CONSOLE_RELEASE.md` (the runbook still says 1.5.11 / versionCode 16).

**Do not publish, pay, or submit from this document.** Host the privacy policy and terms first.

---

## 0) Privacy policy and terms URLs

| Field | Value |
| --- | --- |
| Privacy policy URL | **TODO for K S:** public HTTPS URL of `docs/release/privacy/index.html` (GitHub Pages or another host). Until then, do not submit the policy form. The live rentry page (3 Sep 2026) is outdated — it is missing payments, iOS, and iCloud. |
| Terms of use URL (if asked) | **TODO for K S:** public HTTPS URL of `docs/release/terms/index.html`, after you fill in governing law. |

Support email on the policy and this worksheet: `khaidar.safarov@gmail.com`.

**TODO for K S:** in-app improvement feedback currently opens mail to `Truckorig@gmail.com`. Pick one official address and use it in Console, the policy, and the app.

---

## 1) Data safety

### Recommended overview (current release posture)

This matches `DATA_SAFETY.md` and the current code, **if** the Play AAB is built **without** `google-services.json`.

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |
| Privacy policy URL | **TODO for K S:** paste the hosted URL |

Rationale, so the answer stays consistent with the policy:

- The journal stays on the device (SQLCipher / Room). Android auto-backup is off.
- There is no TruckoRig backend and no developer-operated account.
- First-run “login” is an optional local name (`local_dev`). Google Sign-In is **not** the journal login; it is used only if the driver later connects Google Drive.
- Optional Drive backup, Telegram bot, file export, feedback email, and map tiles are **user-initiated** transfers to the user’s own Drive, the user’s own Telegram bot, a destination the user picks, or Google Maps — not to a TruckoRig server. Play’s user-initiated-action exception can apply when the user reasonably expects that transfer.
- Play Billing talks to Google Play to check and acknowledge the subscription. The app does not send purchase tokens to a developer server. A local subscribed flag is stored on the device only (72-hour cache).
- Crashlytics is compiled in only when `app/google-services.json` is present. **TODO for K S:** confirm the release AAB does **not** include that file. If it does, change this form to Yes and declare crash logs.
- In-app “Analytics” and the leftover `ConsentStore.analyticsAccepted` flag do not send telemetry in the current code. The day-7 survey is a local banner; sending feedback uses the system mail app.

### TODO for K S — third-party SDK review (do this before submitting)

Play requires you to declare data that **libraries and SDKs** send off the device, even if it goes to Google and not to you. Review the official Data safety disclosures for:

- Google Maps SDK / Play Services Maps, and Directions if used
- Play Services Location
- Play Billing Library
- Play Services Auth (Drive OAuth)
- ML Kit document scanner / text recognition

If any of those SDKs collect diagnostics, advertising ID, or device data on their own, change the overview answer to **Yes** and declare those types (usually collected and/or shared, purpose App functionality, optional where the user can avoid the feature). Do not guess; use Google’s published SDK disclosures.

### If you later enable Crashlytics or a backend

Declare at least:

| Data type | Collected? | Shared? | Purpose | Optional? | Encrypted in transit? |
| --- | --- | --- | --- | --- | --- |
| Crash logs | Yes, if `google-services.json` is in the AAB | Firebase (Google) | Analytics / app functionality (stability) | Build-dependent | Yes |

And update the public privacy policy on the same day.

### Other declarations (only if the overview becomes Yes)

- Is all collected user data encrypted in transit? **Yes** (HTTPS) for optional network calls.
- Users can request that data be deleted? **Yes** — clear app storage or uninstall; Drive / iCloud files in the user’s own account. There is no developer-held account to delete.
- Independent security review? **No**.
- Play Families / designed for children? **No**.

### Sensitive permissions / photo and location forms

Declare as required by the current Play forms, purpose **App functionality**:

| Permission / form | Answer |
| --- | --- |
| Camera | Yes, used to capture load photos and document scans, only when the user takes or picks a picture. Not required for the core journal. |
| Photos / videos | On-device attachments. They leave the device only if the user exports, backs up to Drive, or (on iOS) backs up to iCloud. |
| Location | **Foreground only.** Photo geotag and map. The manifest does **not** request `ACCESS_BACKGROUND_LOCATION`. Approximate and precise location may both be requested. Optional; denying it does not block the journal. |
| Notifications | Reminders (loads, maintenance, trial) and optional Telegram status. |
| Foreground service | **Do not declare a data-sync FGS.** The current manifest removes `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC`. Telegram import no longer uses a persistent notification (see 1.5.11 notes). |
| Internet / Billing | Used for optional Drive, Telegram, maps, and Play subscription checks. |

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

**TODO for K S:** IARC correspondence email — use `khaidar.safarov@gmail.com` unless you prefer another address. **TODO for K S:** Play app category is not fixed in the repo; suggested **Business** or **Auto & Vehicles** (productivity / driver journal, not a bank).

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
| Can users share their location with other users? | **No** | Location is for photo geotag and the driver’s own map. |
| Unrestricted internet / open web browser? | **No** | No in-app open web browser. Maps, Drive, Telegram, and Play billing are specific features. |
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
| Data safety / account deletion | No developer-held account. Deletion is uninstall / clear storage, plus the user’s own Drive file if they made one. |
| Trader status (EU/UK) | **TODO for K S.** Complete this in Console before 1 February 2027. You are publishing as an individual (Khaidar Safarov), not an organization, unless you later enroll as an LLC. |
| Closed testing | Internal testing does **not** satisfy the 14-day / 12-tester rule for new personal accounts. Start **Closed testing** when you are ready; this worksheet does not start it for you. |

---

## 7) Subscription product (create in Console, do not skip)

Play Console → Monetize → Products → Subscriptions. The app cannot set the price itself.

| Field | Value |
| --- | --- |
| Product ID | `premium_monthly_subscription` (cannot be changed later) |
| Name | TruckoRig Premium |
| Description | First month free, then monthly Premium: Telegram, analytics, weekly goal, map, taxes, scanner, camera, export, Drive backup. Loads, diesel, paychecks, and deadhead stay free. |
| Base plan ID | `monthly` |
| Type | Auto-renewing, billing period 1 month |
| Price | **USD 2.99**; let Play convert other countries, or set them yourself |
| Offer ID | `free-month` |
| Offer | Free trial, 1 month, tag `free-month`, new subscribers, same regions as the base plan |

Activate the offer, the base plan, and the subscription. License testers: add your Gmail under License testing; install the build **from Play**, not as a sideloaded APK.

---

## 8) Store listing — use these, not the outdated files as-is

Repo files live in `truckerload/store/google-play/listing/`. Two problems to fix when you paste into Console:

1. **Title length.** Current EN title is 40 characters and current RU title is 31; Play’s limit is **30**.
2. **Getting started** in the full description still says “Sign in with Google or continue with email.” That is outdated. Journal login is a local first-run name; Google is only for optional Drive backup.

### Title (max 30)

| Locale | Paste this | Length |
| --- | --- | --- |
| en-US | `TruckoRig: Driver Logbook` | 25 |
| ru-RU | `TruckoRig — журнал водителя` | 27 |

### Short description (max 80) — existing text is within the limit

**EN (79):** Loads, paychecks, diesel & weekly goals — local-first logbook for truck drivers

**RU (78):** Лоуды, зарплаты, дизель и недельные цели — локальный журнал для дальнобойщиков

### Full description — EN (corrected)

```
TruckoRig is a local-first logbook for truck drivers. Keep loads, paychecks, diesel, and weekly goals on your phone — with optional Telegram import and Google Drive backup.

WHAT YOU CAN DO
• Paste or forward Amazon Relay / broker load messages and save them to your journal
• Track paychecks and diesel receipts in one place
• Set a weekly profit goal and see pace at a glance
• Scan documents and attach photos to loads
• Home-screen widgets for quick diesel / camera / scan actions
• Optional Telegram bot on the device to import chat history
• Optional Google Drive backup (you stay in control)

SUBSCRIPTION
The app is free to install. Loads, diesel, and paychecks stay free.
TruckoRig Premium is optional: the first month is free, then Google Play charges $2.99 per month in the United States (the price can differ by country). Premium includes Telegram import, analytics, the weekly goal, the map, taxes, scanner, camera, export, and Drive backup. Loads, diesel, paychecks, and deadhead miles stay free. It renews every month until you cancel in Google Play → Payments and subscriptions.

PRIVACY BY DESIGN
• Journal data lives in an on-device database
• Drive backup and Telegram are optional — turn them on only if you need them
• Camera and location are used only when you take geotagged photos or enable map features
• No ads. No selling of load data
• Google or email sign-in is not required to use the journal

GETTING STARTED
1. Enter a name on first launch, or skip
2. Add your first load (paste a Relay message or enter it manually)
3. Optionally link Telegram or Google Drive in Settings

Support: in-app Settings feedback, or khaidar.safarov@gmail.com

Package: com.truckorig
```

### Full description — RU (corrected)

```
TruckoRig — локальный журнал для водителей. Лоуды, зарплаты, дизель и недельные цели на телефоне; по желанию — импорт из Telegram и бэкап в Google Drive.

ЧТО УМЕЕТ
• Вставка или пересылка сообщений Amazon Relay / брокера в журнал
• Учёт зарплат и чеков на дизель в одном месте
• Недельная цель по прибыли и наглядный темп
• Сканер документов и фото к лоудам
• Виджеты: быстрый дизель / камера / скан
• Опциональный Telegram-бот на устройстве (импорт истории чата)
• Опциональный бэкап в Google Drive

ПОДПИСКА
Приложение бесплатно в установке. Грузы, дизель и зарплаты остаются бесплатными.
TruckoRig Premium — по желанию: первый месяц бесплатно, затем Google Play списывает 2,99 $ в месяц в США (в других странах цена может отличаться). В Premium входят импорт из Telegram, аналитика, недельная цель, карта, налоги, сканер, камера, экспорт и копия в Drive. Грузы, дизель, зарплаты и deadhead остаются бесплатными. Подписка продлевается каждый месяц, пока вы не отмените её в Google Play → Платежи и подписки.

ПРИВАТНОСТЬ
• Данные журнала хранятся локально
• Drive и Telegram включаются только по вашему желанию
• Камера и геолокация — только для геотега фото и карты
• Без рекламы. Данные лоудов не продаём
• Вход через Google или email для журнала не требуется

СТАРТ
1. Укажите имя при первом запуске или пропустите шаг
2. Добавьте первый лоуд (вставьте сообщение Relay или введите вручную)
3. При необходимости подключите Telegram или Drive в Настройках

Поддержка: обратная связь в Настройках или khaidar.safarov@gmail.com

Пакет: com.truckorig
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
- [ ] Confirm governing law in the Terms
- [ ] Confirm support email (`khaidar.safarov@gmail.com` vs `Truckorig@gmail.com`)
- [ ] Confirm the release AAB has no `google-services.json`
- [ ] Review Maps / Billing / Auth / ML Kit / Location Data safety disclosures
- [ ] Paste the 30-character titles and the corrected full descriptions
- [ ] Create and activate the subscription product and free-month offer
- [ ] Complete trader status when it applies to you
- [ ] Start Closed testing (12 testers / 14 days) if your account requires it; internal testing is not enough
