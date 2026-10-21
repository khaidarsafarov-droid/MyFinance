package com.truckerload.domain.premium

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Day7SurveyPolicyTest {
    private val zone = ZoneOffset.UTC
    private val day = 24L * 60L * 60L * 1000L
    private val week = Day7SurveyPolicy.CADENCE_MILLIS
    private val t0 = Instant.parse("2026-01-08T15:00:00Z").toEpochMilli()

    @Test
    fun firstPromptWaitsUntilSevenDaysAfterFirstLaunch() {
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false)
        assertFalse(show(record, t0 + week - 1))
        assertTrue(show(record, t0 + week))
    }

    @Test
    fun existingInstallAnchorsFirstLaunchAndDoesNotShowImmediately() {
        val blank = Day7SurveyPolicy.Record(firstLaunchAt = null, lastShownAt = null, optedOut = false)
        val saved = Day7SurveyPolicy.withFirstLaunch(blank, t0)
        assertEquals(t0, saved.firstLaunchAt)
        assertFalse(show(saved, t0))
        assertEquals(t0, Day7SurveyPolicy.withFirstLaunch(saved, t0 + day).firstLaunchAt)
    }

    @Test
    fun nextDisplayWaitsSevenDaysAfterThePreviousOne() {
        val shownAt = t0 + week
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = shownAt, optedOut = false)
        assertFalse(show(record, shownAt + week - 1))
        assertTrue(show(record, shownAt + week))
        assertEquals(shownAt + week, Day7SurveyPolicy.recordDisplay(record, shownAt + week).lastShownAt)
    }

    @Test
    fun sameLocalDayIsNeverShownTwice() {
        val shownAt = t0 + week
        val laterSameDay = shownAt + 3L * 60L * 60L * 1000L
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = shownAt, optedOut = false)
        assertTrue(Day7SurveyPolicy.sameLocalDay(shownAt, laterSameDay, zone))
        assertFalse(show(record, laterSameDay))
        assertEquals(shownAt, Day7SurveyPolicy.recordDisplay(record, laterSameDay).lastShownAt)
    }

    @Test
    fun aLaterDayInsideTheWindowStaysHidden() {
        val shownAt = t0 + week
        val nextDay = shownAt + day
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = shownAt, optedOut = false)
        assertFalse(Day7SurveyPolicy.sameLocalDay(shownAt, nextDay, zone))
        assertFalse(show(record, nextDay))
    }

    @Test
    fun bannerAndNotificationShareOneTimestamp() {
        val afterPush = Day7SurveyPolicy.recordDisplay(
            Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false),
            t0 + week,
        )
        assertFalse(show(afterPush, t0 + week + 60_000L))
        assertEquals(t0 + week, Day7SurveyPolicy.recordDisplay(afterPush, t0 + week + 60_000L).lastShownAt)
    }

    @Test
    fun optOutIsPermanent() {
        val opted = Day7SurveyPolicy.optOut(
            Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false),
        )
        assertTrue(opted.optedOut)
        assertFalse(show(opted, t0 + week * 12))
        assertNull(
            Day7SurveyPolicy.nextCheckDelayMillis(
                record = opted,
                nowMillis = t0 + week * 12,
                premiumActive = false,
                notificationsPermitted = true,
            ),
        )
    }

    @Test
    fun premiumSuppressesADuePrompt() {
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false)
        assertFalse(show(record, t0 + week * 3, premiumActive = true))
        assertEquals(
            week,
            Day7SurveyPolicy.nextCheckDelayMillis(
                record = record,
                nowMillis = t0 + week * 3,
                premiumActive = true,
                notificationsPermitted = true,
            ),
        )
    }

    @Test
    fun premiumDoesNotPushTheFirstCheckPastTheSevenDayMark() {
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false)
        assertEquals(
            day,
            Day7SurveyPolicy.nextCheckDelayMillis(
                record = record,
                nowMillis = t0 + 6 * day,
                premiumActive = true,
                notificationsPermitted = true,
            ),
        )
    }

    @Test
    fun missingNotificationPermissionDefersADuePromptWithoutShowing() {
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false)
        assertEquals(
            week,
            Day7SurveyPolicy.nextCheckDelayMillis(
                record = record,
                nowMillis = t0 + week,
                premiumActive = false,
                notificationsPermitted = false,
            ),
        )
    }

    @Test
    fun duePromptWithPermissionIsReadyNow() {
        val record = Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false)
        assertEquals(
            0L,
            Day7SurveyPolicy.nextCheckDelayMillis(
                record = record,
                nowMillis = t0 + week,
                premiumActive = false,
                notificationsPermitted = true,
            ),
        )
    }

    @Test
    fun wantMoreCountsAsThisWeeksDisplay() {
        val displayed = Day7SurveyPolicy.recordDisplay(
            Day7SurveyPolicy.Record(firstLaunchAt = t0, lastShownAt = null, optedOut = false),
            t0 + week,
        )
        assertFalse(show(displayed, t0 + week + day))
        assertEquals(
            week - day,
            Day7SurveyPolicy.nextCheckDelayMillis(
                record = displayed,
                nowMillis = t0 + week + day,
                premiumActive = false,
                notificationsPermitted = true,
            ),
        )
    }

    private fun show(
        record: Day7SurveyPolicy.Record,
        nowMillis: Long,
        premiumActive: Boolean = false,
    ): Boolean = Day7SurveyPolicy.shouldShow(record, nowMillis, premiumActive, zone)
}
