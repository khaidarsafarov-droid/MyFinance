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
    fun source_hasNoTruckSilhouette_andDrawsPercentInHeadroom() {
        val src = readSource("widget/WidgetTruckProgressBitmap.kt")
        assertTrue(src.contains("progressStart"))
        assertTrue(src.contains("progressEnd"))
        assertTrue(src.contains("drawPercentLabel"))
        assertTrue(src.contains("percentTextSizePx"))
        assertTrue(src.contains("headroomPx"))
        assertTrue(src.contains("formatRingPercent"))
        assertTrue(!src.contains("drawFlatTruck"))
        assertTrue(!src.contains("drawSpeedLines"))
        assertTrue(!src.contains("buildTruckSilhouette"))
        assertTrue(!src.contains("barTop - truckHeight"))
        assertTrue(!src.contains("TruckGeom"))
    }

    @Test
    fun percentTextSize_isLargeRelativeToHeadroom() {
        assertEquals(34, WidgetTruckProgressBitmap.percentTextSizePx(44).toInt())
        assertTrue(WidgetTruckProgressBitmap.percentTextSizePx(24) >= 18f)
    }

    @Test
    fun create_bitmapHeightMatchesBarPlusHeadroom() {
        val barPx = 28
        val headroomPx = 44
        val bmp = WidgetTruckProgressBitmap.create(
            context = RuntimeEnvironment.getApplication(),
            progressPercent = 9.2f,
            goalSet = true,
            widthPx = 900,
            barHeightPx = barPx,
            headroomPx = headroomPx,
            colors = WidgetCabinColors.ForestLight,
        )
        assertEquals(barPx + headroomPx, bmp.height)
        assertTrue(bmp.width >= 900)

        val out = File("/tmp/truckorig_progress_percent.png")
        val plate = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(plate).apply {
            drawColor(0xFFF0F2F8.toInt())
            drawBitmap(bmp, 0f, 0f, null)
        }
        FileOutputStream(out).use { stream ->
            assertTrue(plate.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue(out.length() > 200)

        val labelPx = sampleDarkInk(plate, xStart = 4, xEnd = 180, yStart = 4, yEnd = headroomPx - 4)
        assertTrue(
            "percent in headroom must be near-black text, got #$labelPx",
            labelPx != null && Color.red(labelPx) < 50 && Color.green(labelPx) < 50 &&
                Color.blue(labelPx) < 50,
        )

        // Old truck sat on the bar at the fill edge (~9% of 900 ≈ 83px).
        val fillEdge = (900 * 0.092f).toInt()
        val truckBodyY = headroomPx - 8
        val aboveBar = plate.getPixel(fillEdge.coerceAtLeast(20), truckBodyY.coerceAtLeast(2))
        assertTrue(
            "no truck body above the fill edge, got #${Integer.toHexString(aboveBar)}",
            Color.alpha(aboveBar) < 40 || isPlateLike(aboveBar),
        )
    }

    private fun isPlateLike(px: Int): Boolean {
        val r = Color.red(px)
        val g = Color.green(px)
        val b = Color.blue(px)
        return r > 220 && g > 220 && b > 230
    }

    private fun sampleDarkInk(
        bmp: Bitmap,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int,
    ): Int? {
        for (y in yStart until yEnd) {
            for (x in xStart until xEnd) {
                val px = bmp.getPixel(x, y)
                if (Color.alpha(px) < 180) continue
                if (Color.red(px) < 50 && Color.green(px) < 50 && Color.blue(px) < 50) return px
            }
        }
        return null
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
