package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTierTest {

    @Test
    fun square2x2_usesCompactPlate() {
        assertEquals(
            WidgetRemoteViewsFactory.LayoutTier.COMPACT,
            WidgetRemoteViewsFactory.resolveTierFromSize(110, 110, WidgetKind.SQUARE),
        )
        assertEquals(
            WidgetRemoteViewsFactory.LayoutTier.COMPACT,
            WidgetRemoteViewsFactory.resolveTierFromSize(146, 146, WidgetKind.SQUARE),
        )
    }

    @Test
    fun squareResizedWide_usesStandard() {
        assertEquals(
            WidgetRemoteViewsFactory.LayoutTier.STANDARD,
            WidgetRemoteViewsFactory.resolveTierFromSize(250, 110, WidgetKind.SQUARE),
        )
    }

    @Test
    fun wide4x2_usesStandardDashboard() {
        assertEquals(
            WidgetRemoteViewsFactory.LayoutTier.STANDARD,
            WidgetRemoteViewsFactory.resolveTierFromSize(250, 110, WidgetKind.WIDE),
        )
        assertEquals(
            WidgetRemoteViewsFactory.LayoutTier.STANDARD,
            WidgetRemoteViewsFactory.resolveTierFromSize(320, 146, WidgetKind.WIDE),
        )
    }

    @Test
    fun wideTall_usesExpanded() {
        assertEquals(
            WidgetRemoteViewsFactory.LayoutTier.EXPANDED,
            WidgetRemoteViewsFactory.resolveTierFromSize(250, 200, WidgetKind.WIDE),
        )
    }
}
