package com.truckerload.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the restored cabin budget widget: ring hero + camera/scanner/diesel shortcuts.
 */
class WidgetBudgetRingActionsTest {

    @Test
    fun glanceWideBudget_usesCabinPlateAndRingHero() {
        val src = readSource("widget/glance/OneUiGlanceWidgets.kt")
        assertTrue(src.contains("widget_cabin_plate"))
        assertTrue(src.contains("BudgetRing"))
        assertTrue(src.contains("formatGrossUsd"))
        assertTrue(src.contains("R.string.widget_goal_out_of"))
        assertTrue(src.contains("formatRingPercent"))
        assertTrue(src.contains("WidgetWeekDaysBitmap"))
        assertTrue(src.contains("formatWidgetRpm"))
        assertTrue(src.contains("SelectWidgetDayAction"))
        assertTrue(src.contains("loadsCount.toString()"))
        assertTrue(src.contains("Size2x2, Size4x2"))
    }

    @Test
    fun glanceQuickActions_areCameraThenScannerThenDiesel() {
        val src = readSource("widget/glance/OneUiGlanceWidgets.kt")
        val camera = src.indexOf("ROUTE_ATTACH_CAMERA")
        val scanner = src.indexOf("ROUTE_ATTACH_SCANNER")
        val diesel = src.indexOf("dieselQuickAddIntent")
        assertTrue(camera >= 0)
        assertTrue(scanner > camera)
        assertTrue(diesel > scanner)
        assertTrue(src.contains("ic_widget_camera"))
        assertTrue(src.contains("ic_widget_scanner"))
        assertTrue(src.contains("ic_widget_diesel"))
    }

    @Test
    fun cabinPlateDrawable_isAlwaysDark() {
        val xml = readRes("drawable/widget_cabin_plate.xml")
        assertTrue(xml.contains("#FF1A2420"))
        assertTrue(xml.contains("#FF24302A"))
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

    private fun readRes(relativePath: String): String {
        val candidates = listOf(
            File("src/main/res/$relativePath"),
            File("app/src/main/res/$relativePath"),
            File("../app/src/main/res/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Resource not found: $relativePath")
    }
}
