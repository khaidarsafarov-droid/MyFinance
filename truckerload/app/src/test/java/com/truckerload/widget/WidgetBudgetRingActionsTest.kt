package com.truckerload.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the mockup-style cabin widget: progress bar, stats row, day chips, actions.
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
        assertTrue(wide.contains("WidgetTruckProgressBitmap"))
        assertTrue(wide.contains("progressBarHeadroomDp"))
        assertTrue(wide.contains("MockupStatsRow"))
        assertTrue(wide.contains("widget_metric_revenue"))
        assertTrue(core.contains("WidgetTruckProgressBitmap"))
        assertTrue(core.contains("WidgetCabinColors.resolve"))
        assertTrue(core.contains("LocalCabinColors"))
        assertTrue(!core.contains("R.string.widget_goal_out_of"))
        assertTrue(wide.contains("WidgetWeekDaysBitmap"))
        assertTrue(wide.contains("formatUsdRpm"))
        assertTrue(wide.contains("SelectWidgetDayAction"))
        assertTrue(wide.contains("widget_metric_goal"))
        assertTrue(wide.contains("widget_metric_rpm"))
        assertTrue(wide.contains("widget_camera_short"))
        assertTrue(wide.contains("widget_scanner_short"))
        assertTrue(wide.contains("widget_diesel_short"))
        assertTrue(!wide.contains("widget_quick_actions"))
        val bar = readSource("widget/WidgetTruckProgressBitmap.kt")
        assertTrue(bar.contains("headroomPx"))
        assertTrue(bar.contains("drawPercentLabel"))
        assertTrue(bar.contains("percentTextSizePx"))
        assertTrue(!bar.contains("drawFlatTruck"))
        assertTrue(!bar.contains("buildTruckSilhouette"))
        assertTrue(wide.contains("WidgetDayCaption"))
        assertTrue(core.contains("cabinActionFill"))
        assertTrue(core.contains("CabinSize4x4"))
        assertTrue(core.contains("SizeMode.Exact"))
        assertTrue(core.contains("SizeMode.Responsive"))
        assertTrue(core.contains("LocalWidgetSizeMode"))
        assertTrue(core.contains("WidgetPrefsStore.load"))
    }

    @Test
    fun wideWidgetXml_defaultsToThreeRows() {
        val xml = readRes("xml/truckerload_widget_glance_4x2_info.xml")
        assertTrue(xml.contains("minHeight=\"110dp\""))
        assertTrue(xml.contains("minResizeHeight=\"110dp\""))
        assertTrue(xml.contains("targetCellHeight=\"3\""))
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
    fun widgetRefresh_appliesGlanceOnMainAndFlushesOnLeave() {
        val refresh = readSource("widget/WidgetRefresh.kt")
        assertTrue(refresh.contains("Dispatchers.Main.immediate"))
        assertTrue(refresh.contains("OneUiGlanceWidgets.updateAll"))
        assertTrue(refresh.contains("flushForHomeScreen"))
        assertTrue(refresh.contains("postAtFrontOfQueue"))
        assertTrue(refresh.contains("WidgetDataProvider.refresh"))
        val activity = readSource("presentation/MainActivity.kt")
        assertTrue(activity.contains("override fun onPause()"))
        assertTrue(activity.contains("WidgetRefresh.flushForHomeScreen"))
        assertTrue(activity.contains("onNewIntent"))
        val app = readSource("TruckerLoadApp.kt")
        assertTrue(app.contains("flushForHomeScreen"))
        assertTrue(app.contains("WidgetDataUpdater.updateWidgetData"))
        assertTrue(!app.contains("avoid Room+bitmap"))
        val home = readSource("presentation/screens/home/HomeViewModel.kt")
        assertTrue(!home.contains("debounce(400)"))
        val diesel = readSource("data/repository/DieselRepository.kt")
        assertTrue(diesel.contains("notifyWidgetDataChanged"))
        val paycheck = readSource("data/repository/PaycheckRepository.kt")
        assertTrue(paycheck.contains("notifyWidgetDataChanged"))
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
