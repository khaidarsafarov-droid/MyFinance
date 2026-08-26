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
        assertTrue(CloudSyncPolicy.needsFullHydration(-1L, localEntityCount = 0, remoteEntityCount = 1))
        assertFalse(CloudSyncPolicy.needsFullHydration(0L, localEntityCount = 2, remoteEntityCount = 5))
        assertFalse(CloudSyncPolicy.needsFullHydration(100L, localEntityCount = 0, remoteEntityCount = 5))
        assertFalse(CloudSyncPolicy.needsFullHydration(0L, localEntityCount = 0, remoteEntityCount = 0))
        assertFalse(CloudSyncPolicy.needsFullHydration(-1L, localEntityCount = 0, remoteEntityCount = 0))
    }

    @Test
    fun shouldPullIncremental() {
        assertTrue(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 10L, remoteUpdatedAt = 20L))
        assertTrue(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 1L, remoteUpdatedAt = Long.MAX_VALUE))
        assertFalse(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 20L, remoteUpdatedAt = 20L))
        assertFalse(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 20L, remoteUpdatedAt = 19L))
        assertFalse(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = 0L, remoteUpdatedAt = 20L))
        assertFalse(CloudSyncPolicy.shouldPullIncremental(lastSyncedAt = -1L, remoteUpdatedAt = 20L))
    }

    @Test
    fun localSnapshotForPush_keepsOnlyLocalIds() {
        val local = mapOf("keep" to Row("keep", 10, "local"))
        val pushed = CloudSyncPolicy.localSnapshotForPush(local)
        assertEquals(setOf("keep"), pushed.keys)
        assertEquals("local", pushed["keep"]?.value)
    }

    @Test
    fun orphanLocalIds_listsRemoteMissingRows() {
        val orphans = CloudSyncPolicy.orphanLocalIds(
            localIds = setOf("a", "b", "c"),
            remoteIds = setOf("a", "c"),
        )
        assertEquals(setOf("b"), orphans)
    }

    data class IntRow(val id: Int, val addedAt: Long, val amount: Double)

    @Test
    fun remoteIntIdsToApplyOnPull_insertsNewAndUpdatesWhenRemoteNewer() {
        val local = mapOf(
            1 to IntRow(1, 100L, 50.0),
            2 to IntRow(2, 200L, 60.0),
        )
        val remote = mapOf(
            1 to IntRow(1, 150L, 55.0),
            2 to IntRow(2, 150L, 70.0),
            3 to IntRow(3, 1L, 10.0),
        )
        val apply = CloudSyncPolicy.remoteIntIdsToApplyOnPull(local, remote) { it.addedAt }
        assertEquals(setOf(1, 3), apply)
    }

    @Test
    fun remoteIntIdsToApplyOnPull_skipsWhenLocalNewerOrEqual() {
        val local = mapOf(1 to IntRow(1, 300L, 50.0))
        val remote = mapOf(1 to IntRow(1, 200L, 99.0))
        val apply = CloudSyncPolicy.remoteIntIdsToApplyOnPull(local, remote) { it.addedAt }
        assertTrue(apply.isEmpty())
    }

    @Test
    fun remoteIntIdsToApplyOnPull_equalAddedAt_skipsEvenIfAmountChanged() {
        val local = mapOf(1 to IntRow(1, 100L, 50.0))
        val remote = mapOf(1 to IntRow(1, 100L, 99.0))
        val apply = CloudSyncPolicy.remoteIntIdsToApplyOnPull(local, remote) { it.addedAt }
        assertTrue(apply.isEmpty())
    }

    @Test
    fun mergeById_stillUnionsRemoteOnly_forIncrementalFieldMerge() {
        // Documented: mergeById is NOT for full-snapshot push (would resurrect deletes).
        val local = mapOf("a" to Row("a", 10, "local-a"))
        val remote = mapOf("b" to Row("b", 1, "remote-b"))
        val merged = CloudSyncPolicy.mergeById(local, remote) { it.updatedAt }
        assertTrue(merged.containsKey("b"))
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
