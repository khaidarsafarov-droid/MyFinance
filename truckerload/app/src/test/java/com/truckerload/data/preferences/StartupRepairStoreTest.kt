package com.truckerload.data.preferences

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartupRepairStoreTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `backfill done is per user and success gated`() {
        val store = StartupRepairStore(context)
        val a = "user-a"
        val b = "user-b"

        assertFalse(store.isBackfillDone(a))
        store.markBackfillDone(a)
        assertTrue(store.isBackfillDone(a))
        assertFalse(store.isBackfillDone(b))
    }

    @Test
    fun `needs retry cleared when mark done`() {
        val store = StartupRepairStore(context)
        val user = "retry-user"
        store.markBackfillNeedsRetry(user)
        assertTrue(store.needsBackfillRetry(user))
        store.markBackfillDone(user)
        assertFalse(store.needsBackfillRetry(user))
        assertTrue(store.isBackfillDone(user))
    }

    @Test
    fun `key helpers sanitize user id`() {
        val key = StartupRepairStore.backfillKey("ab/../cd")
        assertFalse(key.contains(".."))
        assertTrue(key.startsWith("startup_backfill_v2_"))
    }

    @Test
    fun `session repair is per user one-shot`() {
        val store = StartupRepairStore(context)
        val a = "user-a"
        val b = "user-b"

        assertFalse(store.isSessionRepairDone(a))
        store.markSessionRepairDone(a)
        assertTrue(store.isSessionRepairDone(a))
        assertFalse(store.isSessionRepairDone(b))

        val key = StartupRepairStore.sessionRepairKey("ab/../cd")
        assertFalse(key.contains(".."))
        assertTrue(key.startsWith("session_repair_v1_done_"))
    }
}
