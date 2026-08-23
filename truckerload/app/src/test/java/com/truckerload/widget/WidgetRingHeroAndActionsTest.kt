package com.truckerload.widget

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.truckerload.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRingHeroAndActionsTest {

    private val weekStats = WidgetStats(
        loadsCount = 3,
        totalMiles = 2546.0,
        totalLoadRate = 8455.0,
        weeklyProfitGoal = 13_000.0,
        goalProgressPercent = 65f,
        weekLabel = "Aug 16 — Aug 22",
        weekLoadMask = 0b0100101,
        updatedAtMillis = 1L,
    )

    @Test
    fun standardWidget_putsGrossAndGoalOnRing_andShowsDieselAction() {
        val context = RuntimeEnvironment.getApplication()
        WidgetPrefsStore.save(
            context,
            appWidgetId = 42,
            prefs = WidgetPrefs(sizeMode = WidgetSizeMode.MEDIUM),
        )

        val views = WidgetRemoteViewsFactory.build(
            context,
            appWidgetId = 42,
            stats = weekStats,
            kind = WidgetKind.WIDE,
        )
        val root = views.apply(context, FrameLayout(context))

        val gross = root.findViewById<TextView>(R.id.widget_gross_hero)
        val goal = root.findViewById<TextView>(R.id.widget_goal_subtitle)
        val percent = root.findViewById<TextView>(R.id.widget_ring_percent)
        val ringCenter = root.findViewById<View>(R.id.widget_ring_center)
        val diesel = root.findViewById<ImageView>(R.id.widget_btn_diesel)

        assertNotNull(gross)
        assertNotNull(goal)
        assertNotNull(percent)
        assertNotNull(ringCenter)
        assertNotNull(diesel)
        assertEquals("$8,455", gross.text.toString())
        assertTrue(goal.text.toString().contains("$13,000"))
        assertTrue(percent.text.toString().contains("65"))
        assertEquals(View.VISIBLE, diesel.visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_btn_camera).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_btn_scanner).visibility)
        assertEquals(
            context.getString(R.string.widget_diesel_short),
            root.findViewById<TextView>(R.id.widget_btn_diesel_label).text.toString(),
        )
    }

    @Test
    fun compactWidget_keepsDieselIconWithCameraAndScan() {
        val context = RuntimeEnvironment.getApplication()
        WidgetPrefsStore.save(
            context,
            appWidgetId = 7,
            prefs = WidgetPrefs(sizeMode = WidgetSizeMode.SMALL),
        )

        val views = WidgetRemoteViewsFactory.build(
            context,
            appWidgetId = 7,
            stats = weekStats,
            kind = WidgetKind.SQUARE,
        )
        val root = views.apply(context, FrameLayout(context))

        assertNotNull(root.findViewById<ImageView>(R.id.widget_btn_diesel))
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_btn_diesel_label).visibility)
        assertEquals("$8,455", root.findViewById<TextView>(R.id.widget_gross_hero).text.toString())
    }
}
