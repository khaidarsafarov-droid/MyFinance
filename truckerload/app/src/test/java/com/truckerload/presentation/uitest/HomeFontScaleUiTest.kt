package com.truckerload.presentation.uitest

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.components.SoftBottomBar
import com.truckerload.presentation.di.MaintenanceBadgeViewModel
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.screens.home.HomeQuickActionRow
import com.truckerload.presentation.screens.home.HomeScreenTopBar
import com.truckerload.presentation.theme.TruckerLoadTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Navigation labels, home heroes, and quick actions must stay readable
 * at large font scales (M15).
 */
@RunWith(RobolectricTestRunner::class)
class HomeFontScaleUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h1200dp", fontScale = 1.5f)
    fun chromeStaysReadable_atFontScale1_5() {
        assertChromeVisible(expectedScale = 1.5f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h1200dp", fontScale = 2.0f)
    fun chromeStaysReadable_atFontScale2_0() {
        assertChromeVisible(expectedScale = 2.0f)
    }

    private fun assertChromeVisible(expectedScale: Float) {
        var scannerOpened = false
        val maintenance = mock<MaintenanceRepository>()
        whenever(maintenance.watchActiveProgress()).thenReturn(flowOf(emptyList()))
        val badge = MaintenanceBadgeViewModel(maintenance)

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                Column {
                    HomeScreenTopBar(
                        welcomeName = "Demo Driver",
                        weekLabel = "",
                        filter = LoadFilter.THIS_WEEK,
                        openDrawer = {},
                        onSearchToggle = {},
                    )
                    HomeQuickActionRow(
                        onDiesel = {},
                        onPaycheck = {},
                        onExpense = {},
                        onScanner = { scannerOpened = true },
                    )
                    SoftBottomBar(
                        currentRoute = Routes.HOME,
                        onNavigate = {},
                        onQuickAction = {},
                        viewModel = badge,
                    )
                }
            }
        }

        val fontScale = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .resources.configuration.fontScale
        assertEquals(expectedScale, fontScale, 0.01f)
        composeRule.onNodeWithText("TruckoRig").assertIsDisplayed()
        composeRule.onNodeWithText("Demo Driver", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Stats").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Profile")[0].assertHasClickAction()
        composeRule.onNodeWithText("Scanner").assertIsDisplayed().assertHasClickAction().performClick()
        composeRule.onNodeWithText("Diesel").assertIsDisplayed().assertHasClickAction()
        assertTrue(scannerOpened)
    }
}
