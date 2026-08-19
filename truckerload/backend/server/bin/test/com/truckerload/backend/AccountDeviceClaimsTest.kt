package com.truckerload.backend

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class AccountDeviceClaimsTest {
    private val user = UUID.fromString("11111111-1111-4111-8111-111111111111")

    @Test
    fun `empty slot is claimed`() {
        val result = AccountDeviceClaims.decide(
            self = null,
            occupant = null,
            userId = user,
            deviceId = "phone-a",
            formFactor = "phone",
            now = 10L,
        )
        val claimed = assertIs<DeviceClaimResult.Claimed>(result)
        assertEquals("phone-a", claimed.record.deviceId)
        assertEquals(10L, claimed.record.registeredAt)
        assertEquals(10L, claimed.record.lastSeenAt)
    }

    @Test
    fun `same device refreshes last seen`() {
        val existing = AccountDeviceRecord(user, "phone-a", "phone", 1L, 1L)
        val result = AccountDeviceClaims.decide(existing, existing, user, "phone-a", "phone", 20L)
        val claimed = assertIs<DeviceClaimResult.Claimed>(result)
        assertEquals(1L, claimed.record.registeredAt)
        assertEquals(20L, claimed.record.lastSeenAt)
    }

    @Test
    fun `second phone is rejected while first remains occupant`() {
        val occupant = AccountDeviceRecord(user, "phone-a", "phone", 1L, 1L)
        val result = AccountDeviceClaims.decide(null, occupant, user, "phone-b", "phone", 20L)
        val denied = assertIs<DeviceClaimResult.SlotTaken>(result)
        assertEquals("phone", denied.formFactor)
        assertEquals("phone-a", denied.occupantDeviceId)
    }

    @Test
    fun `tablet can join an account that already has a phone`() {
        val phone = AccountDeviceRecord(user, "phone-a", "phone", 1L, 1L)
        val result =
            AccountDeviceClaims.decide(null, occupant = null, user, "tablet-a", "tablet", 20L)
        val claimed = assertIs<DeviceClaimResult.Claimed>(result)
        assertEquals("tablet", claimed.record.formFactor)
        assertNotEquals(phone.deviceId, claimed.record.deviceId)
    }
}
