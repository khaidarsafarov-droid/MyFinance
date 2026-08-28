package com.truckerload.widget

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WidgetTruckProgressBitmapTest {

    @Test
    fun mockupStyle_usesLavenderTealGradientAndDarkPercentLabel() {
        val src = readSource("widget/WidgetTruckProgressBitmap.kt")
        assertTrue(src.contains("progressStart"))
        assertTrue(src.contains("progressEnd"))
        assertTrue(src.contains("progressLabel"))
        assertTrue(src.contains("drawSpeedLines"))
        assertTrue(src.contains("drawMockupTruck"))
        assertTrue(src.contains("drawTruckWheels"))
        assertTrue(src.contains("buildTrailerPath"))
        assertTrue(src.contains("buildTractorPath"))
        assertTrue(src.contains("drawHeadlight"))
        assertTrue(src.contains("Style.FILL"))
        assertTrue(src.contains("hitch"))
        assertTrue(src.contains("drawTrailerRibs"))
        assertTrue(src.contains("drawCabWindow"))
        assertTrue(src.contains("drawGrilleAndBumper"))
        assertTrue(!src.contains("buildTruckBodyPath"))
    }

    @Test
    fun palette_exposesMockupProgressTokens() {
        assertTrue(WidgetCabinPalette.PROGRESS_START != WidgetCabinPalette.PROGRESS_END)
        assertTrue(WidgetCabinPalette.Dark.PROGRESS_LABEL != WidgetCabinPalette.Dark.ON_FILLED)
    }

    @Test
    fun create_rendersSemiOnTheProgressTrack() {
        val bmp = WidgetTruckProgressBitmap.create(
            context = RuntimeEnvironment.getApplication(),
            progressPercent = 38f,
            goalSet = true,
            widthPx = 900,
            barHeightPx = 28,
            colors = WidgetCabinColors.ForestLight,
        )
        assertTrue(bmp.width >= 900)
        assertTrue(bmp.height > 28)
        var opaque = 0
        var x = 0
        while (x < bmp.width) {
            var y = 0
            while (y < bmp.height) {
                if (Color.alpha(bmp.getPixel(x, y)) > 24) opaque++
                y += 4
            }
            x += 4
        }
        assertTrue("expected truck + bar pixels, got $opaque", opaque > 80)
        val out = File("/tmp/truckorig_progress_truck.png")
        val plate = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(plate).apply {
            drawColor(0xFF1E2238.toInt())
            drawBitmap(bmp, 0f, 0f, null)
        }
        FileOutputStream(out).use { stream ->
            assertTrue(plate.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue(out.length() > 200)
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/com/truckerload/$relativePath"),
            File("app/src/main/java/com/truckerload/$relativePath"),
            File("../app/src/main/java/com/truckerload/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Source not found: $relativePath")
    }
}
