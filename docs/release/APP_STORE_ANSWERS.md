# App Store Connect answers — TruckoRig

Worksheet for the iOS app (`TruckoRig-Apple`). Bundle ID in the project and entitlements: `com.truckorig`. Current Xcode marketing version is **1.5.7**, build **1**. These answers follow `PrivacyInfo.xcprivacy`, `Info.plist` (`ITSAppUsesNonExemptEncryption` = false), `Premium.storekit`, and the iOS README.

This is not legal advice. Confirm every **TODO for K S** before submitting. Do not publish or pay from this document. The paid Apple Developer account is still the main blocker.

---

## 0) Before you fill the forms

| Item | Status in the repo | TODO for K S |
| --- | --- | --- |
| Apple Developer Program ($99/year) | Not registered from the repo | Enroll as Individual (or Organization if you want the legal name to be a company) |
| Bundle ID `com.truckorig` | In code and entitlements; not registered at Apple yet | Register the App ID with iCloud container `iCloud.com.truckorig`, App Group `group.com.truckorig`, and Push/other capabilities only if you actually ship them |
| iCloud, App Group, widget | Present in entitlements and code | If the first TestFlight or App Store build ships **without** iCloud, the widget, or App Groups, remove those capabilities from the App ID and trim the privacy policy / this worksheet until they ship |
| Marketing version | 1.5.7 (1) | **Bump** before submission (for example 1.6.0 to align with Android, or 1.0.0 if this is the first public iOS release) |
| Privacy policy URL | Draft only | Host `docs/release/privacy/index.html` on HTTPS and paste the URL |
| Terms URL | Draft; governing law is the Commonwealth of Pennsylvania, USA | Host `docs/release/terms/index.html` on HTTPS |
| Subscription | Local StoreKit config only | Create the product in App Store Connect before the Subscribe button can charge |
| Screenshots | 2 frames (Premium, Analytics) in `store/app-store/` for 6.5" and 6.9" | Apple asks for at least **3** screenshots per required size. Add at least one more (journal / home) for 6.9" and 6.5" (and 6.7" if Console still lists it) |
| Support email | `truckorig@gmail.com` | Decided. Use this address in App Store Connect. |

Contact for App Store Connect: Khaidar Safarov, `truckorig@gmail.com`.

**TODO for K S:** phone number for App Review contact, if Connect requires one.

---

## 1) App Privacy (nutrition labels)

### What the privacy manifest already says

`TruckoRig/Resources/PrivacyInfo.xcprivacy` and `TruckoRigWidget/PrivacyInfo.xcprivacy`:

| Key | Value |
| --- | --- |
| Tracking (`NSPrivacyTracking`) | **No** (`false`) |
| Tracking domains | none |
| Collected data types | **empty** |
| Required Reason APIs | UserDefaults `CA92.1` (app’s own settings only). App also declares File Timestamp `C617.1` (file size / timestamps inside the app container). The widget declares UserDefaults only. |

The iOS README states: no tracking, no data sent to the developer. The in-app Privacy screen says the same: no account, no analytics, no server.

### Recommended App Store Connect answers

| Question | Answer |
| --- | --- |
| Do you or your third-party partners collect data from this app? | **No** — “Data Not Collected” |
| Used for tracking? | **No** |
| Privacy policy URL | **TODO for K S:** hosted HTTPS URL of the updated policy |

This is the answer that matches the shipping privacy manifest. Use it only if all of the following stay true:

- No Crashlytics, analytics SDK, or developer backend in the App Store build.
- iCloud Drive backup writes only to the **user’s** container `iCloud.com.truckorig` (Cloud Documents). The developer cannot read that container from a server. Treat it as a user-initiated backup, not data collected by you.
- Telegram traffic goes to the API the user configured with their own bot token.
- StoreKit / App Store payment is handled by Apple; you do not receive card numbers.
- Export / share / mail are user-initiated.

### TODO for K S — confirm before you submit “Data Not Collected”

1. **Google Maps SDK for iOS.** The app embeds Google Maps (`GMSApiKey` in Info.plist). Apple requires disclosure of data that **third-party partners** collect. Read Google’s current privacy disclosures for Maps SDK for iOS. If Google collects diagnostics, identifiers, or location on its own, you may need to switch the overview to **Yes** and declare the types Google lists (typically not used for tracking by you, used for App Functionality). Do not guess.
2. **If the first build omits iCloud**, the policy and these answers should not promise iCloud until it ships.
3. If you later add Crashlytics, a backend, or advertising SDKs, you must update both `PrivacyInfo.xcprivacy` and this form. “Data Not Collected” would no longer be valid.

### Alternative (only if you decide iCloud or Maps must be declared)

If you choose to declare optional, user-controlled backups or Maps diagnostics, keep tracking **off**, mark the types **not used for tracking**, **not linked to the user’s identity on a developer account** (there is no TruckoRig account), purpose **App Functionality**, and **optional**. Possible types, only if you decide they apply:

- User Content / Other User Content (journal backup file)
- Photos or Videos (iCloud media backup)
- Location (photo geotag stored in a backup, or Maps)
- Product Interaction / Diagnostics (only if a third-party SDK documents that collection)

The recommended default remains **Data Not Collected**, matching the manifest, until the Maps SDK review says otherwise.

---

## 2) Age rating

Not made for kids. Complete the standard age-rating questionnaire.

Recommended answers (Apple’s wording can vary slightly; keep the facts the same):

| Topic | Answer | Notes |
| --- | --- | --- |
| Made for Kids? | **No** | Adult trucking journal. Privacy policy: not directed at children under 13. |
| Cartoon or fantasy violence | **No** | |
| Realistic or graphic violence | **No** | |
| Sexual content or nudity | **No** | Users may photograph work documents; the app does not include sexual content. |
| Profanity or crude humor | **No** | |
| Alcohol, tobacco, or drug use or references | **No** | Diesel / fuel receipts are not alcohol or tobacco content. |
| Mature or suggestive themes | **No** | |
| Horror / fear themes | **No** | |
| Medical or treatment information | **No** | |
| Gambling | **No** | |
| Unrestricted web access (in-app open browser) | **No** | No open in-app web browser. Maps and Telegram are specific features. |
| User-generated content that is publicly posted or shared with other users of the app? | **No** | Photos and journal entries stay on the device unless the user exports them or enables iCloud / a share sheet. There is no in-app social feed or chat with other TruckoRig users. |
| Location sharing with other users? | **No** | Location When In Use is for photo geotag only. |
| In-app purchases / subscriptions? | **Yes** | Auto-renewing subscription (see below). |

Expected rating: **4+**, with an **In-App Purchases** notice.

---

## 3) Export compliance

`Info.plist` already sets:

```xml
<key>ITSAppUsesNonExemptEncryption</key>
<false/>
```

The iOS README says this means the App Store Connect encryption questionnaire should not appear on upload.

Recommended answer if the question still appears: **No** — the app uses only **exempt** encryption (HTTPS, local data protection). The journal is encrypted on the device with SQLCipher; keys live in the Keychain. There is no custom non-exempt crypto and no proprietary communications encryption beyond ordinary HTTPS to Google, Apple, and (if the user enables it) Telegram.

**TODO for K S:** confirm that SQLCipher / Keychain local encryption is still treated as exempt under US export rules before you ship. The plist already takes that position; do not change it to “Yes, non-exempt” unless counsel tells you to.

---

## 4) Subscription product (create in App Store Connect)

The local StoreKit file is `TruckoRig/Resources/Premium.storekit`. Create a matching auto-renewable subscription before review, in a subscription group (the file names the group “Premium”).

| Field | Value |
| --- | --- |
| Product ID | `premium_monthly_subscription` (same ID as Android) |
| Reference name | Premium Monthly |
| Family Share | **No** (`familyShareable` is false in the StoreKit file) |
| Duration | 1 month |
| US price | **$2.99** |
| Introductory offer | Free for 1 month, offer / internal ID `free-month`, new subscribers |
| Display name (EN) | TruckoRig Premium |
| Description (EN) | First month free, then the monthly price. Telegram import, analytics, the weekly goal, scanner, map, and export. |
| Display name (RU) | TruckoRig Premium |
| Description (RU) | Первый месяц бесплатно, дальше цена за месяц. |
| Display name (ES) | TruckoRig Premium |
| Description (ES) | Primer mes gratis y después el precio mensual. |

Paid Premium features in the iOS paywall: Telegram, analytics, weekly goal, map, taxes, scanner, camera, export, and file backup (iCloud / file restore — iOS does **not** offer Google Drive). Free forever: loads, diesel, paychecks, deadhead.

Reviewers should use a sandbox Apple ID. Opening the app does not start the free month; subscribing does.

---

## 5) App Store listing copy

**TODO for K S:** category — suggested **Business** or **Productivity** (driver journal, not a bank). Copyright: `2026 Khaidar Safarov`.

### Name (30)

`TruckoRig`

### Subtitle (30)

Recommended: `Local trucking logbook` (22 characters).

Alternates: `On-device truck logbook` (23), `Loads, diesel & paychecks` (25).

### Promotional text (170)

```
Local-first truck logbook. Loads, diesel, and paychecks stay on your iPhone. Optional Premium: first month free, then $2.99/month in the US. Cancel anytime in Settings.
```

Length: 168.

### Description (EN)

```
TruckoRig is a local-first logbook for truck drivers. Keep loads, paychecks, diesel, and weekly goals on your iPhone — with optional Telegram import and iCloud backup.

WHAT YOU CAN DO
• Paste or forward Amazon Relay / broker load messages and save them to your journal
• Track paychecks and diesel receipts in one place
• Set a weekly profit goal and see pace at a glance
• Scan documents and attach photos to loads
• Home-screen widget for a quick look at your goal
• Optional on-device Telegram bot to import chat history
• Optional iCloud Drive backup (your iCloud, not a TruckoRig server)

SUBSCRIPTION
The app is free to download. Loads, diesel, paychecks, and deadhead miles stay free.
TruckoRig Premium is optional: the first month is free, then $2.99 per month in the United States (the price can differ by country). Premium includes Telegram import, analytics, the weekly goal, the map, taxes, scanner, camera, export, and file / iCloud backup. It renews every month until you cancel in iPhone Settings → Subscriptions, or in the App Store.

PRIVACY BY DESIGN
• Journal data lives in an on-device encrypted database
• iCloud and Telegram are optional — turn them on only if you need them
• Camera, photo library, and location are used only when you take or attach geotagged photos
• No ads. No selling of load data
• No account is required to use the journal

GETTING STARTED
1. Open the app — there is no sign-in
2. Add your first load (paste a Relay message or enter it manually)
3. Optionally connect Telegram or iCloud in Settings

Support: truckorig@gmail.com
```

A Russian (and Spanish / French) localization can use the same structure; RU Play copy is in `PLAY_CONSOLE_ANSWERS.md` and can be adapted (replace the backup-file lines with iCloud, Google Play with App Store). **TODO for K S:** decide whether to upload RU / ES / FR store localizations in the first submission (the app already localizes UI to EN, ES, FR, and RU).

### Keywords (100, no trailing comma)

```
truck,trucker,logbook,diesel,freight,load,paycheck,relay,mileage,owneroperator,amazonrelay,journal
```

Length: 98. Do not repeat the app name “TruckoRig” in keywords. Apple treats spaces as part of a keyword; the string above uses no spaces after commas.

### URLs

| Field | Value |
| --- | --- |
| Support URL | **TODO for K S:** public HTTPS page (can be the same host as the privacy policy, or a simple support page with the email). Placeholder: `https://TODO-KS-SUPPORT-URL` |
| Marketing URL | Optional. **TODO for K S:** leave blank unless you have a public site. Placeholder: `https://TODO-KS-MARKETING-URL` (omit in Connect if unused) |
| Privacy Policy URL | **TODO for K S:** hosted `docs/release/privacy/index.html` |

---

## 6) App Review notes

Paste into the Review Notes field (and attach a note that no demo account is required):

```
TruckoRig is a local-first trucking journal. There is no cloud account and no sign-in. The reviewer can skip any first-run name prompt and use the journal immediately.

Core features (loads, diesel, paychecks, deadhead) are free and work offline. Optional Premium is an auto-renewing subscription, product ID premium_monthly_subscription, with a one-month free introductory offer (free-month), then $2.99/month in the US. Opening the app does not start the free month; only a sandbox or production subscribe does. Please use a sandbox Apple ID to review Premium.

Telegram and iCloud backup are optional. Please do not treat them as required. The journal does not need a bot token or iCloud to function.

Camera, photo library, and location (When In Use) are optional and are used for document photos, scans, and geotags. You can deny them and still use the journal.

There are no ads and no in-app user-to-user chat.

Support: truckorig@gmail.com
```

If iCloud or the widget is disabled in the uploaded build, add one sentence saying so.

---

## 7) Version and review checklist

- [ ] Paid Apple Developer account approved
- [ ] Bundle ID, iCloud container, and App Group registered to match the build you actually upload
- [ ] Marketing version bumped from 1.5.7
- [ ] Privacy policy and terms hosted (governing law: Commonwealth of Pennsylvania, USA)
- [ ] App Privacy form matches `PrivacyInfo.xcprivacy` (and the Maps SDK review)
- [ ] Age rating submitted
- [ ] Export compliance left as exempt / `ITSAppUsesNonExemptEncryption` = false
- [ ] Subscription and free-month offer created and cleared for sale in the countries you ship
- [ ] At least 3 screenshots on each required iPhone size
- [ ] Support URL live
- [ ] Review notes pasted
- [ ] TestFlight smoke test on a real device before App Review
