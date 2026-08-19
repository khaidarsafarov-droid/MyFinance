package com.truckerload.presentation.screens.social

import com.truckerload.domain.social.SocialMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialChatUiStateTest {

    @Test
    fun allMessages_sortOldestFirstThenById() {
        val later = message(id = "b", sentAt = 200L)
        val earlier = message(id = "a", sentAt = 100L)
        val sameTimeLaterId = message(id = "c", sentAt = 100L)
        val state = SocialChatUiState(
            messages = listOf(later),
            olderMessages = listOf(sameTimeLaterId, earlier),
        )
        assertEquals(listOf("a", "c", "b"), state.allMessages.map { it.id })
    }

    private fun message(id: String, sentAt: Long) = SocialMessage(
        id = id,
        chatId = "chat",
        senderId = "u1",
        senderName = "Ann",
        text = id,
        sentAt = sentAt,
    )
}
