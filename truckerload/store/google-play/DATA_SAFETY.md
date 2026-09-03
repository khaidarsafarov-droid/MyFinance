# Play Console — Data safety (draft answers)

Fill Play Console → **App content** → **Data safety** using this draft.

Current production posture (after removing Ktor / Supabase / cloud sync):
the developer does **not** collect user data on developer servers and does
**not** share data with third parties. Core journal stays on-device.

## Overview answers

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** — matches the Play listing preview |
| Data shared with third parties? | **No** |
| Privacy policy URL | Public HTTPS page at `docs/privacy/index.html` |

Optional features (Drive backup, Telegram bot, Maps tiles) are **user-initiated**.
They send data to the **user’s** Google Drive, the **user’s** Telegram bot, or
Google Maps — not to a TruckoRig backend. Do **not** add Crashlytics
(`google-services.json`) to the Play AAB if you want to keep “no collection”.

## If you later enable Firebase / a backend

| Data type | Collected? | Shared? | Purpose | Optional? | Encrypted in transit? |
| --- | --- | --- | --- | --- | --- |
| Crash logs | Only if `google-services.json` present | Firebase | Stability | Build-dependent | Yes |
| App activity | Only if user enables Drive / Telegram / export | User’s Drive or Telegram | App functionality | Yes | Yes |
| Photos / videos | On device; leave device only if user exports / Drive | Only if user chooses | App functionality | Yes | Yes |
| Location | On device when permitted | Not sold | Photo geotag / map | Yes | N/A local |

## Other declarations

- **Encrypted in transit?** Yes (HTTPS) for optional network calls.
- **Users can delete data?** Yes — Android app storage / uninstall; Drive file in the user’s Drive.
- **Play Families?** No (not designed for children).
- **Independent security review?** No.

## Sensitive permissions

Declare **Camera**, **Location** (foreground), **Notifications**, and
**Foreground service** as required by the current Play form.
Use “App functionality” as the primary purpose.

## Ads / monetization

- No ads SDK
- No in-app products required for core use
