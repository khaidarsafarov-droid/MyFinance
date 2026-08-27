package com.truckerload.data.remote

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramBotLogoJpegTest {

    @Test
    fun encodeBitmap_producesJpeg() {
        val src = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.BLUE)
        val jpeg = TelegramBotLogoJpeg.encodeBitmap(src)
        assertTrue(jpeg != null && jpeg.size > 100)
        assertTrue(TelegramBotLogoJpeg.isJpeg(jpeg!!))
        src.recycle()
    }

    @Test
    fun isJpeg_rejectsNonJpeg() {
        assertTrue(!TelegramBotLogoJpeg.isJpeg(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)))
        assertTrue(!TelegramBotLogoJpeg.isJpeg(byteArrayOf(1, 2)))
    }

    @Test
    fun plateFill_isKitPurpleNotLegacyNavy() {
        val src = File("src/main/java/com/truckerload/data/remote/TelegramBotLogoJpeg.kt").takeIf { it.isFile }
            ?: File("app/src/main/java/com/truckerload/data/remote/TelegramBotLogoJpeg.kt").takeIf { it.isFile }
            ?: File("../app/src/main/java/com/truckerload/data/remote/TelegramBotLogoJpeg.kt")
        val text = src.readText()
        assertTrue(text.contains("#5B54E6"))
        assertTrue(!text.contains("#143882"))
    }
}
