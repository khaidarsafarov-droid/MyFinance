# DigitalOcean deployment and rollback

DigitalOcean App Platform is the only production cloud target. It terminates TLS,
runs one backend instance initially, pulls `truckerload-backend:production` from
DigitalOcean Container Registry (DOCR), and binds a DigitalOcean Managed PostgreSQL
cluster. The API is stateless and Telegram updates are idempotent, so increasing
`instance_count` is safe after database and request metrics justify it.

## One-time provisioning

1. In one DigitalOcean region, create:
   - a DOCR registry;
   - a Managed PostgreSQL 18 cluster, database `truckerload`, and restricted app user;
   - a private Space for media. Enable object versioning and deny anonymous listing;
   - an App Platform app from a local copy of `app.yaml`.
2. In that local copy, replace every `REPLACE_` value. `cluster_name` must be the
   existing managed database cluster name. Keep the database component name
   `truckerload-db` because the JDBC binding refers to it.
3. Generate independent random values of at least 32 bytes for
   `SUPABASE_JWT_SECRET`, `TELEGRAM_WEBHOOK_SECRET`, and
   `METRICS_BEARER_TOKEN`. Set the Space access key pair as encrypted variables.
   Never commit the submitted or downloaded spec because it contains
   account-specific encrypted values.
4. Set `SUPABASE_JWT_ISSUER` to
   `https://<project>.supabase.co/auth/v1`. Use the same legacy HS256 JWT secret
   configured by Supabase and keep audience `authenticated`.
5. Optional FCM: set `FIREBASE_PROJECT_ID` and place the complete Firebase service
   account JSON in the encrypted `FIREBASE_CREDENTIALS_JSON` variable. Leave both
   disabled when FCM is not in the rollout phase.
6. Validate before submission:

   ```bash
   doctl apps spec validate --schema-only deploy/digitalocean/app.yaml
   doctl apps create --spec /secure/path/app.yaml
   ```

   The committed spec intentionally contains placeholders and is a template; submit
   only the locally completed copy.
7. Attach the production domain in App Platform. App Platform provisions and renews
   managed TLS. Set `PUBLIC_BASE_URL` to that HTTPS origin.
8. Confirm `/health/live` and `/health/ready` return 200. Readiness verifies both
   PostgreSQL and the configured Space. Scrape `/metrics` with
   `Authorization: Bearer <METRICS_BEARER_TOKEN>`; do not put this token in a query
   string.

## Telegram webhook

The script disables shell tracing and passes credentials to curl over standard input,
so tokens are not placed in command arguments.

```bash
export TELEGRAM_BOT_TOKEN='...'
export TELEGRAM_WEBHOOK_SECRET='...'
export TELEGRAM_WEBHOOK_URL='https://api.example.com/v1/telegram/webhook'
deploy/digitalocean/telegram-webhook.sh set
deploy/digitalocean/telegram-webhook.sh status
```

The webhook secret must exactly equal the backend variable. Do not run with shell
debug tracing. To stop server processing without discarding pending Telegram updates:

```bash
DROP_PENDING_UPDATES=false deploy/digitalocean/telegram-webhook.sh delete
```

## Routine deployment

Create the GitHub environment `production`, require an approver, and add only
`DIGITALOCEAN_ACCESS_TOKEN` as a GitHub secret. The token needs registry push access;
limit its DigitalOcean scope and rotate it independently. Run the manual
`Deploy TruckerLoad to DigitalOcean` workflow and provide the non-secret registry
name. The workflow:

1. runs contract and backend tests;
2. builds the backend image;
3. pushes immutable `<git-sha>` and moving `production` tags to DOCR.

The App Platform spec has `deploy_on_push: true`, so the `production` push starts a
managed rolling deployment. No credentials are exposed to pull-request workflows.

After deployment, verify:

```bash
curl --fail --silent --show-error https://api.example.com/health/live
curl --fail --silent --show-error https://api.example.com/health/ready
deploy/digitalocean/telegram-webhook.sh status
```

Check HTTP 5xx rate, request latency, snapshot stale ratio, Telegram rejection and
duplicate rates, rate-limit hits, and FCM failure count before completing the change record.

## Edge rate limits (recommended)

In-process `SlidingWindowRateLimiter` is a safety net only. Put CDN / App Platform /
Cloudflare (or nginx) limits in front of the API:

| Route | Suggested edge limit |
|-------|----------------------|
| `POST /v1/telegram/webhook` | 60 req/min/IP |
| `PUT /v1/sync/snapshot` | 20 req/min/user (or IP) |
| `PUT/GET /v1/media/local-*` | 30 req/min/IP |
| Other `/v1/*` | 120 req/min/IP |

Reject with HTTP 429 and keep origin `METRICS_BEARER_TOKEN` scrape private.

## Monitoring alerts

Scrape `/metrics` with `Authorization: Bearer <METRICS_BEARER_TOKEN>`. Suggested alerts:

| Metric | Condition | Why |
|--------|-----------|-----|
| `truckerload_telegram_webhook_updates_total{result="rejected"}` | rate > 5/min for 5m | Webhook secret brute-force / misconfig |
| `truckerload_http_rate_limited_total` | rate > 10/min for 5m | Abuse or need to raise edge limits |
| `truckerload_snapshot_writes_total{result="accepted"}` | sudden spike | Account sync flood |
| `truckerload_http_server_requests_seconds_count{status="5xx"}` | any sustained | Backend fault |
| `truckerload_push_notification_failures_total` | rate > 1/min | FCM credentials / quota |

Client breadcrumb (Crashlytics custom key when Firebase is configured):
`import_dedup_dropped` — count of older Trip ID revisions discarded during import.

## Application rollback

Every deployment keeps an immutable Git SHA tag. To roll back, authenticate to DOCR,
retag the last known-good SHA as `production`, and push it:

```bash
image="registry.digitalocean.com/<registry>/truckerload-backend"
docker pull "$image:<known-good-sha>"
docker tag "$image:<known-good-sha>" "$image:production"
docker push "$image:production"
```

App Platform deploys that image. Verify both health endpoints and the OpenAPI version.
Flyway migrations are forward-only: application rollback is allowed only while the
known-good image is compatible with the current schema. Do not reverse a migration in
place. For destructive schema incidents, stop writes and follow `BACKUP_RESTORE.md`.

Telegram can be rolled back independently with `deleteWebhook`; Android
`TELEGRAM_SYNC_MODE=device` remains the client fallback. A failed media phase can be
disabled by removing the feature flag/client rollout while retaining objects and
metadata for later reconciliation.
