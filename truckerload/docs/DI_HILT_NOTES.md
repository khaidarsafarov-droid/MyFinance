# DI notes (TruckerLoad)

## Current

- Dagger/Hilt 2.60.1 is applied to the Android app with KSP.
- `TruckerLoadApp` and `MainActivity` are Hilt entry points.
- `ApplicationStoreModule` provides only application-context wrappers in
  `SingletonComponent`.
- The Firebase messaging service receives the process-wide `PushTokenStore` through
  Hilt.
- UI still uses `CompositionLocal` for the active account's repositories.

## Intentional boundary

`AppDatabase` and repositories are not `SingletonComponent` bindings. Their identity
depends on the active user, which can change without a process restart, so
`MainActivity` rebuilds them after login and closes them on logout.
`ActiveDatabaseProvider` remains an explicit account-aware factory for the same
reason. Existing WorkManager workers also remain framework-constructed; HiltWorker
configuration is deferred until the complete worker set can be migrated safely.
