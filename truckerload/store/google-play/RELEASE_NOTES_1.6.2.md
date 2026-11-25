# TruckoRig 1.6.2 — Play Store release notes (what’s new)

## User-facing (EN)

• Google Drive sync has been removed. The app no longer signs in to any account and keeps your data only on this phone.
• To move or keep a copy of your journal, use Settings → Backup to save a backup file and restore it later.

## Internal

- `versionCode` 20, `versionName` 1.6.2 (1.6.1 was `versionCode` 19).
- Removed Google Sign-In (`play-services-auth`), the Drive REST client, the 12-hour Drive upload worker, the Settings → Google Drive screen, and the Profile "Google account" / "Google Drive" rows.
- Removed the unused local email/password verifier store.
- First start after the update: cancels the `drive_sync_periodic` job and wipes the Drive link, Play Services sign-in cache, old Google ID token, and email/password verifiers. The journal, photos, scans, profile, and local backups are not touched.
- Older JSON/zip backups (including ones that were made for Drive) still restore from a file.
- Photo GPS, date, and time stamps are unchanged. PDF statements stay English-only.
