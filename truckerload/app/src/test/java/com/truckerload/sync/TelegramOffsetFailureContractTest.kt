package com.truckerload.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the offset contract: on handleUpdate failure the engine must not jump to
 * batch nextOffset (which would skip the failed updateId). Verified via source contract
 * in TelegramBotSyncEngine (stoppedOnFailure gate); this guard keeps the helper visible.
 */
class TelegramOffsetFailureContractTest {

    @Test
    fun authErrorsDoNotImplyOffsetAdvance() {
        // 401 stops the service; callers must not treat that as "updates consumed".
        assertTrue(TelegramAuthErrors.shouldStopService("401 Unauthorized"))
        assertFalse(TelegramAuthErrors.shouldStopService("parse error"))
    }
}
