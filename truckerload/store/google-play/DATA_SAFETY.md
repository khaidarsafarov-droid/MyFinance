# Play Console — Data safety answer sheet

Fill **Play Console → Policy → App content → Data safety** for `com.truckorig` from this sheet. Do not submit from this file.

Definitions used here are Google’s, from [Provide information for Google Play’s Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469) (collected = sent off the device by the app **or by an SDK**, to the developer **or** a third party). On-device processing is not collection. End-to-end encryption (only sender and recipient can read it) is not collection. A user-initiated transfer the user reasonably expects is not **sharing**; Google’s Drive/Dropbox FAQ also says a backup the user sends to their own cloud account is not **collection** when the developer does not take a copy. Ephemeral processing (memory only, only long enough to answer that request, not used to build a profile) is still answered on the form, and Play hides it on the store listing.

Audit date: 2026-11-23. Package `com.truckorig`, versionName 1.6.0, versionCode 18. Merged debug manifest (before this change) had no `FOREGROUND_SERVICE`, no `FOREGROUND_SERVICE_DATA_SYNC`, and no `AD_ID`. It did embed `FirebaseInitProvider` because the Crashlytics SDK was a normal dependency. That SDK is removed; see Crash logs below.

## 1. Overview

| Form question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **Yes** |
| Independent security review (MASA) | **No** |
| Committed to the Families policy | **No** — not a children’s app |
| Privacy policy URL | https://rentry.co/truckorig-privacy-policy — paste the current `PRIVACY_POLICY.md` there before you submit the form. The repo cannot update rentry. |

**Why the overview is Yes, not No.** The previous draft answered No while also describing Drive, Telegram, and Maps. Those three are not the same kind of transfer:

- Maps SDK and ML Kit send diagnostics off the device to Google by themselves. That is collection (and, for Maps, sharing). It does not meet the on-device, end-to-end, or “user’s own Drive” exemptions.
- Drive backup and Telegram are optional and user-configured. They are explained in section 4 and are **not** ticked as collected types, for the reasons in that section.
- The developer still has no server that receives the journal.

## 2. Data types to tick

Tick only the rows in this section. For each, Play asks collected vs shared, ephemeral, optional vs required, and purposes.

| Data type | Collected | Shared | Ephemeral | Optional or required | Purposes |
| --- | --- | --- | --- | --- | --- |
| Location → Precise location | Yes | Yes | Yes | Optional | App functionality |
| Location → Approximate location | Yes | Yes | Yes | Optional | App functionality |
| Personal info → Email address | Yes | No | No | Optional | App functionality, Account management |
| App info and performance → Crash logs | Yes | Yes | No | Optional | Analytics |
| App info and performance → Diagnostics | Yes | Yes | No | Optional | Analytics |
| App activity → App interactions | Yes | Yes | No | Optional | Analytics |
| Device or other IDs → Device or other IDs | Yes | Yes | No | Optional | Analytics |

Encryption in transit for every row: **Yes** (HTTPS). `network_security_config.xml` sets `cleartextTrafficPermitted="false"`.

### Precise location

Collected and shared, ephemeral, optional, purpose App functionality.

The manifest requests `ACCESS_FINE_LOCATION` (`AndroidManifest.xml`). `LocationHelper.getCurrentLocation` reads a fused fix and calls `Geocoder.getFromLocation` (`LocationHelper.kt`). On a Play-services phone that lookup leaves the device for Google, which is why this row is shared. The app uses the result to fill city, state, and ZIP for that photo. It does not send the coordinate to a TruckoRig server, and it does not build an ad profile, so the form answer is ephemeral. The user can refuse location and still use the journal, so it is optional. There is no `ACCESS_BACKGROUND_LOCATION`.

The coordinates are also stored on the phone (next section). That on-device copy is not itself collection.

### Approximate location

Collected and shared, ephemeral, optional, purpose App functionality.

The manifest also requests `ACCESS_COARSE_LOCATION`. `LocationHelper.hasLocationPermission` accepts a coarse-only grant, and the same fused call uses `PRIORITY_BALANCED_POWER_ACCURACY`. A coarse fix is approximate location under Play’s “3 square kilometers or coarser” definition. Same geocoder path, same ephemeral and optional answers.

Do **not** also tick approximate location just because the Maps SDK collects an IP address. Google’s Maps disclosure says the IP is used to understand SDK usage, not that the SDK infers a place from it. IP-related SDK data is covered by Diagnostics and Device or other IDs.

### Email address

Collected, not shared, not ephemeral, optional, purposes App functionality and Account management.

