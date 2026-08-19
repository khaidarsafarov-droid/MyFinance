package com.truckerload.presentation.screens.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualLoadFieldsTest {

    @Test
    fun startsWithAAndBAndOffersC() {
        val fields = ManualLoadFields()
        assertEquals(listOf("", ""), fields.allPoints())
        assertEquals("C", fields.nextPointLetter())
        assertTrue(fields.canAddPoint())
    }

    @Test
    fun addingCUnlocksDThenE() {
        val withC = ManualLoadFields().addPoint()
        assertEquals(listOf("", "", ""), withC.allPoints())
        assertEquals("D", withC.nextPointLetter())

        val withD = withC.addPoint()
        assertEquals("E", withD.nextPointLetter())
        assertEquals("C", ManualLoadFields.pointLetter(2))
        assertEquals("D", ManualLoadFields.pointLetter(3))
    }

    @Test
    fun filledPointsSkipBlanksAndEnableSave() {
        val fields = ManualLoadFields(
            rate = "1500",
            pointA = "Garner, NC",
            extraPoints = listOf("", "Dallas, TX"),
        )
        assertEquals(listOf("Garner, NC", "Dallas, TX"), fields.filledPoints())
        assertTrue(fields.canSave())
        assertFalse(ManualLoadFields(rate = "1500").canSave())
    }

    @Test
    fun removeExtraPointReindexesLetters() {
        val fields = ManualLoadFields(extraPoints = listOf("Atlanta, GA", "Dallas, TX"))
            .removePoint(2)
        assertEquals(listOf("", "", "Dallas, TX"), fields.allPoints())
        assertEquals("D", fields.nextPointLetter())
    }

    @Test
    fun cannotAddPastZ() {
        val full = ManualLoadFields(
            extraPoints = List(ManualLoadFields.MAX_ROUTE_POINTS - 2) { "" },
        )
        assertEquals(26, full.allPoints().size)
        assertNull(full.nextPointLetter())
        assertEquals(full, full.addPoint())
    }
}
