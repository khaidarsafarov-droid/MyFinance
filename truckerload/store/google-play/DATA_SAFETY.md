# Play Console — Data safety answer sheet

Fill **Play Console → Policy → App content → Data safety** for `com.truckorig` from this sheet. Do not submit from this file.

Definitions used here are Google’s, from [Provide information for Google Play’s Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469) (collected = sent off the device by the app **or by an SDK**, to the developer **or** a third party). On-device processing is not collection. End-to-end encryption (only sender and recipient can read it) is not collection. A user-initiated transfer the user reasonably expects is not **sharing**. Ephemeral processing (memory only, only long enough to answer that request, not used to build a profile) is still answered on the form, and Play hides it on the store listing.

Audit date: 2026-11-25, after Google Drive sync and Google Sign-In were removed (branch `feat/remove-drive-sync`). Package `com.truckorig`, versionName 1.6.2, versionCode 20, targetSdk 36, compileSdk 37. The release runtime classpath no longer contains `play-services-auth`; the Google libraries left are Maps (`play-services-maps`), fused location (`play-services-location`), ML Kit text recognition and document scanner, Play Billing, and their `play-services-base`/`basement`/`tasks` and `datatransport` dependencies. The merged app manifest still has no `FOREGROUND_SERVICE`, no `FOREGROUND_SERVICE_DATA_SYNC`, and no `AD_ID`. Firebase Crashlytics is not a dependency; see Crash logs below.

## 1. Overview

| Form question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **Yes** |
| Independent security review (MASA) | **No** |
| Committed to the Families policy | **No** — not a children’s app |
| Privacy policy URL | https://khaidarsafarov-droid.github.io/MyFinance/privacy/ (GitHub Pages, `docs/privacy/index.html`). Replace the old rentry link in the form with this one. |
| Delete data URL | https://khaidarsafarov-droid.github.io/MyFinance/delete-data/ (`docs/delete-data/index.html`) |

**Why the overview is Yes, not No.** The developer has no server and no accounts, and the journal never goes to the developer. The overview is still Yes because of Google libraries built into the app:

- Maps SDK and ML Kit send diagnostics off the device to Google by themselves. That is collection (and, for Maps, sharing). It does not meet the on-device or end-to-end exemptions.
- The address lookup for the photo stamp and the diesel place goes through Google's geocoder (location rows).
- Telegram is optional and user-configured. It is explained in section 4 and is **not** ticked, for the reasons in that section.
- Google Drive sync was removed in 1.6.2, so there is no cloud backup to explain any more.

### Account questions

| Form question | Answer |
| --- | --- |
| Account creation methods | **My app does not allow users to create an account** |
| Can users log in to your app with accounts created outside the app? | **No.** Google Sign-In was removed in 1.6.2. The app has no login of any kind (an optional fingerprint/face unlock on the phone is not an account). |
| Delete account URL | Not needed without accounts. If the form still asks, use the delete-data URL above. |
| Can users request that some or all of their data is deleted without deleting their account? | If asked: **Yes** — "Delete all data" in the app, or deleting single loads/photos. The delete-data page explains both. |

## 2. Data types to tick

> **Email address is not ticked.** Since 1.6.2 the app does not sign in to Google and never reads the user's email address.

Tick only the rows in this section. For each, Play asks collected vs shared, ephemeral, optional vs required, and purposes.

| Data type | Collected | Shared | Ephemeral | Optional or required | Purposes |
| --- | --- | --- | --- | --- | --- |
| Location → Precise location | Yes | Yes | Yes | Optional | App functionality |
| Location → Approximate location | Yes | Yes | Yes | Optional | App functionality |
| App info and performance → Crash logs | Yes | Yes | No | Optional | Analytics |
| App info and performance → Diagnostics | Yes | Yes | No | Optional | Analytics |
| App activity → App interactions | Yes | Yes | No | Optional | Analytics |
| Device or other IDs → Device or other IDs | Yes | Yes | No | Optional | Analytics |

Encryption in transit for every row: **Yes** (HTTPS). `network_security_config.xml` sets `cleartextTrafficPermitted="false"`.

### Precise location

Collected and shared, ephemeral, optional, purpose App functionality.

This row is **only** the one geocoder lookup. It is not the photo stamp.

Stamping a photo with GPS, date, and time is the feature drivers install the app for. The user chooses it by taking the photo. It never runs in the background. `PhotoManager.savePhoto` draws the place, date, and time onto the JPEG, and `PhotoRepository` stores the same fields on the phone (`CameraViewModel`, `PhotoEntity`). That processing stays on the device, so it is not collection. When the driver later shares or exports that photo (for example to their trucking company), Play treats it as user-initiated sharing, so it is not ticked as sharing either. See section 5.

