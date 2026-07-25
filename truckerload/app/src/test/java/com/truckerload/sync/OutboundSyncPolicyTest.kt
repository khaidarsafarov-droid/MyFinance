package com.truckerload.sync

import com.truckerload.data.local.entities.SyncOutboxEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutboundSyncPolicyTest {
    @Test
    fun `row becomes synced only after snapshot acknowledgement`() {
        val update = OutboundSyncPolicy.afterSnapshotUpload(2, uploadAcknowledged = true)

        assertEquals(SyncOutboxEntity.STATUS_SYNCED, update.status)
        assertEquals(3, update.attempts)
        assertNull(update.lastError)
    }

    @Test
    fun `failed upload retains pending row and records retry attempt`() {
        val update = OutboundSyncPolicy.afterSnapshotUpload(2, uploadAcknowledged = false)

        assertEquals(SyncOutboxEntity.STATUS_PENDING, update.status)
        assertEquals(3, update.attempts)
        assertEquals("snapshot_upload_failed", update.lastError)
    }
}
