package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeViewModelPeriodSummaryTest {

    private val loadsFlow = MutableStateFlow<List<Load>>(emptyList())
    private lateinit var loadRepository: LoadRepository
    private lateinit var app: Application
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        loadRepository = mock()
        app = mock()
        whenever(app.getString(any<Int>())).thenReturn("period")
        whenever(app.getString(any<Int>(), any())).thenReturn("period")
        whenever(app.getString(any<Int>(), any(), any())).thenReturn("period")
        whenever(app.getString(any<Int>(), any(), any(), any())).thenReturn("period")
        whenever(loadRepository.watchLoads()).thenReturn(loadsFlow)
        whenever(loadRepository.getLoadsByWeek(any(), any())).thenReturn(loadsFlow)
        whenever(loadRepository.getLoadsByDateRange(any(), any())).thenReturn(loadsFlow)
        viewModel = HomeViewModel(loadRepository, isBotConfigured = false, app)
    }

    @Test
    fun periodSummaryHeader_thisWeek_returnsHeaderWithTotals() {
        val totals = LoadFilterUseCase.Totals(loadCount = 2, totalRate = 5000.0, totalMiles = 2000.0)
        viewModel.setFilter(LoadFilter.THIS_WEEK)

        val header = viewModel.periodSummaryHeader(totals)

        assertTrue(header != null)
        assertEquals(totals, header!!.totals)
    }

    @Test
    fun periodSummaryHeader_allWithoutYear_returnsNull() {
        val totals = LoadFilterUseCase.Totals(loadCount = 5, totalRate = 10000.0, totalMiles = 4000.0)
        viewModel.setFilter(LoadFilter.ALL)

        assertNull(viewModel.periodSummaryHeader(totals))
    }

    @Test
    fun flattenedListItems_filteredPeriod_doesNotIncludeSectionHeader() {
        viewModel.setFilter(LoadFilter.THIS_WEEK)
        val loads = listOf(sampleLoad("a"), sampleLoad("b"))
        val totals = LoadFilterUseCase.Totals(loadCount = 2, totalRate = 5000.0, totalMiles = 2000.0)

        val items = viewModel.flattenedListItems(loads, totals)

        assertEquals(2, items.size)
        assertTrue(items.all { it is HomeListItem.LoadItem })
    }

    @Test
    fun flattenedListItems_allWithYear_doesNotDuplicateYearSummaryHeader() {
        viewModel.setSelectedYear(2026)
        val loads = listOf(sampleLoad("a"))
        val totals = LoadFilterUseCase.Totals(loadCount = 1, totalRate = 2500.0, totalMiles = 850.0)

        val items = viewModel.flattenedListItems(loads, totals)

        assertTrue(items.none { it is HomeListItem.FilteredSectionHeader })
        assertTrue(items.any { it is HomeListItem.YearHeader })
        assertTrue(items.any { it is HomeListItem.LoadItem })
    }

    private fun sampleLoad(id: String) = Load(
        id = id,
        tripId = "T-$id",
        date = "2026-07-16",
        totalRate = 2500.0,
        totalMiles = 850.0,
        pointA = "Atlanta, GA",
        pointB = "Aurora, CO",
        puCount = 1,
        delCount = 1,
        weekNumber = 29,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
