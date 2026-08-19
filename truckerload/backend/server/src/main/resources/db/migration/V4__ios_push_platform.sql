ALTER TABLE device_push_tokens
    DROP CONSTRAINT IF EXISTS device_push_tokens_platform_check;

ALTER TABLE device_push_tokens
    ADD CONSTRAINT device_push_tokens_platform_check
    CHECK (platform IN ('android', 'ios'));
