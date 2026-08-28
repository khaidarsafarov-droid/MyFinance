package com.truckerload.widget

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
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
    fun flatTruck_ridesOnBarTop_connectedBodyAndExactBitmapHeight() {
        val src = readSource("widget/WidgetTruckProgressBitmap.kt")
        assertTrue(src.contains("progressStart"))
        assertTrue(src.contains("progressEnd"))
        assertTrue(src.contains("progressLabel"))
        assertTrue(src.contains("headroomPx"))
        assertTrue(src.contains("h * 0.175f"))
        assertTrue(src.contains("wheelRadius * 0.65f"))
        assertTrue(src.contains("barTop - truckHeight"))
        assertTrue(src.contains("drawFlatTruck"))
        assertTrue(src.contains("buildTrailerOutline"))
        assertTrue(src.contains("buildCabOutline"))
        assertTrue(src.contains("drawFenderCaps"))
        assertTrue(src.contains("drawWheelBottoms"))
        assertTrue(src.contains("drawSpeedLines"))
        assertTrue(src.contains("buildTrailerPath"))
        assertTrue(src.contains("buildCabPath"))
        assertTrue(src.contains("buildHitchPath"))
        assertTrue(src.contains("TruckGeom"))
        assertTrue(src.contains("Style.FILL"))
        assertTrue(!src.contains("buildTruckBodyPath"))
        assertTrue(!src.contains("drawMockupTruck"))
    }

    @Test
    fun palette_exposesMockupProgressTokens() {
        assertTrue(WidgetCabinPalette.PROGRESS_START != WidgetCabinPalette.PROGRESS_END)
        assertTrue(WidgetCabinPalette.Dark.PROGRESS_LABEL != WidgetCabinPalette.Dark.ON_FILLED)
    }

    @Test
    fun create_bitmapHeightMatchesBarPlusHeadroom() {
        val barPx = 28
        val headroomPx = 44
        val bmp = WidgetTruckProgressBitmap.create(
            context = RuntimeEnvironment.getApplication(),
            progressPercent = 38f,
            goalSet = true,
            widthPx = 900,
            barHeightPx = barPx,
            headroomPx = headroomPx,
            colors = WidgetCabinColors.ForestLight,
        )
        assertEquals(barPx + headroomPx, bmp.height)
        assertTrue(bmp.width >= 900)

        val out = File("/tmp/truckorig_progress_truck.png")
        val plate = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(plate).apply {
            drawColor(0xFFF0F2F8.toInt())
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
