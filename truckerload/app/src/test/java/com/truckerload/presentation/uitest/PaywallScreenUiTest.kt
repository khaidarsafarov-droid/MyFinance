package com.truckerload.presentation.uitest

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.truckerload.R
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.presentation.premium.PaywallScreen
import com.truckerload.presentation.theme.TruckerLoadTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Paywall on a short phone. The subscribe action has to scroll into view and
 * stay fully above the navigation bar and the IME. Qualifiers are 320×568dp
 * at font scale 1.3.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h568dp", fontScale = 1.3f)
class PaywallScreenUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun smallScreen_subscribeCtaScrollsAboveNavigationBar() {
        val navBar = 48.dp
        composePaywall(navigationBar = navBar, ime = 0.dp)

        val screenHeightPx = screenHeightPx()
        val navBarPx = with(composeRule.density) { navBar.toPx() }
        val cta = scrollSubscribeCtaIntoView()
        assertFullyInside(
            node = cta,
            visibleBottom = screenHeightPx - navBarPx,
            label = "navigation bar",
        )
    }

    @Test
    fun smallScreen_featureRowsRender() {
        composePaywall(navigationBar = 48.dp, ime = 0.dp)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val body = app.getString(R.string.premium_body)
        assertTrue(body.contains("Google Play"))
        assertFalse(body.contains("scanner, camera"))

        composeRule.onNodeWithText(app.getString(R.string.premium_price_pill)).assertIsDisplayed()
        scrollIntoView(app.getString(R.string.premium_included_title))
        featureRows().forEach { (title, benefit) ->
            scrollIntoView(app.getString(title))
            scrollIntoView(app.getString(benefit))
        }
        scrollIntoView(app.getString(R.string.premium_free_title))
        listOf(
            R.string.premium_free_loads,
            R.string.premium_free_diesel,
            R.string.premium_free_pay,
            R.string.premium_free_deadhead,
        ).forEach { scrollIntoView(app.getString(it)) }
    }

    @Test
    fun smallScreen_subscribeCtaScrollsAboveIme() {
        val navBar = 48.dp
        val ime = 240.dp
        composePaywall(navigationBar = navBar, ime = ime)

        val screenHeightPx = screenHeightPx()
        val coveredPx = with(composeRule.density) { maxOf(navBar, ime).toPx() }
        val cta = scrollSubscribeCtaIntoView()
        assertFullyInside(
            node = cta,
            visibleBottom = screenHeightPx - coveredPx,
            label = "IME",
        )
    }

    private fun composePaywall(navigationBar: Dp, ime: Dp) {
        val config = composeRule.activity.resources.configuration
        assertEquals(320, config.screenWidthDp)
        assertEquals(568, config.screenHeightDp)
        assertEquals(1.3f, config.fontScale, 0.01f)

        val density = composeRule.density.density
        val statusPx = (24f * density).toInt()
        val navPx = (navigationBar.value * density).toInt()
        val imePx = (ime.value * density).toInt()
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusPx, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navPx))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, imePx))
            .build()

        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(insets)) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                    PaywallScreen(feature = PremiumFeature.TELEGRAM, onBack = {})
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun featureRows(): List<Pair<Int, Int>> = listOf(
        R.string.premium_feature_telegram_title to R.string.premium_feature_telegram_benefit,
        R.string.premium_feature_analytics_title to R.string.premium_feature_analytics_benefit,
        R.string.premium_feature_goal_title to R.string.premium_feature_goal_benefit,
        R.string.premium_feature_map_title to R.string.premium_feature_map_benefit,
        R.string.premium_feature_tax_title to R.string.premium_feature_tax_benefit,
        R.string.premium_feature_scanner_title to R.string.premium_feature_scanner_benefit,
        R.string.premium_feature_camera_title to R.string.premium_feature_camera_benefit,
        R.string.premium_feature_export_title to R.string.premium_feature_export_benefit,
        R.string.premium_feature_drive_title to R.string.premium_feature_drive_benefit,
    )

    private fun scrollIntoView(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun scrollSubscribeCtaIntoView(): SemanticsNode {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val loading = app.getString(R.string.premium_price_loading)
        val trial = app.getString(R.string.premium_subscribe)
        val ctaMatcher = hasClickAction() and (
            hasText(loading, substring = true) or
                hasText(trial, substring = true) or
                hasText("Subscribe", substring = true)
            )

        composeRule.onNodeWithText(app.getString(R.string.premium_title)).assertIsDisplayed()
        val before = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()
        assertEquals(0f, before, 0.5f)

        val cta = composeRule.onNode(ctaMatcher)
            .performScrollTo()
            .assertIsDisplayed()
            .fetchSemanticsNode()

        val axis = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        val after = axis.value()
        val max = axis.maxValue()
        assertTrue(
            "Subscribe action should scroll into view on a 320×568dp screen (offset=$after max=$max)",
            after > before,
        )
        return cta
    }

    private fun screenHeightPx(): Float {
        val heightDp = composeRule.activity.resources.configuration.screenHeightDp
        return with(composeRule.density) { heightDp.dp.toPx() }
    }

    private fun assertFullyInside(node: SemanticsNode, visibleBottom: Float, label: String) {
        val root = node.semanticsRoot()
        val bounds = node.boundsInRoot
        val width = root.boundsInRoot.width
        assertTrue(
            "CTA bounds $bounds should sit fully above the $label " +
                "(visibleBottom=$visibleBottom, rootWidth=$width, root=${root.boundsInRoot})",
            bounds.width > 0f &&
                bounds.height > 0f &&
                bounds.top >= -1f &&
                bounds.left >= -1f &&
                bounds.right <= width + 1f &&
                bounds.bottom <= visibleBottom + 1f,
        )
    }

    private fun SemanticsNode.semanticsRoot(): SemanticsNode {
        var current = this
        while (current.parent != null) {
            current = current.parent!!
        }
        return current
    }
}
