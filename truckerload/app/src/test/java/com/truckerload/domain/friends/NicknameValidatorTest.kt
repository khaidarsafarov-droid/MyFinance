package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknameValidatorTest {
    @Test
    fun acceptsValidHandles() {
        assertTrue(NicknameValidator.isValid("Ivan"))
        assertTrue(NicknameValidator.isValid("truck_42"))
        assertTrue(NicknameValidator.isValid("@DriverOne"))
        assertEquals("DriverOne", NicknameValidator.sanitizeOrNull("@DriverOne"))
    }

    @Test
    fun rejectsInvalid() {
        assertFalse(NicknameValidator.isValid("ab"))
        assertFalse(NicknameValidator.isValid("1start"))
        assertFalse(NicknameValidator.isValid("bad-name"))
        assertFalse(NicknameValidator.isValid(""))
        assertNull(NicknameValidator.sanitizeOrNull("x"))
    }
}
