CREATE TABLE device_push_tokens (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    platform TEXT NOT NULL CHECK (platform IN ('android')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, device_id)
);

CREATE INDEX device_push_tokens_user_idx
    ON device_push_tokens (user_id, updated_at DESC);
