package com.truckerload.data.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceMeshSessionTest {

    @Test
    fun pairId_isStableRegardlessOfArgumentOrder() {
        val room = "room-1"
        val a = "user-aaa"
        val b = "user-bbb"
        assertEquals(VoiceMeshSession.pairId(room, a, b), VoiceMeshSession.pairId(room, b, a))
        assertEquals("room-1__user-aaa__user-bbb", VoiceMeshSession.pairId(room, b, a))
    }

    @Test
    fun isOfferer_usesLexicographicUserId() {
        assertTrue(VoiceMeshSession.isOfferer("aaa", "bbb"))
        assertFalse(VoiceMeshSession.isOfferer("bbb", "aaa"))
    }
}
