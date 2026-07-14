package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AvatarCropUtilsTest {

    @Test
    fun cropSquare_centeredImage_returnsRequestedOutputSize() {
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)

        val cropped = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset.Zero,
            outputSize = 256,
        )

        assertEquals(256, cropped.width)
        assertEquals(256, cropped.height)
    }
}
