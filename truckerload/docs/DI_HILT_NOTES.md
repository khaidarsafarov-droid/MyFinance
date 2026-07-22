# DI notes (TruckerLoad)

## Current

- UI still uses `CompositionLocal` repositories from `MainActivity`.
- `com.truckerload.di.ActiveDatabaseProvider` is the Hilt-ready seam for Room/repos.

## Hilt status

AGP **9.3.0** currently fails applying `com.google.dagger.hilt.android` (`Android BaseExtension not found`).
When Dagger/Hilt supports this AGP:

1. Re-add `hilt` plugin + `hilt-android` / `hilt-compiler` (KSP).
2. Annotate `TruckerLoadApp` with `@HiltAndroidApp` and `MainActivity` with `@AndroidEntryPoint`.
3. Convert `ActiveDatabaseProvider` into `@Module` / `@Inject` and migrate ViewModels to `@HiltViewModel`.
