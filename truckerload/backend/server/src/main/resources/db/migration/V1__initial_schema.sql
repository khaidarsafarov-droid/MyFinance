CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE account_snapshots (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    updated_at BIGINT NOT NULL CHECK (updated_at >= 0),
    entity_count INTEGER NOT NULL DEFAULT 0 CHECK (entity_count >= 0),
    checksum TEXT NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX account_snapshots_updated_at_idx ON account_snapshots (updated_at);

CREATE TABLE sync_cursors (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    cursor BIGINT NOT NULL CHECK (cursor >= 0),
    updated_at BIGINT NOT NULL CHECK (updated_at >= 0),
    PRIMARY KEY (user_id, device_id)
);

CREATE TABLE telegram_link_tokens (
    token_hash BYTEA PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX telegram_link_tokens_user_idx ON telegram_link_tokens (user_id, expires_at);
CREATE INDEX telegram_link_tokens_expiry_idx ON telegram_link_tokens (expires_at) WHERE used_at IS NULL;

CREATE TABLE telegram_links (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    chat_id BIGINT NOT NULL UNIQUE,
    username TEXT,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE telegram_inbox (
    update_id BIGINT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    message_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    message_text TEXT NOT NULL,
    sender_username TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMPTZ
);
CREATE INDEX telegram_inbox_user_update_idx ON telegram_inbox (user_id, update_id);
CREATE INDEX telegram_inbox_unacked_idx ON telegram_inbox (user_id, update_id)
    WHERE acknowledged_at IS NULL;

CREATE TABLE media_objects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    object_key TEXT NOT NULL UNIQUE,
    file_name TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    checksum TEXT,
    status TEXT NOT NULL CHECK (status IN ('pending', 'ready')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX media_objects_user_created_idx ON media_objects (user_id, created_at DESC);

-- Future normalized projection of the snapshot journal. The JSON payload remains authoritative.
CREATE TABLE loads (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    trip_id TEXT,
    payload JSONB NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX loads_user_updated_idx ON loads (user_id, updated_at);

CREATE TABLE stops (
    user_id UUID NOT NULL,
    load_id TEXT NOT NULL,
    id TEXT NOT NULL,
    payload JSONB NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, load_id, id),
    FOREIGN KEY (user_id, load_id) REFERENCES loads(user_id, id) ON DELETE CASCADE
);
CREATE INDEX stops_user_updated_idx ON stops (user_id, updated_at);

CREATE TABLE penalties (
    user_id UUID NOT NULL,
    load_id TEXT NOT NULL,
    id TEXT NOT NULL,
    payload JSONB NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, load_id, id),
    FOREIGN KEY (user_id, load_id) REFERENCES loads(user_id, id) ON DELETE CASCADE
);
CREATE INDEX penalties_user_updated_idx ON penalties (user_id, updated_at);

CREATE TABLE diesel (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    payload JSONB NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX diesel_user_updated_idx ON diesel (user_id, updated_at);

CREATE TABLE paychecks (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    payload JSONB NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX paychecks_user_updated_idx ON paychecks (user_id, updated_at);
