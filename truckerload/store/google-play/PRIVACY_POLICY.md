# TruckoRig — Privacy Policy (Play Store)

**Last updated:** 2026-08-28  
**App:** TruckoRig (`com.truckorig`)

This policy describes what TruckoRig collects and how it is used. Host this page on a public HTTPS URL and paste that URL into Play Console → App content → Privacy policy.

## Summary

TruckoRig is a **local-first** trucking journal. Core load, paycheck, and diesel data are stored on your device. Optional cloud features (Google Sign-In, Google Drive backup, backend sync, Telegram) only run when you configure them.

## Data we process

| Category | Examples | Where stored | When |
| --- | --- | --- | --- |
| Account | Google account id / email, or email auth | Device; optional Supabase Auth | Sign-in |
| Journal | Loads, stops, paychecks, diesel, goals | On-device Room database | Always (local) |
| Media | Photos, scans attached to loads | Device storage; optional cloud media | When you capture/attach |
| Location | GPS for photo watermark / map | Device; not sold | Only with permission + feature use |
| Telegram | Bot token, chat id, message text you forward | Device prefs / inbox | Only if you set up the bot |
| Diagnostics | Crash logs (if Firebase is configured) | Firebase Crashlytics | Optional build config |

## Permissions

- **Camera** — capture load/diesel photos and document scans  
- **Location** — geotag photos / map features (optional)  
- **Notifications** — load reminders and Telegram sync status  
- **Internet** — Google Sign-In, Drive, optional backend, Telegram API, maps tiles  
- **Foreground service (data sync)** — optional on-device Telegram long-poll  

You can revoke permissions in system settings. Denying optional permissions does not block the core journal.

## Sharing

We do **not** sell personal data. Data leaves the device only when you:

1. Sign in with Google / use Google Drive backup  
2. Enable cloud sync against your configured backend  
3. Use Telegram with a bot token you provide  
4. Explicitly export/share files (CSV, PDF, backup)

## Children

TruckoRig is not directed at children under 13.

## Contact

Use in-app Settings feedback or the developer contact email on the Play Store listing.

## Changes

We may update this policy; the “Last updated” date will change. Continued use after an update means you accept the revised policy.
