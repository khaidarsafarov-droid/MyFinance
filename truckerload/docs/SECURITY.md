# Security decisions

Short notes for the fixes in the public-repo audit. This is not a full threat model.

## API keys

Google API keys are read from the `MAPS_API_KEY` environment variable (the CI secret) or from gitignored `local.properties`. If neither is set, the value is empty. `checkNoTrackedApiKeys` fails the build when an `AIza...` key shows up in a Gradle file, an example file, or `local.defaults.properties`.

The Drive key that was committed on 14 Oct 2026 is still in git history. Deleting history does not un-publish it. Rotate that key in Google Cloud and restrict the new one to the Drive API, package `com.truckorig`, and the Play upload certificate.

## Database key

The Room passphrase is random. Android Keystore wraps it. The wrap key is **not** tied to a fingerprint or PIN.

`setUserAuthenticationRequired(true)` would invalidate the key when the lock screen changes, and the database opens at startup and from background jobs with no prompt. That would lock people out of an existing journal. We do not set it.

New keys on Android 15 and later use `setUnlockedDeviceRequired(true)`, which matches [the Keystore guidance](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setUnlockedDeviceRequired(boolean)): that flag had data-loss bugs on Android 12 through 14. Existing keys are left as they are, so an upgrade does not re-wrap or delete the journal.

## Passwords and CDL numbers

Password checkers are PBKDF2 verifiers and are written only when encrypted preferences open. If the Keystore fails, the verifier is not written to a plaintext file.

CDL values that were stored as `plain:` are rewritten to `v1:` ciphertext on the next read, and plaintext is then refused. If the Keystore cannot hold the CDL key, the old ciphertext stays in the database and is not copied into a plaintext preferences file.

## Backup and shared files

`android:allowBackup` stays `false`. `res/xml/data_extraction_rules.xml` excludes every domain from cloud backup, device-to-device transfer, and cross-platform transfer. `res/xml/full_backup_content.xml` does the same for Android 11 and lower. Android treats a missing section as "back up everything".

FileProvider only names app-specific folders. Financial PDFs and scans stay in app-private storage and are shared with a one-time URI grant. "Save to phone" still writes through MediaStore on Android 10+, or `Download/TruckoRig/` on older versions. File names are stripped of slashes and `..`.

## Release build

Release forces `LOCAL_ONLY_MODE` to false and `TELEGRAM_BOT_TOKEN` to empty. `verifyReleaseSecretsEmpty` fails if either comes back. Debug builds never sign with the Play upload keystore.
