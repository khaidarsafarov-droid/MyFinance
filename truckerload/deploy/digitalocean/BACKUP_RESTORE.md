# Backup and restore runbook

## Objectives and protected data

- PostgreSQL RPO: at most 5 minutes, using Managed PostgreSQL continuous WAL/PITR.
- PostgreSQL service RTO: at most 60 minutes from incident declaration to a healthy
  backend against a verified replacement database.
- Media RPO: no committed object loss for single-object overwrite/delete events,
  using Space versioning; reconciliation may lag by at most 15 minutes.
- Full API plus media-verification RTO: at most 4 hours.

These are technical recovery objectives measured during restore exercises. They are
not backup schedules or delivery-date estimates. Account snapshots, cursors, Telegram
links/inbox, push tokens, and media metadata live in PostgreSQL. Binary media lives in
Spaces. Both must be recovered to the same consistency boundary.

## Controls

1. Enable DigitalOcean Managed PostgreSQL automated backups and point-in-time
   recovery. Alert if the provider reports backup or replication failure.
2. Enable Space object versioning and block public ACLs/listing. Use a dedicated
   application key without bucket-administration permission and a separate recovery
   key held outside App Platform.
3. Produce a compressed logical PostgreSQL dump every 24 hours into a private backup
   prefix in a second Space/region. Retain multiple generations and record a SHA-256
   checksum. Logical dumps are a secondary recovery path, not the 5-minute RPO path.
4. Export the active App Platform spec and Flyway migration version after each
   successful deployment. Store the export with deployment evidence, never in this
   repository if it contains encrypted environment values.
5. A backup is not considered usable until a restore into an isolated database passes
   Flyway validation, row-count checks, and authenticated snapshot/inbox smoke tests.

## Create a logical backup

Run from a restricted administration host. Prefer private database networking.
Credentials must not appear in command arguments or shell tracing.

```bash
set +x
umask 077
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

: "${PGHOST:?}" "${PGPORT:?}" "${PGDATABASE:?}" "${PGUSER:?}" "${PGPASSWORD:?}"
: "${BACKUP_SPACE_URI:?for example s3://private-backups/truckerload/postgres}"

printf '%s:%s:%s:%s:%s\n' \
  "$PGHOST" "$PGPORT" "$PGDATABASE" "$PGUSER" "$PGPASSWORD" > "$work/pgpass"
unset PGPASSWORD
export PGPASSFILE="$work/pgpass"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
dump="$work/truckerload-$stamp.dump"
pg_dump --format=custom --compress=9 --no-owner --no-privileges \
  --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" \
  --dbname="$PGDATABASE" --file="$dump"
sha256sum "$dump" > "$dump.sha256"
aws s3 cp "$dump" "$BACKUP_SPACE_URI/" --endpoint-url "$S3_BACKUP_ENDPOINT"
aws s3 cp "$dump.sha256" "$BACKUP_SPACE_URI/" --endpoint-url "$S3_BACKUP_ENDPOINT"
```

Use an S3 lifecycle policy to expire generations only after the retention control has
been reviewed. Monitor command exit status, uploaded size, and checksum-object
presence.

## PostgreSQL recovery

1. Stop or maintenance-gate writes. Delete the Telegram webhook with
   `DROP_PENDING_UPDATES=false`; clients retain local Room data for replay.
2. Select a recovery timestamp immediately before the first corrupt write. Restore
   the managed cluster to a new cluster using DigitalOcean PITR. Never restore over
   the only copy.
3. Bind a staging App Platform component to the new cluster and run:

   ```bash
   sh ./gradlew :backend:server:test
   # Starting the candidate image runs Flyway; then inspect:
   psql -c 'select version, success from flyway_schema_history order by installed_rank'
   ```

4. Compare counts and maximum timestamps for `app_users`, `account_snapshots`,
   `sync_cursors`, `telegram_inbox`, `media_objects`, and `device_push_tokens`.
   Sample account snapshots must deserialize, and all `ready` media records sampled
   must resolve in Spaces.
5. Update the production database binding to the recovered cluster and deploy the
   same known-good image. Require `/health/ready` to remain healthy through multiple
   checks before restoring the Telegram webhook.
6. Preserve the damaged cluster read-only for forensics. Record the recovered
   timestamp, measured data gap, RTO, checksum evidence, and validation results.

If PITR is unavailable, create an empty managed cluster and restore the verified
logical dump:

```bash
sha256sum --check truckerload-<timestamp>.dump.sha256
pg_restore --exit-on-error --no-owner --no-privileges \
  --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" \
  --dbname="$PGDATABASE" truckerload-<timestamp>.dump
```

## Spaces recovery and reconciliation

For accidental deletion, list versions with a recovery credential and copy the
selected prior version back to the same key:

```bash
aws s3api list-object-versions \
  --bucket "$S3_BUCKET" --prefix "$OBJECT_KEY" \
  --endpoint-url "$S3_ENDPOINT"
aws s3api copy-object \
  --bucket "$S3_BUCKET" --key "$OBJECT_KEY" \
  --copy-source "$S3_BUCKET/$OBJECT_KEY?versionId=$VERSION_ID" \
  --endpoint-url "$S3_ENDPOINT"
```

Reconcile database rows with object metadata:

- `pending` rows older than the upload expiry are safe candidates for cleanup;
- every `ready` row must have an object with the recorded size;
- unreferenced objects are quarantined before deletion;
- checksum mismatch is an incident, not an automatic overwrite.

After recovery, exercise a presigned upload and completion against a test account,
then verify the object through authenticated metadata APIs.
