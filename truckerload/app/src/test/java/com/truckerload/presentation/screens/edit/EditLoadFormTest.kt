package com.truckerload.presentation.screens.edit

import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.MoneyInput
import com.truckerload.presentation.components.formatDisputeAmountInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditLoadFormTest {

    @Test
    fun moneyText_formatsCentsAsDecimal() {
        assertEquals("2111.72", EditLoadForm.moneyText(Money.fromCents(211_172)))
        assertEquals("0.05", EditLoadForm.moneyText(Money.fromCents(5)))
        assertEquals("2111.72", EditLoadForm.rateField("Money(cents=211172)", Money.fromCents(211_172)))
        assertEquals(211_172L, MoneyInput.parse("2111.72")!!.cents)
        assertEquals(5L, MoneyInput.parse("0.05")!!.cents)
        assertEquals(211_172L, MoneyInput.parse("2,111.72")!!.cents)
        assertEquals("0.05", formatDisputeAmountInput(Money.fromCents(5)))
        assertEquals("2111.72", formatDisputeAmountInput(Money.fromCents(211_172)))
    }

    @Test
    fun roundTrip_untouchedFormEqualsOriginal() {
        val original = storedLoad()
        val form = EditLoadForm.fromLoad(original)

        assertEquals("2111.72", form.totalRate)
        assertEquals("720.125", form.totalMiles)
        assertEquals("2026-08-21 07:05", form.finishDate)
        assertEquals(original, EditLoadForm.apply(original, form, nowMillis = 9_000L))
    }

    @Test
    fun roundTrip_metricMilesAndStopDerivedFinishStayStored() {
        val original = storedLoad().copy(
            totalMiles = 1.0,
            actualFinishDate = null,
            pointA = "ATL Hub",
        )
        val form = EditLoadForm.fromLoad(original, metric = true)

        assertEquals("1.6093", form.totalMiles)
        assertEquals(original, EditLoadForm.apply(original, form, metric = true, nowMillis = 9_000L))
    }

    @Test
    fun wholeMiles_prefillWithoutTrailingZero() {
        val original = storedLoad().copy(totalMiles = 720.0)
        val form = EditLoadForm.fromLoad(original)

        assertEquals("720", form.totalMiles)
        assertEquals(original, EditLoadForm.apply(original, form, nowMillis = 1L))
    }

    @Test
    fun zeroRate_untouchedSaveStaysZero() {
        val original = storedLoad().copy(totalRate = Money.ZERO)
        val form = EditLoadForm.fromLoad(original)

        assertEquals("0.00", form.totalRate)
        assertEquals(original, EditLoadForm.apply(original, form, nowMillis = 1L))
    }

    @Test
    fun debugMoneyString_isRejected() {
        val original = storedLoad()
        val form = EditLoadForm.fromLoad(original).copy(totalRate = "Money(cents=211172)")

        assertNull(EditLoadForm.apply(original, form, nowMillis = 1L))
    }

    @Test
    fun disputeToggle_changesOnlyDisputeAndTimestamp() {
        val original = storedLoad().copy(
            isDispute = false,
            disputeCompleted = false,
            disputeApplyToLoad = false,
            disputeAmountApplied = false,
        )
        val form = EditLoadForm.fromLoad(original).copy(
            disputeLoad = original.copy(isDispute = true),
        )
        val saved = EditLoadForm.apply(original, form, nowMillis = 50L)!!

        assertEquals(original.copy(isDispute = true, updatedAt = 50L), saved)
    }

    @Test
    fun editedRate_changesRateAndPaceOnly() {
        val original = storedLoad()
        val form = EditLoadForm.fromLoad(original).copy(totalRate = "2200.50")
        val saved = EditLoadForm.apply(original, form, nowMillis = 77L)!!
        val rate = Money.parse("2200.50")

        assertEquals(
            original.copy(totalRate = rate, pace = rate.dividedBy(10), updatedAt = 77L),
            saved,
        )
    }

    private fun storedLoad(): Load = Load(
        id = "load-1",
        tripId = "t-116KYL6KW",
        date = "2026-08-20",
        totalRate = Money.fromCents(211_172),
        totalMiles = 720.125,
        pointA = "ATL Hub",
        pointB = "CARLISLE, PA",
        puCount = 3,
        delCount = 4,
        weekNumber = 7,
        year = 1999,
        rawMessage = "driver note: lumper paid cash",
        parsedAt = 99L,
        updatedAt = 100L,
        route = "kept route",
        firstPuCityState = "kept pu",
        lastDelCityState = "kept del",
        durationDays = 9.5,
        pace = Money.fromCents(1),
        stopCount = 9,
        isDispute = true,
        disputeResponseDate = "2026-08-25",
        disputeCompleted = true,
        disputeAmount = Money.fromCents(5),
        disputeApplyToLoad = true,
        disputeAmountApplied = true,
        actualFinishDate = "2026-08-21 07:05",
        stops = listOf(
            stop(1, StopType.PU, "2026-08-19 08:00", "Atlanta", "GA", "dock detention"),
            stop(2, StopType.DEL, "2026-08-21 18:00", "Carlisle", "PA", null),
        ),
        penalties = listOf(
            Penalty(
                id = 4,
                loadId = "load-1",
                description = "detention",
                amount = Money.fromCents(1_550),
            ),
        ),
        equipmentType = EquipmentType.REEFER,
        deadheadMiles = 12.25,
    )

    private fun stop(
        number: Int,
        type: StopType,
        whenAt: String,
        city: String,
        state: String,
        note: String?,
    ) = Stop(
        id = number,
        loadId = "load-1",
        stopNumber = number,
        type = type,
        puNumber = null,
        note = note,
        scheduledTime = whenAt,
        timezone = "America/New_York",
        facilityCode = null,
        fullAddress = "$city, $state",
        city = city,
        state = state,
        zip = "",
    )
}
