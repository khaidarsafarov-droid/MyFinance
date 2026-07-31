package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.di.UserScope
import com.truckerload.domain.social.ChatMember
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID

@UserScope
class GroupRepositoryImpl(
    private val chatDao: SocialChatDao,
    private val chatMemberDao: ChatMemberDao,
    private val profileDao: DriverProfileDao,
    private val appContext: Context,
) : GroupRepository {

    override suspend fun createGroupChat(name: String, category: String): SocialResult<String> = runCatching {
        val chatId = "group_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val inviteCode = chatId.takeLast(6).uppercase()
        val displayName = profileDao.getProfile()?.displayName.orEmpty()
        chatDao.upsert(
            SocialChatEntity(
                id = chatId,
                title = name.ifBlank { appContext.getString(R.string.social_error_new_group_title) },
                type = ChatType.GROUP.name,
                participantCount = 1,
                lastMessage = "",
                lastMessageAt = now,
                unreadCount = 0,
                avatarEmoji = "👥",
                onlineCount = 1,
                category = category,
                creatorId = DriverProfileEntity.LOCAL_USER_ID,
                inviteCode = inviteCode,
            ),
        )
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                displayName = displayName.ifBlank { "You" },
                role = "OWNER",
                joinedAt = now,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_group, it), it) }

    override suspend fun leaveGroup(chatId: String): SocialResult<Unit> = runCatching {
        chatMemberDao.remove(chatId, DriverProfileEntity.LOCAL_USER_ID)
        chatDao.archiveChat(chatId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_leave_group, it), it) }

    override fun watchGroupMembers(chatId: String): Flow<List<ChatMember>> =
        chatMemberDao.watchMembers(chatId).map { members ->
            members.map {
                ChatMember(
                    chatId = it.chatId,
                    userId = it.userId,
                    displayName = it.displayName,
                    role = it.role,
                    joinedAt = it.joinedAt,
                    isMe = it.userId == DriverProfileEntity.LOCAL_USER_ID,
                )
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit> = runCatching {
        if (chatMemberDao.isMember(chatId, DriverProfileEntity.LOCAL_USER_ID)) {
            return SocialResult.Success(Unit)
        }
        val now = System.currentTimeMillis()
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                displayName = displayName.ifBlank { "You" },
                role = "MEMBER",
                joinedAt = now,
            ),
        )
        val chat = chatDao.getChat(chatId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_error_group_not_found))
        chatDao.upsert(chat.copy(participantCount = chatMemberDao.countMembers(chatId).coerceAtLeast(1)))
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_join_challenge, it), it) }

    override suspend fun joinGroupByInviteCode(code: String, displayName: String): SocialResult<String> {
        val normalized = com.truckerload.domain.social.GroupInviteCode.normalize(code)
        if (com.truckerload.domain.social.GroupInviteCode.isBlank(normalized)) {
            return SocialResult.Error(appContext.getString(R.string.social_error_group_code_not_found))
        }
        val chat = chatDao.getChatByInviteCode(normalized)
            ?: return SocialResult.Error(appContext.getString(R.string.social_error_group_code_not_found))
        return when (val joined = joinGroup(chat.id, displayName)) {
            is SocialResult.Success -> SocialResult.Success(chat.id)
            is SocialResult.Error -> joined
        }
    }
}
