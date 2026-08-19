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
        assertEquals(Routes.STATS, phoneTabForRoute(Routes.ADVANCED_STATS))
        assertEquals(Routes.STATS, phoneTabForRoute(Routes.MAP))
        assertTrue(isPhoneDestinationSelected(Routes.MAP, Routes.STATS))
        assertFalse(isPhoneDestinationSelected(Routes.MAP, Routes.HOME))
    }

    @Test
    fun keepsBottomBarOnStatsNestedScreens() {
        assertTrue(shouldShowPhoneBottomBar(Routes.STATS))
        assertTrue(shouldShowPhoneBottomBar(Routes.ANALYTICS))
        assertTrue(shouldShowPhoneBottomBar(Routes.ADVANCED_STATS))
        assertTrue(shouldShowPhoneBottomBar(Routes.MAP))
        assertTrue(shouldShowPhoneBottomBar("load_detail/{loadId}"))
        assertTrue(shouldShowPhoneBottomBar("social_chat/{chatId}"))
    }

    @Test
    fun hidesBottomBarOnImmersiveCameraAndCalls() {
        assertFalse(shouldShowPhoneBottomBar(Routes.CAMERA))
        assertFalse(shouldShowPhoneBottomBar("camera_load/a/b/c"))
        assertFalse(shouldShowPhoneBottomBar(Routes.SCANNER))
        assertFalse(shouldShowPhoneBottomBar("call/{callId}"))
        assertFalse(shouldShowPhoneBottomBar("voice_room/{roomId}"))
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
                currentRoute = Routes.ADVANCED_STATS,
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
    fun communityAndProfileNestedStayOnTheirTabs() {
        assertEquals(Routes.COMMUNITY, phoneTabForRoute(Routes.FRIENDS_LIVE))
        assertEquals(Routes.COMMUNITY, phoneTabForRoute("group_detail/{chatId}"))
        assertEquals(Routes.PROFILE, phoneTabForRoute(Routes.PROFILE_EDIT))
        assertEquals(Routes.HOME, phoneTabForRoute(Routes.ADD_LOAD))
    }
}
