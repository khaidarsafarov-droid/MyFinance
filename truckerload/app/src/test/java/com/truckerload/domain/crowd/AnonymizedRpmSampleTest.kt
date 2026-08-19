package com.truckerload.domain.crowd

import com.truckerload.domain.model.EquipmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymizedRpmSampleTest {

    @Test
    fun fromStorage_parsesEnumAndRejectsJunk() {
        assertEquals(EquipmentType.AMAZON_RELAY, EquipmentType.fromStorage("AMAZON_RELAY"))
        assertEquals(EquipmentType.DRY_VAN, EquipmentType.fromStorage("dry_van"))
        assertNull(EquipmentType.fromStorage(null))
        assertNull(EquipmentType.fromStorage(" "))
        assertNull(EquipmentType.fromStorage("semi"))
    }

    @Test
    fun payload_onlyHasAgreedCrowdFields() {
        val sample = AnonymizedRpmSample(
            rpm = 2.4,
            miles = 800.0,
            fromState = "NC",
            toState = "GA",
            week = 12,
            year = 2026,
            equipmentType = EquipmentType.AMAZON_RELAY,
        )
        val names = sample::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(names.contains("rpm"))
        assertTrue(names.contains("miles"))
        assertTrue(names.contains("fromState"))
        assertTrue(names.contains("equipmentType"))
        assertTrue(names.contains("week"))
        assertTrue(!names.contains("tripId"))
        assertTrue(!names.contains("rawMessage"))
        assertTrue(!names.contains("pointA"))
        assertTrue(!names.contains("id"))
    }
}
