package com.truckerload.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

/**
 * Renders pace-colored rings with an in-ring percent label for visual QA.
 * Output: `/tmp/cursor/artifacts/widget-ring-*.png`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetProgressRingPreviewTest {

    @Test
    fun renderPaceScenarios() {
        val context = RuntimeEnvironment.getApplication()
        val outDir = File("/tmp/cursor/artifacts").apply { mkdirs() }
        scenarios().forEach { scenario ->
            val color = WidgetProgressRingBitmap.progressColorForStatus(
                context,
                scenario.pace,
                goalMet = scenario.pace == "GOAL_MET",
                daysRemaining = scenario.daysRemaining,
            )
            val ring = WidgetProgressRingBitmap.create(
                context,
                scenario.percent,
                sizePx = 264,
                progressColor = color,
            )
            val labeled = overlayPercent(ring, scenario.percentLabel)
            FileOutputStream(File(outDir, scenario.fileName)).use { stream ->
                labeled.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }

    private fun overlayPercent(ring: Bitmap, label: String): Bitmap {
        val out = createBitmap(ring.width, ring.height)
        val canvas = Canvas(out)
        canvas.drawBitmap(ring, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3A5748.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = ring.width * 0.18f
        }
        val y = ring.height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(label, ring.width / 2f, y, paint)
        return out
    }

    private fun scenarios() = listOf(
        Scenario("widget-ring-ahead.png", "AHEAD", 24.8f, "24.8%", daysRemaining = 5),
        Scenario("widget-ring-on-track.png", "ON_TRACK", 47.4f, "47.4%", daysRemaining = 3),
        Scenario("widget-ring-behind.png", "BEHIND", 71.0f, "71.0%", daysRemaining = 4),
    )

    private data class Scenario(
        val fileName: String,
        val pace: String,
        val percent: Float,
        val percentLabel: String,
        val daysRemaining: Int,
    )
}
