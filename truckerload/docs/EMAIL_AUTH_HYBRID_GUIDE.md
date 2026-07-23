# Email + Password auth & hybrid sync (Truck Load)

Companion to `GOOGLE_AUTH_OFFLINE_GUIDE.md`. Covers classic email registration,
driver onboarding, JWT refresh, biometric unlock, and the outbound sync queue.

## Part 1 — Hybrid online mode

1. Loads, diesel expenses, and profile updates are written to **Room first**.
2. Each mutation enqueues a row in `sync_outbox` (`OutboundSyncQueue`).
3. `OutboundSyncWorker` (WorkManager, network required) drains pending rows when online.
4. If the network drops mid-day, the queue buffers; on reconnect the worker retries the batch.

## Part 2 — Email registration

1. Login screen: Google button + expand **Email / Create account**.
2. Sign-up validates password: **≥8 chars, digit, uppercase** (`PasswordPolicy`).
3. Password is stored as a **PBKDF2-SHA256** verifier in EncryptedSharedPreferences
   (`AuthCredentialsStore`) — never plaintext at rest. Legacy plaintext entries upgrade on next successful login.
4. Server (Supabase) receives the plaintext password only over TLS for registration/login;
   the client hash is for offline credential checks.

## Part 3 — Driver onboarding wizard

`ProfileSetupScreen` (3 steps):

1. Personal — name, DOB (`yyyy-MM-dd`), country, phone, avatar.
2. Professional — CDL / license class, CDL number, truck type, axle count.
3. Hub — home terminal city.

Persisted on `driver_profile` (Room migration 23→24) and pushed to the outbox.

## Part 4 — Email verification (soft)

After signup / profile wizard for `AuthProvider.EMAIL`, `EmailVerificationScreen`
asks for a 6-digit code. Account stays usable (`Verify later`). In
`LOCAL_ONLY_MODE` / debug builds the code is shown on-screen for QA.

## Part 5 — Session restore (email)

`SilentAuthRestorer.restoreEmailTokens`:

- Offline → `OFFLINE_LOCAL` banner.
- Online + refresh token → `SupabaseAuthService.refreshSession` → update JWT pair.
- Refresh failure with leftover access → `SESSION_UNCONFIRMED` (journal stays open).

Tokens live in EncryptedSharedPreferences via `AuthStore`.

## Part 6 — Biometric unlock

After the first successful email login, biometric unlock is offered when the device
supports it (`BiometricUnlockStore`). Cold starts for email accounts show
`BiometricUnlockGate` before the main NavGraph.

## Checklist

- [x] Auth UI: Google + Email / Create account
- [x] Driver profile fields (CDL, axles, DOB, hub) in Room + wizard
- [x] Biometric unlock option for email accounts
- [x] Password policy + PBKDF2 at rest
- [x] Soft email verification UI
- [x] JWT refresh for email sessions
- [x] Hybrid outbound queue (Room + WorkManager)
