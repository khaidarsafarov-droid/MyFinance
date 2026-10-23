package com.truckerload.data.preferences

import com.truckerload.domain.units.DistanceUnit
import com.truckerload.domain.units.DistanceUnitPolicy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Miles/Kilometers switch is always manual. French only supplies the starting
 * value while no choice is saved. A saved choice survives restart and a later
 * language change, including choosing French again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DistanceUnitPreferenceTest {

    private lateinit var store: SettingsDataStore

    @Before
    fun setUp() {
        store = SettingsDataStore(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        runBlocking {
            store.clearExplicitDistanceUnit()
            store.clearExplicitLanguage()
        }
    }

    @Test
    fun frenchWithNoChoice_isKilometers() = runBlocking {
        store.clearExplicitDistanceUnit()
        assertNull(store.getExplicitDistanceUnitOnce())
        assertEquals(
            DistanceUnit.KILOMETERS,
            DistanceUnitPolicy.resolve(store.getExplicitDistanceUnitOnce(), "fr"),
        )
    }

    @Test
    fun explicitMiles_stayMilesAfterRestartWhileLanguageStaysFrench() = runBlocking {
        store.saveLanguage(AppLanguage.FR)
        store.saveDistanceUnit(DistanceUnit.MILES)

        val reloaded = SettingsDataStore(RuntimeEnvironment.getApplication())
        assertEquals(AppLanguage.FR, reloaded.getExplicitLanguageOnce())
        assertEquals(DistanceUnit.MILES, reloaded.getExplicitDistanceUnitOnce())
        assertEquals(
            DistanceUnit.MILES,
            DistanceUnitPolicy.resolve(reloaded.getExplicitDistanceUnitOnce(), "fr"),
        )
    }

    @Test
    fun explicitKilometers_stayKilometersForEnglish() = runBlocking {
        store.saveLanguage(AppLanguage.EN)
        store.saveDistanceUnit(DistanceUnit.KILOMETERS)

        val reloaded = SettingsDataStore(RuntimeEnvironment.getApplication())
        assertEquals(AppLanguage.EN, reloaded.getExplicitLanguageOnce())
        assertEquals(DistanceUnit.KILOMETERS, reloaded.getExplicitDistanceUnitOnce())
        assertEquals(
            DistanceUnit.KILOMETERS,
            DistanceUnitPolicy.resolve(reloaded.getExplicitDistanceUnitOnce(), "en"),
        )
    }

    @Test
    fun saveLanguage_doesNotClearOrOverrideAnExplicitChoice() = runBlocking {
        store.clearExplicitDistanceUnit()
        store.saveDistanceUnit(DistanceUnit.MILES)
        store.saveLanguage(AppLanguage.FR)
        store.saveLanguage(AppLanguage.FR)
        assertEquals(DistanceUnit.MILES, store.getExplicitDistanceUnitOnce())
        assertEquals(
            DistanceUnit.MILES,
            DistanceUnitPolicy.resolve(store.getExplicitDistanceUnitOnce(), "fr"),
        )
    }
}
