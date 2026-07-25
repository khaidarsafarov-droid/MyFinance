ALTER TABLE media_objects
    ADD COLUMN kind TEXT,
    ADD COLUMN client_id TEXT,
    ADD COLUMN load_id TEXT,
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN updated_at BIGINT;

-- The old API had no media kind/client id. Preserve those rows with a stable,
-- conservative kind and use the server id as their idempotency key.
UPDATE media_objects
SET kind = 'SCAN',
    client_id = id::text,
    updated_at = (EXTRACT(EPOCH FROM COALESCE(completed_at, created_at)) * 1000)::BIGINT;

ALTER TABLE media_objects
    ALTER COLUMN kind SET NOT NULL,
    ALTER COLUMN client_id SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT media_objects_kind_check CHECK (kind IN ('PHOTO', 'SCAN'));

CREATE SEQUENCE media_objects_revision_seq;
ALTER TABLE media_objects
    ADD COLUMN revision BIGINT NOT NULL DEFAULT nextval('media_objects_revision_seq');

CREATE UNIQUE INDEX media_objects_user_kind_client_unique
    ON media_objects (user_id, kind, client_id);
CREATE INDEX media_objects_user_revision_idx
    ON media_objects (user_id, revision);
CREATE INDEX media_objects_user_kind_revision_idx
    ON media_objects (user_id, kind, revision);
CREATE INDEX media_objects_user_active_idx
    ON media_objects (user_id, created_at DESC)
    WHERE deleted_at IS NULL;