The lookups that **are** ticked are two foreground, user-started actions. Both use Play’s fused location client (`PRIORITY_BALANCED_POWER_ACCURACY`) and then `Geocoder.getFromLocation` (`LocationHelper.kt`):

1. Taking a photo: `getCurrentLocation` so the stamp can say city, state, and ZIP (`CameraViewModel`).
2. Adding a diesel fill: `getCurrentStopLabel` prefills the station or address (`AddDieselViewModel.ensureLocation`, from `AddDieselScreen` and `DieselQuickAddScreen`).

On a Play-services phone those requests go to Google, which is why the row is shared. The app does not send them to a TruckoRig server, does not keep a server copy, and does not build a profile, so they are ephemeral. The user can refuse location and still use the journal, the camera (the stamp then has the time without a place), and diesel entry (the field stays empty to type), so it is optional. There is no `ACCESS_BACKGROUND_LOCATION`.

The map does not send the driver’s GPS. `GoogleMapsHeatmapCard` does not turn on my-location. Maps SDK diagnostics are the Diagnostics, Crash logs, App interactions, and Device or other IDs rows, not this one.

### Approximate location

Collected and shared, ephemeral, optional, purpose App functionality.

The same two fused-location plus geocoder lookups, and only those. The manifest also requests `ACCESS_COARSE_LOCATION`. `LocationHelper.hasLocationPermission` accepts a coarse-only grant, and the fused fix uses `PRIORITY_BALANCED_POWER_ACCURACY`. A coarse fix is approximate location under Play’s “3 square kilometers or coarser” definition. Same optional, ephemeral, not-background answers. The on-device stamp is still not this row. A diesel station/address string stored in the journal stays on the device.

Do **not** also tick approximate location because the Maps SDK collects an IP address. Google’s Maps disclosure says the IP is used to understand SDK usage, not that the SDK infers a place from it. IP-related SDK data is covered by Diagnostics and Device or other IDs. `geocodeAddress` is not called from production code.

### Crash logs

Collected and shared, not ephemeral, optional, purpose Analytics.

This row is **only** the Maps SDK. [Google’s Maps SDK data-disclosure page](https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure) (page updated 2026-11-09) says the SDK collects stack traces and crash metrics for crashes inside the SDK and uses them to improve Google services. The heatmap creates a `MapView` (`GoogleMapsHeatmapCard.kt`). The user can never open the map, so this is optional. Google keeps the metrics, so it is not ephemeral. Google uses them for Google’s own services, not only on the developer’s instructions, so the service-provider exemption does not apply and the row is shared.

**Firebase Crashlytics is not included.** `app/build.gradle.kts` sets `FIREBASE_CONFIGURED` to `false`, does not apply the Google services or Crashlytics plugins, and does not depend on the Firebase BoM. `CrashReporting.kt` only writes a redacted logcat line. `:app:verifyNoFirebaseCrashReporting` fails the build if that changes. A `google-services.json` on the build machine does not turn it back on. Do not tick a second crash-log source for Firebase.

### Diagnostics

Collected and shared, not ephemeral, optional, purpose Analytics.

Two SDKs:

