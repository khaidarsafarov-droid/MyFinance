package com.truckerload.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncPolicyTest {

    data class Row(val id: String, val updatedAt: Long, val value: String)

    @Test
    fun remoteWins_whenNewer() {
        assertTrue(CloudSyncPolicy.remoteWins(localUpdatedAt = 100L, remoteUpdatedAt = 200L))
        assertFalse(CloudSyncPolicy.remoteWins(localUpdatedAt = 200L, remoteUpdatedAt = 200L))
        assertFalse(CloudSyncPolicy.remoteWins(localUpdatedAt = 300L, remoteUpdatedAt = 200L))
        assertTrue(CloudSyncPolicy.remoteWins(localUpdatedAt = null, remoteUpdatedAt = 1L))
    }

    @Test
    fun mergeById_keepsNewerAndUnion() {
        val local = mapOf(
            "a" to Row("a", 10, "local-a"),
            "b" to Row("b", 50, "local-b"),
        )
        val remote = mapOf(
            "a" to Row("a", 20, "remote-a"),
            "c" to Row("c", 1, "remote-c"),
        )
        val merged = CloudSyncPolicy.mergeById(local, remote) { it.updatedAt }
        assertEquals("remote-a", merged["a"]?.value)
        assertEquals("local-b", merged["b"]?.value)
        assertEquals("remote-c", merged["c"]?.value)
        assertEquals(3, merged.size)
    }

    @Test
    fun needsFullHydration_onlyWhenEmptyLocalAndRemoteHasData() {
        assertTrue(CloudSyncPolicy.needsFullHydration(0L, localEntityCount = 0, remoteEntityCount = 5))
        assertFalse(CloudSyncPolicy.needsFullHydration(0L, localEntityCount = 2, remoteEntityCount = 5))
        assertFalse(CloudSyncPolicy.needsFullHydration(100L, localEntityCount = 0, remoteEntityCount = 5))
        assertFalse(CloudSyncPolicy.needsFullHydration(0L, localEntityCount = 0, remoteEntityCount = 0))
    }

    @Test
    fun shouldPullIncremental() {
        assertTrue(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 10L, remoteUpdatedAt = 20L))
        assertFalse(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 20L, remoteUpdatedAt = 20L))
        assertFalse(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 0L, remoteUpdatedAt = 20L))
    }

    @Test
    fun snapshotCodec_roundTrip() {
        val snap = AccountCloudSnapshot(
            accountId = "local_abc",
            updatedAt = 42L,
            driverProfileJson = """{"displayName":"Ivan"}""",
        )
        val json = AccountCloudSnapshotCodec.toJson(snap)
        val back = AccountCloudSnapshotCodec.fromJson(json)!!
        assertEquals("local_abc", back.accountId)
        assertEquals(42L, back.updatedAt)
        assertEquals("""{"displayName":"Ivan"}""", back.driverProfileJson)
    }
}
