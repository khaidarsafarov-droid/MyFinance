package com.truckerload.presentation.uitest

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.screens.home.HomeListItem
import com.truckerload.presentation.screens.home.HomeScreenContent
import com.truckerload.presentation.screens.home.HomeScreenTopBar
import com.truckerload.presentation.screens.home.HomeUiState
import com.truckerload.presentation.screens.home.HomeViewModel
import com.truckerload.presentation.theme.TruckerLoadTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phone home dashboard: brand header, quick actions, and a demo journal card.
 * [HomeScreen] itself pulls a Hilt view model; this drives the same content
 * composable with a fake [LoadRepository].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1200dp")
class HomeDashboardUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_showsDemoLoad_andScannerAction() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val loads = mock<LoadRepository>()
        whenever(loads.watchTotalLoadStats()).thenReturn(flowOf(LoadStatsAgg(1, 850.0, 250_000L)))
        val viewModel = HomeViewModel(loads, app)
        val demo = demoLoad()
        var scannerOpened = false

        composeRule.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
                CompositionLocalProvider(
                    LocalLoadRepository provides loads,
                    LocalWeeklyProfitGoalStore provides WeeklyProfitGoalStore(app),
                    LocalUserProfileStore provides UserProfileStore(app),
                ) {
                    Scaffold { padding ->
                        Column {
                            HomeScreenTopBar(
                                welcomeName = "Demo Driver",
                                weekLabel = "",
                                filter = LoadFilter.THIS_WEEK,
                                openDrawer = {},
                                onSearchToggle = {},
                            )
                            HomeScreenContent(
                                paddingValues = PaddingValues(0.dp),
                                uiState = HomeUiState(filter = LoadFilter.THIS_WEEK),
                                listItems = listOf(HomeListItem.LoadItem(demo)),
                                periodSummary = null,
                                rpmThresholds = RpmThresholds(minProfit = 2.0, targetProfit = 2.5),
                                viewModel = viewModel,
                                filteredLoads = listOf(demo),
                                useRoomPaging = false,
                                pagedLoads = null,
                                onLoadClick = {},
                                context = app,
                                onOpenCalendar = {},
                                onLoadCamera = { _, _, _ -> },
                                onLoadScan = { _, _, _ -> },
                                onOpenScanner = { scannerOpened = true },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("TruckoRig").assertIsDisplayed()
        composeRule.onNodeWithText("Demo Driver", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Recent loads").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("#T-DEMO1", substring = true).performScrollTo().assertIsDisplayed()
        val scannerLabel = composeRule.onNodeWithText("Scanner")
            .performScrollTo()
            .fetchSemanticsNode()
            .boundsInRoot
        val actions = composeRule.onAllNodes(hasClickAction())
        val scannerIndex = actions.fetchSemanticsNodes().indexOfFirst { node ->
            val bounds = node.boundsInRoot
            val gap = scannerLabel.top - bounds.bottom
            abs(bounds.center.x - scannerLabel.center.x) < 24f && gap in 0f..48f
        }
        check(scannerIndex >= 0) { "Scanner quick action was not on screen" }
        actions[scannerIndex].performSemanticsAction(SemanticsActions.OnClick) { click ->
            click()
        }
        assertTrue(scannerOpened)
    }

    private fun demoLoad() = Load(
        id = "demo-load-1",
        tripId = "T-DEMO1",
        date = "2026-10-20",
        totalRate = Money.fromDollars(2500.0),
        totalMiles = 850.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 43,
        year = 2026,
        rawMessage = "Trip ID: T-DEMO1",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
