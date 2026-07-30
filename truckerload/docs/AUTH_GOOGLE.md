# Auth entry (Android)

Truck Log requires a signed-in session before the main UI.

| Platform | Provider |
|----------|----------|
| Android (current) | **Google Sign-In** (Credential Manager + legacy fallback) |
| iOS (planned) | **Sign in with Apple** / iCloud |

## Behavior

- Cold start without a **Google** session → login screen only.
- Old `local_dev` / email-only on-device sessions are cleared and must re-auth with Google.
- After Google login, Room remains local-first (per-user DB).
- Optional Supabase: when URL + anon key are set and `LOCAL_ONLY_MODE=false`, Google ID token is exchanged for a Supabase JWT (needed for friends live sync). If Supabase is down, Google identity still logs in locally.

## Config

```
LOCAL_ONLY_MODE=false
GOOGLE_WEB_CLIENT_ID=<Web OAuth client>
# optional for cloud Auth + friends:
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
```

`LOCAL_ONLY_MODE=true` no longer skips login; it only turns off cloud workers / Supabase client.
