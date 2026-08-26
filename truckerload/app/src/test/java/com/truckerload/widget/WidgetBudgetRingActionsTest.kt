package com.truckerload.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the green cabin mockup: ring hole, side metrics, day captions, actions.
 */
class WidgetBudgetRingActionsTest {

    @Test
    fun glanceWideBudget_matchesGreenMockup() {
        val wide = readSource("widget/glance/CabinWideGlanceContent.kt")
        val core = readSource("widget/glance/OneUiGlanceWidgets.kt")
        assertTrue(core.contains("widget_cabin_plate"))
        assertTrue(core.contains("BudgetRing"))
        assertTrue(core.contains("formatGrossUsd"))
        assertTrue(core.contains("formatRingPercent"))
        assertTrue(!core.contains("R.string.widget_goal_out_of"))
        assertTrue(wide.contains("WidgetWeekDaysBitmap"))
        assertTrue(wide.contains("formatUsdRpm"))
        assertTrue(wide.contains("SelectWidgetDayAction"))
        assertTrue(wide.contains("widget_metric_goal"))
        assertTrue(wide.contains("widget_metric_rpm"))
        assertTrue(wide.contains("widget_metric_trips"))
        assertTrue(wide.contains("WidgetDayCaption"))
        assertTrue(core.contains("CabinSize4x3"))
        assertTrue(core.contains("CabinSize2x2, CabinSize4x2, CabinSize4x3"))
    }

    @Test
    fun glanceQuickActions_areCameraThenScannerThenDiesel() {
        val src = readSource("widget/glance/CabinWideGlanceContent.kt")
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
    fun cabinPlateDrawable_isForestGreen() {
        val xml = readRes("drawable/widget_cabin_plate.xml")
        assertTrue(xml.contains("#FF0B1A12"))
        assertTrue(xml.contains("#FF07140E"))
        assertTrue(!xml.contains("#FF143882"))
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
