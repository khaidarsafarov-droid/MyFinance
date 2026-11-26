package com.truckerload.widget.glance

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.truckerload.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w110dp-h110dp-xhdpi", fontScale = 1.3f)
class GoalLineFitTest {

    @Test
    fun narrowestCellFitsLongPercentInEveryLanguage() {
        val context = RuntimeEnvironment.getApplication()
        val layout = cabinLayoutFor(DpSize(110.dp, 110.dp))
        val width = percentLineWidthDp(layout, 110f)
        val density = context.resources.displayMetrics.density
        val scaled = density * context.resources.configuration.fontScale
        assertEquals(1.3f, context.resources.configuration.fontScale, 0.01f)
        listOf(
            "ru" to Locale.forLanguageTag("ru"),
            "en" to Locale.US,
            "es" to Locale.forLanguageTag("es"),
            "fr" to Locale.FRANCE,
        ).forEach { (name, locale) ->
            val localized = localized(context, locale)
            val percentLine = localized.getString(R.string.widget_goal_percent, "245%")
            val microLine = localized.getString(R.string.widget_goal_micro, "245%", "$13,000")
            listOf(percentLine, microLine).forEach { text ->
                val sp = goalLineSp(text, width, layout.metricSp.value, density, scaled)
                val measured = goalLineWidthDp(text, sp, density, scaled)
                assertTrue(
                    "$name '$text' measured $measured dp at ${sp}sp, cell $width dp",
                    measured <= width - 2f,
                )
            }
        }
    }

    @Test
    fun shortPercentKeepsTheDefaultSizeOnAWideCell() {
        val context = RuntimeEnvironment.getApplication()
        val layout = cabinLayoutFor(DpSize(360.dp, 220.dp))
        val width = percentLineWidthDp(layout, 360f)
        val density = context.resources.displayMetrics.density
        val sp = goalLineSp("130% цели", width, layout.metricSp.value, density, density)
        assertEquals(layout.metricSp.value, sp, 0.05f)
    }

    private fun localized(base: Context, locale: Locale): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.fontScale = base.resources.configuration.fontScale
        return base.createConfigurationContext(config)
    }
}
