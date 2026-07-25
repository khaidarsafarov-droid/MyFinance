# TruckerLoad

TruckerLoad is a native Android trucking journal built with Kotlin, Jetpack Compose,
Room, and WorkManager. It remains fully local-first. When an HTTPS
`SYNC_BACKEND_URL` is configured, the app also synchronizes account snapshots through
the Ktor/JDK 21 backend; blank URL or `LOCAL_ONLY_MODE=true` retains the local mirror
fallback.

The repository contains:

- `app/` — Android app, including on-device Telegram fallback, OCR/scanner, media,
  analytics, and widgets;
- `shared/contract/` — versioned Kotlin API contracts;
- `backend/server/` — Ktor API for PostgreSQL snapshots, Telegram inbox, S3 media
  metadata, FCM wake-ups, health, and Prometheus metrics;
- `deploy/digitalocean/` — the single-cloud App Platform target and recovery runbooks.

There is no web/Expo client. The Telegram bot runs in the Android foreground service
in device mode or through the Ktor webhook in server mode.

## Build and test

JDK 21 and Android SDK 34 are required.

```bash
sh ./gradlew :shared:contract:test \
  :backend:server:test \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

Backend unit tests use in-memory fakes and need no database, Docker daemon, Telegram,
Firebase, Supabase, or DigitalOcean credentials. The debug APK is written under
`app/build/outputs/apk/debug/`.

For local backend containers:

```bash
cp .env.example .env
# Replace every change-me development value.
docker compose up --build
```

## Configuration and operations

- [Target architecture](docs/TARGET_ARCHITECTURE.md)
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
- Optional FCM and Crashlytics only when `app/google-services.json` is present
- Durable photo/scan cloud queue and verified cross-device download, gated off by
  default with `CLOUD_MEDIA_ENABLED=false`

ML Kit document scanning requires Google Play Services on a physical device. The
camera flow remains the scanner fallback.
