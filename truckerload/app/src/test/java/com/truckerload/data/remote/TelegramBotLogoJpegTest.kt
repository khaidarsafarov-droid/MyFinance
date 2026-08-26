package com.truckerload.data.remote

import android.graphics.Bitmap
import android.graphics.Color
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
}
