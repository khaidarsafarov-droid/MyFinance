# Auth entry & stay signed in

TruckoRig runs locally. There is no Google Sign-In for the journal.

| Platform | Identity |
|----------|----------|
| Android (current) | **First-run name** (optional skip) → `local_dev` Room on this device |
| iOS (planned) | Local journal; Sign in with Apple only if App Store requires it |

There is no Google Sign-In of any kind since 1.6.2: Google Drive sync was removed.
Backups are files the user saves from Settings → Backup file.

## First launch

1. User enters first + last name, or skips.
2. [LocalDeviceOnboarding](../app/src/main/java/com/truckerload/data/preferences/LocalDeviceOnboarding.kt)
   writes `local_dev` to encrypted prefs (`AuthStore`).
3. Room opens the on-device journal.

## Later launches

1. Cold start restores `is_logged_in` + `user_id` from encrypted prefs → **no login UI**.
2. `SilentAuthRestorer` confirms the on-device session (never shows a Google sheet).

## Logout

Drawer / Settings → Logout clears the stored session; next open shows the first-run
name screen again.

## Config

```
LOCAL_ONLY_MODE=false
```

`LOCAL_ONLY_MODE=true` auto-opens `local_dev` for debugging (Diesel quick-add, etc.).

