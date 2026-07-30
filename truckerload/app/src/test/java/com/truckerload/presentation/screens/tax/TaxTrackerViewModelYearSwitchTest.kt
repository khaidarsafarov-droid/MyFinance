package com.truckerload.presentation.screens.tax

import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QUALITY_100 #62 — TaxTrackerViewModel.setYear recalculates from year-scoped repos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TaxTrackerViewModelYearSwitchTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var paycheckRepository: PaycheckRepository
    private lateinit var dieselRepository: DieselRepository
    private lateinit var loadRepository: LoadRepository
    private lateinit var viewModel: TaxTrackerViewModel

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        paycheckRepository = org.mockito.kotlin.mock()
        dieselRepository = org.mockito.kotlin.mock()
        loadRepository = org.mockito.kotlin.mock()

        whenever(paycheckRepository.getPaychecksForYear(2025)).thenReturn(
            listOf(paycheck(net = 10_000.0, year = 2025)),
        )
        whenever(paycheckRepository.getPaychecksForYear(2026)).thenReturn(
            listOf(paycheck(net = 20_000.0, year = 2026)),
        )
        whenever(dieselRepository.getDieselForYear(2025)).thenReturn(
            listOf(diesel(amount = 500.0, year = 2025)),
        )
        whenever(dieselRepository.getDieselForYear(2026)).thenReturn(
            listOf(diesel(amount = 800.0, year = 2026)),
        )
        whenever(loadRepository.getLoadsByYear(2025)).thenReturn(
            listOf(sampleLoad("2025-06-01", year = 2025)),
        )
        whenever(loadRepository.getLoadsByYear(2026)).thenReturn(
            listOf(
                sampleLoad("2026-06-01", year = 2026),
                sampleLoad("2026-07-01", year = 2026),
            ),
        )

        // init loads current calendar year — stub that year too so init does not NPE
        val current = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        if (current != 2025 && current != 2026) {
            whenever(paycheckRepository.getPaychecksForYear(current)).thenReturn(emptyList())
            whenever(dieselRepository.getDieselForYear(current)).thenReturn(emptyList())
            whenever(loadRepository.getLoadsByYear(current)).thenReturn(emptyList())
        }

        viewModel = TaxTrackerViewModel(paycheckRepository, dieselRepository, loadRepository)
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setYear_recalculatesGrossAndDiesel() = runTest {
        val collectJob = launch { viewModel.uiState.collect { } }
        try {
            viewModel.setYear(2025)
            val deadline2025 = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < deadline2025) {
                advanceUntilIdle()
                val state = viewModel.uiState.value
                if (state.year == 2025 && !state.isLoading) break
                kotlinx.coroutines.delay(10)
            }
            assertEquals(2025, viewModel.uiState.value.year)
            assertEquals(10_000.0, viewModel.uiState.value.totalGrossIncome, 0.01)
            assertEquals(500.0, viewModel.uiState.value.dieselDeductions, 0.01)
            assertEquals(1, viewModel.uiState.value.perDiemDays)

            viewModel.setYear(2026)
            val deadline2026 = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < deadline2026) {
                advanceUntilIdle()
                val state = viewModel.uiState.value
                if (state.year == 2026 && !state.isLoading) break
                kotlinx.coroutines.delay(10)
            }
            assertEquals(2026, viewModel.uiState.value.year)
            assertEquals(20_000.0, viewModel.uiState.value.totalGrossIncome, 0.01)
            assertEquals(800.0, viewModel.uiState.value.dieselDeductions, 0.01)
            assertEquals(2, viewModel.uiState.value.perDiemDays)
        } finally {
            collectJob.cancel()
        }
    }

    private fun paycheck(net: Double, year: Int) = Paycheck(
        id = year,
        weekNumber = 1,
        year = year,
        weekLabel = "",
        weekStartDate = "$year-01-01",
        weekEndDate = "$year-01-07",
        driverName = null,
        grossAmount = net,
        netAmount = net,
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )

    private fun diesel(amount: Double, year: Int) = Diesel(
        id = year,
        weekNumber = 1,
        year = year,
        weekLabel = "",
        weekStartDate = "$year-01-01",
        weekEndDate = "$year-01-07",
        totalAmount = amount,
        gallons = null,
        pricePerGallon = null,
        location = null,
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )

    private fun sampleLoad(date: String, year: Int) = Load(
        id = date,
        tripId = date,
        date = date,
        totalRate = 1000.0,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 1,
        year = year,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        durationDays = 1.0,
    )
}
