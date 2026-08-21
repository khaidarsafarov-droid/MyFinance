package com.truckerload.presentation.screens.assistant

import com.truckerload.data.assistant.GeminiFunctionCallingClient
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

class GeminiFunctionDispatcherTest {

    @Test
    fun emptyTranscriptIsAmbiguousWithoutCallingGemini() = runBlocking {
        val gemini = RecordingGemini()
        val dispatcher = GeminiFunctionDispatcher(gemini, mock())
        val result = dispatcher.interpret("   ", "ru")
        assertTrue(result is AssistantResult.Ambiguous)
        assertEquals(0, gemini.calls)
    }

    @Test
    fun missingApiKeyFailsWithoutNetwork() = runBlocking {
        val gemini = RecordingGemini(configured = false)
        val dispatcher = GeminiFunctionDispatcher(gemini, mock())
        val result = dispatcher.interpret("добавь дизель 80", "ru")
        assertEquals(AssistantFailKind.NO_API_KEY, (result as AssistantResult.Failed).kind)
        assertEquals(0, gemini.calls)
    }

    @Test
    fun addDieselReturnsConfirmAndDoesNotSave() = runBlocking {
        val dispatcher = GeminiFunctionDispatcher(RecordingGemini(), mock())
        val now = calendarMillis(2026, Calendar.AUGUST, 21, 12, 0)
        val result = dispatcher.dispatch(
            AssistantToolCall.AddDiesel(amount = 80.0, gallons = 20.0, date = null),
            now,
        )
        val confirm = result as AssistantResult.Confirm
        val draft = confirm.mutation as PendingAssistantMutation.DieselDraft
        assertEquals(80.0, draft.diesel.totalAmount, 0.0)
        assertEquals(20.0, draft.diesel.gallons ?: 0.0, 0.0)
    }

    @Test
    fun addPaycheckWithoutWeekUsesCurrentWeek() = runBlocking {
        val dispatcher = GeminiFunctionDispatcher(RecordingGemini(), mock())
        val now = calendarMillis(2026, Calendar.AUGUST, 21, 12, 0)
        val result = dispatcher.dispatch(
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
        val dispatcher = GeminiFunctionDispatcher(RecordingGemini(), weekRepository)
        val result = dispatcher.dispatch(
            AssistantToolCall.QueryWeeklyGross(weekNumber = 34, year = 2026),
            calendarMillis(2026, Calendar.AUGUST, 21, 12, 0),
        )
        val gross = result as AssistantResult.WeeklyGross
        assertEquals(4100.0, gross.summary.totalLoadRate, 0.0)
        assertEquals(2, gross.summary.loadsCount)
    }

    @Test
    fun textOnlyGeminiResponseIsAmbiguous() = runBlocking {
        val body = """{"candidates":[{"content":{"parts":[{"text":"not sure"}]}}]}"""
        val dispatcher = GeminiFunctionDispatcher(RecordingGemini(body = body), mock())
        val result = dispatcher.interpret("ну сделай что-нибудь", "ru")
        assertTrue(result is AssistantResult.Ambiguous)
    }

    @Test
    fun geminiNetworkErrorIsFailed() = runBlocking {
        val dispatcher = GeminiFunctionDispatcher(
            RecordingGemini(fail = true),
            mock(),
        )
        val result = dispatcher.interpret("гросс недели", "ru")
        assertEquals(AssistantFailKind.NETWORK, (result as AssistantResult.Failed).kind)
    }

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(Locale.US).apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

private class RecordingGemini(
    private val configured: Boolean = true,
    private val body: String = "{}",
    private val fail: Boolean = false,
) : GeminiFunctionCallingClient {
    var calls: Int = 0
        private set

    override fun isConfigured(): Boolean = configured

    override suspend fun generateFunctionCall(
        userText: String,
        localeTag: String,
        currentDateIso: String,
        currentWeekNumber: Int,
        currentWeekYear: Int,
    ): Result<String> {
        calls += 1
        return if (fail) Result.failure(IllegalStateException("gemini_http_500")) else Result.success(body)
    }
}
