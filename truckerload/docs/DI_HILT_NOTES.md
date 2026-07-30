# DI notes (TruckerLoad)

## Current (Phase 2)

- Dagger/Hilt 2.60.1 is applied to the Android app with KSP.
- `TruckerLoadApp` and `MainActivity` are Hilt entry points.
- `TruckerLoadApp` implements `Configuration.Provider` with `HiltWorkerFactory`
  (WorkManager default initializer is disabled in the manifest).
- `ApplicationStoreModule` provides only process-safe stores in
  `SingletonComponent` (`AuthStore`, credentials, profile, settings, push token).
- Account data lives in `UserComponent` / `UserComponentManager`:
  - `startSession(userId)` on login (and from workers that need DB)
  - `endSession()` on logout / account switch (closes Room, unbinds profile)
- `UserAccountModule` bridges the **active** session into `SingletonComponent`
  with **unscoped** `@Provides` so `@HiltViewModel` / `@HiltWorker` can inject
  repositories without caching a previous user's graph as `@Singleton`.
- ViewModels use `@HiltViewModel` + `hiltViewModel()`.
- Workers use `@HiltWorker` + `@AssistedInject`.

## Intentional boundary

`AppDatabase` and repositories must never be `@Singleton`. Their identity depends
on the active user, which can change without a process restart. Caching them in
`SingletonComponent` would leak data across accounts.

`CompositionLocal` providers in `MainActivity` remain for a few screens that still
collect repository Flows outside ViewModels (`LocalSocialRepository`,
`LocalAuthStore`, etc.). Prefer moving those reads into `@HiltViewModel` next;
do not add new CompositionLocal repository seams.

## Verify

1. Login → logout → login as another account: Room file is `truckerload_<userId>`,
   old connection closed, NavGraph remounted via `key(userId)`.
2. Enqueue a `@HiltWorker` (e.g. Telegram sync): worker receives injected deps
   after `userComponentManager.startSession(userId)` inside `doWork` when needed.
3. Open Home / Settings / Social / Voice screens: no crash from missing session
   (session must be started before NavGraph).
