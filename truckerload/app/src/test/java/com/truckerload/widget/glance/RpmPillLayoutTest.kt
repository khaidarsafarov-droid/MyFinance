package com.truckerload.widget.glance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RpmPillLayoutTest {

    @Test
    fun baseSize_matchesTheGoalLineAndKeepsThePillShape() {
        val layout = resolve(RpmPillSpec.BASE_GOAL_SP, row = 400f, amount = 80f, amountLine = 40f)
        assertEquals(RpmPillSpec.BASE_TEXT_SP, layout.textSp, 0.01f)
        assertEquals(RpmPillSpec.BASE_PAD_H_DP, layout.padHDp, 0.01f)
        assertEquals(RpmPillSpec.BASE_PAD_V_DP, layout.padVDp, 0.01f)
        assertEquals(RpmPillSpec.STROKE_DP, layout.strokeDp, 0.01f)
        assertEquals(RpmPillSpec.CORNER_DP, layout.cornerDp, 0.01f)
        assertNull(layout.textMaxWidthDp)
    }

    @Test
    fun grownGoalLine_scalesThePillAndCapsTheStroke() {
        val scale = 1.36f
        val layout = resolve(RpmPillSpec.BASE_GOAL_SP * scale, row = 800f, amount = 100f, amountLine = 50f)
        assertEquals(RpmPillSpec.BASE_TEXT_SP * scale, layout.textSp, 0.05f)
        assertEquals(RpmPillSpec.BASE_PAD_H_DP * scale, layout.padHDp, 0.05f)
        assertEquals(RpmPillSpec.BASE_PAD_V_DP * scale, layout.padVDp, 0.05f)
        assertEquals(RpmPillSpec.MAX_STROKE_DP, layout.strokeDp, 0.01f)
        assertEquals(RpmPillSpec.CORNER_DP, layout.cornerDp, 0.01f)
    }

    @Test
    fun longLabel_scalesDownAndStaysClearOfTheAmount() {
        val chars = 10
        val layout = resolveRpmPill(
            metricSp = RpmPillSpec.BASE_GOAL_SP,
            space = RpmPillSpace(
                rowWidthDp = 180f,
                amountWidthDp = 120f,
                amountLineHeightDp = 40f,
                measureWidthDp = { sp -> chars * sp * 0.55f },
                measureHeightDp = { sp -> sp * 1.35f },
            ),
        )
        val textWidth = layout.textMaxWidthDp ?: (chars * layout.textSp * 0.55f)
        val pill = textWidth + layout.padHDp * 2f + layout.strokeDp * 2f
        assertTrue("pill $pill", 120f + RpmPillSpec.GAP_DP + pill <= 180.2f)
        assertTrue(layout.textSp < RpmPillSpec.BASE_TEXT_SP)
        assertTrue(layout.textSp >= RpmPillSpec.BASE_TEXT_SP * RpmPillSpec.MIN_TEXT_SCALE - 0.05f)
    }

    @Test
    fun shortAmountLine_shrinksVerticalPaddingInsteadOfGrowingTheRow() {
        val layout = resolve(RpmPillSpec.BASE_GOAL_SP, row = 400f, amount = 40f, amountLine = 22f)
        val textHeight = layout.textSp * 1.35f
        val total = textHeight + layout.padVDp * 2f + layout.strokeDp * 2f
        assertTrue("pill $total amount 22 padV ${layout.padVDp}", total <= 22.2f)
        assertTrue(layout.padVDp < RpmPillSpec.BASE_PAD_V_DP)
    }

    private fun resolve(metricSp: Float, row: Float, amount: Float, amountLine: Float): RpmPillLayout = resolveRpmPill(
        metricSp = metricSp,
        space = RpmPillSpace(
            rowWidthDp = row,
            amountWidthDp = amount,
            amountLineHeightDp = amountLine,
            measureWidthDp = { sp -> sp * 4f },
            measureHeightDp = { sp -> sp * 1.35f },
        ),
    )
}
