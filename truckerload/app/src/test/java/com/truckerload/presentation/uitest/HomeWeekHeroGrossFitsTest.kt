package com.truckerload.presentation.uitest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.money.Money
import com.truckerload.presentation.screens.home.HomeWeekHero
import com.truckerload.presentation.theme.TruckerLoadTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * On a 10" tablet the week hero sits in a side column. The week total
 * must stay on one line so "$11,606" is not split mid-number.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeWeekHeroGrossFitsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "en-rUS-w800dp-h500dp-xxhdpi")
    fun weekGrossStaysOnOneLineInNarrowTabletColumn() {
        composeRule.setContent {
            TruckerLoadTheme(darkTheme = false, dynamicColor = false, reduceMotion = true) {
                Box(modifier = Modifier.width(320.dp)) {
                    HomeWeekHero(
                        periodLabel = "",
                        filter = LoadFilter.THIS_WEEK,
                        gross = Money.fromDollars(11_606.0),
                        miles = 4_119.0,
                        loadCount = 4,
                        avgRpm = 2.81,
                        weeklyGoal = 13_000.0,
                        summaryContentDescription = "week hero",
                        onOpenWeeklyGoal = {},
                    )
                }
            }
        }
        val node = composeRule.onNodeWithText("$11,606", useUnmergedTree = true)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        val action = node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action
        assertTrue("missing text layout action", action != null)
        action?.invoke(layouts)
        assertTrue(layouts.isNotEmpty())
        assertEquals(1, layouts.first().lineCount)
        assertEquals("$11,606", layouts.first().layoutInput.text.text)
    }
}
