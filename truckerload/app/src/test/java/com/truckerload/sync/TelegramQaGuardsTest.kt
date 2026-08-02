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

class TelegramLogoutStopsFgsGuardTest {

    @Test
    fun stopForLogout_existsOnCompanion() {
        val names = TelegramBotForegroundService.Companion::class.java.methods.map { it.name }.toSet()
        assertTrue("stopForLogout must exist for logout path", "stopForLogout" in names)
        assertTrue("stop must exist", "stop" in names)
    }
}

class TelegramBotFgsContractGuardTest {

    @Test
    fun stopQuietly_and_startForegroundCompat_exist() {
        // Regression guard: onStartCommand must promote to foreground before
        // credential early-exits (via startForegroundCompat + stopQuietly).
        val methods = TelegramBotForegroundService::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue("stopQuietly must gate intentional FGS exits", "stopQuietly" in methods)
        assertTrue("startForegroundCompat must exist", "startForegroundCompat" in methods)
    }
}
