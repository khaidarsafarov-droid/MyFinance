package com.truckerload.presentation.screens.assistant

import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.assistant.AssistantToolCall
import com.truckerload.domain.model.WeekSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Locale

class LocalAssistantDispatcherTest {

    private fun createDispatcher(
        weekRepository: WeekRepository = mock(),
    ) = LocalAssistantDispatcher(JournalAssistantPreview(weekRepository))

    @Test
    fun emptyTranscriptIsAmbiguous() = runBlocking {
        val result = createDispatcher().interpret("   ", "ru")
        assertTrue(result is AssistantResult.Ambiguous)
    }

    @Test
    fun dieselPhraseReturnsConfirmWithoutCloud() = runBlocking {
        val result = createDispatcher().interpret("добавь дизель 80 долларов 20 галлонов", "ru")
        val confirm = result as AssistantResult.Confirm
        val draft = confirm.mutation as PendingAssistantMutation.DieselDraft
        assertEquals(80.0, draft.diesel.totalAmount, 0.0)
        assertEquals(20.0, draft.diesel.gallons ?: 0.0, 0.0)
    }

    @Test
    fun unclearPhraseIsAmbiguous() = runBlocking {
        val result = createDispatcher().interpret("ну сделай что-нибудь", "ru")
        assertTrue(result is AssistantResult.Ambiguous)
    }

    @Test
    fun addPaycheckWithoutWeekUsesCurrentWeek() = runBlocking {
        val now = calendarMillis(2026, Calendar.AUGUST, 21, 12, 0)
        val result = createDispatcher().dispatch(
            AssistantToolCall.AddPaycheck(amount = 2500.0, weekNumber = null, year = null),
            now,
        )
        val confirm = result as AssistantResult.Confirm
        val draft = confirm.mutation as PendingAssistantMutation.PaycheckDraft
        assertEquals(2500.0, draft.paycheck.netAmount, 0.0)
        assertTrue(draft.paycheck.weekNumber in 1..53)
        assertEquals(2026, draft.paycheck.year)
    }

    @Test
    fun queryWeeklyGrossReadsLocalSummary() = runBlocking {
        val weekRepository = mock<WeekRepository>()
        val summary = WeekSummary(
            weekNumber = 34,
            year = 2026,
            weekLabel = "W34",
            weekStartDate = "2026-08-16",
            weekEndDate = "2026-08-22",
            loadsCount = 2,
            totalLoadRate = 4100.0,
            totalMiles = 900.0,
            paycheckAmount = 0.0,
            hasPaycheck = false,
            dieselAmount = 80.0,
            hasDiesel = true,
            netProfit = -80.0,
        )
        whenever(weekRepository.getWeekSummaryOnce(any(), any())).thenReturn(summary)
        val result = createDispatcher(weekRepository).dispatch(
            AssistantToolCall.QueryWeeklyGross(weekNumber = 34, year = 2026),
            calendarMillis(2026, Calendar.AUGUST, 21, 12, 0),
        )
        val gross = result as AssistantResult.WeeklyGross
        assertEquals(4100.0, gross.summary.totalLoadRate, 0.0)
        assertEquals(2, gross.summary.loadsCount)
    }

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(Locale.US).apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
