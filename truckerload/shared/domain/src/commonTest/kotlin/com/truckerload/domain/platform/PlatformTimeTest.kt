package com.truckerload.domain.platform

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTimeTest {
    @Test
    fun `epochMillis is a positive wall clock`() {
        val now = PlatformTime.epochMillis()
        assertTrue(now > 1_700_000_000_000L, "epochMillis=$now")
    }
}
