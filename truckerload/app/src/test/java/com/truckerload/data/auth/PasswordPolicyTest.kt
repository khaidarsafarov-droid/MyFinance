package com.truckerload.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test
    fun validate_rejectsShortAndWeakPasswords() {
        assertFalse(PasswordPolicy.validate("Ab1").ok)
        assertFalse(PasswordPolicy.validate("abcdefgh").ok)
        assertFalse(PasswordPolicy.validate("abcdefg1").ok)
        assertFalse(PasswordPolicy.validate("ABCDEFGH").ok)
        assertTrue(PasswordPolicy.validate("Abcdefg1").ok)
    }

    @Test
    fun hash_and_matches_roundTrip() {
        val password = "TruckLoad1"
        val hashed = PasswordPolicy.hash(password)
        assertTrue(PasswordPolicy.isHashed(hashed))
        assertTrue(PasswordPolicy.matches(password, hashed))
        assertFalse(PasswordPolicy.matches("WrongPass1", hashed))
    }

    @Test
    fun matches_acceptsLegacyPlaintext() {
        assertTrue(PasswordPolicy.matches("legacy", "legacy"))
        assertFalse(PasswordPolicy.matches("legacy", "other"))
    }
}
