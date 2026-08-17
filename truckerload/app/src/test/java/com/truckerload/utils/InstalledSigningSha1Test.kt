package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstalledSigningSha1Test {

    @Test
    fun format_isColonSeparatedUpperHex() {
        val sha = InstalledSigningSha1.format(
            byteArrayOf(0x6F.toByte(), 0xAD.toByte(), 0x00, 0x27, 0xE3.toByte()),
        )
        assertEquals("6F:AD:00:27:E3", sha)
    }

    @Test
    fun fingerprint_doesNotThrow() {
        val sha = InstalledSigningSha1.fingerprint(RuntimeEnvironment.getApplication())
        if (sha != null) {
            assertTrue(sha.matches(Regex("([0-9A-F]{2}:){19}[0-9A-F]{2}")))
        }
    }
}
