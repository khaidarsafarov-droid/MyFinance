package com.truckerload.data.sync

import android.content.Context
import com.truckerload.contract.DeviceSlotPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceIdentityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("truckerload_device_identity", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("truckerload_device_slot_denial", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun deviceIdIsStableAcrossReads() {
        val first = DeviceIdentity(context).id()
        val second = DeviceIdentity(context).id()
        assertEquals(first, second)
        assertNotEquals("", first)
    }

    @Test
    fun tabletFormFactorIsPinnedEvenIfWidthLaterLooksLikeAPhone() {
        val config = context.resources.configuration
        config.smallestScreenWidthDp = 800
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        assertEquals(DeviceSlotPolicy.TABLET, DeviceIdentity(context).formFactor())

        config.smallestScreenWidthDp = 360
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        assertEquals(DeviceSlotPolicy.TABLET, DeviceIdentity(context).formFactor())
    }

    @Test
    fun phoneFormFactorUsesSmallestWidthBelowTabletBreakpoint() {
        val config = context.resources.configuration
        config.smallestScreenWidthDp = 411
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        assertEquals(DeviceSlotPolicy.PHONE, DeviceIdentity(context).formFactor())
    }

    @Test
    fun denialStoreIsConsumedOnce() {
        val store = DeviceSlotDenialStore(context)
        assertNull(store.consume())
        store.save("taken")
        assertEquals("taken", store.consume())
        assertNull(store.consume())
    }
}
