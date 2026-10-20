package com.truckerload.presentation.uitest

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.glance.CabinSize2x2
import com.truckerload.widget.glance.CabinSize4x2
import com.truckerload.widget.glance.SplitCabinBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Home-screen Glance widget composition. The square and wide receivers both
 * paint [SplitCabinBody]; these tests feed it demo week stats and the empty
 * default (no goal, no loads).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeGlanceWidgetUiTest {

    @Test
    fun squareWidget_showsDemoGrossAndGoal() = runGlanceAppWidgetUnitTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setAppWidgetSize(CabinSize2x2)
        provideComposable {
            SplitCabinBody(
                context = context,
                shown = demoWeek(),
                week = demoWeek(),
                selectedOffset = 0,
                wide = false,
            )
        }
        awaitIdle()
        onNode(hasText("$2,500")).assertExists()
        onNode(hasText("50% · goal $5,000")).assertExists()
        onNode(hasContentDescription("Camera")).assertExists()
    }

    @Test
    fun wideWidget_showsDemoGrossAndCamera() = runGlanceAppWidgetUnitTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setAppWidgetSize(CabinSize4x2)
        provideComposable {
            SplitCabinBody(
                context = context,
                shown = demoWeek(),
                week = demoWeek(),
                selectedOffset = 1,
                wide = true,
            )
        }
        awaitIdle()
        onNode(hasText("$2,500")).assertExists()
        onNode(hasContentDescription("Camera")).assertExists()
    }

    @Test
    fun emptyState_showsNoGoalSet() = runGlanceAppWidgetUnitTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setAppWidgetSize(DpSize(170.dp, 170.dp))
        provideComposable {
            SplitCabinBody(
                context = context,
                shown = WidgetStats(),
                week = WidgetStats(),
                selectedOffset = 0,
                wide = false,
            )
        }
        awaitIdle()
        onNode(hasText("No goal set")).assertExists()
    }

    private fun demoWeek() = WidgetStats(
        loadsCount = 3,
        totalMiles = 850.0,
        totalLoadRate = 2500.0,
        weeklyProfitGoal = 5000.0,
        goalProgressPercent = 50f,
        weekLabel = "Oct 19 – Oct 25",
        updatedAtMillis = 1L,
        dayGross = listOf(0.0, 2500.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        dayLoads = listOf(0, 1, 0, 0, 0, 0, 0),
    )
}
