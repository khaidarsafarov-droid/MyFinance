package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendMapLabelsTest {

    @Test
    fun friendVisibleName_prefersLinkDisplayName() {
        assertEquals(
            "Alex",
            FriendMapLabels.friendVisibleName(
                presenceDisplayName = "Driver",
                linkDisplayName = "Alex",
                nickname = "Keystone",
            ),
        )
    }

    @Test
    fun friendVisibleName_usesPresenceWhenNotPlaceholder() {
        assertEquals(
            "Maria",
            FriendMapLabels.friendVisibleName(
                presenceDisplayName = "Maria",
                linkDisplayName = "  ",
                nickname = "Keystone",
            ),
        )
    }

    @Test
    fun friendVisibleName_fallsBackToNickname() {
        assertEquals(
            "Keystone",
            FriendMapLabels.friendVisibleName(
                presenceDisplayName = "Driver",
                linkDisplayName = null,
                nickname = "@Keystone",
            ),
        )
    }

    @Test
    fun initials_twoWordsAndHandle() {
        assertEquals("AK", FriendMapLabels.initials("Alex Keystone"))
        assertEquals("KE", FriendMapLabels.initials("@Keystone"))
        assertEquals("?", FriendMapLabels.initials("   "))
    }

    @Test
    fun ellipsize_shortAndLong() {
        assertEquals("Alex", FriendMapLabels.ellipsize("Alex", 18))
        assertEquals("abcdefghijklmnopq…", FriendMapLabels.ellipsize("abcdefghijklmnopqrstuvwxyz", 18))
    }
}
