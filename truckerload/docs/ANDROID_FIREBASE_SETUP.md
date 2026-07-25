# Optional Android Firebase and Crashlytics setup

Firebase is optional. The repository does not contain `google-services.json`, and the
default build must remain usable without Firebase credentials.

## Enable a credentialed build

1. Register Android application `com.truckerload` in the Firebase project.
2. Download its `google-services.json` into `app/google-services.json`. The path is
   gitignored; store the file only in the protected build environment.
3. Build normally:

   ```bash
   sh ./gradlew :app:assembleDebug
   sh ./gradlew :app:assembleRelease
   ```

`app/build.gradle.kts` checks that exact file. Only then does it apply
`com.google.gms.google-services` and `com.google.firebase.crashlytics` 3.0.7.
Without the file, both plugins remain unapplied and
`BuildConfig.FIREBASE_CONFIGURED=false`.

The app obtains `FirebaseCrashlytics` only when that build flag is true. The previous
activity-level default uncaught-exception wrapper was removed so it cannot recursively
invoke or duplicate Crashlytics' handler.

## Data policy

Crashlytics custom keys are deliberately limited to:

- `app_version`;
- `sync_mode` (`device` or `server`);
- `local_only` (`true` or `false`).

Do not add email, account/user ID, device ID, load or Telegram text, file names/object
keys, bearer tokens, bot tokens, API keys, or raw request/response data. Review any
future log or exception metadata against this list before release.

## Release symbols

The release build enables R8 minification. When Firebase is configured, the
Crashlytics Gradle plugin creates its normal release mapping-upload task and wires it
to the release build, so the R8 mapping is uploaded to the Firebase application.
Run release builds only in a protected environment authorized for that Firebase
project. Confirm the mapping-upload task succeeds and that a non-fatal test from an
internal release resolves to readable stack frames before production promotion.

Do not make `google-services.json`, mapping files, service-account JSON, or Firebase
CLI credentials workflow artifacts. Builds without Firebase skip mapping upload and
remain green.

Backend FCM is configured separately with encrypted `FIREBASE_PROJECT_ID` and
`FIREBASE_CREDENTIALS_JSON`; no Firebase service account belongs in the Android app.
