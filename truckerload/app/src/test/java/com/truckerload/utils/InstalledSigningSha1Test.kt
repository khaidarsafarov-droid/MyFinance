package com.truckerload.utils

import org.junit.Assert.assertNotNull
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
    fun fingerprint_isColonSeparatedHex() {
        val sha = InstalledSigningSha1.fingerprint(RuntimeEnvironment.getApplication())
        assertNotNull(sha)
        assertTrue(sha!!.matches(Regex("([0-9A-F]{2}:){19}[0-9A-F]{2}")))
    }
}
