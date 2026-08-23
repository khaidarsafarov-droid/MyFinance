package com.truckerload.presentation.screens.analytics

import android.app.Application
import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.domain.model.analytics.WeekData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalyticsViewModelTest {

    private lateinit var repository: AnalyticsRepository
    private lateinit var app: Application

    @Before
    fun setUp() {
        repository = mock()
        app = mock()
        whenever(app.getString(any())).thenReturn("error")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refresh_staleSlowResponseDoesNotOverwriteNewerPeriod() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val slowDashboard = dashboard(
            periodLabel = "slow-period",
            weekNumber = 1,
        )
        val fastDashboard = dashboard(
            periodLabel = "fast-period",
            weekNumber = 2,
        )

        whenever(repository.loadDashboard(AnalyticsPeriod.LAST_12_WEEKS)).thenAnswer {
            runBlocking {
                delay(1_000)
                slowDashboard
            }
        }
        whenever(repository.loadDashboard(AnalyticsPeriod.ALL_TIME)).thenReturn(fastDashboard)
        whenever(repository.getLoadsForWeek(any(), any())).thenReturn(emptyList())

        val viewModel = AnalyticsViewModel(repository, app)
        runCurrent()

        viewModel.setPeriod(AnalyticsPeriod.ALL_TIME)
        advanceUntilIdle()

        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(AnalyticsPeriod.ALL_TIME, viewModel.uiState.value.period)
        assertEquals("fast-period", viewModel.uiState.value.weeks.single().label)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun dashboard(periodLabel: String, weekNumber: Int): AnalyticsDashboard {
        val week = WeekData(
            weekNumber = weekNumber,
            year = 2026,
            label = periodLabel,
            gross = 100.0,
            miles = 50.0,
            loadCount = 1,
        )
        return AnalyticsDashboard(
            weeks = listOf(week),
            routes = emptyList(),
            daily = emptyList(),
            summary = AnalyticsSummary(
                totalLoads = 1,
                totalGross = 100.0,
                totalMiles = 50.0,
                avgRpm = 2.0,
                avgGrossPerLoad = 100.0,
                bestWeek = week,
            ),
        )
    }
}
