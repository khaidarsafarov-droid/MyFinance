package com.truckerload.widget.glance

import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.truckerload.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-rRU-xxhdpi")
class RpmPillFitTest {

    @Test
    fun narrowestRpmWidget_longValueFitsWithoutClippingOrCoveringTheAmount() {
        val size = narrowestRpmSize()
        val layout = cabinLayoutFor(size)
        assertTrue(layout.showRpm)
        val spec = fit(layout, size, AMOUNT, LONG_RPM)
        val ratio = pillHeight(spec) / previousPillHeight()
        assertNull("scaled to ${spec.textSp}sp max=${spec.textMaxWidthDp}", spec.textMaxWidthDp)
        assertTrue("text ${spec.textSp}", spec.textSp >= RpmPillSpec.BASE_TEXT_SP - 0.05f)
        assertTrue("height ratio $ratio", ratio in 1.42f..1.58f)
        assertTrue(
            "pill ${pillHeight(spec)} amount ${amountHeight(layout)}",
            pillHeight(spec) <= amountHeight(layout) + 0.5f,
        )
        assertFits(layout, size, AMOUNT, LONG_RPM, spec)
    }

    @Test
    fun fourByThree_keepsThePillProportionalToTheGoalLine() {
        val size = CabinSize4x3
        val layout = cabinLayoutFor(size)
        assertEquals(CabinBucket.FULL, layout.bucket)
        val spec = fit(layout, size, AMOUNT, SHORT_RPM)
        val expected = RpmPillSpec.BASE_TEXT_SP * layout.metricSp.value / RpmPillSpec.BASE_GOAL_SP
        assertEquals(expected, spec.textSp, 0.05f)
        assertEquals(RpmPillSpec.CORNER_DP, spec.cornerDp, 0.01f)
        assertNull(spec.textMaxWidthDp)
        assertFits(layout, size, AMOUNT, SHORT_RPM, spec)
        assertTrue(pillHeight(spec) <= amountHeight(layout) + 0.5f)
    }

    @Test
    fun hugeAmount_keepsPriorityAndThePillDoesNotOverlap() {
        val size = narrowestRpmSize()
        val layout = cabinLayoutFor(size)
        val amount = "\$1,234,567"
        val spec = fit(layout, size, amount, LONG_RPM)
        assertFits(layout, size, amount, LONG_RPM, spec)
        assertTrue(spec.textSp < RpmPillSpec.BASE_TEXT_SP || spec.textMaxWidthDp != null)
    }

    @Test
    fun colorsAndDimens_matchThePillSpec() {
        val context = RuntimeEnvironment.getApplication()
        val resources = context.resources
        val density = resources.displayMetrics.density
        val scaled = density * resources.configuration.fontScale
        assertEquals(RpmPillSpec.TEXT_ARGB, ContextCompat.getColor(context, R.color.widget_rpm_pill_text))
        assertEquals(RpmPillSpec.BG_ARGB, ContextCompat.getColor(context, R.color.widget_rpm_pill_bg))
        assertEquals(RpmPillSpec.STROKE_ARGB, ContextCompat.getColor(context, R.color.widget_rpm_pill_stroke))
        assertEquals(RpmPillSpec.BASE_TEXT_SP, resources.getDimension(R.dimen.widget_rpm_pill_text) / scaled, 0.05f)
        assertEquals(RpmPillSpec.BASE_PAD_H_DP, resources.getDimension(R.dimen.widget_rpm_pill_pad_h) / density, 0.05f)
        assertEquals(RpmPillSpec.BASE_PAD_V_DP, resources.getDimension(R.dimen.widget_rpm_pill_pad_v) / density, 0.05f)
        assertEquals(RpmPillSpec.STROKE_DP, resources.getDimension(R.dimen.widget_rpm_pill_stroke) / density, 0.05f)
        assertEquals(RpmPillSpec.CORNER_DP, resources.getDimension(R.dimen.widget_rpm_pill_radius) / density, 0.05f)
    }

