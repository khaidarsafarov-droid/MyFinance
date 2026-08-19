# TruckerLoad backend

Ktor service for account snapshots, per-device cursors, direct object-storage uploads,
the durable Telegram inbox, and optional FCM sync pushes. The Android app remains
local-first.

## Run locally

From the repository root:

```bash
cp .env.example .env
# Replace every change-me value in .env.
docker compose up --build
```

PostgreSQL is available only to the Compose network. MinIO is exposed on ports `9000`
(S3) and `9001` (console), and the API is exposed on port `8080`. Flyway migrations run
before Ktor starts. Compose uses an internal MinIO endpoint for backend operations and
`S3_PUBLIC_ENDPOINT=http://localhost:9000` when generating browser/device upload URLs.

For a non-container development server, provide the same environment variables and run:

```bash
sh ./gradlew :backend:server:run
```

`DATABASE_URL` must be a JDBC URL, for example
`jdbc:postgresql://localhost:5432/truckerload`.

## Required production configuration

- `APP_ENV=prod`
- `DATABASE_URL`, and optionally `DATABASE_USER` / `DATABASE_PASSWORD`
- `SUPABASE_JWT_SECRET`
- `SUPABASE_JWT_ISSUER` (or `SUPABASE_URL`) and `SUPABASE_JWT_AUDIENCE`
- `TELEGRAM_WEBHOOK_SECRET` (the same exact value configured with Telegram)
- `STORAGE_KIND=s3`, `S3_BUCKET`, `S3_REGION`; optionally `S3_ENDPOINT`,
  `S3_PUBLIC_ENDPOINT`, and `S3_PATH_STYLE`
- AWS credentials through the standard AWS SDK provider chain
- `PUBLIC_BASE_URL` and `LOCAL_STORAGE_SIGNING_SECRET` when `STORAGE_KIND=local`
- Optional `METRICS_BEARER_TOKEN` (minimum 32 characters in production). Production
  returns 404 from `/metrics` when it is absent and requires a constant-time-checked
  Bearer credential when it is set. Development and test expose metrics without auth.
- Optional `FIREBASE_PROJECT_ID`; Firebase Admin uses encrypted
  `FIREBASE_CREDENTIALS_JSON` when supplied, otherwise application default
  credentials. Without usable credentials notification delivery is a no-op.

`TEST_AUTH_ENABLED` is rejected unless `APP_ENV=test`. Production accepts only an
HMAC Supabase JWT with matching issuer, audience, and UUID subject.

## Telegram setup

Configure Telegram's webhook URL as `/v1/telegram/webhook` and set its
`secret_token` to `TELEGRAM_WEBHOOK_SECRET`. An authenticated client creates a
single-use token with `POST /v1/telegram/link-token`; the user then sends
`/start <token>` to the bot. Subsequent text updates are stored idempotently by
Telegram `update_id` and read through the authenticated inbox endpoint.

For Android server mode, set `SYNC_BACKEND_URL` to the HTTPS API root,
`TELEGRAM_SYNC_MODE=server`, and optionally `TELEGRAM_SERVER_BOT_USERNAME` for the
clickable `https://t.me/<bot>?start=<token>` settings link. The app does not read or
store the server bot token in this mode. Configure Telegram's webhook separately:

```bash
curl -X POST "https://api.telegram.org/bot<token>/setWebhook" \
  -d "url=https://api.example.com/v1/telegram/webhook" \
  -d "secret_token=<TELEGRAM_WEBHOOK_SECRET>"
```

FCM registration uses authenticated `PUT /v1/devices/push-token`. Add
`app/google-services.json` only in credentialed Android build environments; builds
without it compile and Firebase registration remains inactive.

## Health, metrics, and logs

- `/health/live` checks the process only.
- `/health/ready` checks PostgreSQL and the configured local/S3 object store.
- `/metrics` uses the Prometheus text format. HTTP timers have only `method` and
  `status` labels; domain counters use bounded result labels. No account, user,
  device, token, or object identifiers are labels.
- Metrics include accepted/stale snapshot writes, accepted/rejected/duplicate
  Telegram webhook updates, and failed FCM sends.
- Request logs contain method, path, response status, and request ID. They deliberately
  omit query strings, Authorization and webhook-secret headers, and request bodies.

Example production scrape:

```bash
set +x
curl --fail --silent --show-error --config - <<EOF
url = "https://api.example.com/metrics"
header = "Authorization: Bearer ${METRICS_BEARER_TOKEN}"
EOF
```

Do not pass the metrics token in a URL or enable shell tracing around this command.

## API and verification

The OpenAPI document is served at `/openapi.yaml`; `/docs` redirects to it.

```bash
sh ./gradlew :shared:contract:jvmTest :shared:domain:jvmTest :backend:server:test :app:testDebugUnitTest :app:assembleDebug
```

Tests use in-memory repository and storage fakes and do not require Docker.
Production logs use `logback-prod.xml` (JSON); the default local/test config is readable
text. The container selects the production config through `JAVA_OPTS`.

The complete environment and DigitalOcean procedures are in
`docs/BACKEND_SETUP.md` and `deploy/digitalocean/`.
