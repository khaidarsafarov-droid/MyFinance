package com.truckerload.domain.model

import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.money.Money
import com.truckerload.presentation.components.toCardModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadCancellationTest {

    private val now = 1_800_000_000_000L
    private val hour = 60L * 60L * 1000L

    @Test
    fun cancel_setsTonuRate_zeroesLoadedMiles_keepsDeadheadAndOriginals() {
        val load = sample()
        val cancelled = LoadCancellation.cancel(load, Money.fromCents(17_500L), now)

        assertTrue(cancelled.isCancelled)
        assertEquals(now, cancelled.cancelledAt)
        assertEquals(Money.fromCents(17_500L), cancelled.totalRate)
        assertEquals(Money.fromCents(17_500L), cancelled.tonuAmount)
        assertEquals(0.0, cancelled.totalMiles, 0.0)
        assertEquals(55.0, cancelled.deadheadMiles, 0.0)
        assertEquals(55.0, cancelled.drivenMiles, 0.0)
        assertEquals(Money.fromCents(441_893L), cancelled.originalRate)
        assertEquals(900.0, cancelled.originalMiles!!, 0.0)
        assertEquals(now, cancelled.updatedAt)
    }

    @Test
    fun cancel_withNoPay_isAllowed() {
        val cancelled = LoadCancellation.cancel(sample(), Money.ZERO, now)
        assertTrue(cancelled.isCancelled)
        assertEquals(Money.ZERO, cancelled.totalRate)
    }

    @Test
    fun cancel_negativeAmount_becomesZero() {
        val cancelled = LoadCancellation.cancel(sample(), Money.fromCents(-100L), now)
        assertEquals(Money.ZERO, cancelled.totalRate)
    }

    @Test
    fun cancelAgain_onlyChangesTonu_andKeepsOriginals() {
        val first = LoadCancellation.cancel(sample(), Money.fromCents(17_500L), now)
        val second = LoadCancellation.cancel(first, Money.fromCents(22_500L), now + 1)

        assertEquals(Money.fromCents(22_500L), second.totalRate)
        assertEquals(Money.fromCents(441_893L), second.originalRate)
        assertEquals(900.0, second.originalMiles!!, 0.0)
        assertEquals(now, second.cancelledAt)
    }

    @Test
    fun undo_restoresRateAndMiles_andKeepsDeadhead() {
        val load = sample()
        val restored = LoadCancellation.undo(LoadCancellation.cancel(load, Money.fromCents(22_500L), now), now + 5)

        assertFalse(restored.isCancelled)
        assertNull(restored.cancelledAt)
        assertNull(restored.originalRate)
        assertNull(restored.originalMiles)
        assertNull(restored.tonuAmount)
        assertEquals(load.totalRate, restored.totalRate)
        assertEquals(load.totalMiles, restored.totalMiles, 0.0)
        assertEquals(55.0, restored.deadheadMiles, 0.0)
        assertEquals(load, restored.copy(updatedAt = load.updatedAt))
    }

    @Test
    fun undo_onActiveLoad_isNoOp() {
        val load = sample()
        assertEquals(load, LoadCancellation.undo(load, now))
    }

    @Test
    fun tonuWindow_isTwoHoursBeforePickupOrLater() {
        val pickup = now + 3 * hour
        assertFalse(LoadCancellation.isWithinTonuWindow(pickup, now))
        assertTrue(LoadCancellation.isWithinTonuWindow(pickup, pickup - 2 * hour))
        assertTrue(LoadCancellation.isWithinTonuWindow(pickup, pickup - hour))
        assertTrue(LoadCancellation.isWithinTonuWindow(pickup, pickup + hour))
        assertFalse(LoadCancellation.isWithinTonuWindow(null, now))
    }

    @Test
    fun suggestedTonu_onlyPreselectedInsideWindow_orCurrentWhenCancelled() {
        val load = sample()
        assertEquals(Money.fromCents(17_500L), LoadCancellation.suggestedTonu(load, now + hour, now))
        assertNull(LoadCancellation.suggestedTonu(load, now + 5 * hour, now))
        assertNull(LoadCancellation.suggestedTonu(load, null, now))
        val cancelled = LoadCancellation.cancel(load, Money.fromCents(22_500L), now)
        assertEquals(Money.fromCents(22_500L), LoadCancellation.suggestedTonu(cancelled, null, now))
    }

    @Test
    fun isValidTonu_acceptsZeroAndRejectsMissingOrHuge() {
        assertTrue(LoadCancellation.isValidTonu(Money.ZERO))
        assertTrue(LoadCancellation.isValidTonu(Money.fromCents(17_500L)))
        assertFalse(LoadCancellation.isValidTonu(null))
        assertFalse(LoadCancellation.isValidTonu(Money.fromCents(-1L)))
        assertFalse(LoadCancellation.isValidTonu(Money.fromCents(1_000_001L)))
    }

    @Test
    fun carryOver_keepsCancelledState_whenRateConIsResent() {
        val cancelled = LoadCancellation.cancel(sample(), Money.fromCents(17_500L), now)
        val incoming = sample().copy(totalRate = Money.fromCents(450_000L), totalMiles = 910.0)
        val merged = LoadCancellation.carryOver(cancelled, incoming)

        assertTrue(merged.isCancelled)
        assertEquals(Money.fromCents(17_500L), merged.totalRate)
        assertEquals(0.0, merged.totalMiles, 0.0)
        assertEquals(Money.fromCents(450_000L), merged.originalRate)
        assertEquals(910.0, merged.originalMiles!!, 0.0)
        assertEquals(incoming, LoadCancellation.carryOver(sample(), incoming))
    }

    @Test
    fun stats_countTonuAsIncome_oneDay_andNoRpm() {
        val cancelled = LoadCancellation.cancel(sample(), Money.fromCents(17_500L), now).withRouteMetrics()

        assertEquals(1.0, LoadYieldCalculator.computeActiveDurationDays(cancelled), 0.0)
        assertEquals(1.0, cancelled.durationDays, 0.0)
        assertEquals(Money.fromCents(17_500L), cancelled.pace)
        val card = cancelled.toCardModel(minProfitRpm = 2.0, targetProfitRpm = 3.0)
        assertTrue(card.isCancelled)
        assertNull(card.rpm)
        assertNull(card.rpmTier)
        assertEquals(Money.fromCents(17_500L), card.totalRate)
        assertEquals(55.0, card.drivenMiles, 0.0)
    }

    @Test
    fun cancelledLoad_withoutDeadhead_hasNoMilesAndNoDivideByZero() {
        val cancelled = LoadCancellation.cancel(sample().copy(deadheadMiles = 0.0), Money.fromCents(17_500L), now)
        assertEquals(0.0, cancelled.drivenMiles, 0.0)
        val card = cancelled.toCardModel(minProfitRpm = 2.0, targetProfitRpm = 3.0)
        assertNull(card.rpm)
    }

    private fun sample() = Load(
        id = "1",
        tripId = "T-1",
        date = "2026-11-20",
        totalRate = Money.fromCents(441_893L),
        totalMiles = 900.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 47,
        year = 2026,
        rawMessage = "",
        parsedAt = 0L,
        updatedAt = 0L,
        deadheadMiles = 55.0,
        stops = listOf(
            Stop(
                id = 1,
                loadId = "1",
                stopNumber = 1,
                type = StopType.PU,
                puNumber = null,
                note = null,
                scheduledTime = "11/20 08:00 EST",
                timezone = "EST",
                facilityCode = null,
                fullAddress = "",
                city = "",
                state = "",
                zip = "",
            ),
            Stop(
                id = 2,
                loadId = "1",
                stopNumber = 2,
                type = StopType.DEL,
                puNumber = null,
                note = null,
                scheduledTime = "11/23 08:00 EST",
                timezone = "EST",
                facilityCode = null,
                fullAddress = "",
                city = "",
                state = "",
                zip = "",
            ),
        ),
    )
}
