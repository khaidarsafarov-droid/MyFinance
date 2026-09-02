# TruckoRig

TruckoRig is a native Android trucking journal built with Kotlin, Jetpack Compose,
Room, and WorkManager. The journal, Telegram bot, and Google Drive backup all run
on the device. There is no product backend or Supabase service.

The repository contains:

- `app/` — Android app, including on-device Telegram bot, OCR/scanner, media,
  analytics, and widgets;
- `ios/` — SwiftUI iOS client that links the KMP umbrella framework `TruckerLoadShared`
  (`:shared`). Requires Xcode on macOS; see [ios/README.md](ios/README.md);
- `shared/` — umbrella KMP module exported to iOS as `TruckerLoadShared.framework`;
- `shared/contract/` — KMP identifiers shared with iOS (`:shared`);
- `shared/domain/` — KMP portable domain (goal math today; parsers later).

There is no web/Expo client. The Telegram bot runs in the Android foreground service.
iOS sharing plan: [docs/KMP_IOS_ROADMAP.md](docs/KMP_IOS_ROADMAP.md).

## Google Play release

Publish a signed Android App Bundle (not a sideloaded friends APK):

```bash
cd truckerload
sh ./gradlew :app:bundleRelease
```

The AAB is written under `app/build/outputs/bundle/release/`. Upload that file in
Play Console. Installs come from Google Play; in-app share sends the store link.

JDK 21 and Android SDK 34 are required.

```bash
sh ./gradlew :shared:jvmTest \
  :shared:contract:jvmTest \
  :shared:domain:jvmTest \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

The debug APK is written under `app/build/outputs/apk/debug/`.

## Dependency injection

The Android app uses Dagger/Hilt 2.60.1 with KSP. `SingletonComponent` contains only
process-safe, application-context wrappers such as auth, profile, and settings
stores. `TruckerLoadApp` and `MainActivity` are Hilt entry points; the existing
Compose `CompositionLocal` surface remains in place.

Room databases and their repositories are deliberately created after the active user
is known and are rebuilt on account changes. They are not application singletons,
which prevents one user's database from surviving into another user's session.

## Configuration and operations

- [Target architecture](docs/TARGET_ARCHITECTURE.md)
- [KMP / iOS client](docs/KMP_IOS_ROADMAP.md)
- [Optional Firebase/Crashlytics](docs/ANDROID_FIREBASE_SETUP.md)
- [Google Sign-In](docs/AUTH_GOOGLE.md)

## Android capabilities

- Local journal, diesel, paychecks, goals, and analytics
- ML Kit document scanner and Latin OCR, with Tesseract `rus+eng`
- Geotagged camera, gallery, PDFs, and load attachments
- Google/email account flow with local-only development mode
- On-device Telegram bot (foreground service)
- Optional Google Drive backup of the local journal
- Optional Crashlytics when `app/google-services.json` is present

ML Kit document scanning requires Google Play Services on a physical device. The
camera flow remains the scanner fallback.
