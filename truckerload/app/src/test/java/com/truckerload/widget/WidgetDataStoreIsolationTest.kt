package com.truckerload.widget

import com.truckerload.data.preferences.AuthStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetDataStoreIsolationTest {

    @Test
    fun statsDoNotLeakAcrossAccounts() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)

        auth.login(userId = "widget_user_a", email = "a@example.com", rememberMe = true)
        WidgetDataStore.save(
            context,
            WidgetStats(
                loadsCount = 3,
                avgCpm = 2.0,
                totalMiles = 100.0,
                totalLoadRate = 200.0,
                netProfit = 150.0,
                weekLabel = "A",
                statsLine = "a",
                updatedAtMillis = 10L,
            ),
        )

        auth.login(userId = "widget_user_b", email = "b@example.com", rememberMe = true)
        WidgetDataStore.save(
            context,
            WidgetStats(
                loadsCount = 9,
                avgCpm = 3.0,
                totalMiles = 900.0,
                totalLoadRate = 900.0,
                netProfit = 700.0,
                weekLabel = "B",
                statsLine = "b",
                updatedAtMillis = 20L,
            ),
        )

        auth.login(userId = "widget_user_a", email = "a@example.com", rememberMe = true)
        assertEquals(3, WidgetDataStore.load(context).loadsCount)
        assertEquals("A", WidgetDataStore.load(context).weekLabel)

        auth.login(userId = "widget_user_b", email = "b@example.com", rememberMe = true)
        assertEquals(9, WidgetDataStore.load(context).loadsCount)
        assertEquals("B", WidgetDataStore.load(context).weekLabel)
    }
}
