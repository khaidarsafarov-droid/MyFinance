# Auth entry & stay signed in

Truck Log requires a signed-in session before the main UI. After the **first** successful login, the session is stored on device and the next launches open the app **without** asking again (until the user taps Logout).

| Platform | Providers |
|----------|-----------|
| Android (current) | **Google Sign-In** or **email + password** |
| iOS (planned) | **Sign in with Apple** / iCloud |

## First launch

1. User picks Google **or** creates/signs in with email + password.
2. Identity (+ tokens when Supabase is configured) is written to encrypted prefs (`AuthStore`).
3. Room DB is opened for that account id — local-first data stays on device.

## Later launches

1. Cold start restores `is_logged_in` + `user_id` + provider from encrypted prefs → **no login UI**.
2. `SilentAuthRestorer` refreshes tokens in the background when online (never shows Google sheet).
3. Offline → soft banner, app keeps working on Room.

## Logout

Drawer / Settings → Logout clears the stored session; next open shows the login screen again.

## Config

```
LOCAL_ONLY_MODE=false
GOOGLE_WEB_CLIENT_ID=<Web OAuth client>
# optional cloud Auth:
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
```

`LOCAL_ONLY_MODE=true` does **not** skip login; it only disables cloud workers / Supabase client.

`GOOGLE_WEB_CLIENT_ID` falls back to the project Web client in `app/build.gradle.kts`
when omitted from `local.properties`. Android OAuth client (package `com.truckerload` +
APK signing SHA-1) must still be registered in Google Cloud Console — ApiException **10**
means the SHA-1 does not match. Exact fingerprints and Cloud Console steps:
`docs/GOOGLE_SIGNIN_SETUP.md`.
