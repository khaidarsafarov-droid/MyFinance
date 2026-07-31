# Phase 3 — God-file split (production-ready agent prompt)

> Copy everything below the line into a new Cloud Agent / Cursor chat.
> Base branch: **`main`** (already contains Phase 0–2, Phase 4 BOM, Phase 3.6 lint-gate).

---

## CONTEXT (do not re-do)

`main` already has:

| Tag / merge | Content |
|-------------|---------|
| `v1.5.8-phase1` | Room schema export, safe migrations, session repairs |
| `v1.5.9-phase2` | Hilt `UserComponent` / `UserComponentManager`, `@HiltViewModel`, `@HiltWorker` |
| `v1.5.10-phase4` | Compose BOM 2026.06.01 + AndroidX cleanup (`compileSdk` 35) |
| `v1.5.11-phase3-lint` | `:app:checkKotlinFileSize` — soft limit **600** LOC, ideal **350**, baseline in `truckerload/config/kotlin-file-size-baseline.txt` |

DI facts (see `docs/DI_HILT_NOTES.md`):

- Account repos live in **`UserComponent`**, bridged via unscoped `@Provides` in `UserAccountModule`.
- Never mark `AppDatabase` / repositories `@Singleton`.
- Prefer `@HiltViewModel` + constructor injection; do not add new `CompositionLocal` repository seams.
- `SocialRepository` is constructed in `UserComponent.create(...)` today — after a facade/sub-repo split, wire sub-repos there (or keep facade construction only).

Open draft PRs **#100–#104** were cut from pre-Hilt `main` and **must be rebased or redone** on current `main` (they do not contain Phase 1/2/4). Prefer **fresh branches** from `origin/main` named `cursor/<step>-e265` rather than force-pushing stale history unless you carefully rebase.

---

## GOAL

Bring every production Kotlin file under the **600-line soft limit** (and preferably ≤350) by splitting the remaining god-files, then **tighten / remove baseline entries**. Do **not** raise baseline caps except when a prior merge already grew a file and you are only refreshing after a successful shrink elsewhere.

## HARD RULES

1. One PR = one step below. Branch: `cursor/<descriptive-name>-e265` off `main`.
2. After each step: `sh ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:checkKotlinFileSize` from `truckerload/`.
3. When a baselined file drops ≤600, **delete its baseline line** in the same PR.
4. Preserve public behaviour; keep facade/delegate APIs so call sites compile with minimal churn.
5. No new features, no drive-by refactors outside the target files.
6. Commit, push, draft PR via `ManagePullRequest` (`base_branch: main`).
7. Do **not** estimate calendar time; characterize difficulty by files touched / Hilt wiring risk.

---

## STEP 3.1 — `SocialRepository` (~1029 LOC)

**File:** `data/repository/SocialRepository.kt`  
**Baseline cap:** 1029 → remove after split.

### Target layout (`data/repository/social/`)

| Class | Responsibility | Target LOC |
|-------|----------------|------------|
| `ProfileRepository` | identity, avatar, presence, block/follow, peers | ≤450 (ideal ≤350) |
| `ChatRepository` | DMs, messages, reactions, unread | ≤250 |
| `GroupRepository` | groups, membership, invites, recommendations | ≤200 |
| `StatusRepository` | ephemeral friend statuses | ≤150 |
| `MediaRepository` | chat image/voice attachments (delegate to chat) | ≤80 |
| `SocialSyncCoordinator` | seed/init, leaderboard, weekly challenge | ≤250 |
| `EnhancedProfileFactory` / `SocialSupport` | mapping + shared helpers | small |
| `SocialRepository` **facade** | wires sub-repos, same public API | ≤400 |

### Compatibility

- Keep `SocialRepository(db, loadRepository, userProfileStore, context)` signature **or** update **all** construction sites (`UserComponent`, tests) in the same PR.
- Preserve public methods + `MESSAGE_PAGE_SIZE` / companions used by VMs/UI.
- Wire facade (or concrete repos) inside `UserComponent.create` + keep `UserAccountModule.provideSocialRepository`.

### Verify

- Compile + unit tests + file-size gate.
- Smoke: Community / Profile / Chat / Status / Groups; logout clears identity.

**Prior art (stale):** PR #100 — reuse ideas, redo on current `main` with Hilt wiring.

---

## STEP 3.2 — `FriendsLiveMapScreen` (~976 LOC)

**File:** `presentation/screens/map/FriendsLiveMapScreen.kt`  
**Baseline cap:** 976 → remove after split.

### Extract into same package (or `map/friends/`)

| Module | Contents |
|--------|----------|
| `FriendsLiveMapUiState.kt` | state models / sealed UI events |
| `FriendsLiveMapPermission.kt` | location permission helpers |
| `FriendsLiveMapOverlays.kt` | markers, polylines, route drawing |
| `FriendsLiveMapBottomSheet.kt` | friend / load bottom sheet |
| `FriendsLiveMapFullscreen.kt` | fullscreen map chrome |
| `FriendsLiveMapScreen.kt` | composition root only — **≤400 LOC** |

Keep `FriendsLiveMapViewModel` (`@HiltViewModel`) as the state owner; do not move business logic into Composables.

### Verify

- Compile + tests + file-size gate.
- Smoke: open Friends map, see self marker / friend markers when available.

**Prior art (stale):** PR #101.

---

## STEP 3.3 — `TelegramBotSyncEngine` (~871 LOC)

**File:** `sync/TelegramBotSyncEngine.kt`  
**Baseline cap:** 871 → remove after split.

### Target layout (`sync/telegram/`)

