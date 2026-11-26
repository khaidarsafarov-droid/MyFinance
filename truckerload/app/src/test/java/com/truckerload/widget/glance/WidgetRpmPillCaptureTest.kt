package com.truckerload.widget.glance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetSizeMode
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

/**
 * Renders the live split-header Glance widget for the RPM pill review.
 * Output directory: env RPM_CAPTURE_DIR, or /tmp/widget-rpm-captures.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-rRU-w411dp-h891dp-xxhdpi")
class WidgetRpmPillCaptureTest {

    @Test
    fun captureNormalNarrowAndLongRpm() {
        val narrow = narrowestRpmSize()
        val narrowLayout = cabinLayoutFor(narrow)
        assertTrue("narrow ${narrow.height} bucket=${narrowLayout.bucket}", narrowLayout.showRpm)
        val normal = CabinSize4x3
        val normalLayout = cabinLayoutFor(normal)
        assertEquals(CabinBucket.FULL, normalLayout.bucket)
        assertTrue(normalLayout.showRpm)

        val rpm360 = stats(3.60)
        val rpm1245 = stats(12.45)
        assertEquals("$3.60", WidgetStatsFormatter.formatUsdRpm(rpm360.currentWeeklyRpm))
        assertEquals("$12.45", WidgetStatsFormatter.formatUsdRpm(rpm1245.currentWeeklyRpm))
        assertEquals("$16,472", WidgetStatsFormatter.formatGrossUsd(rpm360.totalLoadRate))

        val dir = File(System.getenv("RPM_CAPTURE_DIR") ?: "/tmp/widget-rpm-captures")
        dir.mkdirs()
        save(render(normal, rpm360), dir, "normal-4x3-rpm-3.60.png")
        save(render(narrow, rpm360), dir, "narrow-rpm-3.60.png")
        save(render(narrow, rpm1245), dir, "narrow-rpm-12.45.png")
        println("RPM captures in ${dir.absolutePath} narrow=${narrow.width.value}x${narrow.height.value}")
    }

    private fun narrowestRpmSize(): DpSize {
        val width = 250.dp
        for (height in 180..320) {
            val size = DpSize(width, height.dp)
            if (cabinLayoutFor(size).showRpm) return size
        }
        error("No wide size at 250dp shows the RPM pill")
    }

    private fun stats(rpm: Double): WidgetStats {
        val gross = 16_472.0
        return WidgetStats(
            loadsCount = 4,
            totalMiles = gross / rpm,
            totalLoadRate = gross,
            weeklyProfitGoal = 12_000.0,
            goalProgressPercent = 100f,
            goalRemainingAmount = 0.0,
            weekLoadMask = 0b0011110,
            dayLoads = listOf(0, 1, 1, 1, 1, 0, 0),
            dayGross = listOf(0.0, 4_200.0, 5_100.0, 3_800.0, 3_372.0, 0.0, 0.0),
            updatedAtMillis = 1L,
        )
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class, ExperimentalCoroutinesApi::class)
    private fun render(size: DpSize, shown: WidgetStats): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val done = CompletableDeferred<android.widget.RemoteViews>()
        CoroutineScope(Dispatchers.Main).launch {
            val result = GlanceRemoteViews().compose(context = activity, size = size) {
                CompositionLocalProvider(
                    LocalCabinColors provides WidgetCabinColors.Forest,
                    LocalWidgetSizeMode provides WidgetSizeMode.AUTO,
                ) {
                    SplitCabinBody(
                        context = activity,
                        shown = shown,
                        week = shown,
                        selectedOffset = 3,
                        wide = size.width >= CabinWideMinWidth,
                        today = LocalDate.of(2026, 11, 26),
                        weekRange = "22 – 28 нояб",
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
        val host = FrameLayout(activity)
        host.addView(remote.apply(activity, host))
        val density = activity.resources.displayMetrics.density
        val width = (size.width.value * density).toInt().coerceAtLeast(1)
        val height = (size.height.value * density).toInt().coerceAtLeast(1)
        host.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, width, height)
        looper.idle()
        val widget = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(widget))
        activity.finish()
        assertTrue("blank ${size.width}x${size.height}", !isBlank(widget))
        return framed(widget, density)
    }

    private fun framed(widget: Bitmap, density: Float): Bitmap {
        val margin = (12f * density).toInt().coerceAtLeast(8)
        val out = Bitmap.createBitmap(
            widget.width + margin * 2,
            widget.height + margin * 2,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(out)
        canvas.drawColor(Color.parseColor("#243044"))
        canvas.drawBitmap(widget, margin.toFloat(), margin.toFloat(), null)
        return out
    }

    private fun isBlank(bitmap: Bitmap): Boolean {
        val first = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        var different = 0
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                if (bitmap.getPixel(x, y) != first) different++
                x += stepX
            }
            y += stepY
        }
        return different < 4
    }

    private fun save(bitmap: Bitmap, dir: File, name: String) {
        FileOutputStream(File(dir, name)).use { out ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        }
    }
}
