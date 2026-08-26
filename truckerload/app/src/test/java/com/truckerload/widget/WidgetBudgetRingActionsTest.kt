package com.truckerload.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the kit widget mockup: ring hole, side metrics, day captions, actions.
 */
class WidgetBudgetRingActionsTest {

    @Test
    fun glanceWideBudget_matchesKitMockup() {
        val wide = readSource("widget/glance/CabinWideGlanceContent.kt")
        val core = readSource("widget/glance/OneUiGlanceWidgets.kt")
        val chrome = readSource("widget/glance/CabinChrome.kt")
        assertTrue(chrome.contains("cabinPlate"))
        assertTrue(chrome.contains("ColorProvider"))
        assertTrue(chrome.contains("colors.bg"))
        assertTrue(chrome.contains("cornerRadius"))
        assertTrue(core.contains("cabinPlate"))
        assertTrue(core.contains("BudgetRing"))
        assertTrue(core.contains("formatGrossUsd"))
        assertTrue(core.contains("formatRingPercent"))
        assertTrue(core.contains("WidgetCabinColors.resolve"))
        assertTrue(core.contains("LocalCabinColors"))
        assertTrue(!core.contains("R.string.widget_goal_out_of"))
        assertTrue(wide.contains("WidgetWeekDaysBitmap"))
        assertTrue(wide.contains("formatUsdRpm"))
        assertTrue(wide.contains("SelectWidgetDayAction"))
        assertTrue(wide.contains("widget_metric_goal"))
        assertTrue(wide.contains("widget_metric_rpm"))
        assertTrue(wide.contains("widget_metric_trips"))
        assertTrue(wide.contains("WidgetDayCaption"))
        assertTrue(wide.contains("CompactFinanceBlock"))
        assertTrue(wide.contains("showRing"))
        assertTrue(core.contains("cabinActionFill"))
        assertTrue(core.contains("CabinSize4x4"))
        assertTrue(core.contains("SizeMode.Exact"))
        assertTrue(core.contains("SizeMode.Responsive"))
        assertTrue(core.contains("LocalWidgetSizeMode"))
        assertTrue(core.contains("WidgetPrefsStore.load"))
    }

    @Test
    fun wideWidgetXml_allowsTwoRowResize() {
        val xml = readRes("xml/truckerload_widget_glance_4x2_info.xml")
        assertTrue(xml.contains("minHeight=\"110dp\""))
        assertTrue(xml.contains("minResizeHeight=\"110dp\""))
        assertTrue(!xml.contains("minHeight=\"180dp\""))
    }

    @Test
    fun themeSettings_repaintWidgetWhenDynamicColorChanges() {
        val src = readSource("presentation/screens/settings/ThemeSettingsSection.kt")
        assertTrue(src.contains("saveDynamicColor"))
        assertTrue(src.contains("WidgetRefresh.refreshAndUpdateAsync"))
        assertTrue(src.contains("saveThemeMode"))
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
    fun cabinPlateDrawable_usesKitBackgroundToken() {
        val xml = readRes("drawable/widget_cabin_plate.xml")
        assertTrue(xml.contains("@color/widget_bg"))
        assertTrue(xml.contains("20dp"))
        assertTrue(!xml.contains("#FF12251C"))
        assertTrue(!xml.contains("#FF0B1A12"))
        assertTrue(!xml.contains("#FF07140E"))
        assertTrue(!xml.contains("#FF1E3D2E"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF00E676"))
        val light = readRes("values/widget_colors.xml")
        assertTrue(light.contains("#FFF8F9FE"))
        assertTrue(light.contains("#FF5B54E6"))
        assertTrue(light.contains("#FF1A1A1A"))
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
