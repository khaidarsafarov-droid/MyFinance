package com.truckerload.data.repository.social

import android.content.Context
import androidx.annotation.StringRes
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.domain.social.ChatType
import kotlinx.coroutines.flow.first

internal object SocialConstants {
    const val MESSAGE_PAGE_SIZE = 50
    const val STATUS_TTL_MS = 24 * 60 * 60_000L
    const val WEEKLY_CHALLENGE_ID = "miles_week"
    const val LOCAL_SENDER_ID = "me"
}

internal fun Context.socialError(@StringRes fallbackRes: Int, throwable: Throwable): String =
    throwable.message?.takeIf { it.isNotBlank() } ?: getString(fallbackRes)

internal fun privateChatIdForPeer(peerId: String): String = "dm_$peerId"

internal suspend fun SocialChatDao.findPrivateChatByTitle(title: String): SocialChatEntity? =
    watchChats().first()
        .firstOrNull { it.type == ChatType.PRIVATE.name && it.title.equals(title, ignoreCase = true) }

internal suspend fun SocialChatDao.findPrivateChatForPeer(
    peerDao: SocialPeerDao,
    peerId: String,
): SocialChatEntity? {
    getChat(privateChatIdForPeer(peerId))?.let { return it }
    val peer = peerDao.getById(peerId) ?: return null
    return findPrivateChatByTitle(peer.displayName)
}
