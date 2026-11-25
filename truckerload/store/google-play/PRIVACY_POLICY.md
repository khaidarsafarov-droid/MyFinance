# TruckoRig — Privacy Policy

**Last updated:** 25 November 2026  
**App:** TruckoRig (`com.truckorig`), Android  
**Developer:** Khaidar Safarov  
**Contact:** [truckorig@gmail.com](mailto:truckorig@gmail.com)  
**Public URL (use in Play Console):** https://khaidarsafarov-droid.github.io/MyFinance/privacy/ (GitHub Pages, `docs/privacy/index.html`)  
**Delete data:** https://khaidarsafarov-droid.github.io/MyFinance/delete-data/

The old rentry page (https://rentry.co/truckorig-privacy-policy) still describes Google Drive backup. Do not use it any more.

**Postal address:** none is stored in this repository. **TODO for Khaidar Safarov:** add a real mailing address here only if you want it published. Do not invent one.

This policy describes the Android app. The separate TruckoRig iPhone app also has no accounts and no email or Google sign-in; its optional copy goes to iCloud Drive under the Apple ID already signed in on the iPhone.

## Summary

TruckoRig is a local-first trucking journal. Loads, paychecks, diesel, and photos stay on your phone. The app has **no accounts and no sign-in** (no email login, no Google sign-in), and since version 1.6.2 it has **no Google Drive sync**. The developer does not run a server that receives your journal. The app does not show ads and does not sell personal data.

### Your data belongs only to you

- Only you have access to your data and full control over it.
- We cannot view, change, or delete your data. It lives only on your phone and in backup files you save yourself.
- Built-in Google components (Maps, the ML Kit document scanner, and the address lookup for the photo stamp) send Google technical diagnostics, crash reports, and device identifiers, and for the address lookup the coordinates at that moment. Google handles this under its own policy; your journal, photos, and amounts are not included.
- You can only write to us at [truckorig@gmail.com](mailto:truckorig@gmail.com) to suggest changes to the app or new features.
- Only you can delete your data: with “Delete all data” in Settings, or by following the delete-data page.

Some optional features, and Google libraries that ship inside the app, do send data off the phone. Each case is listed below, with a link to that company’s policy.

## Data that stays on the phone

| Category | Examples | Where |
| --- | --- | --- |
| Profile | Optional name from the first launch | This phone |
| Journal | Loads, stops, paychecks, diesel, goals, maintenance, tax notes | On-device Room database, encrypted with SQLCipher |
| Photos and scans | Image files, recognized text | App storage on this phone |
| Photo stamp | GPS, date, and time the driver asked to put on a photo | On the photo and in the photo record, on this phone |
| Telegram | Bot token and chat id you type | Encrypted preferences on this phone |
| Backup file | A zip with the journal and photos | Only where you save it with the Android file picker, or where you send it |
| Subscription | A local flag that Premium is active | This phone |

Android system backup of the app is off. Biometric unlock, if you turn it on, stays on the phone. The in-app Analytics screens are your own charts. They are not sent to the developer.

### Photo stamp

Putting location, date, and time on a photo is a core feature. Drivers use it so they can show their trucking company where and when a photo was taken. It happens only when you take the photo. It does not run in the background.

The stamp is drawn onto the picture and saved with the photo on this phone. The developer does not receive it. The stamped photo leaves the phone only when you share or export it, or save a backup file, yourself. That share is your action.

## When data leaves the phone

The app blocks unencrypted http. The transfers below use HTTPS.

### Google Maps

Opening the map is optional. Google’s Maps SDK then sends Google device and request details, a pseudonymous Maps identifier, your IP address, map gestures such as pan and zoom, and crash metrics from inside the Maps SDK. The developer does not receive those. The map does not call Google’s Directions API.

- Google privacy policy: https://policies.google.com/privacy
- What the Maps SDK lists: https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure

### City line on the stamp, and the diesel place field

Location is optional and foreground-only. The app does not ask for background location, and opening the map does not send your GPS.

If you allow location, the app can ask Android’s geocoder in two cases: when you take a photo, so the stamp can include city, state, and ZIP; and when you add a diesel fill, so the station or address field can be prefilled. On a phone with Google Play services, Google answers those lookups and does not keep a copy for TruckoRig. You can refuse location and still take photos or add diesel: the stamp then has the time without a place, and the diesel field stays empty for you to type.

### ML Kit scanner and text recognition

The document scanner and Latin text recognition are optional. Google’s ML Kit processes the image on the device and sends Google diagnostic details (device, app, performance, error codes), not a TruckoRig copy of the document.

- Google privacy policy: https://policies.google.com/privacy
- ML Kit disclosure: https://developers.google.com/ml-kit/android-data-disclosure

The first time offline recognition needs language files, the app downloads English and Russian data from GitHub. That download does not upload your journal.

- GitHub privacy statement: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement

### Telegram

Optional. If you paste your own bot token, the app calls Telegram’s Bot API at `https://api.telegram.org`. Once the token is saved, the app can poll that bot in the background (this is not an Android foreground service). A status command can send counts, weekly income, and miles back to your chat. A file you export can be sent as a document. Incoming chats can be imported into the journal on this phone. Telegram can read Bot API messages. They are not end-to-end encrypted. The token stays on the phone. The developer does not run the bot.

- Telegram privacy policy: https://telegram.org/privacy

### Export, share, and feedback

A CSV, PDF, backup file, or feedback email leaves the phone when you save it or pick an app in the share sheet or your mail app.

### Google Play billing

TruckoRig is free to install. An optional subscription, TruckoRig Premium (`premium_monthly_subscription`), is sold by Google Play. The first month can be free (`free-month`). After that, Google Play charges the monthly price — **$2.99 in the United States**; other countries use the local Play price. It renews until you cancel in Google Play → Payments and subscriptions. Uninstalling the app does not cancel it.

Google processes the payment. The app does not receive your card number and does not send the purchase token to a developer server.

- Google privacy policy: https://policies.google.com/privacy

## What we do not do

- We do not have accounts, a login, or cloud sync. The app does not sign in to Google and does not read your email address.
- We do not sell personal data and we do not show ads. The app does not request the advertising ID.
- We do not send Firebase Crashlytics reports, Firebase session data, or any other Firebase crash log. A `google-services.json` file on a build machine does not turn that on.
- We do not run a foreground service. The manifest removes the foreground-service permissions. Telegram polling is not an Android foreground service.
- We do not upload “crowd” rates. The map uses your on-device loads only.
- We do not request the microphone.

## Permissions

- **Camera** — photos and scans, when you take them
- **Location (foreground, fine and coarse)** — when you take a photo, so the stamp can include the place, or when you add a diesel fill, to prefill the station or address. The map does not use your GPS. No background location.
- **Notifications** — local reminders
- **Internet** — the optional features above, and Play billing
- **Exact alarms and boot** — local reminders after restart
- **Biometrics** — optional unlock on this phone
- **Storage on older Android versions** — saving an export you asked for

You can deny camera, location, and notifications in Android settings. The journal still opens.

## How long data is kept

- **On the phone:** until you tap “Delete all data” in the app, clear the app’s storage, or uninstall.
- **Backup files you saved:** until you delete them. The developer does not hold a copy.
- **Old Google Drive copy (only versions before 1.6.2):** a hidden file in your own Google Drive until you delete hidden app data there. The developer does not hold a copy.
- **Telegram:** under Telegram’s policy. The developer does not hold a copy.
- **Maps and ML Kit diagnostics:** under Google’s policy. The developer does not receive them.
- **Developer servers:** no journal is stored there.

## How to delete data

- In the app, Settings → Delete all data. That erases the on-device journal, photos, scans, and saved Telegram token.
- Or clear the app’s storage, or uninstall. Uninstalling does not cancel Premium.
- Delete backup files where you saved them.
- Only if an older version (before 1.6.2) backed up to Google Drive: in Google Drive on the web, open Settings → Manage apps → TruckoRig and delete hidden app data. Google’s steps: https://support.google.com/drive/answer/2523073 . Connected apps: https://myaccount.google.com/connections
- Step-by-step page: https://khaidarsafarov-droid.github.io/MyFinance/delete-data/
- Telegram: remove the token in Settings, and delete the chat in Telegram if you want those messages gone.
- Cancel Premium in Google Play → Payments and subscriptions.
- Privacy requests: [truckorig@gmail.com](mailto:truckorig@gmail.com) or in-app Settings feedback. The developer cannot delete a file on your device, in your cloud storage, or a message on Telegram’s servers.

## Children

TruckoRig is not directed at children under 13. We do not knowingly collect children’s data.

## Changes

We may update this policy. The date at the top will change. Continued use after an update means you accept the revised policy.

## Contact

Khaidar Safarov — [truckorig@gmail.com](mailto:truckorig@gmail.com)

Or in-app Settings feedback.

**TODO:** postal address, only if you have a real one to publish.
