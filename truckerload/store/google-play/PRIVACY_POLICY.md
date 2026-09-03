# TruckoRig — Privacy Policy (Play Store)

**Last updated:** 2026-09-03  
**App:** TruckoRig (`com.truckorig`)  
**Public URL:** host `docs/privacy/index.html` (this Markdown is the source text).

This policy describes how TruckoRig handles data. The developer does **not** collect personal data on developer servers and does **not** share it with third parties.

## Summary

TruckoRig is a **local-first** trucking journal. Core load, paycheck, and diesel data are stored on your device. Optional features (Google Drive backup, Telegram) only run when you configure them.

## Data processed on the device

| Category | Examples | Where stored | When |
| --- | --- | --- | --- |
| Profile | Optional first-run name | Device | First launch |
| Journal | Loads, stops, paychecks, diesel, goals | On-device Room database | Always (local) |
| Media | Photos, scans attached to loads | Device storage | When you capture/attach |
| Location | GPS for photo watermark / map | Device; not sold | Only with permission + feature use |
| Telegram | Bot token, chat id you enter | Device prefs | Only if you set up the bot |

## Permissions

- **Camera** — capture load/diesel photos and document scans
- **Location** — geotag photos / map features (optional)
- **Notifications** — load reminders and Telegram sync status
- **Internet** — optional Google Drive backup, Telegram API, maps tiles
- **Foreground service (data sync)** — optional on-device Telegram long-poll

You can revoke permissions in system settings. Denying optional permissions does not block the core journal.

## Sharing

We do **not** sell personal data. The developer does not operate a backend that receives your journal. Data leaves the device only when you:

1. Use optional Google Drive backup (copy goes to **your** Drive)
2. Use Telegram with a bot token you provide
3. Explicitly export/share files (CSV, PDF, backup)
4. Open the map (Google Maps tiles)

Crash reports are not sent unless a release build includes `google-services.json`.

## Children

TruckoRig is not directed at children under 13.

## Contact

Khaidar Safarov — khaidar.safarov@gmail.com  
Or in-app Settings feedback.

## Changes

We may update this policy; the “Last updated” date will change. Continued use after an update means you accept the revised policy.
