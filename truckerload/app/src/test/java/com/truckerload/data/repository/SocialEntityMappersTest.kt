package com.truckerload.data.repository

import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.ReactionSummary
import com.truckerload.domain.social.StatusType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialEntityMappersTest {

    @Test
    fun chatEntityMapsFieldsAndFallsBackToGroupType() {
        val entity = SocialChatEntity(
            id = "chat-1",
            title = "Dispatch",
            type = "UNKNOWN",
            participantCount = 7,
            lastMessage = "See you",
            lastMessageAt = 123L,
            unreadCount = 2,
            avatarEmoji = "D",
            onlineCount = 3,
            category = "lane",
            archived = true,
            description = "Team chat",
            rating = 4.7,
            isPublic = false,
            creatorId = "owner",
            inviteCode = "ROAD77",
        )

        val domain = entity.toDomain(isMember = false)

        assertEquals("chat-1", domain.id)
        assertEquals("Dispatch", domain.title)
        assertEquals(ChatType.GROUP, domain.type)
        assertEquals(7, domain.participantCount)
        assertEquals("See you", domain.lastMessage)
        assertEquals(123L, domain.lastMessageAt)
        assertEquals(2, domain.unreadCount)
        assertEquals("D", domain.avatarEmoji)
        assertEquals(3, domain.onlineCount)
        assertEquals("lane", domain.category)
        assertTrue(domain.archived)
        assertEquals("Team chat", domain.description)
        assertEquals(4.7, domain.rating, 0.0)
        assertFalse(domain.isPublic)
        assertEquals("owner", domain.creatorId)
        assertEquals("ROAD77", domain.inviteCode)
        assertFalse(domain.isMember)
    }

    @Test
    fun messageEntityMapsFieldsReactionsHashtagsAndFallbackType() {
        val reactions = listOf(ReactionSummary("+1", count = 2, includesMe = true))
        val entity = SocialMessageEntity(
            id = "message-1",
            chatId = "chat-1",
            senderId = "me",
            senderName = "Driver",
            text = "Rolling #I95",
            sentAt = 456L,
            messageType = "BAD_TYPE",
            attachmentUrl = "/tmp/photo.jpg",
            replyToId = "message-0",
            locationLabel = "NC",
            isAnnouncement = true,
            durationMs = 9000L,
        )

        val domain = entity.toDomain(
            isMine = true,
            reactions = reactions,
            replyPreview = "Previous",
        )

        assertEquals("message-1", domain.id)
        assertEquals("chat-1", domain.chatId)
        assertEquals("me", domain.senderId)
        assertEquals("Driver", domain.senderName)
        assertEquals("Rolling #I95", domain.text)
        assertEquals(456L, domain.sentAt)
        assertEquals(MessageType.TEXT, domain.messageType)
        assertEquals("/tmp/photo.jpg", domain.attachmentUrl)
        assertTrue(domain.isMine)
        assertEquals("message-0", domain.replyToId)
        assertEquals("Previous", domain.replyPreview)
        assertEquals("NC", domain.locationLabel)
        assertTrue(domain.isAnnouncement)
        assertEquals(reactions, domain.reactions)
        assertEquals(listOf("I95"), domain.hashtags)
        assertEquals(9000L, domain.durationMs)
    }

    @Test
    fun peerEntityMapsStats() {
        val domain = SocialPeerEntity(
            id = "peer-1",
            displayName = "Ivan",
            rating = 4.9,
            weeklyMiles = 1800.0,
            weeklyRevenue = 5200.0,
            weeklyLoads = 5,
            weeklyRpm = 2.89,
        ).toPeerProfile()

        assertEquals("peer-1", domain.id)
        assertEquals("Ivan", domain.displayName)
        assertEquals(4.9, domain.rating, 0.0)
        assertEquals(1800.0, domain.weeklyMiles, 0.0)
        assertEquals(5200.0, domain.weeklyRevenue, 0.0)
        assertEquals(5, domain.weeklyLoads)
        assertEquals(2.89, domain.weeklyRpm, 0.0)
    }

    @Test
    fun statusEntityMapsFieldsAndFallsBackToTextType() {
        val domain = DriverStatusEntity(
            id = "status-1",
            userId = "user-1",
            displayName = "Alex",
            type = "BAD_TYPE",
            text = "Available",
            mediaPath = "/tmp/status.jpg",
            createdAt = 111L,
            expiresAt = 222L,
            viewed = true,
            durationMs = 3000L,
        ).toDomain()

        assertEquals("status-1", domain.id)
        assertEquals("user-1", domain.userId)
        assertEquals("Alex", domain.displayName)
        assertEquals(StatusType.TEXT, domain.type)
        assertEquals("Available", domain.text)
        assertEquals("/tmp/status.jpg", domain.mediaPath)
        assertEquals(111L, domain.createdAt)
        assertEquals(222L, domain.expiresAt)
        assertTrue(domain.viewed)
        assertEquals(3000L, domain.durationMs)
    }
}
