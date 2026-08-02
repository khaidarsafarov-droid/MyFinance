package com.truckerload.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelegramFgsQuotaTest {

    @Before
    fun setUp() {
        TelegramFgsQuota.resetForTests()
    }

    @Test
    fun markTimedOut_pausesUntilCleared() {
        assertFalse(TelegramFgsQuota.isPaused())
        TelegramFgsQuota.markTimedOut()
        assertTrue(TelegramFgsQuota.isPaused())
        TelegramFgsQuota.clearPause()
        assertFalse(TelegramFgsQuota.isPaused())
    }
}

class TelegramFgsTimeoutApiGuardTest {

    @Test
    fun serviceDeclaresOnTimeoutOverride() {
        // Compile-time + reflection guard: Android 15 requires onTimeout(startId, fgsType)
        // or the process crashes with ForegroundServiceDidNotStopInTimeException.
        val methods = TelegramBotForegroundService::class.java.declaredMethods
        val hasTimeout = methods.any { m ->
            m.name == "onTimeout" &&
                m.parameterTypes.size == 2 &&
                m.parameterTypes[0] == Int::class.javaPrimitiveType &&
                m.parameterTypes[1] == Int::class.javaPrimitiveType
        }
        assertTrue("TelegramBotForegroundService must override onTimeout(int, int)", hasTimeout)
    }
}
