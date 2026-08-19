# Backend setup

## Local Docker Compose

Requirements: JDK 21, Docker with Compose v2, and ports 8080, 7880, 9000, and 9001
available.

```bash
cd truckerload
cp .env.example .env
```

Replace every `change-me` value in `.env`. Use random development-only values; never
reuse production credentials. The Compose stack supplies PostgreSQL, MinIO, bucket
creation, the Ktor backend, and a LiveKit SFU (`--dev` on port 7880,
keys `devkey`/`secret` via `LIVEKIT_*` in `.env.example`). Without Docker:
`sh ./scripts/run-livekit.sh`.

```bash
docker compose config --quiet
docker compose up --build
```

Flyway runs before Ktor serves traffic. Verify:

```bash
curl --fail http://localhost:8080/health/live
curl --fail http://localhost:8080/health/ready
curl --fail http://localhost:8080/openapi.yaml
```

Compose sets `APP_ENV=prod` to exercise production validation. `/metrics` is disabled
unless `METRICS_BEARER_TOKEN` is non-empty. MinIO uses the internal
`http://minio:9000` endpoint while presigned URLs use `http://localhost:9000`.

To stop without deleting state use `docker compose down`; adding `--volumes`
irreversibly deletes local PostgreSQL and MinIO data.

## Local process without Docker

A PostgreSQL server is still required. For filesystem media storage, export:

```bash
export APP_ENV=dev
export DATABASE_URL='jdbc:postgresql://localhost:5432/truckerload'
export DATABASE_USER='truckerload'
export DATABASE_PASSWORD='development-only'
export SUPABASE_JWT_ISSUER='https://example.supabase.co/auth/v1'
export SUPABASE_JWT_AUDIENCE='authenticated'
export SUPABASE_JWT_SECRET='development-only-jwt-secret'
export TELEGRAM_WEBHOOK_SECRET='development-webhook-secret'
export STORAGE_KIND='local'
export LOCAL_STORAGE_PATH='./data/media'
export LOCAL_STORAGE_SIGNING_SECRET='development-storage-signing-secret'
export PUBLIC_BASE_URL='http://localhost:8080'
sh ./gradlew :backend:server:run
```

Development and test expose `/metrics` without a token. Test-only authentication is
available only with `APP_ENV=test` and `TEST_AUTH_ENABLED=true`; production startup
rejects that combination.

## Android client

Create gitignored `local.properties` in the project root:

```properties
sdk.dir=/absolute/path/to/android-sdk
LOCAL_ONLY_MODE=false
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<publishable-anon-key>
SYNC_BACKEND_URL=https://api.example.com
CLOUD_MEDIA_ENABLED=false
TELEGRAM_SYNC_MODE=device
TELEGRAM_SERVER_BOT_USERNAME=
```

`SYNC_BACKEND_URL` must use HTTPS outside loopback/debug builds. When it is blank, or
when `LOCAL_ONLY_MODE=true`, the app uses the on-device account mirror and Room only.
Media cloud sync is separately disabled by default. Set `CLOUD_MEDIA_ENABLED=true`
only for a staged authenticated cohort after upload, download, and deletion checks;
the flag still has no effect without the backend URL, non-local mode, and a JWT.
Set `TELEGRAM_SYNC_MODE=server` only after the webhook and authenticated inbox are
healthy. The server bot token never belongs in Android configuration.

## Production on DigitalOcean

Use `deploy/digitalocean/app.yaml` and the deployment runbook. Production runtime
configuration is:

| Variable | Requirement |
| --- | --- |
| `APP_ENV` | `prod` |
| `DATABASE_URL` | `${truckerload-db.JDBC_DATABASE_URL}` managed binding |
| `DATABASE_USER`, `DATABASE_PASSWORD` | managed binding values |
| `SUPABASE_JWT_ISSUER` | exact Supabase issuer ending `/auth/v1` |
| `SUPABASE_JWT_AUDIENCE` | normally `authenticated` |
| `SUPABASE_JWT_SECRET` | encrypted, minimum 32 characters |
| `TELEGRAM_WEBHOOK_SECRET` | encrypted, Telegram-compatible, minimum 16 characters |
| `METRICS_BEARER_TOKEN` | encrypted, minimum 32 characters; absence disables metrics |
| `STORAGE_KIND` | `s3` |
| `S3_BUCKET`, `S3_REGION` | Space bucket and region |
| `S3_ENDPOINT`, `S3_PUBLIC_ENDPOINT` | regional Spaces HTTPS endpoint |
| `S3_PATH_STYLE` | `false` for Spaces |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | encrypted restricted Space key |
| `PUBLIC_BASE_URL` | public App Platform HTTPS origin |
| `UPLOAD_EXPIRY_SECONDS`, `DOWNLOAD_EXPIRY_SECONDS` | short-lived signed URL TTLs (default 900) |
| `FIREBASE_PROJECT_ID` | optional FCM project |
| `FIREBASE_CREDENTIALS_JSON` | optional encrypted service-account JSON |
| `LIVEKIT_URL` | optional `wss://` / `https://` LiveKit Cloud or self-host URL |
| `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` | optional; all three LiveKit vars must be set together. Mints `/v1/voice/token`. Secret never goes to the APK |

`TEST_AUTH_ENABLED` must be absent. Local object-storage variables are unnecessary
with S3. App Platform terminates TLS and forwards to port 8080.

Post-deploy checks:

1. `/health/live` is 200.
2. `/health/ready` is 200 and therefore both PostgreSQL and Spaces respond.
3. `/metrics` is 401 without a credential and returns Prometheus text with the
   configured Bearer credential.
4. `deploy/digitalocean/telegram-webhook.sh status` reports the expected HTTPS URL
   and no last error.
5. An authenticated test account can upload/read a snapshot; Telegram and FCM remain
   behind their rollout phases.

## Verification

No database, Docker daemon, Firebase credential, Supabase secret, or Telegram token is
needed for the unit suites:

```bash
sh ./gradlew :shared:contract:jvmTest \
  :shared:domain:jvmTest \
  :backend:server:test \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

Validate the API and deployment inputs separately:

```bash
npx --yes @redocly/cli@2.40.0 lint --extends=minimal \
  backend/server/src/main/resources/openapi.yaml
test -f .env || cp .env.example .env
docker compose config --quiet
doctl apps spec validate --schema-only deploy/digitalocean/app.yaml
```

The last two commands require Docker Compose and `doctl` respectively, but schema-only
App Platform validation does not require DigitalOcean authentication.
