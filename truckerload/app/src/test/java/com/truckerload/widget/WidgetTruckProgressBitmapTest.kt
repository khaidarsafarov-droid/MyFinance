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
    fun flatTruck_ridesOnBarTop_unifiedSilhouette() {
        val src = readSource("widget/WidgetTruckProgressBitmap.kt")
        assertTrue(src.contains("progressStart"))
        assertTrue(src.contains("progressEnd"))
        assertTrue(src.contains("progressLabel"))
        assertTrue(src.contains("headroomPx"))
        assertTrue(src.contains("h * 0.20f"))
        assertTrue(src.contains("buildTruckSilhouette"))
        assertTrue(src.contains("addCircle"))
        assertTrue(src.contains("addRoundRect"))
        assertTrue(src.contains("wheelRadius * 0.35f"))
        assertTrue(src.contains("barTop - truckHeight"))
        assertTrue(src.contains("drawFlatTruck"))
        assertTrue(src.contains("drawHubs"))
        assertTrue(src.contains("drawSpeedLines"))
        assertTrue(src.contains("buildTrailerPath"))
        assertTrue(src.contains("buildCabPath"))
        assertTrue(src.contains("buildHitchPath"))
        assertTrue(src.contains("TruckGeom"))
        assertTrue(src.contains("Style.FILL"))
        assertTrue(!src.contains("buildTruckBodyPath"))
        assertTrue(!src.contains("drawMockupTruck"))
        assertTrue(!src.contains("drawWheelBottoms"))
        assertTrue(!src.contains("drawFenderCaps"))
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

        val truckH = (headroomPx * 0.92f).toInt()
        val truckW = (truckH * 1.55f).toInt()
        val fillX = (900 * 0.38f).toInt()
        val left = fillX - truckW
        val wheelX = left + (truckW * 0.17f).toInt()
        val barTop = headroomPx
        val stroke = maxOf(truckH * 0.045f, 1.4f)
        val wheelR = truckH * 0.20f
        val wheelCy = (barTop - truckH) + truckH - wheelR - stroke * 0.5f
        // Outer tire ring (below hub) must stay body-colored — silhouette union.
        val tireRingY = (wheelCy + wheelR * 0.52f + 1.5f).toInt()
        val bodyPx = plate.getPixel(wheelX, tireRingY.coerceAtMost(barTop - 2))
        assertTrue(
            "tire ring must be body-colored, got #${Integer.toHexString(bodyPx)} at y=$tireRingY",
            Color.red(bodyPx) in 70..140 && Color.blue(bodyPx) > 150,
        )
        val hubPx = plate.getPixel(wheelX, wheelCy.toInt())
        assertTrue(
            "lower hub must be white-ish, got #${Integer.toHexString(hubPx)}",
            Color.red(hubPx) > 200 && Color.green(hubPx) > 200 && Color.blue(hubPx) > 200,
        )
        val upperTirePx = plate.getPixel(wheelX, (wheelCy - wheelR * 0.35f).toInt())
        assertTrue(
            "upper tire must stay body-colored, got #${Integer.toHexString(upperTirePx)}",
            Color.red(upperTirePx) in 70..140 && Color.blue(upperTirePx) > 150,
        )
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
