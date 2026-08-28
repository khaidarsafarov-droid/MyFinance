package com.truckerload.presentation.components

import com.truckerload.presentation.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabNavigationTest {

    @Test
    fun highlightsStatsTabOnMapAndAnalytics() {
        assertEquals(Routes.STATS, phoneTabForRoute(Routes.STATS))
        assertEquals(Routes.STATS, phoneTabForRoute(Routes.ANALYTICS))
        assertEquals(Routes.STATS, phoneTabForRoute(Routes.MAP))
        assertTrue(isPhoneDestinationSelected(Routes.MAP, Routes.STATS))
        assertFalse(isPhoneDestinationSelected(Routes.MAP, Routes.HOME))
    }

    @Test
    fun keepsBottomBarOnStatsNestedScreens() {
        assertTrue(shouldShowPhoneBottomBar(Routes.STATS))
        assertTrue(shouldShowPhoneBottomBar(Routes.ANALYTICS))
        assertTrue(shouldShowPhoneBottomBar(Routes.MAP))
        assertTrue(shouldShowPhoneBottomBar("load_detail/{loadId}"))
    }

    @Test
    fun keepsBottomBarOnSettingsAndPrivacy() {
        assertTrue(shouldShowPhoneBottomBar(Routes.SETTINGS))
        assertTrue(shouldShowPhoneBottomBar(Routes.PRIVACY_SETTINGS))
        assertTrue(shouldShowPhoneBottomBar(Routes.LANGUAGE_SETTINGS))
        assertTrue(shouldShowPhoneBottomBar(Routes.ABOUT))
        assertNull(phoneTabForRoute(Routes.SETTINGS))
    }

    @Test
    fun settingsScreenUsesSwitchTabForProfileReselect() {
        assertEquals(
            MainTabClickAction.SWITCH_TAB,
            resolveMainTabClick(
                currentRoute = Routes.SETTINGS,
                targetTab = Routes.PROFILE,
                tabRootInBackStack = true,
            ),
        )
        assertEquals(
            MainTabClickAction.SWITCH_TAB,
            resolveMainTabClick(
                currentRoute = Routes.PRIVACY_SETTINGS,
                targetTab = Routes.PROFILE,
                tabRootInBackStack = true,
            ),
        )
        assertEquals(
            MainTabClickAction.SWITCH_TAB,
            resolveMainTabClick(
                currentRoute = Routes.LANGUAGE_SETTINGS,
                targetTab = Routes.PROFILE,
                tabRootInBackStack = true,
            ),
        )
    }

    @Test
    fun hidesBottomBarOnImmersiveCamera() {
        assertFalse(shouldShowPhoneBottomBar(Routes.CAMERA))
        assertFalse(shouldShowPhoneBottomBar("camera_load/a/b/c"))
        assertFalse(shouldShowPhoneBottomBar(Routes.SCANNER))
        assertFalse(shouldShowPhoneBottomBar(Routes.DIESEL))
        assertFalse(shouldShowPhoneBottomBar(Routes.PAYCHECK))
        assertFalse(shouldShowPhoneBottomBar(Routes.MISC_EXPENSES))
        assertNull(phoneTabForRoute(Routes.CAMERA))
    }

    @Test
    fun reselectOnMapPopsToStatsWhenTabRootIsInStack() {
        assertEquals(
            MainTabClickAction.POP_TO_TAB_ROOT,
            resolveMainTabClick(
                currentRoute = Routes.MAP,
                targetTab = Routes.STATS,
                tabRootInBackStack = true,
            ),
        )
    }

    @Test
    fun reselectOnMapPopsOnceWhenOpenedFromStatistics() {
        assertEquals(
            MainTabClickAction.POP_ONCE,
            resolveMainTabClick(
                currentRoute = Routes.MAP,
                targetTab = Routes.STATS,
                tabRootInBackStack = false,
            ),
        )
        assertEquals(
            MainTabClickAction.POP_ONCE,
            resolveMainTabClick(
                currentRoute = Routes.ANALYTICS,
                targetTab = Routes.STATS,
                tabRootInBackStack = false,
            ),
        )
    }

    @Test
    fun tappingAnotherTabSwitches() {
        assertEquals(
            MainTabClickAction.SWITCH_TAB,
            resolveMainTabClick(
                currentRoute = Routes.MAP,
                targetTab = Routes.HOME,
                tabRootInBackStack = false,
            ),
        )
        assertEquals(
            MainTabClickAction.SWITCH_TAB,
            resolveMainTabClick(
                currentRoute = Routes.HOME,
                targetTab = Routes.STATS,
                tabRootInBackStack = false,
            ),
        )
    }

    @Test
    fun tappingCurrentTabRootIsNoOp() {
        assertEquals(
            MainTabClickAction.NO_OP,
            resolveMainTabClick(
                currentRoute = Routes.STATS,
                targetTab = Routes.STATS,
                tabRootInBackStack = true,
            ),
        )
    }

    @Test
    fun profileTabIsOnlyTheProfileScreen() {
        assertEquals(Routes.PROFILE, phoneTabForRoute(Routes.PROFILE))
        assertEquals(Routes.HOME, phoneTabForRoute(Routes.ADD_LOAD))
        assertFalse(Routes::class.java.declaredFields.any { it.name == "PROFILE_EDIT" })
    }
}
