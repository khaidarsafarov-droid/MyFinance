package com.truckerload.data.repository.social

import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.data.social.ContentModerator
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.ReactionSummary
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.StatusType

internal fun SocialPeerEntity.toPeerProfile() = SocialPeerProfile(
    id = id,
    displayName = displayName,
    rating = rating,
    weeklyMiles = weeklyMiles,
    weeklyRevenue = weeklyRevenue,
    weeklyLoads = weeklyLoads,
    weeklyRpm = weeklyRpm,
)

internal fun SocialChatEntity.toDomain(isMember: Boolean = true) = SocialChat(
    id = id,
    title = title,
    type = runCatching { ChatType.valueOf(type) }.getOrDefault(ChatType.GROUP),
    participantCount = participantCount,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
    avatarEmoji = avatarEmoji,
    onlineCount = onlineCount,
    category = category,
    archived = archived,
    description = description,
    rating = rating,
    isPublic = isPublic,
    creatorId = creatorId,
    inviteCode = inviteCode,
    isMember = isMember,
)

internal fun SocialMessageEntity.toDomain(
    isMine: Boolean,
    reactions: List<ReactionSummary> = emptyList(),
    replyPreview: String? = null,
) = SocialMessage(
    id = id,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    text = text,
    sentAt = sentAt,
    messageType = runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT),
    attachmentUrl = attachmentUrl,
    isMine = isMine,
    replyToId = replyToId,
    replyPreview = replyPreview,
    locationLabel = locationLabel,
    isAnnouncement = isAnnouncement,
    reactions = reactions,
    hashtags = ContentModerator.extractHashtags(text),
    durationMs = durationMs,
)

internal fun DriverStatusEntity.toDomain() = DriverStatusPost(
    id = id,
    userId = userId,
    displayName = displayName,
    type = runCatching { StatusType.valueOf(type) }.getOrDefault(StatusType.TEXT),
    text = text,
    mediaPath = mediaPath,
    createdAt = createdAt,
    expiresAt = expiresAt,
    viewed = viewed,
    durationMs = durationMs,
)
