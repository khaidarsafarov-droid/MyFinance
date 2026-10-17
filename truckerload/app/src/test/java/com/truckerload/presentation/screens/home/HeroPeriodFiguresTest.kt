package com.truckerload.presentation.screens.home

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class HeroPeriodFiguresTest {

    @Test
    fun fitsBeside_whenGrossAndMetricsLeaveRoom() {
        assertTrue(heroAmountFitsBeside(grossWidthPx = 100, metricsWidthPx = 40, gapPx = 12, rowWidthPx = 152))
        assertTrue(heroAmountFitsBeside(grossWidthPx = 100, metricsWidthPx = 40, gapPx = 12, rowWidthPx = 400))
    }

    @Test
    fun fitsBeside_falseWhenTheRowIsTooNarrow() {
        assertFalse(heroAmountFitsBeside(grossWidthPx = 100, metricsWidthPx = 40, gapPx = 12, rowWidthPx = 151))
        assertFalse(heroAmountFitsBeside(grossWidthPx = 100, metricsWidthPx = 40, gapPx = 12, rowWidthPx = 0))
    }

    @Test
    fun largestFittingSize_returnsMaxWhenItAlreadyFits() {
        assertEquals(40f, largestFittingSize(8f, 40f) { true }, 0.001f)
    }

    @Test
    fun largestFittingSize_returnsMinWhenNothingFits() {
        assertEquals(8f, largestFittingSize(8f, 40f) { false }, 0.001f)
        assertEquals(8f, largestFittingSize(8f, 8f) { false }, 0.001f)
    }

    @Test
    fun largestFittingSize_picksTheLargestSizeThatFits() {
        val fitted = largestFittingSize(1f, 40f) { it <= 20.25f }
        assertTrue(fitted <= 20.25f)
        assertTrue(fitted > 20.15f)
    }

    @Test
    fun fitSingleLine_keepsTheHeroSizeWhenTheWidthIsEnough() {
        val size = fitSingleLineFontSize(
            preferred = 40.sp,
            minFontSize = 11.sp,
            maxWidthPx = 300,
            widthAt = { (it * 5f).toInt() },
        )
        assertEquals(40f, size.value, 0.001f)
    }

    @Test
    fun fitSingleLine_shrinksAWeekTotalOntoOneLine() {
        // 10px per sp. A 221px slot (220px after the safety inset) holds 22sp, not 40sp.
        val widthAt = { sizeSp: Float -> (sizeSp * 10f).toInt() }
        val size = fitSingleLineFontSize(
            preferred = 40.sp,
            minFontSize = 11.sp,
            maxWidthPx = 221,
            widthAt = widthAt,
        )
        assertTrue(widthAt(size.value) <= 220)
        assertTrue(size.value > 21.9f)
    }

    @Test
    fun fitSingleLine_keepsGoingBelowTheFloorForAHugeAmount() {
        // 20px per sp. 11sp needs 220px, so a 101px slot (100px limit) must go under 11sp.
        val widthAt = { sizeSp: Float -> (sizeSp * 20f).toInt() }
        val size = fitSingleLineFontSize(
            preferred = 40.sp,
            minFontSize = 11.sp,
            maxWidthPx = 101,
            widthAt = widthAt,
        )
        assertTrue(widthAt(size.value) <= 100)
        assertTrue(size.value > 4.9f)
        assertTrue(size.value < 11f)
    }

    @Test
    fun fitSingleLine_unboundedWidthKeepsThePreferredSize() {
        val size = fitSingleLineFontSize(
            preferred = 40.sp,
            minFontSize = 11.sp,
            maxWidthPx = Constraints.Infinity,
            widthAt = { 10_000 },
        )
        assertEquals(40f, size.value, 0.001f)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HeroAmountPaintFitTest {

    @Test
    fun twelveThousand_staysReadableOnOneLineOfACompactCard() {
        val dm = Resources.getSystem().displayMetrics
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40f, dm)
        }
        val sample = "$12,200.00"
        val innerPx = (360f - 16f * 2f - 20f * 2f) * dm.density
        var sizeSp = 40f
        while (sizeSp > 1f && paint.measureText(sample) > innerPx) {
            sizeSp -= 0.5f
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, dm)
        }
        assertTrue(
            "$sample fitted to ${sizeSp}sp inside ${innerPx}px",
            sizeSp >= 24f && paint.measureText(sample) <= innerPx + 0.5f,
        )
    }

    @Test
    fun millionDollarGross_stillFitsOneLine() {
        val dm = Resources.getSystem().displayMetrics
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sample = "$1,234,567.89"
        val innerPx = (360f - 16f * 2f - 20f * 2f) * dm.density
        var sizeSp = 40f
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, dm)
        while (sizeSp > 1f && paint.measureText(sample) > innerPx) {
            sizeSp -= 0.5f
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, dm)
        }
        assertTrue(
            "$sample fitted to ${sizeSp}sp inside ${innerPx}px",
            sizeSp >= 16f && paint.measureText(sample) <= innerPx + 0.5f,
        )
    }
}
