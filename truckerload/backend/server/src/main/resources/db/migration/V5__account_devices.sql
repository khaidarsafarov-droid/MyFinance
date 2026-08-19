CREATE TABLE account_devices (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    form_factor TEXT NOT NULL CHECK (form_factor IN ('phone', 'tablet')),
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, device_id),
    CONSTRAINT account_devices_one_slot_per_form UNIQUE (user_id, form_factor)
);

CREATE INDEX account_devices_user_form_idx
    ON account_devices (user_id, form_factor);