| Class | Role |
|-------|------|
| `TelegramApiClient` | HTTP / Bot API calls |
| `TelegramUpdateParser` | parse updates → domain |
| `TelegramSyncScheduler` | schedule / backoff helpers |
| `TelegramSyncStateMachine` | sync states |
| `TelegramUpdateDispatcher` | route updates to handlers |
| `TelegramBotSyncEngine` | thin facade used by workers / FGS | ≤350 |

Preserve entry points used by `TelegramSyncWorker`, foreground service, and settings “send to Telegram”.

### Verify

- Compile + unit tests (parser/engine tests if present) + file-size gate.
- Smoke: with a token configured (or unit-level), sync path still constructs.

**Prior art (stale):** PR #102.

---

## STEP 3.4 — `SocialViewModels.kt` (~770 LOC)

**File:** `presentation/screens/social/SocialViewModels.kt`  
**Baseline cap:** 770 → remove after split.

### Rule

- **1 ViewModel = 1 file** by default.
- Exception: two tiny VMs of the same feature each &lt;80 LOC may share a file.

### Suggested files

`ProfileViewModel.kt`, `ChatsViewModel.kt`, `SocialChatViewModel.kt`, `CommunityViewModel.kt`, `StatusViewModel.kt`, `GroupsViewModels.kt` (Groups + GroupDetail if both small), `PeerProfileViewModel.kt`, shared `SocialViewModelExt.kt` if needed.

Keep `@HiltViewModel` + same constructor deps (`SocialRepository` facade is fine). Same package so screens need **zero import changes**.

### Verify

- Compile + tests + file-size gate.
- Smoke: open each social tab once.

**Prior art (stale):** PR #103.

---

## STEP 3.5 — Extract auth from `LoginScreen` (~587 LOC)

**File:** `presentation/screens/login/LoginScreen.kt`  
(Also consolidate with `presentation/auth/GoogleSignInLauncher.kt`.)

### Deliverables

1. **`AuthRepository`** (`data/repository/AuthRepository.kt`) — unified:
   - Supabase email/password
   - Google id-token / legacy account completion
   - Local credentials fallback (`AuthCredentialsStore`)
   - Profile + `AuthLogin.tryCompleteLogin` (keep ~400ms delay behaviour)
2. **`AuthViewModel`** (`@HiltViewModel`) — email UI state, loading, field errors, toast events; biometric offer after email success via existing `offerBiometricAfterEmailLogin`.
3. **`LoginScreen`** — UI only, **≤250 LOC** (helpers may live in `LoginScreenContent.kt`).

Provide `AuthRepository` from a Hilt module (app/singleton or unscoped with `@ApplicationContext`) — it is **not** account-Room scoped; it uses `AuthStore` / profile / credentials.

Wire Google UI through `rememberGoogleSignInLauncher` → repository (avoid a third copy of Google completion).

### Verify

- Compile + tests + file-size gate (LoginScreen must leave the “warn &gt;350” band ideally; must stay &lt;600).
- Smoke: local email login with `LOCAL_ONLY_MODE=true`.

**Prior art (stale):** PR #104.

---

## STEP 3.6 — Already done (do not re-implement)

Lint gate lives on `main`:

```bash
cd truckerload
sh ./gradlew :app:checkKotlinFileSize
```

After steps 3.1–3.5, baseline should shrink toward **empty**. Optional follow-up PR: only baseline cleanup + docs.

---

## STEP 3.7 — Remaining baseline / near-limit files (optional stretch)

Do only after 3.1–3.5 are merged. One PR per cluster if large.

| File | LOC | Suggested peel |
|------|----:|----------------|
| `MaintenanceScreen.kt` | 942 | form / list / dialogs into `maintenance/` composables |
| `StatsScreen.kt` | 779 | charts / period picker / lists |
| `DatabaseMigrations.kt` | 776 | keep migrations; extract helpers only if readable |
| `NavGraph.kt` | 735 | auth graph / social graph / tools graph |
| `HomeScreen.kt` | 727 | header / list / FABs |
| `ProfileScreen.kt` | 722 | header / tabs / actions |
| `CommunityScreen.kt` | 637 | feed vs filters |
| `HomeViewModel.kt` | 615 | paging / summary / pending-delete helpers |

Ideal ≤350; soft fail at &gt;600 without baseline.

---

## MERGE ORDER

```
main (Phase 0–2 + 4 + lint-gate)
  → 3.1 SocialRepository      → tag v1.5.12-phase3-social-repo
  → 3.2 FriendsLiveMapScreen  → tag v1.5.13-phase3-friends-map
  → 3.3 TelegramBotSyncEngine → tag v1.5.14-phase3-telegram
  → 3.4 SocialViewModels      → tag v1.5.15-phase3-social-vms
  → 3.5 LoginScreen auth      → tag v1.5.16-phase3-login-auth
  → (optional) 3.7 cleanup    → tag v1.5.17-phase3-cleanup
```

Rebase each next step onto updated `main` after the previous merge.

---

## VERIFICATION CHECKLIST (per PR)

```bash
cd truckerload
source ~/.bashrc   # or login shell
sh ./gradlew :app:compileDebugKotlin
sh ./gradlew :app:testDebugUnitTest
sh ./gradlew :app:checkKotlinFileSize
```

- File-size gate green; removed baseline lines for files you shrunk under 600.
- No new CompositionLocal repository APIs.
- Hilt graph still builds (`UserComponent` constructs repos).
- Draft PR describes files moved + LOC table + how to smoke.

---

## OUT OF SCOPE

- New product features, sync protocol changes, Room schema bumps (unless a split forces a tiny helper move).
- Full detekt / Android lint baseline cleanup.
- Merging stale #100–#104 without rebase onto current `main`.
