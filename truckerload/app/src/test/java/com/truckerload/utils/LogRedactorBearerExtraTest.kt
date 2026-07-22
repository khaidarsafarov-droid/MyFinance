package com.truckerload.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorBearerExtraTest {
    @Test
    fun redactsAnonKeyHint() {
        val raw = "supabase_anon_key=eyJhbGciOiJIUzI1NiJ9.abc.def"
        val out = LogRedactor.redact(raw)
        assertTrue(out.contains("***"))
        assertFalse(out.contains("eyJhbGciOiJIUzI1NiJ9.abc.def"))
    }
}
