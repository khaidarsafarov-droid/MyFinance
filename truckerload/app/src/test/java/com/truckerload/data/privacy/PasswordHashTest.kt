package com.truckerload.data.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHashTest {

    @Test
    fun `hash verifies matching password`() {
        val (hash, salt) = BackupCrypto.hashPassword("Secret123!")
        assertTrue(BackupCrypto.verifyPassword("Secret123!", hash, salt))
        assertFalse(BackupCrypto.verifyPassword("wrong", hash, salt))
    }

    @Test
    fun `same password different salts produce different hashes`() {
        val (h1, s1) = BackupCrypto.hashPassword("same")
        val (h2, s2) = BackupCrypto.hashPassword("same")
        assertTrue(s1 != s2)
        assertTrue(h1 != h2)
        assertTrue(BackupCrypto.verifyPassword("same", h1, s1))
        assertTrue(BackupCrypto.verifyPassword("same", h2, s2))
    }
}
