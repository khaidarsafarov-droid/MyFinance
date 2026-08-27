package com.truckerload.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

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
    }

    @Test
    fun palette_exposesMockupProgressTokens() {
        assertTrue(WidgetCabinPalette.PROGRESS_START != WidgetCabinPalette.PROGRESS_END)
        assertTrue(WidgetCabinPalette.Dark.PROGRESS_LABEL != WidgetCabinPalette.Dark.ON_FILLED)
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
