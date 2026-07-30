package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemorySharedPreferencesTest {

    @Test
    fun `put get remove and clear work in memory`() {
        val prefs = InMemorySharedPreferences()
        prefs.edit().putString("token", "secret").putBoolean("flag", true).apply()
        assertEquals("secret", prefs.getString("token", null))
        assertTrue(prefs.getBoolean("flag", false))

        prefs.edit().remove("token").apply()
        assertFalse(prefs.contains("token"))

        prefs.edit().clear().apply()
        assertTrue(prefs.all.isEmpty())
    }
}
