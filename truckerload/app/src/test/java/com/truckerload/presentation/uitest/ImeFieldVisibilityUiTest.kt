package com.truckerload.presentation.uitest

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.truckerload.R
import com.truckerload.domain.money.Money
import com.truckerload.domain.model.Paycheck
import com.truckerload.presentation.screens.analytics.AnalyticsShareDialog
import com.truckerload.presentation.screens.maintenance.AddTaskDialog
import com.truckerload.presentation.screens.maintenance.TaskDraft
import com.truckerload.presentation.screens.paycheck.PaycheckEditorDialog
import com.truckerload.presentation.screens.paycheck.PaycheckEditorState
import com.truckerload.presentation.theme.TruckerLoadTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Short phone with the keyboard open. A focused or scrolled field has to sit
 * fully above the IME, matching [PaywallScreenUiTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h568dp", fontScale = 1.3f)
class ImeFieldVisibilityUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun analyticsShareDialog_familyNameStaysAboveIme() {
        val ime = 280.dp
        val label = string(R.string.analytics_share_family_name)
        composeRule.setContent {
            DeviceConfigurationOverride(insets(ime.value)) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                    AnalyticsShareDialog(
                        givenName = "",
                        familyName = "",
                        onDismiss = {},
                        onPick = { _, _, _ -> },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val node = composeRule.onNodeWithText(label)
            .performScrollTo()
            .fetchSemanticsNode()
        assertAboveIme(node, ime.value, label)
    }

    @Test
    fun addTaskDialog_odometerStaysAboveIme() {
        val ime = 280.dp
        val label = string(R.string.maintenance_current_odometer)
        composeRule.setContent {
            DeviceConfigurationOverride(insets(ime.value)) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                    AddTaskDialog(
                        draft = TaskDraft(),
                        isSaving = false,
                        errorKey = null,
                        onDismiss = {},
                        onChange = {},
                        onSave = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val node = composeRule.onNodeWithText(label)
            .performScrollTo()
            .fetchSemanticsNode()
        assertAboveIme(node, ime.value, label)
    }

    @Test
    fun paycheckEditor_netAmountStaysAboveIme() {
        val ime = 280.dp
        val label = string(R.string.paycheck_net_amount)
        val paycheck = Paycheck(
            id = 1,
            weekNumber = 12,
            year = 2026,
            weekLabel = "Mar 16 – Mar 22",
            weekStartDate = "2026-03-16",
            weekEndDate = "2026-03-22",
            driverName = "Alex Driver",
            grossAmount = Money.fromCents(250_000),
            netAmount = Money.fromCents(180_000),
            rawExtractedText = "",
            sourceFileName = "statement.pdf",
            addedAt = 1_700_000_000_000L,
        )
        composeRule.setContent {
            DeviceConfigurationOverride(insets(ime.value)) {
                TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                    PaycheckEditorDialog(
                        editor = PaycheckEditorState(
                            paycheck = paycheck,
                            netText = "1800",
                            sourceFileName = "statement.pdf",
                            sourceFilePath = null,
                            originalSourceFilePath = null,
                        ),
                        isSaving = false,
                        fileAvailable = false,
                        onDismiss = {},
                        onChange = {},
                        onSave = {},
                        onDelete = {},
                        onOpenFile = {},
                        onAttachFile = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val node = composeRule.onNode(hasSetTextAction())
            .performScrollTo()
            .fetchSemanticsNode()
        assertAboveIme(node, ime.value, label)
    }

    private fun insets(imeDp: Float): DeviceConfigurationOverride {
        val density = composeRule.density.density
        val statusPx = (24f * density).toInt()
        val navPx = (48f * density).toInt()
        val imePx = (imeDp * density).toInt()
        val built = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusPx, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navPx))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, imePx))
            .build()
        return DeviceConfigurationOverride.WindowInsets(built)
    }

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun assertAboveIme(node: SemanticsNode, imeDp: Float, label: String) {
        val screenHeightPx = with(composeRule.density) {
            composeRule.activity.resources.configuration.screenHeightDp.dp.toPx()
        }
        val imePx = with(composeRule.density) { imeDp.dp.toPx() }
        val visibleBottom = screenHeightPx - imePx
        val bounds = node.boundsInRoot
        val scroll = runCatching {
            composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
        }.getOrNull()
        assertTrue(
            "$label bounds $bounds should sit fully above the IME " +
                "(visibleBottom=$visibleBottom, scroll=${scroll?.value()}/${scroll?.maxValue()})",
            bounds.height > 0f && bounds.bottom <= visibleBottom + 1f && bounds.top >= -1f,
        )
    }
}
