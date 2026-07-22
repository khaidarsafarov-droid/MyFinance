package com.truckerload.domain.social

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupInviteCodeTest {

    @Test
    fun blankAndWhitespaceAreBlank() {
        assertTrue(GroupInviteCode.isBlank(""))
        assertTrue(GroupInviteCode.isBlank("   "))
        assertFalse(GroupInviteCode.isBlank("I95ROAD"))
    }

    @Test
    fun wellFormedRequiresLengthAndAlphanumeric() {
        assertFalse(GroupInviteCode.isWellFormed(""))
        assertFalse(GroupInviteCode.isWellFormed("ab"))
        assertFalse(GroupInviteCode.isWellFormed("bad code!"))
        assertTrue(GroupInviteCode.isWellFormed("I95ROAD"))
        assertTrue(GroupInviteCode.isWellFormed("FUELNOW"))
    }

    @Test
    fun normalizeTrims() {
        assertTrue(GroupInviteCode.normalize("  ABC  ") == "ABC")
    }
}
