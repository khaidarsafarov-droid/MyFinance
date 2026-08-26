package com.truckerload.data.remote

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramBotLogoJpegTest {

    @Test
    fun encode_producesJpegOfBrandLogo() {
        val jpeg = TelegramBotLogoJpeg.encode(RuntimeEnvironment.getApplication())
        assertTrue(jpeg != null && jpeg.size > 100)
        assertTrue(TelegramBotLogoJpeg.isJpeg(jpeg!!))
    }

    @Test
    fun isJpeg_rejectsNonJpeg() {
        assertTrue(!TelegramBotLogoJpeg.isJpeg(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)))
        assertTrue(!TelegramBotLogoJpeg.isJpeg(byteArrayOf(1, 2)))
    }
}
