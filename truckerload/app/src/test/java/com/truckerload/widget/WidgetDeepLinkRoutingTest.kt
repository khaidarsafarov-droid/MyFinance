package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun resolveNavRoute_unknown_returnsNull() {
        assertNull(WidgetDeepLink.resolveNavRoute("nope"))
    }

    @Test
    fun resolveNavRoute_socialChat_passthrough() {
        assertEquals("social_chat/abc%2Fdef", WidgetDeepLink.resolveNavRoute("social_chat/abc%2Fdef"))
    }
}
