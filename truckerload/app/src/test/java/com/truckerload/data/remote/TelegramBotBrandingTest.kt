package com.truckerload.data.remote

import com.truckerload.utils.BrandConstants
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramBotBrandingTest {

    @Test
    fun displayName_isTruckoRig() {
        assertEquals("TruckoRig", TelegramBotBranding.DISPLAY_NAME)
        assertEquals("TruckoRig", BrandConstants.DISPLAY_NAME)
    }

    @Test
    fun nameStamp_tiesFingerprintToBrand() {
        val token = "123456789:AA" + "x".repeat(35)
        val fp = TelegramBotTokenFingerprint.of(token)
        val stamp = TelegramBotBranding.nameStamp(fp)
        assertTrue(stamp.startsWith("$fp:"))
        assertTrue(stamp.endsWith("TruckoRig"))
    }

    @Test
    fun isNameApplied_falseUntilStampStored() {
        val context = RuntimeEnvironment.getApplication()
        val token = "123456789:AA" + "x".repeat(35)
        val prefs = context.getSharedPreferences("telegram_sync", 0)
        prefs.edit().clear().commit()
        assertFalse(TelegramBotBranding.isNameApplied(prefs, token))
        assertFalse(TelegramBotBranding.isPhotoApplied(prefs, token))
        val fp = TelegramBotTokenFingerprint.of(token)
        prefs.edit()
            .putString(TelegramBotBranding.KEY_NAME, TelegramBotBranding.nameStamp(fp))
            .putString(TelegramBotBranding.KEY_PHOTO, fp)
            .commit()
        assertTrue(TelegramBotBranding.isNameApplied(prefs, token))
        assertTrue(TelegramBotBranding.isPhotoApplied(prefs, token))
    }

    @Test
    fun tokenActivator_appliesBrandingOnSave() {
        val src = readMain("data/remote/TelegramTokenActivator.kt")
        assertTrue(src.contains("TelegramBotBranding.apply"))
        assertTrue(src.contains("forceName = true"))
    }

    @Test
    fun pollerAndWorker_retryBranding() {
        val fgs = readMain("sync/TelegramBotForegroundService.kt")
        val worker = readMain("sync/TelegramSyncWorker.kt")
        assertTrue(fgs.contains("TelegramBotBranding.apply"))
        assertTrue(worker.contains("TelegramBotBranding.apply"))
    }

    private fun readMain(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/com/truckerload/$relativePath"),
            File("app/src/main/java/com/truckerload/$relativePath"),
            File("../app/src/main/java/com/truckerload/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Source not found: $relativePath")
    }
}

class TelegramApiBrandingPayloadTest {

    @Test
    fun setMyNamePayload_isTruckoRigWithoutLanguageOverride() {
        val obj = JSONObject(TelegramApi.setMyNamePayload(BrandConstants.DISPLAY_NAME))
        assertEquals("TruckoRig", obj.getString("name"))
        assertFalse(obj.has("language_code"))
    }

    @Test
    fun requireTelegramOk_rejectsOkFalseEvenOnHttp200() {
        try {
            TelegramApi.requireTelegramOk(
                "setMyName",
                200,
                """{"ok":false,"description":"BOT_NAME_INVALID"}""",
            )
            org.junit.Assert.fail("expected failure")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("BOT_NAME_INVALID"))
        }
    }

    @Test
    fun requireTelegramOk_acceptsOkTrue() {
        TelegramApi.requireTelegramOk("setMyName", 200, """{"ok":true,"result":true}""")
    }
}
