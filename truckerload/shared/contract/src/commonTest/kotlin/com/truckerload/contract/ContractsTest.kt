package com.truckerload.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContractsTest {
    @Test
    fun `push platforms used by the iOS facade stay stable`() {
        assertEquals("android", PushPlatforms.ANDROID)
        assertEquals("ios", PushPlatforms.IOS)
        assertTrue(PushPlatforms.isSupported(PushPlatforms.IOS))
        assertFalse(PushPlatforms.isSupported("web"))
    }
}
