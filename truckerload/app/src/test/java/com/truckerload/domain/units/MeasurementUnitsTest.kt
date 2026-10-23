package com.truckerload.domain.units

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementUnitsTest {

    @Test
    fun distance_roundTripsThroughKilometers() {
        val km = MeasurementUnits.displayDistance(850.0, metric = true)
        assertEquals(850.0 * MeasurementUnits.MILES_TO_KM, km, 1e-6)
        assertEquals(850.0, MeasurementUnits.storedDistance(km, metric = true), 1e-6)
    }

    @Test
    fun milesMode_isIdentity() {
        assertEquals(1250.4, MeasurementUnits.displayDistance(1250.4, metric = false), 0.0)
        assertEquals(12.5, MeasurementUnits.displayVolume(12.5, metric = false), 0.0)
        assertEquals(2.5, MeasurementUnits.displayRatePerDistance(2.5, metric = false), 0.0)
        assertEquals(4.199, MeasurementUnits.displayPricePerVolume(4.199, metric = false), 0.0)
    }

    @Test
    fun zero_staysZero() {
        assertEquals(0.0, MeasurementUnits.displayDistance(0.0, metric = true), 0.0)
        assertEquals(0.0, MeasurementUnits.storedVolume(0.0, metric = true), 0.0)
        assertEquals(0.0, MeasurementUnits.displayRatePerDistance(0.0, metric = true), 0.0)
        assertEquals(0.0, MeasurementUnits.displayCostPer100(0.0, metric = true), 0.0)
    }

    @Test
    fun nonFinite_passesThrough() {
        assertTrue(MeasurementUnits.displayDistance(Double.NaN, metric = true).isNaN())
        assertTrue(MeasurementUnits.storedDistance(Double.POSITIVE_INFINITY, metric = true).isInfinite())
    }

    @Test
    fun blankAndGarbage_parseAsNull() {
        assertNull(MeasurementUnits.parseDisplayOrNull(null))
        assertNull(MeasurementUnits.parseDisplayOrNull(""))
        assertNull(MeasurementUnits.parseDisplayOrNull("   "))
        assertNull(MeasurementUnits.parseDisplayOrNull("abc"))
        assertEquals(1370.5, MeasurementUnits.parseDisplayOrNull("1,370.5") ?: Double.NaN, 0.0)
    }

    @Test
    fun largeDistance_roundTrips() {
        val miles = 1_000_000.0
        val back = MeasurementUnits.storedDistance(
            MeasurementUnits.displayDistance(miles, metric = true),
            metric = true,
        )
        assertEquals(miles, back, 1e-4)
    }

    @Test
    fun ratePerDistance_usesExactMileFactor() {
        val perKm = MeasurementUnits.displayRatePerDistance(MeasurementUnits.MILES_TO_KM, metric = true)
        assertEquals(1.0, perKm, 1e-9)
        assertEquals(
            MeasurementUnits.MILES_TO_KM,
            MeasurementUnits.storedRatePerDistance(perKm, metric = true),
            1e-9,
        )
    }

    @Test
    fun pricePerVolume_roundTrips() {
        val perLiter = MeasurementUnits.displayPricePerVolume(3.785411784, metric = true)
        assertEquals(1.0, perLiter, 1e-9)
        assertEquals(3.785411784, MeasurementUnits.storedPricePerVolume(perLiter, metric = true), 1e-9)
    }

    @Test
    fun fuelEconomy_mpgConvertsToLitersPer100KmAndBack() {
        val mpg = 10.0
        val expected = (MeasurementUnits.GALLONS_TO_LITERS * 100.0) / (mpg * MeasurementUnits.MILES_TO_KM)
        val shown = MeasurementUnits.displayFuelEconomy(mpg, metric = true)
        assertEquals(expected, shown!!, 1e-9)
        assertEquals(mpg, MeasurementUnits.storedFuelEconomy(shown, metric = true)!!, 1e-6)
        assertEquals(mpg, MeasurementUnits.displayFuelEconomy(mpg, metric = false)!!, 0.0)
    }

    @Test
    fun fuelEconomy_nonPositiveIsNull() {
        assertNull(MeasurementUnits.displayFuelEconomy(0.0, metric = true))
        assertNull(MeasurementUnits.displayFuelEconomy(-2.0, metric = true))
        assertNull(MeasurementUnits.storedFuelEconomy(Double.NaN, metric = true))
    }

    @Test
    fun costPer100_dividesByMileFactor() {
        val per100Km = MeasurementUnits.displayCostPer100(MeasurementUnits.MILES_TO_KM, metric = true)
        assertEquals(1.0, per100Km, 1e-9)
    }
}
