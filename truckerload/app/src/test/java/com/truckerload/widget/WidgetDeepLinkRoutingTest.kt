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
        assertEquals("camera", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_CAMERA))
        assertEquals("scanner", WidgetDeepLink.resolveNavRoute(WidgetDeepLink.ROUTE_SCANNER))
    }

    @Test
    fun resolveNavRoute_unknown_returnsNull() {
        assertNull(WidgetDeepLink.resolveNavRoute("nope"))
    }
}
