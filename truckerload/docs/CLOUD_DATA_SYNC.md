# Cloud data synchronization

Truck Load is local-first: screens read Room and mutations commit locally before any
network operation. Cloud synchronization is implemented through the Ktor backend when
`SYNC_BACKEND_URL` is configured and `LOCAL_ONLY_MODE=false`. The on-device account
mirror remains the fallback; it is not the hosted source of truth.

## Active data path

1. Supabase Auth provides a stable UUID account and bearer JWT.
2. A local mutation writes the account-specific Room database and enqueues
   `sync_outbox`.
3. `CloudSyncEngine` builds a versioned `AccountCloudSnapshot`.
4. `HybridAccountCloudBackend` writes
   `files/cloud_account_mirror/<accountId>.json` first, then sends the snapshot to
   `PUT /v1/sync/snapshot` with the JWT and source device ID.
5. Ktor verifies that `snapshot.accountId` equals the JWT subject. PostgreSQL applies
   strict timestamp last-write-wins; an equal or older timestamp is stale.
6. A remote pull reads `GET /v1/sync/snapshot`. If the backend is unavailable or has
   no snapshot, the client reads the local mirror. UI remains on Room in either case.

When `SYNC_BACKEND_URL` is blank, the exact same orchestration uses
`LocalAccountCloudBackend` only. This is the supported offline/deployment rollback
behavior, not an error mode.

## Session and hydration

- On session restore, `CloudSyncCursorStore` supplies `lastSyncedAt`.
- A newer remote/mirror snapshot is merged by the tested LWW policy.
- An empty account Room database with no prior cursor can be fully hydrated from the
  server snapshot.
- After pull/merge, local state is pushed best-effort. The home screen and widgets do
  not wait for the request.
- On a new device, cross-device hydration requires a configured backend and a valid
  Supabase session. The local mirror alone cannot move data between devices.

## Server Telegram and FCM

With `TELEGRAM_SYNC_MODE=server`, Telegram sends secret-authenticated updates to Ktor.
PostgreSQL stores linked text messages idempotently by `update_id`; Android fetches and
acknowledges them through the authenticated inbox. Device mode keeps the existing
on-phone bot service as the fallback.

When Firebase is configured, an accepted snapshot can send a data-only `type=sync`
notification to the user's other registered Android devices. FCM contains no account
data and does not replace periodic WorkManager pull.

## Conflict and security rules

| Rule | Implementation |
| --- | --- |
| Local-first | Room commit and local mirror do not depend on cloud availability |
| Account scope | JWT UUID subject; payload account ID must match |
| Snapshot conflict | Strict `updatedAt` LWW in PostgreSQL |
| Stable IDs | Entity IDs remain stable inside the snapshot |
| Cursor | Per-account local cursor and per-device server cursor endpoint |
| Retry | WorkManager and `sync_outbox`; remote failure leaves local state intact |
| Push fan-out | Source device is excluded; push is only a wake-up |

The snapshot is an account blob, so concurrent edits to different entities can still
conflict at snapshot timestamp granularity. The rollout therefore monitors stale
writes and retains Room/outbox data. Normalized per-entity server conflict resolution
is a future schema evolution, not current behavior.

## Key types

- `AccountCloudBackendFactory` selects local-only or hybrid behavior.
- `LocalAccountCloudBackend` and `AccountCloudMirror` provide same-device durability.
- `RemoteAccountCloudClient` implements authenticated Ktor snapshots, Telegram inbox,
  and FCM token registration.
- `CloudSyncEngine` handles push, pull, merge, and full hydration.
- `CloudSyncWorker` and `OutboundSyncWorker` provide periodic and mutation-triggered
  execution.

## Status

- [x] Local-first Room writes and outbox
- [x] Durable local account mirror
- [x] Hosted Ktor/PostgreSQL snapshots and cursors
- [x] Authenticated Android remote client with local fallback
- [x] New-device hydration from a configured backend
- [x] Server Telegram linking, idempotent inbox, and acknowledgement
- [x] Optional FCM sync wake-up
- [ ] Android direct-to-S3 media client rollout
- [ ] Per-entity server conflict model if snapshot LWW proves insufficient

See `TARGET_ARCHITECTURE.md` and `MIGRATION_ROLLOUT.md` for target boundaries and
safe activation/rollback.
