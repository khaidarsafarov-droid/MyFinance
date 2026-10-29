package com.truckerload.presentation.components

import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.components.isImmersivePhoneRoute
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletSidebarDestinationsTest {

    @Test
    fun statsTabHighlightsAnalyticsAndMapOnTabletRail() {
        assertTrue(isRailDestinationSelected(Routes.ANALYTICS, Routes.STATS))
        assertTrue(isRailDestinationSelected(Routes.MAP, Routes.STATS))
        assertFalse(isRailDestinationSelected(Routes.HOME, Routes.STATS))
    }

    @Test
    fun railIncludesEveryDrawerDestination() {
        val inRail = tabletRailToolItems.mapNotNull { it.drawer }.toSet()
        assertEquals(DrawerDestination.entries.toSet(), inRail)
        assertFalse(tabletRailToolItems.any { it.isLogout })
        assertEquals(
            listOf(Routes.HOME, Routes.STATS, Routes.PROFILE),
            tabletRailPrimaryItems.map { it.route },
        )
    }

    @Test
    fun modalDrawerDisabledOnTablet() {
        assertFalse(
            shouldEnableModalNavigationDrawer(showMainNavigation = true, tabletChrome = true),
        )
        assertTrue(
            shouldEnableModalNavigationDrawer(showMainNavigation = true, tabletChrome = false),
        )
        assertFalse(
            shouldEnableModalNavigationDrawer(showMainNavigation = false, tabletChrome = false),
        )
        assertFalse(
            shouldEnableModalNavigationDrawer(showMainNavigation = false, tabletChrome = true),
        )
    }

    @Test
    fun reportsAndMapAreSeparateSelections() {
        assertFalse(isDrawerDestinationSelected(Routes.MAP, DrawerDestination.REPORTS))
        assertTrue(isDrawerDestinationSelected(Routes.MAP, DrawerDestination.MAP))
        assertTrue(isDrawerDestinationSelected(Routes.ANALYTICS, DrawerDestination.REPORTS))
        assertFalse(isDrawerDestinationSelected(Routes.ANALYTICS, DrawerDestination.MAP))
    }

    @Test
    fun scannerIsNotSelectedAsDocuments() {
        assertFalse(isDrawerDestinationSelected(Routes.SCANNER, DrawerDestination.DOCUMENTS))
        assertTrue(isDrawerDestinationSelected(Routes.SCANNER, DrawerDestination.SCANNER))
        assertTrue(isDrawerDestinationSelected(Routes.SCAN_GALLERY, DrawerDestination.DOCUMENTS))
        assertTrue(isDrawerDestinationSelected(Routes.GALLERY, DrawerDestination.DOCUMENTS))
        assertTrue(isDrawerDestinationSelected(Routes.GALLERY_LOAD, DrawerDestination.DOCUMENTS))
        assertTrue(isDrawerDestinationSelected(Routes.GALLERY_VIEWER, DrawerDestination.DOCUMENTS))
        assertTrue(isImmersivePhoneRoute(Routes.GALLERY))
        assertTrue(isImmersivePhoneRoute(Routes.GALLERY_LOAD))
        assertTrue(isImmersivePhoneRoute(Routes.GALLERY_VIEWER))
        assertTrue(isImmersivePhoneRoute(Routes.SCAN_DETAIL))
    }

    @Test
    fun settingsIncludesPrivacyAndCameraIncludesLoadCapture() {
        assertTrue(isDrawerDestinationSelected(Routes.PRIVACY_SETTINGS, DrawerDestination.SETTINGS))
        assertTrue(isDrawerDestinationSelected(Routes.LANGUAGE_SETTINGS, DrawerDestination.SETTINGS))
        assertTrue(isDrawerDestinationSelected("camera_load/1/t/2026-01-01", DrawerDestination.CAMERA))
        assertTrue(isRailDestinationSelected(Routes.ADD_LOAD, Routes.HOME))
        val settings = tabletRailToolItems.first { it.drawer == DrawerDestination.SETTINGS }
        assertFalse(isTabletRailItemSelected(Routes.HOME, settings))
    }

    @Test
    fun adaptiveScaffold_skipsDrawerOnTabletChrome() {
        val src = readSource("presentation/components/AdaptiveScaffold.kt")
        assertTrue(src.contains("shouldEnableModalNavigationDrawer"))
        assertTrue(src.contains("if (useDrawer)"))
        assertTrue(src.contains("LocalOpenDrawer provides {}"))
        assertFalse(src.contains("gesturesEnabled = showMainNavigation"))
    }

    @Test
    fun drawerListsFinanceBeforeSettingsAndAbout() {
        val src = readSource("presentation/components/AppDrawer.kt")
        assertTrue(src.indexOf("DrawerDestination.PAYCHECK") < src.indexOf("DrawerDestination.SETTINGS"))
        assertTrue(src.indexOf("DrawerDestination.DIESEL") < src.indexOf("DrawerDestination.SETTINGS"))
        assertTrue(src.indexOf("DrawerDestination.SETTINGS") < src.indexOf("DrawerDestination.ABOUT"))
        val rail = readSource("presentation/components/TruckLogNavigationRail.kt")
        assertTrue(rail.contains("nav_more"))
        assertTrue(rail.contains("R.string.quick_actions_title"))
        assertFalse(rail.contains("SoftUiColors.ForestPrimary"))
    }

    @Test
    fun tabletRail_rendersEveryDrawerDestination() {
        val src = readSource("presentation/components/TruckLogNavigationRail.kt")
        assertTrue(src.contains("tabletRailPrimaryItems"))
        assertTrue(src.contains("tabletRailToolItems"))
        assertFalse(src.contains("LogoutConfirmDialog"))
        assertTrue(src.contains("drawerIcon("))
        val icons = readSource("presentation/components/DrawerIcons.kt")
        DrawerDestination.entries.forEach { dest ->
            assertTrue(
                "drawer icon mapping missing $dest",
                icons.contains("DrawerDestination.${dest.name}"),
            )
        }
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
