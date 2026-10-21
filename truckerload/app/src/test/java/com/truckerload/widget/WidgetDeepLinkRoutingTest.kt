package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDeepLinkRoutingTest {

    @Test
    fun resolveNavRoute_weeklyGoal_goesToStatsRoute_notAnalytics() {
        assertEquals("stats", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_WEEKLY_GOAL))
        assertEquals("analytics", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_STATS))
    }

    @Test
    fun resolveNavRoute_legacyStatsValue_stillOpensAnalytics() {
        // Pre-fix widgets used EXTRA_ROUTE="stats" for RPM → Analytics.
        // Routes.STATS ("stats") hosts WeeklyGoal; widget must not collide.
        assertEquals("analytics", WidgetDeepLink.resolveNavRoute("stats"))
        assertEquals("analytics", WidgetDeepLink.ROUTE_STATS)
    }

    @Test
    fun resolveNavRoute_homeAndTools() {
        assertEquals("home", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_HOME))
        assertEquals("home", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_JOURNAL_THIS_WEEK))
        assertEquals("add_load", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_ADD_LOAD))
        assertEquals("paywall", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_PAYWALL))
    }

    @Test
    fun resolveNavRoute_cameraAndScanner_openAttachPick() {
        // Widget camera/scan must pick a load before capture so media is attached.
        assertEquals(
            WidgetDeepLink.ROUTE_ATTACH_CAMERA,
            WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_CAMERA),
        )
        assertEquals(
            WidgetDeepLink.ROUTE_ATTACH_SCANNER,
            WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_SCANNER),
        )
        assertEquals("attach_pick/camera", WidgetDeepLink.resolveNavRoute("camera"))
        assertEquals("attach_pick/scanner", WidgetDeepLink.resolveNavRoute("scanner"))
    }

    @Test
    fun resolveNavRoute_diesel_opensAddDiesel() {
        assertEquals(
            WidgetDeepLink.ROUTE_ADD_DIESEL,
            WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_ADD_DIESEL),
        )
        assertEquals("add_diesel", WidgetDeepLink.resolveNavRoute("diesel"))
    }

    @Test
    fun resolveNavRoute_unknown_returnsNull() {
        assertNull(WidgetDeepLink.resolveNavRoute("nope"))
        assertNull(WidgetDeepLink.resolveNavRoute("social_chat/abc"))
        assertNull(WidgetDeepLink.resolveNavRoute("call/c1"))
    }

    @Test
    fun sanitizeIncomingRoute_keepsAllowListedRoutes() {
        assertEquals("add_load", WidgetDeepLink.sanitizeIncomingRoute("add_load"))
        assertEquals("add_paycheck", WidgetDeepLink.sanitizeIncomingRoute(" add_paycheck "))
        assertEquals("paywall", WidgetDeepLink.sanitizeIncomingRoute("paywall"))
        assertEquals("home", WidgetDeepLink.sanitizeIncomingRoute("journal_this_week"))
        assertEquals("analytics", WidgetDeepLink.sanitizeIncomingRoute("stats"))
        assertEquals("attach_pick/camera", WidgetDeepLink.sanitizeIncomingRoute("camera"))
    }

    @Test
    fun sanitizeIncomingRoute_unknownAndMalformedOpenHome() {
        assertEquals("home", WidgetDeepLink.sanitizeIncomingRoute("nope"))
        assertEquals("home", WidgetDeepLink.sanitizeIncomingRoute("social_chat/abc"))
        assertEquals("home", WidgetDeepLink.sanitizeIncomingRoute("add_load?delete=1"))
        assertEquals("home", WidgetDeepLink.sanitizeIncomingRoute("../home"))
        assertEquals("home", WidgetDeepLink.sanitizeIncomingRoute("truckerload://app/sync"))
        assertNull(WidgetDeepLink.sanitizeIncomingRoute(null))
        assertNull(WidgetDeepLink.sanitizeIncomingRoute("   "))
    }

    @Test
    fun journalThisWeekFlag_onlyForTheExactToken() {
        assertTrue(WidgetDeepLink.isJournalThisWeekToken("journal_this_week"))
        assertFalse(WidgetDeepLink.isJournalThisWeekToken("journal_this_week?sync=1"))
        assertFalse(WidgetDeepLink.isJournalThisWeekToken("home"))
        assertFalse(WidgetDeepLink.isJournalThisWeekToken(null))
    }
}
