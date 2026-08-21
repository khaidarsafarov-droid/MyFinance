package com.truckerload.voice

import com.truckerload.presentation.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVoiceJournalTest {

    @Test
    fun addDieselDeepLinkParsesAmountAndGallons() {
        val action = AppVoiceActions.parseUri(
            "truckerload://assistant/add_diesel?amount=80.5&gallons=20",
        ) as AppVoiceAction.AddDiesel
        assertEquals(80.5, action.amount ?: 0.0, 0.0)
        assertEquals(20.0, action.gallons ?: 0.0, 0.0)
        assertNull(action.date)
    }

    @Test
    fun addDieselWithoutAmountOpensFormPath() {
        val action = AppVoiceActions.parseUri("truckerload://assistant/add_diesel")
            as AppVoiceAction.AddDiesel
        assertNull(action.amount)
        assertEquals(Routes.ADD_DIESEL, AppVoiceJournal.formRoute(action))
        assertNull(AppVoiceJournal.toToolCall(action))
    }

    @Test
    fun addPaycheckDeepLinkParsesWeek() {
        val action = AppVoiceActions.parseUri(
            "truckerload://assistant/add_paycheck?amount=2500&weekNumber=12&year=2026",
        ) as AppVoiceAction.AddPaycheck
        assertEquals(2500.0, action.amount ?: 0.0, 0.0)
        assertEquals(12, action.weekNumber)
        assertEquals(2026, action.year)
    }

    @Test
    fun weeklyGrossDeepLinkIsQuery() {
        val action = AppVoiceActions.parseUri("truckerload://assistant/weekly_gross")
            as AppVoiceAction.QueryWeeklyGross
        assertNull(action.weekNumber)
        assertNull(action.year)
    }

    @Test
    fun searchQueryMapsWeeklyGross() {
        val action = AppVoiceActions.parseUri(
            "truckerload://assistant/search?q=weekly%20gross",
        )
        assertTrue(action is AppVoiceAction.QueryWeeklyGross)
        val ru = AppVoiceActions.parseUri(
            "truckerload://assistant/search?q=%D0%B3%D1%80%D0%BE%D1%81%D1%81%20%D0%BD%D0%B5%D0%B4%D0%B5%D0%BB%D0%B8",
        )
        assertTrue(ru is AppVoiceAction.QueryWeeklyGross)
    }

    @Test
    fun spokenGrossDoesNotOpenWeeklyGoalScreen() {
        val action = AppVoiceActions.matchSpoken("какой был гросс на этой неделе")
        assertTrue(action is AppVoiceAction.QueryWeeklyGross)
        val en = AppVoiceActions.matchSpoken("what was my gross this week")
        assertTrue(en is AppVoiceAction.QueryWeeklyGross)
    }

    @Test
    fun openFeatureShortcutIdWeeklyGross() {
        val action = AppVoiceActions.parseUri(
            "truckerload://assistant/open?featureName=weekly_gross",
        )
        assertTrue(action is AppVoiceAction.QueryWeeklyGross)
    }

    @Test
    fun zeroAmountIsRejected() {
        assertNull(AppVoiceJournal.parseAmount("0"))
        assertNull(AppVoiceJournal.parseAmount("-12"))
        assertEquals(80.0, AppVoiceJournal.parseAmount("$80") ?: 0.0, 0.0)
    }

    @Test
    fun toToolCallDoesNotIncludeAmountsInStringFormBeyondTheDataClass() {
        val call = AppVoiceJournal.toToolCall(
            AppVoiceAction.AddDiesel(amount = 80.0, gallons = 20.0, date = "2026-08-21"),
        )
        assertTrue(call is com.truckerload.domain.assistant.AssistantToolCall.AddDiesel)
    }
}
