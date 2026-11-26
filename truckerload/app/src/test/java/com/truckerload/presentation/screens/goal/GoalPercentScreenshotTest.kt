package com.truckerload.presentation.screens.goal

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeekYieldSnapshot
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.domain.money.Money
import com.truckerload.presentation.screens.home.HomeWeekHero
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetSizeMode
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter
import com.truckerload.widget.glance.LocalCabinColors
import com.truckerload.widget.glance.LocalWidgetSizeMode
import com.truckerload.widget.glance.SplitCabinBody
import com.truckerload.widget.glance.cabinLayoutFor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale

/**
 * Renders the weekly-goal percent at 130% and 245% so the text can be compared
 * before and after the cap is removed. Not a golden-image assertion.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-rRU-w390dp-h844dp-xhdpi")
class GoalPercentScreenshotTest {

    @Test
    fun captureGoalHomeAndWidgetAt130() {
        val progress = overGoal(1_300.0, 1_000.0)
        assertEquals(130f, progress.progressPercent, 0.05f)
        assertEquals("130%", WidgetStatsFormatter.formatRingPercent(progress.progressPercent))
        assertEquals("245%", WidgetStatsFormatter.formatRingPercent(245f))
        val label = shotLabel()
        save(
            captureCompose(widthDp = 390, heightDp = 844) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                    Column(Modifier.fillMaxWidth()) {
                        WeeklyGoalContent(
                            progress = progress,
                            dayPlan = emptyList(),
                            motionSettled = true,
                            isEditingGoal = false,
                            goalInput = "",
                            goalError = null,
                            isSavingGoal = false,
                            suggestedGoals = emptyList(),
                            onStartEdit = {},
                            onGoalInputChange = {},
                            onSaveGoal = {},
                            onCancelEdit = {},
                            onSuggestedGoal = {},
                        )
                    }
                }
            },
            "goal-130-$label.png",
        )
        save(
            captureCompose(widthDp = 390, heightDp = 280) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                    HomeWeekHero(
                        periodLabel = "Эта неделя",
                        filter = LoadFilter.THIS_WEEK,
                        gross = Money.fromDollars(1_300.0),
                        miles = 800.0,
                        loadCount = 2,
                        avgRpm = 1.62,
                        weeklyGoal = 1_000.0,
                        summaryContentDescription = "week",
                        onOpenWeeklyGoal = {},
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            },
            "home-130-$label.png",
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        save(
            captureWidget(activity, DpSize(360.dp, 220.dp), stats(progress.progressPercent, 1_300.0, 1_000.0)),
            "widget-130-$label.png",
        )
        println(
            "GOAL130 percent=${progress.progressPercent} ring=${WidgetStatsFormatter.formatRingPercent(
                progress.progressPercent,
            )} label=$label",
        )
    }

    @Test
    @Config(qualifiers = "fr-rFR-w320dp-h700dp-xhdpi", fontScale = 1.3f)
    fun captureFrenchGoalAt245AndLargeFont() {
        val progress = cappedPreview(percent = 245f, gross = 2_450.0, goal = 1_000.0, expected = 1_857.0)
        save(
            captureCompose(widthDp = 320, heightDp = 420) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                    WeeklyGoalContent(
                        progress = progress,
                        dayPlan = emptyList(),
                        motionSettled = true,
                        isEditingGoal = false,
                        goalInput = "",
                        goalError = null,
                        isSavingGoal = false,
                        suggestedGoals = emptyList(),
                        onStartEdit = {},
                        onGoalInputChange = {},
                        onSaveGoal = {},
                        onCancelEdit = {},
                        onSuggestedGoal = {},
                    )
                }
            },
            "goal-245-fr-font13-${shotLabel()}.png",
        )
    }

    @Test
    @Config(qualifiers = "ru-rRU-w110dp-h110dp-xhdpi", fontScale = 1.3f)
    fun captureNarrowWidgetAt245() {
        val progress = overGoal(2_450.0, 1_000.0)
        assertEquals(245f, progress.progressPercent, 0.05f)
        val label = shotLabel()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val narrow = DpSize(110.dp, 110.dp)
        println("narrow bucket=${cabinLayoutFor(narrow).bucket} font=${activity.resources.configuration.fontScale}")
        save(
            captureWidget(activity, narrow, stats(progress.progressPercent, 2_450.0, 1_000.0)),
            "widget-narrow-245-ru-$label.png",
        )
        listOf("en" to Locale.US, "es" to Locale.forLanguageTag("es"), "fr" to Locale.FRANCE).forEach { (name, locale) ->
            val localized = localized(activity, locale, fontScale = 1.3f)
            save(
                captureWidget(localized, narrow, stats(245f, 2_450.0, 1_000.0)),
                "widget-narrow-245-$name-$label.png",
            )
        }
        val micro = DpSize(250.dp, 110.dp)
        println("micro bucket=${cabinLayoutFor(micro).bucket}")
        save(
            captureWidget(activity, micro, stats(245f, 2_450.0, 13_000.0)),
            "widget-micro-245-ru-$label.png",
        )
        val french = localized(activity, Locale.FRANCE, fontScale = 1.3f)
        save(
            captureWidget(french, DpSize(320.dp, 210.dp), stats(245f, 2_450.0, 1_000.0)),
            "widget-wide-245-fr-$label.png",
        )
    }

    private fun overGoal(gross: Double, goal: Double) = WeeklyGoalCalculator.calculate(
        targetAmount = Money.fromDollars(goal),
        weekLoads = emptyList(),
        weekNumber = 1,
        year = 2020,
        sqlYield = WeekYieldSnapshot(totalGross = Money.fromDollars(gross), totalActiveDays = 0.0),
    )

    private fun cappedPreview(percent: Float, gross: Double, goal: Double, expected: Double) = WeeklyGoalProgress(
        targetAmount = Money.fromDollars(goal),
        currentGross = Money.fromDollars(gross),
        progressPercent = percent,
        remainingAmount = Money.ZERO,
        daysActiveCalendar = 3,
        daysRemainingInWeek = 4,
        totalActiveDays = 2.0,
        actualDailyYield = Money.fromDollars(gross / 2.0),
        dailyTargetNeeded = Money.ZERO,
        expectedGrossByNow = Money.fromDollars(expected),
        paceStatus = PaceStatus.GOAL_MET,
        weekLabel = "18 – 24 oct",
        weekNumber = 43,
        year = 2026,
    )

    private fun stats(percent: Float, gross: Double, goal: Double) = WidgetStats(
        loadsCount = 2,
        totalMiles = 800.0,
        totalLoadRate = gross,
        weeklyProfitGoal = goal,
        goalProgressPercent = percent,
        goalRemainingAmount = (goal - gross).coerceAtLeast(0.0),
        weekLabel = "18 – 24 окт",
        updatedAtMillis = 1L,
        dayGross = listOf(gross, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        dayLoads = listOf(2, 0, 0, 0, 0, 0, 0),
    )

    private fun shotLabel(): String = System.getenv("GOAL_PERCENT_SHOT") ?: "after"

    private fun localized(base: Context, locale: Locale, fontScale: Float): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.fontScale = fontScale
        return base.createConfigurationContext(config)
    }

    private fun captureCompose(widthDp: Int, heightDp: Int, screen: @androidx.compose.runtime.Composable () -> Unit): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent(content = screen)
        val decor = activity.window.decorView
        val density = activity.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (heightDp * density).toInt()
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(4) { shadowOf(Looper.getMainLooper()).idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        activity.finish()
        return bitmap
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class, ExperimentalCoroutinesApi::class)
    private fun captureWidget(context: Context, size: DpSize, shown: WidgetStats): Bitmap {
        val activity = context as? ComponentActivity
            ?: Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val done = CompletableDeferred<android.widget.RemoteViews>()
        CoroutineScope(Dispatchers.Main).launch {
            val result = GlanceRemoteViews().compose(context = context, size = size) {
                CompositionLocalProvider(
                    LocalCabinColors provides WidgetCabinColors.Forest,
                    LocalWidgetSizeMode provides WidgetSizeMode.AUTO,
                ) {
                    SplitCabinBody(
                        context = context,
                        shown = shown,
                        week = shown,
                        selectedOffset = 0,
                        wide = size.width >= 250.dp,
                        today = LocalDate.of(2026, 10, 20),
                        weekRange = "18 – 24 окт",
                    )
                }
            }
            done.complete(result.remoteViews)
        }
        val looper = shadowOf(Looper.getMainLooper())
        repeat(40) {
            looper.idle()
            if (done.isCompleted) return@repeat
        }
        val remote = done.getCompleted()
        val host = FrameLayout(context)
        host.addView(remote.apply(context, host))
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).toInt().coerceAtLeast(1)
        val height = (size.height.value * density).toInt().coerceAtLeast(1)
        host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, width, height)
        looper.idle()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))
        if (activity !== context) activity.finish()
        return bitmap
    }

    private fun save(bitmap: Bitmap, name: String) {
        listOf(File("/opt/cursor/artifacts"), File("/tmp")).forEach { dir ->
            dir.mkdirs()
            FileOutputStream(File(dir, name)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
}