    @Test
    fun standardAndExpandedPreviews_useTheSharedPillAndLeaveTheAmountAlone() {
        val context = RuntimeEnvironment.getApplication()
        val inflater = android.view.LayoutInflater.from(context)
        val scaled = context.resources.displayMetrics.density * context.resources.configuration.fontScale
        listOf(R.layout.widget_standard, R.layout.widget_expanded).forEach { layoutId ->
            val root = inflater.inflate(layoutId, null)
            val amount = root.findViewById<TextView>(R.id.widget_header_title)
            val pill = root.findViewById<TextView>(R.id.widget_rpm_value)
            assertEquals(28f, amount.textSize / scaled, 0.1f)
            assertEquals(13f, pill.textSize / scaled, 0.1f)
            assertEquals(RpmPillSpec.TEXT_ARGB, pill.currentTextColor)
            assertEquals(context.resources.getDimensionPixelSize(R.dimen.widget_rpm_pill_pad_h), pill.paddingLeft)
            assertEquals(context.resources.getDimensionPixelSize(R.dimen.widget_rpm_pill_pad_v), pill.paddingTop)
            val params = amount.layoutParams as android.widget.LinearLayout.LayoutParams
            assertEquals(0, params.width)
            assertEquals(1f, params.weight)
        }
        val compact = inflater.inflate(R.layout.widget_compact, null)
        assertNull(compact.findViewById(R.id.widget_rpm_value))
    }

    private fun narrowestRpmSize(): DpSize {
        for (height in 180..320) {
            val size = DpSize(250.dp, height.dp)
            if (cabinLayoutFor(size).showRpm) return size
        }
        error("No 250dp-wide size shows the RPM pill")
    }

    private fun fit(layout: CabinLayout, size: DpSize, amount: String, label: String): RpmPillLayout {
        val (density, scaled) = paintScale()
        return resolveRpmPill(
            metricSp = layout.metricSp.value,
            space = RpmPillSpace(
                rowWidthDp = size.width.value - layout.paddingH.value * 2f,
                amountWidthDp = rpmLabelWidthDp(amount, layout.amountSp.value, true, density, scaled),
                amountLineHeightDp = rpmLabelHeightDp(layout.amountSp.value, true, density, scaled),
                measureWidthDp = { textSp -> rpmLabelWidthDp(label, textSp, false, density, scaled) },
                measureHeightDp = { textSp -> rpmLabelHeightDp(textSp, false, density, scaled) },
            ),
        )
    }

    private fun assertFits(layout: CabinLayout, size: DpSize, amount: String, label: String, spec: RpmPillLayout) {
        val (density, scaled) = paintScale()
        val row = size.width.value - layout.paddingH.value * 2f
        val amountWidth = rpmLabelWidthDp(amount, layout.amountSp.value, true, density, scaled)
        val textWidth = spec.textMaxWidthDp
            ?: rpmLabelWidthDp(label, spec.textSp, false, density, scaled)
        val pill = textWidth + spec.padHDp * 2f + spec.strokeDp * 2f
        assertTrue(
            "row $row amount $amountWidth pill $pill",
            amountWidth + RpmPillSpec.GAP_DP + pill <= row + 0.6f,
        )
    }

    private fun pillHeight(spec: RpmPillLayout): Float {
        val (density, scaled) = paintScale()
        val text = rpmLabelHeightDp(spec.textSp, false, density, scaled)
        return text + spec.padVDp * 2f + spec.strokeDp * 2f
    }

    private fun amountHeight(layout: CabinLayout): Float {
        val (density, scaled) = paintScale()
        return rpmLabelHeightDp(layout.amountSp.value, true, density, scaled)
    }

    private fun previousPillHeight(): Float {
        val (density, scaled) = paintScale()
        val text = rpmLabelHeightDp(RpmPillSpec.PREVIOUS_TEXT_SP, true, density, scaled)
        return text + RpmPillSpec.PREVIOUS_PAD_V_DP * 2f
    }

    private fun paintScale(): Pair<Float, Float> {
        val resources = RuntimeEnvironment.getApplication().resources
        val density = resources.displayMetrics.density
        return density to density * resources.configuration.fontScale
    }

    private companion object {
        const val AMOUNT = "\$16,472"
        const val SHORT_RPM = "RPM \$3.60"
        const val LONG_RPM = "RPM \$12.45"
    }
}
