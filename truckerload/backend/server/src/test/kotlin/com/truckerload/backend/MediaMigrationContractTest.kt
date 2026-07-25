package com.truckerload.backend

import kotlin.test.Test
import kotlin.test.assertTrue

class MediaMigrationContractTest {
    @Test
    fun `v3 contains ownership idempotency tombstone and revision indexes`() {
        val sql = requireNotNull(
            javaClass.getResource("/db/migration/V3__durable_media_sync.sql"),
        ).readText()

        assertTrue(sql.contains("ADD COLUMN kind TEXT"))
        assertTrue(sql.contains("ADD COLUMN client_id TEXT"))
        assertTrue(sql.contains("ADD COLUMN metadata JSONB"))
        assertTrue(sql.contains("ADD COLUMN deleted_at TIMESTAMPTZ"))
        assertTrue(sql.contains("media_objects_user_kind_client_unique"))
        assertTrue(sql.contains("(user_id, kind, client_id)"))
        assertTrue(sql.contains("media_objects_user_revision_idx"))
    }
}
