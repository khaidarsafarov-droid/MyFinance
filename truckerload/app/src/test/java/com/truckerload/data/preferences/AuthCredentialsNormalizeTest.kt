package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthCredentialsNormalizeTest {

    @Test
    fun normalizeEmail_trimsAndLowercases() {
        assertEquals("a@test.com", AuthCredentialsStore.normalizeEmail("  A@Test.COM "))
    }
}
