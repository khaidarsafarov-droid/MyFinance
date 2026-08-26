package com.truckerload.presentation.components

import com.truckerload.presentation.navigation.Routes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletSidebarDestinationsTest {

    @Test
    fun railIncludesEveryDrawerDestinationAndLogout() {
        val inRail = tabletRailToolItems.mapNotNull { it.drawer }.toSet()
        assertEquals(DrawerDestination.entries.toSet(), inRail)
        assertTrue(tabletRailToolItems.any { it.isLogout })
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
    }

    @Test
    fun settingsIncludesPrivacyAndCameraIncludesLoadCapture() {
        assertTrue(isDrawerDestinationSelected(Routes.PRIVACY_SETTINGS, DrawerDestination.SETTINGS))
        assertTrue(isDrawerDestinationSelected("camera_load/1/t/2026-01-01", DrawerDestination.CAMERA))
        assertTrue(isRailDestinationSelected(Routes.ADD_LOAD, Routes.HOME))
        val logout = tabletRailToolItems.first { it.isLogout }
        assertFalse(isTabletRailItemSelected(Routes.HOME, logout))
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
    fun tabletRail_rendersEveryDrawerDestination() {
        val src = readSource("presentation/components/TruckLogNavigationRail.kt")
        assertTrue(src.contains("tabletRailPrimaryItems"))
        assertTrue(src.contains("tabletRailToolItems"))
        assertTrue(src.contains("LogoutConfirmDialog"))
        DrawerDestination.entries.forEach { dest ->
            assertTrue(
                "rail icon mapping missing $dest",
                src.contains("DrawerDestination.${dest.name}"),
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
