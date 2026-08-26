# TruckoRig

TruckoRig is a native Android trucking journal built with Kotlin, Jetpack Compose,
Room, and WorkManager. It remains fully local-first. When an HTTPS
`SYNC_BACKEND_URL` is configured, the app also synchronizes account snapshots through
the Ktor/JDK 21 backend; blank URL or `LOCAL_ONLY_MODE=true` retains the local mirror
fallback.

The repository contains:

- `app/` — Android app, including on-device Telegram fallback, OCR/scanner, media,
  analytics, and widgets;
- `ios/` — SwiftUI iOS client that links the KMP umbrella framework `TruckerLoadShared`
  (`:shared`). Requires Xcode on macOS; see [ios/README.md](ios/README.md);
- `shared/` — umbrella KMP module exported to iOS as `TruckerLoadShared.framework`;
- `shared/contract/` — KMP API contracts (JVM always; iOS targets on macOS);
- `shared/domain/` — KMP portable domain (goal math today; parsers later);
- `backend/server/` — Ktor API for PostgreSQL snapshots, Telegram inbox, S3 media
  metadata, FCM wake-ups, health, and Prometheus metrics;
- `deploy/digitalocean/` — the single-cloud App Platform target and recovery runbooks.

There is no web/Expo client. The Telegram bot runs in the Android foreground service
in device mode or through the Ktor webhook in server mode. iOS does not run a device
Telegram long-poll; use server sync when that client is built.
iOS sharing plan: [docs/KMP_IOS_ROADMAP.md](docs/KMP_IOS_ROADMAP.md).

Send this one file to Cloud Code / Claude Code for product context:
[docs/CLOUD_CODE_BRIEF.md](docs/CLOUD_CODE_BRIEF.md).

## Share with friends (no server)

Local-first APK for friends: data stays on the phone; optional file / Google Drive
backup. See [docs/FRIENDS_SHARE.md](docs/FRIENDS_SHARE.md) and
`scripts/build-friends-apk.sh`.


JDK 21 and Android SDK 34 are required.

```bash
sh ./gradlew :shared:jvmTest \
  :shared:contract:jvmTest \
  :shared:domain:jvmTest \
  :backend:server:test \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

Backend unit tests use in-memory fakes and need no database, Docker daemon, Telegram,
Firebase, Supabase, or DigitalOcean credentials. The debug APK is written under
`app/build/outputs/apk/debug/`.

## Dependency injection

The Android app uses Dagger/Hilt 2.60.1 with KSP. `SingletonComponent` contains only
process-safe, application-context wrappers such as auth, profile, settings, and push
token stores. `TruckerLoadApp`, `MainActivity`, and the Firebase messaging service are
Hilt entry points; the existing Compose `CompositionLocal` surface remains in place.

Room databases and their repositories are deliberately created after the active user
is known and are rebuilt on account changes. They are not application singletons,
which prevents one user's database from surviving into another user's session.
Workers remain framework-constructed; this migration does not install a custom
WorkManager/Hilt worker factory.

For local backend containers:

```bash
cp .env.example .env
# Replace every change-me development value.
docker compose up --build
```

## Configuration and operations

- [Target architecture](docs/TARGET_ARCHITECTURE.md)
- [KMP / iOS client](docs/KMP_IOS_ROADMAP.md)
- [Backend local/production setup](docs/BACKEND_SETUP.md)
- [Cloud sync behavior](docs/CLOUD_DATA_SYNC.md)
- [Phased migration and rollback](docs/MIGRATION_ROLLOUT.md)
- [Optional Firebase/Crashlytics](docs/ANDROID_FIREBASE_SETUP.md)
- [DigitalOcean deployment](deploy/digitalocean/DEPLOYMENT.md)
- [Backup and restore](deploy/digitalocean/BACKUP_RESTORE.md)

The backend serves `/health/live`, dependency-aware `/health/ready`,
`/openapi.yaml`, and `/metrics`. Production metrics are unavailable unless
`METRICS_BEARER_TOKEN` is configured, then require it as a Bearer credential.

## Android capabilities

- Local journal, diesel, paychecks, goals, and analytics
- ML Kit document scanner and Latin OCR, with Tesseract `rus+eng`
- Geotagged camera, gallery, PDFs, and load attachments
- Google/email account flow with local-only development mode
- Device or server Telegram ingestion
- Google Assistant / Gemini: App Actions for real TruckoRig screens (loads, goal, community, friends map, chat/call with a friend). See [docs/VOICE_ASSISTANTS.md](docs/VOICE_ASSISTANTS.md).
- Optional FCM and Crashlytics only when `app/google-services.json` is present
- Durable photo/scan cloud queue and verified cross-device download, gated off by
  default with `CLOUD_MEDIA_ENABLED=false`

ML Kit document scanning requires Google Play Services on a physical device. The
camera flow remains the scanner fallback.