`GoogleSignInClients` uses `DEFAULT_SIGN_IN` and `requestEmail()` only to connect Drive (`GoogleSignInClients.kt`). The email is stored in Drive prefs on the device (`GoogleDriveBackupService.linkAccountEmail`). It is not sent to a developer server and it is not the journal login. Sign-in is a specific action the user takes, and they expect Google to handle that Google account, so it is not declared as sharing. The app keeps the address until disconnect, so it is not ephemeral. The journal works without Drive, so it is optional.

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
| Personal info → User IDs, Address, Phone, Race, Political, Sexual, Other | No | Not sent off the device as those types. The local account id is inside the user’s own Drive file (section 4). |
| Financial info → all four types, including Purchase history and User payment info | No | See billing and the Drive exemption below. Card numbers never reach the app (`BillingManager.kt` talks to Play Billing only). |
| Health, Contacts, Calendar, SMS, Photos, Videos, Audio, Voice, Files, Web browsing, Search history, Installed apps | No | Not collected as those types. Photos are section 5. |
| App activity → In-app search history, Installed apps, Other user-generated content, Other actions | No | Map gestures are already under App interactions. |
| Crash logs from Firebase | No | SDK removed. Do not add a Firebase row. |
| Device or other IDs → Advertising ID | No | Permission not in the merged manifest. |

## 4. Transfers that are real, and are not ticked

These leave the device. They are not ticked because a Play exemption applies. The privacy policy describes each one. If a review rejects an exemption, tick the types in the “If the exemption is rejected” column instead of arguing the overview back to No.

### Google Drive backup — user’s own account

Not ticked. Not end-to-end encrypted.

`GoogleDriveBackupService` uploads the JSON from `BackupSnapshotBuilder` to `https://www.googleapis.com/drive/v3/files` and `https://www.googleapis.com/upload/drive/v3/files`, space `appDataFolder`, scope `https://www.googleapis.com/auth/drive.appdata` (`GoogleDriveApiClient.kt`, `GoogleDriveBackupPrefs.kt`). The JSON contains loads (including rates, miles, stop text, and `rawMessage`), paychecks, diesel, maintenance, and settings (`BackupData.kt`). It does **not** contain photo files.

Play’s FAQ: if the user chooses to upload to their own Drive or similar account, the upload is governed by that service’s terms, and the developer does not take a copy, it is not declared as collection. This upload matches that FAQ on the destination and on “no developer copy.” It does **not** match “the app never accesses the data”: the app builds the JSON. It is also a hidden app-data folder, not a file the user sees in My Drive, and `disconnect()` signs out and clears local prefs without deleting the remote file (`GoogleDriveBackupService.kt`). The policy tells the user how to delete the hidden file.

End-to-end encryption does **not** apply. Google can read the JSON. HTTPS only means encrypted in transit.

After the user connects Drive, `BackupService.createAutoBackup` can call `pushAutoBackupIfEnabled` on a later edit. That is still the backup they turned on, not a new silent destination.

If the exemption is rejected, tick: Financial info → Other financial info (rates, paychecks, diesel), App activity → Other user-generated content (load text), Location → Approximate location (city/state text in stops), Personal info → User IDs (account id). Collected Yes, Shared No (user’s own Google account), Ephemeral No, Optional Yes, Purpose App functionality, Encrypted in transit Yes.

### Telegram — user’s own bot

Not ticked. Not end-to-end encrypted.

`TelegramApi` calls `https://api.telegram.org/bot…` (`TelegramApi.kt`). The user supplies the token. Status and stats replies include load count, paycheck count, diesel count, and weekly income and miles (`TelegramStatusMessages.kt`). `sendDocument` can upload a file the user exports. Bot API messages are readable by Telegram.

The sharing exemption is the specific user action: they created the bot, paired the chat, and sent the command, and they expect the reply in that chat. The same idea as Play’s “user sends the data to a service they chose.” The collection FAQ that names Drive does not name Telegram. Treat Telegram as that chosen-service case only because the bot is theirs and the policy says so. Do not call it end-to-end encryption.

If the exemption is rejected, tick Financial info → Other financial info and App activity → Other user-generated content. Collected Yes, Shared Yes (Telegram), Ephemeral No, Optional Yes, Purpose App functionality, Encrypted in transit Yes.

### Share sheet and feedback mail

Not ticked. The user picks the destination (`LoadExporter`, `ShareHelper`, `ImprovementFeedbackSender`). Play’s example for this exemption is sending a file or an email the user initiated.

