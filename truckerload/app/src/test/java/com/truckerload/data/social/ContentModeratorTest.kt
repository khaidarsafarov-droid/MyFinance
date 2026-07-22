package com.truckerload.data.social

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentModeratorTest {

    @Test
    fun blocksSpamAndLinks() {
        assertFalse(ContentModerator.moderateText("buy this SPAM now").allowed)
        assertFalse(ContentModerator.moderateText("visit https://evil.example").allowed)
        assertFalse(ContentModerator.moderateText("http://bad").allowed)
    }

    @Test
    fun allowsNormalText() {
        assertTrue(ContentModerator.moderateText("Heading to Dallas, ETA 18:00").allowed)
    }

    @Test
    fun rejectsBlankAndTooLong() {
        assertFalse(ContentModerator.moderateText("   ").allowed)
        assertFalse(ContentModerator.moderateText("x".repeat(4001)).allowed)
    }

    @Test
    fun extractsHashtagsAndMentions() {
        val tags = ContentModerator.extractHashtags("hello #RoadLife #Дальнобой")
        assertTrue(tags.contains("RoadLife"))
        assertTrue(tags.contains("Дальнобой"))
        val mentions = ContentModerator.extractMentions("hey @ivan and @Сергей")
        assertTrue(mentions.contains("ivan"))
        assertTrue(mentions.contains("Сергей"))
    }
}
