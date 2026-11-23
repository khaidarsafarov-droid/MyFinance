package com.truckerload.utils

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.truckerload.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatteryOptimizationHelperTest {

    @Test
    fun isIgnoring_readsPowerManager() {
        val app = app()
        val power = powerManager(app)
        shadowOf(power).setIgnoringBatteryOptimizations(app.packageName, false)
        assertFalse(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(app))
        shadowOf(power).setIgnoringBatteryOptimizations(app.packageName, true)
        assertTrue(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(app))
    }

    @Test
    fun isIgnoring_withoutPowerManager_reportsAlreadyIgnoring() {
        val context = NoPowerContext(app())
        assertTrue(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    @Test
    fun request_whenAlreadyIgnoring_doesNotOpenSettings() {
        val app = app()
        shadowOf(powerManager(app)).setIgnoringBatteryOptimizations(app.packageName, true)
        val context = RecordingContext(app)
        assertFalse(BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context))
        assertTrue(context.started.isEmpty())
    }

    @Test
    fun request_opensTheSystemBatteryList() {
        val app = app()
        shadowOf(powerManager(app)).setIgnoringBatteryOptimizations(app.packageName, false)
        val context = RecordingContext(app)
        assertTrue(BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context))
        val intent = context.started.single()
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.action)
        assertNull(intent.data)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertFalse(context.started.any { it.action == Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS })
    }

    @Test
    fun request_fallsBackToAppDetails_whenTheListDoesNotResolve() {
        val app = app()
        shadowOf(powerManager(app)).setIgnoringBatteryOptimizations(app.packageName, false)
        val context = RecordingContext(app) { intent ->
            if (intent.action == Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) {
                ActivityNotFoundException(intent.action)
            } else {
                null
            }
        }
        assertTrue(BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context))
        val intent = context.started.single()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${app.packageName}", intent.data?.toString())
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun request_fallsBackToGeneralSettings_whenAppDetailsDoesNotResolve() {
        val app = app()
        shadowOf(powerManager(app)).setIgnoringBatteryOptimizations(app.packageName, false)
        val context = RecordingContext(app) { intent ->
            when (intent.action) {
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                -> SecurityException(intent.action)
                else -> null
            }
        }
        assertTrue(BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context))
        val intent = context.started.single()
        assertEquals(Settings.ACTION_SETTINGS, intent.action)
        assertNull(intent.data)
    }

    @Test
    fun request_returnsFalse_whenNoScreenResolves() {
        val app = app()
        shadowOf(powerManager(app)).setIgnoringBatteryOptimizations(app.packageName, false)
        val context = RecordingContext(app) { ActivityNotFoundException(it.action) }
        assertFalse(BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context))
        assertTrue(context.started.isEmpty())
    }

    @Test
    fun defaultCopy_tellsTheUserWhichRowToChoose() {
        assertCopy(dontOptimize = "Don't optimize", unrestricted = "Unrestricted")
    }

    @Test
    @Config(qualifiers = "en")
    fun englishCopy_tellsTheUserWhichRowToChoose() {
        assertCopy(dontOptimize = "Don't optimize", unrestricted = "Unrestricted")
    }

    @Test
    @Config(qualifiers = "es")
    fun spanishCopy_tellsTheUserWhichRowToChoose() {
        assertCopy(dontOptimize = "No optimizar", unrestricted = "Sin restricciones")
    }

    @Test
    @Config(qualifiers = "fr")
    fun frenchCopy_tellsTheUserWhichRowToChoose() {
        assertCopy(dontOptimize = "Ne pas optimiser", unrestricted = "Sans restriction")
    }

    @Test
    @Config(qualifiers = "ru")
    fun russianCopy_tellsTheUserWhichRowToChoose() {
        assertCopy(dontOptimize = "Не оптимизировать", unrestricted = "Без ограничений")
    }

    private fun assertCopy(dontOptimize: String, unrestricted: String) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        listOf(
            context.getString(R.string.settings_battery_desc),
            context.getString(R.string.maintenance_battery_body),
        ).forEach { text ->
            assertTrue(text.contains("TruckoRig"))
            assertTrue(text.contains(dontOptimize))
            assertTrue(text.contains(unrestricted))
        }
    }

    private fun app(): Application = ApplicationProvider.getApplicationContext()

    private fun powerManager(context: Context): PowerManager =
        context.getSystemService(PowerManager::class.java)!!

    private class NoPowerContext(base: Context) : ContextWrapper(base) {
        override fun getSystemService(name: String): Any? {
            if (name == POWER_SERVICE) return null
            return super.getSystemService(name)
        }
    }

    private class RecordingContext(
        base: Context,
        private val failure: (Intent) -> Exception? = { null },
    ) : ContextWrapper(base) {
        val started = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            val error = failure(intent)
            if (error != null) throw error
            started += Intent(intent)
        }
    }
}