### Play Billing

Not ticked. Play’s FAQ: you do not declare card data the payment service collects directly from the user when the app never reads it. `BillingManager` uses `BillingClient` for `premium_monthly_subscription`. The purchase token is not sent to a developer server. A local subscribed flag is on-device only.

### Tesseract language download

Not ticked. `TessDataManager` downloads `eng` and `rus` traineddata from `https://github.com/tesseract-ocr/tessdata_fast`. No journal data is uploaded.

## 5. Photos — on the device, location and time kept on purpose

Do **not** tick Photos or Videos.

Taking a photo decodes the camera file, reads EXIF only to fix rotation (`CameraViewModel.decodeBitmap`), and writes a new JPEG. `PhotoManager.savePhoto` draws the capture time, city, state, ZIP, and GPS coordinates onto the image. `PhotoRepository.savePhoto` stores latitude, longitude, city, state, ZIP, and timestamp on `PhotoEntity`. That is intentional. The app does not strip place, date, or time off the photo.

The image file stays in app storage (`PhotoManager` writes under the app’s external `photos` directory). `BackupSnapshotBuilder` does not put photo bytes in the Drive JSON. `MediaSyncQueueDao` has no production caller, so there is no cloud media upload.

The watermarked image leaves the phone only if the user shares it or exports it (section 4). The place, date, and time are visible on that image.

## 6. On-device only (not collection)

| Data | Where | Leaves the device? |
| --- | --- | --- |
| Journal | Room + SQLCipher | Only via section 4 |
| First-run name | Device prefs | No |
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
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Yes. Foreground only. Photo place and address labels. |
| `POST_NOTIFICATIONS` | Yes. Local reminders. |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Used for the optional network features and Play billing. |
| `com.android.vending.BILLING` | Play subscription sheet. |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | Optional unlock. Not a data-safety type. |
| `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` | Local reminders. |
| `WAKE_LOCK` | Merged from a library. Not a data-safety type. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | **Removed** with `tools:node="remove"`. Do not submit a data-sync foreground-service declaration. `TelegramBotForegroundService` is not an `android.app.Service`. |
| `AD_ID` | Not in the merged manifest. Ads declaration: **No**. |
| `RECORD_AUDIO` | Not in the manifest. The privacy screen can show a microphone row; the app does not request the microphone. |

Ads: **No**. Monetization is the optional subscription only. Financial-features form: this is a journal, not banking, loans, or crypto.

## 8. What changed in the answers

| Question | Before | After |
| --- | --- | --- |
| Collect or share any required user data types? | No | **Yes** |
| Data shared with third parties? | No | **Yes** for Maps SDK identifiers, crash metrics, diagnostics, and map interactions, and for the geocoder lookup. Email is collected and not shared. Drive, Telegram, share sheet, and Play card data stay off the ticked list under the exemptions in section 4. |
| Crash logs | Only if `google-services.json` was present | **Yes**, Maps SDK only. Firebase crash reporting is **No** and cannot be turned on by that file. |
| Location | “On device, not sold” | **Precise and approximate, collected, ephemeral, optional.** On-device photo GPS is separate and is kept on purpose. |
| Photos | “Leave only if the user exports” | Still **not ticked**. Files stay on the phone, including GPS, date, and time. Drive JSON does not include the files. |
| Encryption in transit | Yes | **Yes** |
| Users can request deletion | Yes, uninstall and “delete the Drive file” | **Yes**. In-app account delete, uninstall, or `truckorig@gmail.com`. The Drive copy is a hidden app-data file; disconnect does not delete it. |
| Foreground service | Declared | **Do not declare.** The permission is removed. |
| Privacy policy | Said the developer does not share with third parties, and listed a foreground service | Must match `PRIVACY_POLICY.md` dated 2026-11-23 before you submit. |

## 9. Manual steps before you submit

1. Paste `PRIVACY_POLICY.md` over https://rentry.co/truckorig-privacy-policy (or replace that URL with the hosted `docs/release/privacy/index.html`). The form’s URL and the in-app story have to match the page users open.
2. Enter this sheet in the Data safety form. Save as draft. Do not send the app for review from this change.
3. In the photo and location declaration forms, say foreground location and camera are optional and that photo GPS, date, and time are kept on the device on purpose.
4. Do not upload an AAB that was built from an older commit that still depended on Crashlytics. Confirm the merged release manifest has no `FirebaseInitProvider` and no `FirebaseCrashlytics` registrar.
5. There is no postal address in the repo. If the Console account form requires one, use your real address. Do not invent one.
