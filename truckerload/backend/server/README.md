# TruckerLoad backend

Ktor service for account snapshots, per-device cursors, direct object-storage uploads,
and the durable Telegram inbox. The Android app remains local-first and is not required
to depend on this module.

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

`TEST_AUTH_ENABLED` is rejected unless `APP_ENV=test`. Production accepts only an
HMAC Supabase JWT with matching issuer, audience, and UUID subject.

## Telegram setup

Configure Telegram's webhook URL as `/v1/telegram/webhook` and set its
`secret_token` to `TELEGRAM_WEBHOOK_SECRET`. An authenticated client creates a
single-use token with `POST /v1/telegram/link-token`; the user then sends
`/start <token>` to the bot. Subsequent text updates are stored idempotently by
Telegram `update_id` and read through the authenticated inbox endpoint.

## API and verification

The OpenAPI document is served at `/openapi.yaml`; `/docs` redirects to it.

```bash
sh ./gradlew :shared:contract:test :backend:server:test :app:compileDebugKotlin
```

Tests use in-memory repository and storage fakes and do not require Docker.
Production logs use `logback-prod.xml` (JSON); the default local/test config is readable
text. The container selects the production config through `JAVA_OPTS`.
