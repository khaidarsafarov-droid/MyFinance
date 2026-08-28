# Play Console — Data safety (draft answers)

Fill Play Console → **App content** → **Data safety** using this draft. Adjust if your production build enables Firebase / cloud sync differently.

## Data collection overview

| Data type | Collected? | Shared? | Purpose | Optional? | Encrypted in transit? |
| --- | --- | --- | --- | --- | --- |
| Email address | Yes (Google / email auth) | With Google / Supabase if used | Account management | Account required for cloud; local journal usable offline after setup | Yes (HTTPS) |
| User IDs | Yes | Same as above | Account management | Same | Yes |
| App activity (loads etc.) | Stored on device | Only if user enables sync/Drive/export | App functionality | Core local; sync optional | Yes when syncing |
| Photos / videos | Yes if user captures | Only if user enables media sync / shares | App functionality | Yes | Yes when syncing |
| Location | Approximate/precise when permitted | Not sold; may tag local photos | App functionality | Yes | N/A local; HTTPS if ever uploaded |
| Crash logs | Only if `google-services.json` present | Firebase | Analytics / stability | Build-dependent | Yes |

## Declarations

- **Is all user data encrypted in transit?** Yes (HTTPS for network calls).  
- **Can users request deletion?** Yes — in-app account deletion / clear local data (document the path you ship).  
- **Committed to Play Families?** No (not designed for children).  
- **Independent security review?** No (unless you commission one).

## Sensitive permissions (App content → Sensitive permissions)

Declare **Camera**, **Location** (foreground), **Notifications**, and any **Foreground service** / **Alarms** as required by the current Play form. Use “App functionality” as the primary purpose.

## Ads / monetization

- No ads SDK  
- No in-app products required for core use (update if you add billing)
