package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthErrorsTest {

    @Test
    fun shouldStopService_on401() {
        assertTrue(TelegramAuthErrors.shouldStopService("Unauthorized 401"))
        assertTrue(TelegramAuthErrors.shouldStopService("HTTP 401: invalid token"))
    }

    @Test
    fun shouldStopService_falseForOtherErrors() {
        assertFalse(TelegramAuthErrors.shouldStopService(null))
        assertFalse(TelegramAuthErrors.shouldStopService("409 Conflict"))
        assertFalse(TelegramAuthErrors.shouldStopService("network timeout"))
    }
}

class TelegramWatchdogWorkNameTest {

    @Test
    fun uniqueWatchdogWork_isStable() {
        assertEquals("telegram_bot_watchdog", TelegramSyncWorker.UNIQUE_WATCHDOG_WORK)
    }
}

class TelegramLogoutStopsPollerGuardTest {

    @Test
    fun stopForLogout_exists() {
        val names = TelegramBotForegroundService::class.java.methods.map { it.name }.toSet()
        assertTrue("stopForLogout must exist for logout path", "stopForLogout" in names)
        assertTrue("stop must exist", "stop" in names)
    }

    @Test
    fun canStart_exists() {
        val names = TelegramBotForegroundService::class.java.methods.map { it.name }.toSet()
        assertTrue("canStart must exist for guarded poller restarts", "canStart" in names)
    }
}

class TelegramBotNoFgsContractGuardTest {

    @Test
    fun poller_isNotAnAndroidService() {
        assertFalse(
            android.app.Service::class.java.isAssignableFrom(TelegramBotForegroundService::class.java),
        )
    }
}
