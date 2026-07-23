# Google Sign-In — professional offline-first flow (Truck Load)

Scopes used at login: `openid` / profile / email via ID token.
Optional Drive backup uses `drive.appdata` (separate consent in Settings).

## Part 1 — First sign-in

1. User taps branded **Sign in with Google** (`GoogleSignInButton`, loading/disabled while busy).
2. Credential Manager sheet (or legacy Google Sign-In) → account pick → consent.
3. App receives ID token; claims include `sub`, email, name, picture.
4. Tokens (`access` / `refresh` when Supabase is configured) + `google_sub` go to **EncryptedSharedPreferences** (`AuthStore` via `SecurePreferences`).
5. Local Room DB file is keyed by account id: Supabase UUID → `google_<hash(sub)>` → `local_<hash(email)>`.
6. User lands on Home; profile shows name / avatar.

## Part 2 — Later launches (silent)

1. Cold start restores session from encrypted prefs (no login UI).
2. `SilentAuthRestorer` runs in background:
   - No network → `AuthSessionHealth.OFFLINE_LOCAL` + soft banner.
   - Google provider → silent Credential Manager (`filterByAuthorizedAccounts` + `autoSelect`).
   - Failure → `SESSION_UNCONFIRMED` banner: *working locally* (app stays usable).
3. `LOCAL_ONLY_MODE` skips cloud checks and uses `local_dev`.

## Part 3 — Sync

Triggers: after login, Settings manual backup/restore, local change debounce (`BackupService`), **WorkManager** `DriveSyncWorker` (12h, network required).
Last sync time: Settings Drive section + Profile card.
Conflict policy: see `DriveSyncPolicy` (remote newer vs local edits).

## Checklist

- [x] No passwords stored — tokens only in encrypted prefs
- [x] Offline-first Room; banners never block journal entry
- [x] First Google sign-in ~2 taps; subsequent cold starts 0 taps when session valid
- [x] Soft banners for unconfirmed session / offline / secure-storage fallback
