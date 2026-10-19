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
        val split = readSource("widget/glance/CabinSplitContent.kt")
        val actions = readSource("widget/glance/CabinQuickAction.kt")
        val core = readSource("widget/glance/OneUiGlanceWidgets.kt")
        val chrome = readSource("widget/glance/CabinChrome.kt")
        val colors = readSource("widget/WidgetCabinColors.kt")
        assertTrue(chrome.contains("cabinPlate"))
        assertTrue(chrome.contains("ColorProvider"))
        assertTrue(chrome.contains("colors.bg"))
        assertTrue(chrome.contains("cornerRadius"))
        assertTrue(wide.contains("SplitCabinBody"))
        assertTrue(split.contains("WidgetSplitBitmaps"))
        assertTrue(split.contains("formatSplitWeekRange"))
        assertTrue(split.contains("widget_goal_percent"))
        assertTrue(split.contains("widget_goal_remaining"))
        assertTrue(split.contains("formatUsdRpm"))
        assertTrue(split.contains("SelectWidgetDayAction"))
        assertTrue(split.contains("CabinQuickAction.visible"))
        assertTrue(split.contains("text = chip.label"))
        assertTrue(!split.contains("text = todayLabel"))
        assertTrue(actions.indexOf("CAMERA") < actions.indexOf("SCANNER"))
        assertTrue(actions.indexOf("SCANNER") < actions.indexOf("DIESEL"))
        assertTrue(colors.contains("BrandPalettes"))
        assertTrue(!colors.contains("dynamicDarkColorScheme"))
        assertTrue(core.contains("WidgetCabinColors.resolve"))
        assertTrue(core.contains("LocalCabinColors"))
        assertTrue(!core.contains("R.string.widget_goal_out_of"))
        assertTrue(!split.contains("widget_metric_diesel"))
        assertTrue(!split.contains("week.dieselAmount"))
        assertTrue(!split.contains("week.netProfit"))
        assertTrue(core.contains("CabinSize4x3"))
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
    fun widgetRefresh_appliesGlanceOnMainAndFlushesOnLeave() {
        val refresh = readSource("widget/WidgetRefresh.kt")
        assertTrue(refresh.contains("Dispatchers.Main.immediate"))
        assertTrue(refresh.contains("OneUiGlanceWidgets.updateAll"))
        assertTrue(refresh.contains("flushForHomeScreen"))
        assertTrue(refresh.contains("postAtFrontOfQueue"))
        assertTrue(refresh.contains("WidgetDataProvider.refresh"))
        val glance = readSource("widget/glance/OneUiGlanceWidgets.kt")
        assertTrue(glance.contains("WidgetGlanceRevisionKey"))
        assertTrue(glance.contains("ACTION_APPWIDGET_UPDATE"))
        assertTrue(glance.contains("PreferencesGlanceStateDefinition"))
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
        assertTrue(xml.contains("28dp"))
        assertTrue(!xml.contains("#FF12251C"))
        assertTrue(!xml.contains("#FF0B1A12"))
        assertTrue(!xml.contains("#FF07140E"))
        assertTrue(!xml.contains("#FF1E3D2E"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF00E676"))
        val light = readRes("values/widget_colors.xml")
        assertTrue(light.contains("#FFFDFCFA"))
        assertTrue(light.contains("#FF3F3D9E"))
        assertTrue(light.contains("#FF16182B"))
        assertTrue(!light.contains("#FF5B54E6"))
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
