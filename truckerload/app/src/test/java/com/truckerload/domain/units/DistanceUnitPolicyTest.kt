package com.truckerload.domain.units

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Auto-kilometers is only the starting value. An explicit choice always wins,
 * including when the language stays French.
 */
class DistanceUnitPolicyTest {

    @Test
    fun frenchWithNoChoice_startsOnKilometers() {
        assertEquals(DistanceUnit.KILOMETERS, DistanceUnitPolicy.resolve(null, "fr"))
        assertEquals(DistanceUnit.KILOMETERS, DistanceUnitPolicy.resolve(null, "fr-CA"))
        assertEquals(DistanceUnit.KILOMETERS, DistanceUnitPolicy.resolve(null, "FR"))
    }

    @Test
    fun otherLanguagesWithNoChoice_stayOnMiles() {
        assertEquals(DistanceUnit.MILES, DistanceUnitPolicy.resolve(null, "en"))
        assertEquals(DistanceUnit.MILES, DistanceUnitPolicy.resolve(null, "ru"))
        assertEquals(DistanceUnit.MILES, DistanceUnitPolicy.resolve(null, "es"))
        assertEquals(DistanceUnit.MILES, DistanceUnitPolicy.resolve(null, null))
    }

    @Test
    fun explicitMiles_winOverFrench() {
        assertEquals(DistanceUnit.MILES, DistanceUnitPolicy.resolve(DistanceUnit.MILES, "fr"))
        assertEquals(DistanceUnit.MILES, DistanceUnitPolicy.resolve(DistanceUnit.MILES, "fr-CA"))
    }

    @Test
    fun explicitKilometers_winOverEnglish() {
        assertEquals(DistanceUnit.KILOMETERS, DistanceUnitPolicy.resolve(DistanceUnit.KILOMETERS, "en"))
        assertEquals(DistanceUnit.KILOMETERS, DistanceUnitPolicy.resolve(DistanceUnit.KILOMETERS, "ru"))
    }
}
