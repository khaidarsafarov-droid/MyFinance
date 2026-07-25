# Cloud migration rollout

Each phase is independently reversible. Room remains the immediate write path
throughout; no phase may make the home screen depend synchronously on cloud
availability.

## Gates

| Gate | Disabled behavior | Enabled behavior |
| --- | --- | --- |
| `LOCAL_ONLY_MODE=true` | Local development account, no Supabase/cloud client | Supabase account flow is available |
| blank `SYNC_BACKEND_URL` | Room plus on-device account mirror | Hybrid local mirror plus Ktor snapshots |
| `TELEGRAM_SYNC_MODE=device` | Existing foreground/device bot | Authenticated server inbox |
| missing `app/google-services.json` | No Android FCM or Crashlytics initialization | Google Services, FCM, and Crashlytics are configured |
| blank server `FIREBASE_PROJECT_ID` | Push notifier is a no-op | FCM data-only sync pushes |
| `CLOUD_MEDIA_ENABLED=false` | Attachments stay local | Durable upload/pull/delete worker |

The first five are compile-time Android build configuration. Server FCM is runtime
configuration. The media implementation additionally fails closed unless the backend
URL, non-local mode, active account, and JWT are all present.

## Phase 0: local fallback and shadow observation

Ship the current local-first app with `LOCAL_ONLY_MODE=false` where authentication is
being tested, but leave `SYNC_BACKEND_URL` blank for the control cohort. Room and the
on-device account mirror remain authoritative. Establish baselines for local sync
queue age, startup stability, and backup/restore.

For a small internal cohort, configure the backend URL while keeping all UI reads from
Room. `HybridAccountCloudBackend` always writes the local mirror first, attempts the
remote snapshot, and falls back to local data when the remote read is absent or fails.
Compare entity counts and timestamps out of band; never log raw snapshots.

Rollback: distribute a build with blank `SYNC_BACKEND_URL` or
`LOCAL_ONLY_MODE=true`. No server data deletion is required.

## Phase 1: cloud snapshots

Enable `SYNC_BACKEND_URL` for staged cohorts. Supabase JWTs scope all operations.
Observe:

- HTTP success/error and latency;
- accepted versus stale snapshot writes;
- client outbox age and remote acknowledgement;
- sampled local/remote entity counts without user identifiers in metrics.

Advance only when new-device hydration and same-device fallback have been exercised,
stale writes are understood, and server 5xx does not strand local mutations.

Rollback: blank the URL in a new build. Existing Room and mirror state continue to
work. Keep PostgreSQL snapshots for reconciliation. If the backend is reachable but
bad data was accepted, maintenance-gate writes and restore PostgreSQL before
re-enabling.

## Phase 2: server Telegram

Register the Telegram webhook, verify `getWebhookInfo`, then ship
`TELEGRAM_SYNC_MODE=server` to a small cohort. Users link through a one-time token;
the app polls the authenticated inbox and acknowledges processed updates. Monitor
webhook rejected/duplicate counts, inbox processing lag, parser outcomes on-device,
and HTTP errors.

Advance only when duplicate delivery is harmless, one-time link tokens cannot be
reused, and server messages result in the same local records as device mode.

Rollback:

1. call `telegram-webhook.sh delete` with `DROP_PENDING_UPDATES=false`;
2. ship/configure `TELEGRAM_SYNC_MODE=device`;
3. leave durable inbox rows for audit/replay and do not expose the server bot token to
   Android.

## Phase 3: FCM wake-up

Add `google-services.json` only in the credentialed Android build environment. Set
server `FIREBASE_PROJECT_ID` and encrypted `FIREBASE_CREDENTIALS_JSON`. FCM carries
only `type=sync`; it never carries journal content, identity, or tokens. Periodic
WorkManager sync remains the recovery path when pushes are delayed or denied.

Monitor push failure count, registration freshness, battery impact, and time from
accepted snapshot to remote-device pull.

Rollback: clear `FIREBASE_PROJECT_ID` on the server to make sending a no-op, then ship
without `google-services.json` if client registration also must stop. Snapshot polling
and local operation continue.

## Phase 4: media

The Android media client, Room v26 queue/migration, backend tombstones/list/download,
and S3/local signed URLs are implemented. `CLOUD_MEDIA_ENABLED` remains false by
default. Test direct upload against a non-production Space, then enable in sequence:

1. metadata creation and presigned upload;
2. upload completion/size validation;
3. cross-device metadata reads and verified downloads;
4. idempotent deletion/tombstone reconciliation.

Monitor presign failures, incomplete uploads older than expiry, object/metadata size
mismatch, Space availability, and client retry volume. Never label metrics with
object keys or account IDs.

Rollback: ship/set the client build gate false so new attachments remain local.
Existing Room rows continue to work; preserve already uploaded objects and metadata
and do not bulk-delete during an incident. If Spaces is
unavailable, readiness removes the backend from traffic because snapshot and Telegram
operations share the service; restore storage or deploy a deliberately reviewed
readiness-policy change rather than bypassing checks ad hoc.

## Promotion and incident rules

- Promote by build cohort, not by mutating user data.
- Keep one known-good APK and immutable backend image SHA for every phase.
- A rollback changes one gate at a time so its effect is observable.
- Do not roll an application image behind an incompatible Flyway migration. Restore
  or forward-fix the database using the backup runbook.
- Record measured error rate, latency, stale ratio, inbox lag, push failure rate, and
  recovery time at each decision. Email, device ID, raw Telegram text, snapshots,
  object keys, and credentials are prohibited from logs/metric labels.
