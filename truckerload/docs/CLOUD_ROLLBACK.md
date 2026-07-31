# Cloud sync rollback

Room remains the offline-first cache and the UI source of truth. Cloud sync is
gated so a bad backend never strands local mutations.

## SyncMode

| Mode | Behavior |
| --- | --- |
| `DEVICE_ONLY` | No Ktor calls. Room + local account mirror only. Telegram device bot unchanged. |
| `HYBRID` (default) | Room-first writes; snapshot push/pull when `SYNC_BACKEND_URL` is set. |
| `SERVER_PRIMARY` | Same as hybrid, but session/FCM wake prefers a cloud refresh. |

Persisted in SharedPreferences (`truckerload_sync_mode`). Compile-time gates still
win: `LOCAL_ONLY_MODE=true` or blank `SYNC_BACKEND_URL` force effective `DEVICE_ONLY`.

## Restore pure local operation (no data loss)

1. Set SyncMode to `DEVICE_ONLY` in-app (or clear the pref / reinstall with default
   and blank backend URL).
2. Or ship a build with blank `SYNC_BACKEND_URL` / `LOCAL_ONLY_MODE=true`.
3. Do **not** wipe Room, `sync_outbox`, or `files/cloud_account_mirror/`.
4. Keep PostgreSQL snapshots for later reconciliation; they are unused while
   `DEVICE_ONLY` is effective.

Local loads, paychecks, diesel, and media files remain readable. Outbox rows stay
queued until cloud mode is re-enabled.

## Why there is no `syncPending` / `lastSyncedAt` column

Room schema stays at **v29** (see task constraint). Pending sync is represented by
`sync_outbox` rows (`STATUS_PENDING`). Per-account `lastSyncedAt` lives in
`CloudSyncCursorStore` SharedPreferences, not on entity rows.

## Telegram

Device Telegram (`TELEGRAM_SYNC_MODE=device`) is the local path and is never removed
by SyncMode. Server Telegram inbox workers are skipped when SyncMode disallows cloud
calls or when server mode is not configured.

## FCM

`TruckerLoadFirebaseMessagingService` ignores `type=sync` wakes when SyncMode is
`DEVICE_ONLY`. Periodic WorkManager remains the recovery path when cloud is re-enabled.

## Related

- `TARGET_ARCHITECTURE.md` — boundaries
- `MIGRATION_ROLLOUT.md` — phased gates
- `CLOUD_DATA_SYNC.md` — snapshot / media paths
