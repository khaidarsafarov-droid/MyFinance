package com.truckerload.presentation.screens.goal

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeeklyDayPlan
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.domain.money.Money
import com.truckerload.presentation.theme.TruckerLoadTheme
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the weekly-goal preview off the emulator so light and dark pixels
 * can be checked without a hardware GPU. Not a golden-image test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-rRU-w390dp-h844dp")
class WeeklyGoalScreenshotTest {

    @Test
    fun lightHeroIsIndigo() {
        val bitmap = capture(dark = false, name = "weekly-goal-light.png")
        assertIndigoHero(bitmap)
    }

    @Test
    fun darkHeroIsIndigo() {
        val bitmap = capture(dark = true, name = "weekly-goal-dark.png")
        assertIndigoHero(bitmap)
    }

    private fun assertIndigoHero(bitmap: Bitmap) {
        val pixel = bitmap.getPixel(bitmap.width / 2, (bitmap.height * 0.12f).toInt())
        assertTrue(
            "hero should be dark indigo, was ${Integer.toHexString(pixel)}",
            Color.blue(pixel) > 90 && Color.red(pixel) < 90 && Color.green(pixel) < 80,
        )
    }

    private fun capture(dark: Boolean, name: String): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme(darkTheme = dark, dynamicColor = false, reduceMotion = true) {
                WeeklyGoalContent(
                    progress = previewProgress(),
                    dayPlan = WeeklyDayPlan.rows(
                        weekStart = LocalDate.of(2026, 10, 18),
                        today = LocalDate.of(2026, 10, 18),
                        grossCentsBySqliteDow = mapOf(0 to 99_090L),
                        dailyNeeded = Money.fromCents(171_559L),
                    ),
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
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        val decor = activity.window.decorView
        val width = decor.width.takeIf { it > 0 } ?: 390
        val height = decor.height.takeIf { it > 0 } ?: 844
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(4) { shadowOf(Looper.getMainLooper()).idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        save(bitmap, name)
        return bitmap
    }

    private fun save(bitmap: Bitmap, name: String) {
        listOf(File("/tmp"), File("/opt/cursor/artifacts")).forEach { dir ->
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, name)).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    private fun previewProgress(): WeeklyGoalProgress = WeeklyGoalProgress(
        targetAmount = Money.fromDollars(13_000.0),
        currentGross = Money.fromDollars(990.90),
        progressPercent = 7.62f,
        remainingAmount = Money.fromDollars(12_009.10),
        daysActiveCalendar = 1,
        daysRemainingInWeek = 7,
        totalActiveDays = 1.0,
        actualDailyYield = Money.fromDollars(990.90),
        dailyTargetNeeded = Money.fromCents(171_559L),
        expectedGrossByNow = Money.fromDollars(1_857.0),
        paceStatus = PaceStatus.BEHIND,
        weekLabel = "окт 18 – окт 24, 2026",
        weekNumber = 43,
        year = 2026,
    )
}
