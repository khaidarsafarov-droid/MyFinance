# Cloud Data Synchronization — Account-Based Backend (Truck Load)

How Truck Load keeps journal / finance / driver profile available across
restarts and devices: **Local-First Room + account cloud snapshot + LWW**.

Companion docs: `EMAIL_AUTH_HYBRID_GUIDE.md`, `GOOGLE_AUTH_OFFLINE_GUIDE.md`.

---

## Stage 1 — First login («from zero»)

1. User signs in (Google or Email) → stable **account id**
   (`supabase uuid` / `google_<hash>` / `local_<hash(email)>`).
2. Driver works offline-first: Add Load / diesel / profile → **Room**.
3. Each mutation enqueues `sync_outbox` (`OutboundSyncQueue`) with entity id + timestamp.
4. When online, `OutboundSyncWorker` drains the queue and
   `CloudSyncEngine.pushLocalSnapshot` writes an **AccountCloudSnapshot**
   (loads, paychecks, diesel, driver profile JSON) into the account mirror
   (`files/cloud_account_mirror/<accountId>.json`).
5. Optional: Google Drive backup still ships the same journal JSON for
   true cross-device when Drive is linked.

Result: data is bound to the **account**, not only to the phone flash.

---

## Stage 2 — Return on the same device

1. Cold start restores JWT / Google session (`SilentAuthRestorer`).
2. `CloudSyncEngine.onSessionReady` reads `CloudSyncCursorStore.lastSyncedAt`.
3. If the account mirror `updatedAt` is newer → **pull merge** (LWW on `updatedAt`).
4. Always best-effort **push** local Room → mirror.
5. UI (Home / widgets) refreshes from Room — no blocking spinner for sync.

---

## Stage 3 — New device / empty Room (cross-device hydration)

1. User signs in with the same Google / Email on a new phone.
2. Room file for that account id is empty.
3. If the account mirror (or a restored Drive blob written into the mirror)
   has entities and `lastSyncedAt == 0` → **Full Hydration**:
   wipe empty tables and insert the full snapshot (loads + stops + diesel +
   paychecks + driver profile fields).
4. Driver opens Home and sees the same trips / balance / CDL profile.

> True multi-device without Drive requires a hosted DB (Supabase/Postgres).
> The mirror + `AccountCloudSnapshot` schema is the contract that backend
> tables should implement next (`account_snapshots` or normalized rows).

---

## Architectural rules

| Rule | Implementation |
|------|----------------|
| **Local-First** | `LoadRepository.insertLoad` / diesel / profile write Room first |
| **Sync Queue** | `sync_outbox` + WorkManager (`OutboundSyncWorker`, `CloudSyncWorker`) |
| **Last Write Wins** | `CloudSyncPolicy.remoteWins(localUpdatedAt, remoteUpdatedAt)` |
| **Stable ids** | Load `id` / `tripId`; account id from `AccountIds` |
| **Cursor** | `CloudSyncCursorStore` per account (`last_synced_at`) |

---

## Key types

- `CloudSyncPolicy` — pure LWW / hydration predicates (unit-tested)
- `AccountCloudSnapshot` — versioned account blob
- `AccountCloudMirror` — durable local stand-in for the cloud row
- `CloudSyncEngine` — push / pull / full hydrate orchestration
- `CloudSyncWorker` — periodic 6h + oneshot after login

---

## Checklist

- [x] Local-First writes + outbox
- [x] Push snapshot after mutations / session ready
- [x] Pull since last_synced (incremental merge)
- [x] Full restore when local empty + remote has data
- [x] LWW conflict policy
- [ ] Hosted Postgres/Supabase table replacing file mirror (next)
- [ ] Mirror ←→ Drive auto-seed on first Google login on a new device
