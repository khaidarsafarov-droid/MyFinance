package com.truckerload.presentation.store

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeeklyDayPlan
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.PeriodFinance
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.presentation.screens.add.AddDieselUiState
import com.truckerload.presentation.screens.goal.WeeklyGoalContent
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetSizeMode
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.glance.LocalCabinColors
import com.truckerload.widget.glance.LocalWidgetSizeMode
import com.truckerload.widget.glance.SplitCabinBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the current Compose screens (and the live Glance split widget) into
 * PNGs for the Play listing. Not a golden-image test. Demo names and amounts
 * are fictional. Outputs land in /tmp/play-captures; install_captures.py copies
 * them into the listing tree.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class PlayListingCaptureTest {

    @Test
    @Config(qualifiers = "en-rUS-w414dp-h930dp-420dpi")
    fun captureEnglishPhone() {
        captureLocale(lang = "en", tablet = false)
    }

    @Test
    @Config(qualifiers = "ru-rRU-w414dp-h930dp-420dpi")
    fun captureRussianPhone() {
        captureLocale(lang = "ru", tablet = false)
    }

    @Test
    @Config(qualifiers = "en-rUS-w800dp-h500dp-xxhdpi")
    fun captureEnglishTablet() {
        captureLocale(lang = "en", tablet = true)
    }

    @Test
    @Config(qualifiers = "ru-rRU-w800dp-h500dp-xxhdpi")
    fun captureRussianTablet() {
        captureLocale(lang = "ru", tablet = true)
    }

    private fun captureLocale(lang: String, tablet: Boolean) {
        val loads = playLoads()
        val photo = demoCaptured(demoPhotoFile())
        val scan = demoPending(demoScanFile())
        val finance = PeriodFinance(
            paycheckTotal = 4_860.0,
            dieselTotal = 612.40,
            dieselGallons = 168.4,
            dieselSavings = 41.20,
        )
        val routes = listOf(
            RouteData("Garner, NC", "Dallas, TX", "Garner, NC → Dallas, TX", 2500.0, 850.0, 1, 2.94),
            RouteData("Chicago, IL", "Atlanta, GA", "Chicago, IL → Atlanta, GA", 2400.0, 720.0, 1, 3.33),
            RouteData("Kansas City, MO", "Phoenix, AZ", "Kansas City, MO → Phoenix, AZ", 2890.0, 1180.0, 1, 2.45),
        )
        val weeks = (36..43).map { week ->
            WeekData(week, 2026, "W$week", 4200.0 + (week - 36) * 380.0, 1400.0, 3)
        }
        val days = listOf(
            DailyData("Sun", 1, 0.0, 0),
            DailyData("Mon", 2, 2500.0, 1),
            DailyData("Tue", 3, 2400.0, 1),
            DailyData("Wed", 4, 1940.0, 1),
            DailyData("Thu", 5, 0.0, 0),
            DailyData("Fri", 6, 0.0, 0),
            DailyData("Sat", 7, 0.0, 0),
        )
        val diesel = demoDiesel()
        val dieselState = AddDieselUiState(
            gallonsText = "86.4",
            pricePerGallonText = "3.899",
            discountPriceText = "3.649",
            locationText = "Amarillo, TX",
            recordedAtMillis = 1_760_864_460_000L,
            weekNumber = 43,
            year = 2026,
        )
        if (tablet) {
            val bitmap = render(lang, "tablet") { PlayTabletJournal(loads) }
            assertListingBitmap(bitmap, minWidth = 1600)
            return
        }
        val widget = renderGlanceWidget()
        val shots = listOf(
            "journal" to @Composable { PlayJournal(loads, dense = false) },
            "widget" to @Composable { PlayWidgetLauncher(widget) },
            "add_load" to @Composable { PlayAddLoad() },
            "camera" to @Composable { PlayCamera(photo) },
            "scanner" to @Composable { PlayScanner(scan) },
            "diesel" to @Composable { PlayDiesel(dieselState, diesel) },
            "weekly_goal" to @Composable { PlayWeeklyGoal(lang) },
            "telegram" to @Composable { PlayTelegram() },
            "analytics" to @Composable { PlayAnalytics(finance, routes, weeks, days) },
            "weekly_chart" to @Composable { PlayWeeklyChart(weeks) },
            "finance" to @Composable { PlayFinance(finance, diesel) },
            "journal_list" to @Composable { PlayJournal(loads, dense = true) },
            "profile" to @Composable { PlayProfile() },
            "settings" to @Composable { PlaySettings() },
        )
        shots.forEach { (name, content) ->
            val bitmap = render(lang, name, content)
            assertListingBitmap(bitmap, minWidth = 1000)
        }
    }

    private fun render(lang: String, name: String, content: @Composable () -> Unit): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme(darkTheme = false, dynamicColor = false, reduceMotion = true) {
                if (name == "widget") {
                    Box(Modifier.fillMaxSize()) { content() }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        DemoStatusBar(onDark = false)
                        Box(Modifier.weight(1f)) { content() }
                    }
                }
            }
        }
        val metrics = activity.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(6) { shadowOf(Looper.getMainLooper()).idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        save(bitmap, lang, name)
        return bitmap
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class, ExperimentalCoroutinesApi::class)
    private fun renderGlanceWidget(): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val size = DpSize(400.dp, 360.dp)
        val done = CompletableDeferred<android.widget.RemoteViews>()
        CoroutineScope(Dispatchers.Main).launch {
            val result = GlanceRemoteViews().compose(context = activity, size = size) {
                CompositionLocalProvider(
                    LocalCabinColors provides WidgetCabinColors.Forest,
                    LocalWidgetSizeMode provides WidgetSizeMode.LARGE,
                ) {
                    SplitCabinBody(
                        context = activity,
                        shown = demoWidgetStats(),
                        week = demoWidgetStats(),
                        selectedOffset = 1,
                        wide = true,
                    )
                }
            }
            done.complete(result.remoteViews)
        }
        val looper = shadowOf(Looper.getMainLooper())
        repeat(30) {
            looper.idle()
            if (done.isCompleted) return@repeat
        }
        val remote = done.getCompleted()
        val host = FrameLayout(activity)
        val child = remote.apply(activity, host)
        host.addView(child)
        val density = activity.resources.displayMetrics.density
        val width = (400f * density).toInt().coerceAtLeast(1)
        val height = (360f * density).toInt().coerceAtLeast(1)
        host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, width, height)
        looper.idle()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        host.draw(android.graphics.Canvas(bitmap))
        activity.finish()
        return bitmap
    }

    private fun save(bitmap: Bitmap, lang: String, name: String) {
        val dirs = listOf(File("/tmp/play-captures/$lang"), File("/opt/cursor/artifacts/play-captures/$lang"))
        dirs.forEach { dir ->
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, "$name.png")).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    private fun assertListingBitmap(bitmap: Bitmap, minWidth: Int) {
        assertTrue(
            "capture ${bitmap.width}x${bitmap.height} is below $minWidth px; density was not applied",
            bitmap.width >= minWidth && bitmap.height >= 900,
        )
        var indigo = 0
        var samples = 0
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 40).coerceAtLeast(1)
        var y = stepY
        while (y < bitmap.height) {
            var x = stepX
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                samples++
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                if (kotlin.math.abs(r - 0x5B) < 18 && kotlin.math.abs(g - 0x54) < 18 && kotlin.math.abs(b - 0xE6) < 18) {
                    indigo++
                }
                x += stepX
            }
            y += stepY
        }
        assertTrue("sampled $indigo/$samples pixels of legacy indigo #5B54E6", indigo * 20 < samples)
    }

    private fun demoWidgetStats(): WidgetStats = WidgetStats(
        loadsCount = 4,
        avgCpm = 2.81,
        totalMiles = 4119.0,
        totalLoadRate = 11_605.90,
        weekLabel = "Oct 18 – Oct 24",
        weeklyProfitGoal = 13_000.0,
        goalProgressPercent = 89f,
        goalRemainingAmount = 1_394.10,
        goalDailyNeeded = 232.0,
        goalActualDailyYield = 2_901.0,
        goalDaysRemaining = 6,
        goalPaceStatus = "ahead",
        weekLoadMask = 0b01110,
        dayLoads = listOf(0, 1, 1, 2, 0, 0, 0),
        dayGross = listOf(0.0, 2500.0, 2400.0, 6705.9, 0.0, 0.0, 0.0),
        dayMiles = listOf(0.0, 850.0, 720.0, 2549.0, 0.0, 0.0, 0.0),
        updatedAtMillis = 1_760_864_460_000L,
    )

    private fun demoDiesel(): Diesel = Diesel(
        id = 1,
        weekNumber = 43,
        year = 2026,
        weekLabel = "Oct 18 – Oct 24",
        weekStartDate = "2026-10-18",
        weekEndDate = "2026-10-24",
        totalAmount = Money.fromDollars(315.27),
        gallons = 86.4,
        pricePerGallon = UnitPrice.parse("3.899"),
        discountPricePerGallon = UnitPrice.parse("3.649"),
        location = "Amarillo, TX",
        rawExtractedText = "Pilot Travel Center\n86.400 gal\n3.649",
        sourceFileName = "receipt-amarillo.jpg",
        addedAt = 1_760_864_460_000L,
    )
}

@Composable
private fun PlayWeeklyGoal(lang: String) {
    val ru = lang == "ru"
    WeeklyGoalContent(
        progress = WeeklyGoalProgress(
            targetAmount = Money.fromDollars(13_000.0),
            currentGross = Money.fromDollars(6_840.0),
            progressPercent = 52.6f,
            remainingAmount = Money.fromDollars(6_160.0),
            daysActiveCalendar = 2,
            daysRemainingInWeek = 5,
            totalActiveDays = 2.0,
            actualDailyYield = Money.fromDollars(3_420.0),
            dailyTargetNeeded = Money.fromDollars(1_232.0),
            expectedGrossByNow = Money.fromDollars(3_714.0),
            paceStatus = PaceStatus.AHEAD,
            weekLabel = if (ru) "окт 18 – окт 24, 2026" else "Oct 18 – Oct 24, 2026",
            weekNumber = 43,
            year = 2026,
        ),
        dayPlan = WeeklyDayPlan.rows(
            weekStart = LocalDate.of(2026, 10, 18),
            today = LocalDate.of(2026, 10, 19),
            grossCentsBySqliteDow = mapOf(0 to 250_000L, 1 to 434_000L),
            dailyNeeded = Money.fromDollars(1_232.0),
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
