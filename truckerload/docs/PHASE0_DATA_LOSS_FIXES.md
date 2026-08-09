# Phase 0 — Data-loss & secrets fixes

## What changed

1. **Legacy / absorb DB copy** (`AppDatabase`, `DatabaseFileCopy`)
   - Success flag set only after non-empty target passes `PRAGMA integrity_check`
   - Broken targets deleted; up to 2 attempts with 500 ms delay
   - Failures reported to Crashlytics; flag left unset for retry

2. **Startup backfill** (`TruckerLoadApp`, `StartupRepairStore`)
   - Per-user key `startup_backfill_v2_<userId>`
   - Flag set only when PU/DEL backfill **and** reporting-week refresh succeed
   - Soft banner + tap-to-retry on home (`AuthStatusBanner`)

3. **SecurePreferences**
   - When EncryptedSharedPreferences / Keystore fails: durable plaintext fallback + banner
   - High-value secrets (bot tokens, API keys) still refuse plaintext writes
   - Session identity + PBKDF2 password verifiers are allowed on the degraded store so
     registration/login survives process death (tablets with broken Keystore)

4. **Release BuildConfig**
   - `TELEGRAM_BOT_TOKEN` and `CEREBRAS_API_KEY` forced empty in `release`
   - Gradle task `:app:verifyReleaseSecretsEmpty` (wired to `assembleRelease` / CI)

## Rollback

Revert the Phase 0 PR. Users mid-retry keep legacy `truckerload_db` until a successful copy.

## Verify

```bash
cd truckerload
sh ./gradlew :app:testDebugUnitTest \
  --tests 'com.truckerload.data.local.DatabaseFileCopyTest' \
  --tests 'com.truckerload.data.preferences.StartupRepairStoreTest' \
  --tests 'com.truckerload.data.preferences.InMemorySharedPreferencesTest'
sh ./gradlew :app:verifyReleaseSecretsEmpty
```
