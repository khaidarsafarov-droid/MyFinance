package com.truckerload.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeletedLoadLedgerTest {

    @Test
    fun pendingThenDeleted_blocksTripAndLoad() {
        val ctx = RuntimeEnvironment.getApplication()
        DeletedLoadLedger.markPending(ctx, "id-1", "T-114N6Z1N6")
        assertTrue(DeletedLoadLedger.isBlocked(ctx, "id-1", null))
        assertTrue(DeletedLoadLedger.isBlocked(ctx, null, "T-114N6Z1N6"))
        assertTrue(DeletedLoadLedger.pendingHardDeleteIds(ctx).contains("id-1"))

        DeletedLoadLedger.markDeleted(ctx, "id-1", "T-114N6Z1N6")
        assertFalse(DeletedLoadLedger.pendingHardDeleteIds(ctx).contains("id-1"))
        assertTrue(DeletedLoadLedger.isBlocked(ctx, "id-1", "T-114N6Z1N6"))
        assertTrue(DeletedLoadLedger.hasAny(ctx))
    }

    @Test
    fun undoPending_unblocksLoad() {
        val ctx = RuntimeEnvironment.getApplication()
        DeletedLoadLedger.markPending(ctx, "id-2", "T-UNDO")
        DeletedLoadLedger.cancelPending(ctx, "id-2")
        assertFalse(DeletedLoadLedger.isBlocked(ctx, "id-2", null))
        assertFalse(DeletedLoadLedger.isBlocked(ctx, null, "T-UNDO"))
        assertFalse(DeletedLoadLedger.pendingHardDeleteIds(ctx).contains("id-2"))
    }
}