- Maps SDK request metadata (device model, OS, SDK version, internal usage-attribution id) and IP address, from the same Google disclosure. Sent when the map is opened.
- ML Kit, from [Prepare for Google Play’s data disclosure requirements (ML Kit)](https://developers.google.com/ml-kit/android-data-disclosure). “Data collected in all features” includes device and app info, performance, API configuration, input/output size, feature version, event type, and error codes, for diagnostics and usage analytics. The document scanner is `play-services-mlkit-document-scanner` (`DocumentScannerService.kt`, `SCANNER_MODE_FULL`). Latin text recognition is the bundled `text-recognition` dependency (`OCRService.kt`). Google’s page says this diagnostic data is not passed to further third parties. The Maps metadata **is** used to improve Google services, so the combined Diagnostics row is still **shared**.

Both features are optional. The core journal does not open the map or the scanner.

### App interactions

Collected and shared, not ephemeral, optional, purpose Analytics.

Google’s Maps disclosure says map camera interaction (pan and zoom) is collected when Camera APIs are used. `GoogleMapsHeatmapCard` calls `CameraUpdateFactory`. Same optional / not ephemeral / shared answers as the other Maps rows.

### Device or other IDs

Collected and shared, not ephemeral, optional, purpose Analytics.

- Maps SDK identifier (pseudonymous daily-active measurement) and the internal usage-attribution identifier, from Google’s Maps disclosure.
- ML Kit per-installation identifiers and, for the Play-services document scanner, a device identifier used for diagnostics. Google’s ML Kit page says these are not passed to further third parties. The Maps identifier is shared with Google for Google’s measurement, so this row is **shared**.

No advertising ID. The merged manifest does not request `com.google.android.gms.permission.AD_ID`. There is no ads SDK.

## 3. Data types to leave unticked

| Play data type | Answer | Why |
| --- | --- | --- |
| Personal info → Name | No | Optional first-run name stays on the device. |
| Personal info → Email address | No | Google Sign-In was removed. The app never reads an email address. |
| Personal info → User IDs, Address, Phone, Race, Political, Sexual, Other | No | Not sent off the device as those types. There are no accounts. |
| Financial info → all four types, including Purchase history and User payment info | No | Rates, paychecks, and diesel stay in the journal on the phone. See billing below. Card numbers never reach the app (`BillingManager.kt` talks to Play Billing only). |
| Health, Contacts, Calendar, SMS, Photos, Videos, Audio, Voice, Files, Web browsing, Search history, Installed apps | No | Not collected as those types. Photos are section 5. |
| App activity → In-app search history, Installed apps, Other user-generated content, Other actions | No | Map gestures are already under App interactions. |
| Crash logs from Firebase | No | SDK removed. Do not add a Firebase row. |
| Device or other IDs → Advertising ID | No | Permission not in the merged manifest. |

## 4. Transfers that are real, and are not ticked

These leave the device. They are not ticked because a Play exemption applies. The privacy policy describes each one. If a review rejects an exemption, tick the types in the “If the exemption is rejected” column instead of arguing the overview back to No.

### Telegram — user’s own bot

Not ticked. Not end-to-end encrypted.

`TelegramApi` calls `https://api.telegram.org/bot…` (`TelegramApi.kt`). The user supplies the token. Status and stats replies include load count, paycheck count, diesel count, and weekly income and miles (`TelegramStatusMessages.kt`). `sendDocument` can upload a file the user exports. Bot API messages are readable by Telegram. Once a token is saved, `TelegramSyncWorker` can long-poll that bot in the background via WorkManager, including after boot (`TelegramBootReceiver`), so incoming chats can be imported into the local journal. This is not an `android.app.Service` and not a data-sync foreground service.

The sharing exemption is the specific user action: they created the bot, paired the chat, and sent the command, and they expect the reply in that chat. The same idea as Play’s “user sends the data to a service they chose.” The collection FAQ that names Drive does not name Telegram. Treat Telegram as that chosen-service case only because the bot is theirs and the policy says so. Do not call it end-to-end encryption.

If the exemption is rejected, tick Financial info → Other financial info and App activity → Other user-generated content. Collected Yes, Shared Yes (Telegram), Ephemeral No, Optional Yes, Purpose App functionality, Encrypted in transit Yes.

### Share sheet and feedback mail

Not ticked. The user picks the destination (`LoadExporter`, `ShareHelper`, `ImprovementFeedbackSender`). Play’s example for this exemption is sending a file or an email the user initiated.

### Play Billing

Not ticked. Play’s FAQ: you do not declare card data the payment service collects directly from the user when the app never reads it. `BillingManager` uses `BillingClient` for `premium_monthly_subscription`. The purchase token is not sent to a developer server. A local subscribed flag is on-device only.

### Tesseract language download

Not ticked. `TessDataManager` downloads `eng` and `rus` traineddata from `https://github.com/tesseract-ocr/tessdata_fast`. No journal data is uploaded.

## 5. Photos — the stamp drivers install the app for

Do **not** tick Photos or Videos. Do **not** describe the stamp as a privacy risk.

GPS, date, and time on a photo are the product. The driver takes the photo when they choose to, so they can show their trucking company where and when it was taken. Nothing stamps a photo in the background.

`CameraViewModel` reads EXIF only to fix rotation, then `PhotoManager.savePhoto` draws the capture time and, when location was allowed, the city, state, ZIP, and coordinates onto a new JPEG. `PhotoRepository.savePhoto` stores latitude, longitude, city, state, ZIP, and timestamp on `PhotoEntity`. The file stays in app storage. On-device processing is not collection under Play’s definition.

Local backup files are written only where the user saves them (Android file picker) or shares them. `MediaSyncQueueDao` has no production caller.

The stamped photo leaves the phone only when the driver shares or exports it (section 4). That is user-initiated sharing: the user expects the photo, stamp included, to go to the destination they pick. It is not ticked as collection or sharing.

## 6. On-device only (not collection)

| Data | Where | Leaves the device? |
| --- | --- | --- |
| Journal | Room + SQLCipher | Only via section 4 |
| Photo stamp (GPS, date, time) | On the JPEG and in `PhotoEntity` | Only when the user shares or exports that photo |
| First-run name | Device prefs | No |
| Backup file (zip/JSON with media) | Saved by the user through the Android file picker, or shared | Only where the user saves or shares it (user-initiated) |
| Telegram token | Encrypted prefs | Only to `api.telegram.org` when the user configured the bot |
| Crowd RPM | `CrowdRpmShareGate.payloadOrNull` always returns null | No network publish |
| In-app “Analytics” charts | Computed from Room | No telemetry SDK |
| Biometrics | Android BiometricPrompt | No |
| Crash logs the app itself records | `CrashReporting.recordException` → logcat | No |

`android:allowBackup="false"`. System backup is not a hidden copy of the journal.

## 7. Permissions and other Console forms

Merged manifest permissions that matter for the photo, location, and data-safety forms:

| Permission | Declare? |
| --- | --- |
| `CAMERA` | Yes. Photos and scans. Optional for the journal. |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Yes. Foreground only, when the user takes a photo (stamp) or adds a diesel fill (prefill the station/address). The map does not use the driver’s GPS. No background location. |
| `POST_NOTIFICATIONS` | Yes. Local reminders. |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Used for the optional network features and Play billing. |
| `com.android.vending.BILLING` | Play subscription sheet. |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | Optional unlock. Not a data-safety type. |
| `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` | Local reminders. |
| `WAKE_LOCK` | Merged from a library. Not a data-safety type. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | **Removed** with `tools:node="remove"`. Do not submit a data-sync foreground-service declaration. `TelegramBotForegroundService` is not an `android.app.Service`. |
| `AD_ID` | Not in the merged manifest. Ads declaration: **No**. |
| `RECORD_AUDIO` | Not in the manifest. Unused microphone strings remain in resources. A leftover `<queries>` entry for `android.speech.RecognitionService` is still in the manifest; the app does not start speech recognition. |

Ads: **No**. Monetization is the optional subscription only. Financial-features form: this is a journal, not banking, loans, or crypto.

## 8. What changed in the answers

| Question | Before | After |
| --- | --- | --- |
| Collect or share any required user data types? | No | **Yes** |
| Data shared with third parties? | No | **Yes** for Maps SDK identifiers, crash metrics, diagnostics, and map interactions, and for the geocoder lookup. Email is not collected (Google Sign-In removed in 1.6.2). Telegram, share sheet, backup files, and Play card data stay off the ticked list under the exemptions in section 4. |
| Crash logs | Only if `google-services.json` was present | **Yes**, Maps SDK only. Firebase crash reporting is **No** and cannot be turned on by that file. |
| Location | “On device, not sold” | **Precise and approximate: Yes, shared, ephemeral, optional — fused location plus geocoder, for the photo stamp and for prefilling a diesel place.** The photo stamp itself is on-device and is not this row. The map does not use the driver’s GPS. |
| Photos | “Leave only if the user exports” | **Not ticked.** The stamp (GPS, date, time) is the feature. It is written on the phone when the user takes the photo, and it leaves only when the user shares or exports that photo. |
| Encryption in transit | Yes | **Yes** |
| Users can request deletion | Yes, uninstall and “delete the Drive file” | **Yes**. In-app “Delete all data”, uninstall, or `truckorig@gmail.com` for support mail. Delete-data URL: https://khaidarsafarov-droid.github.io/MyFinance/delete-data/ |
| Account creation / outside-account login | OAuth (Google) for Drive / Yes | **No accounts / No.** Google Sign-In and Drive sync were removed in 1.6.2. |
| Foreground service | Declared | **Do not declare.** The permission is removed. |
| Privacy policy | Said the developer does not share with third parties, and listed a foreground service | https://khaidarsafarov-droid.github.io/MyFinance/privacy/ (updated 2026-11-25, no Drive or Google Sign-In). |

## 9. Manual steps before you submit

1. In the form and in Store settings, use https://khaidarsafarov-droid.github.io/MyFinance/privacy/ as the privacy policy URL (the old rentry page still describes Drive). The form’s URL and the in-app story have to match the page users open.
2. Enter this sheet in the Data safety form, including the account questions in section 1 (no accounts; outside-account login **No**). Save as draft. Do not send the app for review from this change.
3. Upload the 1.6.2 AAB (`versionCode` 20) together with the new answers: the old answers about Google login only fit builds that still had Drive sync.
4. In the photo and location declaration forms, say camera and foreground location are optional and user-initiated. The stamp (GPS, date, and time) is the feature drivers install the app for. It is not background collection.
5. Do not upload an AAB that was built from an older commit that still depended on Crashlytics. Confirm the merged release manifest has no `FirebaseInitProvider` and no `FirebaseCrashlytics` registrar.
6. There is no postal address in the repo. If the Console account form requires one, use your real address. Do not invent one.
