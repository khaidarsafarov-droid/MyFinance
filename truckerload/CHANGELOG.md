# Changelog

## [1.6.0] — Legacy audit complete (`v1.6.0-legacy-audit-complete`)

End-to-end quality/architecture pass: Room migration hardening, Hilt account scope,
Compose/AndroidX upgrades, god-file splits, SocialRepository facade removal, and
cloud-first Ktor sync scaffolding. Soft Kotlin file-size baseline is empty.

### Phase 0 — Data-loss / release hygiene
- Fix persistent storage and secret-leak issues (see `docs/PHASE0_DATA_LOSS_FIXES.md`).

### Phase 1 — Room migrations (#97)
- Fail-closed pre-v6 upgrades (`UnsupportedDatabaseUpgradeException`).
- Strengthen idempotent migration helpers and session repairs.

### Phase 2 — Hilt DI (#99)
- Account-scoped `UserComponent` graph for per-user repositories.
- Finish constructor injection for ViewModels / repositories.

### Phase 4 — Compose / AndroidX (#98)
- Compose BOM `2026.06.01` and related AndroidX / deprecated API cleanup.

### Phase 3 — God-file splits & social domain graph
| Step | PR | Summary |
|------|-----|---------|
| 3.1 | #111 | Split `SocialRepository` into domain repositories (+ facade) |
| 3.2 | #122 | Split `FriendsLiveMapScreen` → `social/friends/map/*` |
| 3.3 | #123 | Split `TelegramBotSyncEngine` → `sync/telegram/*` |
| 3.4 | #124 | One social ViewModel per file |
| 3.5 | #125 | Extract `AuthRepository` + `AuthViewModel` from `LoginScreen` |
| 3.6 | #126 | Remove `SocialRepository` facade; inject domain repos |
| 3.7 | #128 | Split remaining presentation god-files (Home, Stats, Nav, Profile, Community, Maintenance) |

Also:
- Kotlin file-size lint gate (soft 600 / ideal 350) with baseline.
- `DatabaseMigrations` split into `migrations/Migrations{Early,Mid,Late}.kt` bands; entry point ≤200 LOC.
- Baseline cleared (zero entries).

Archived planning docs: `docs/archive/PHASE3_PR_DESCRIPTIONS.md`,
`docs/archive/PHASE3_GODFILE_SPLIT_PROMPT.md`.

### Phase 5 — Cloud-first (#127)
- Ktor client layer (`data/remote/ktor/*`).
- `SyncMode` orchestration (`data/sync/cloud/*`).
- Rollback notes: `docs/CLOUD_ROLLBACK.md`.

### Verification
- `./gradlew clean :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:checkKotlinFileSize` (zero baseline entries)

