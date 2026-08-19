package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.community.CommunityInboxSync
import com.truckerload.data.community.CommunityRemoteClient
import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.di.UserScope
import com.truckerload.domain.social.ChatMember
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.SocialIdentity
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
    private val messageDao: SocialMessageDao,
    private val profileDao: DriverProfileDao,
    private val appContext: Context,
    private val actorId: () -> String,
    private val remote: CommunityRemoteClient,
    private val inbox: CommunityInboxSync,
) : GroupRepository {

    override suspend fun createGroupChat(
        name: String,
        category: String,
        description: String,
    ): SocialResult<String> = runCatching {
        if (remote.isReady()) {
            val remoteId = remote.createGroup(name, category, description).getOrElse { err ->
                return SocialResult.Error(
                    socialError(
                        appContext,
                        R.string.social_error_create_group,
                        err
                    ), err
                )
            }
            inbox.pullChats()
            return SocialResult.Success(remoteId)
        }
        val me = actorId()
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
                creatorId = me,
                inviteCode = inviteCode,
                description = description.trim(),
            ),
        )
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = me,
                displayName = displayName.ifBlank { "You" },
                role = "OWNER",
                joinedAt = now,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_group, it), it) }

    override suspend fun leaveGroup(chatId: String): SocialResult<Unit> = runCatching {
        val me = actorId()
        if (remote.isReady()) {
            remote.leaveGroup(chatId)
        }
        chatMemberDao.remove(chatId, me)
        chatDao.archiveChat(chatId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_leave_group, it), it) }

    override fun watchGroupMembers(chatId: String): Flow<List<ChatMember>> {
        val me = actorId()
        return chatMemberDao.watchMembers(chatId).map { members ->
            members.map {
                ChatMember(
                    chatId = it.chatId,
                    userId = it.userId,
                    displayName = it.displayName,
                    role = it.role,
                    joinedAt = it.joinedAt,
                    isMe = SocialIdentity.isMine(it.userId, me),
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit> = runCatching {
        val me = actorId()
        if (chatMemberDao.isMember(chatId, me)) {
            return SocialResult.Success(Unit)
        }
        if (remote.isReady()) {
            remote.joinChat(chatId, displayName).getOrElse { err ->
                return SocialResult.Error(
                    socialError(
                        appContext,
                        R.string.social_error_join_challenge,
                        err
                    ), err
                )
            }
            inbox.pullChats()
            return SocialResult.Success(Unit)
        }
        val now = System.currentTimeMillis()
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = me,
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
        if (remote.isReady()) {
            val remoteId = remote.joinByInvite(normalized).getOrElse { err ->
                return SocialResult.Error(
                    socialError(
                        appContext,
                        R.string.social_error_group_code_not_found,
                        err
                    ), err
                )
            }
            inbox.pullChats()
            return SocialResult.Success(remoteId)
        }
        val chat = chatDao.getChatByInviteCode(normalized)
            ?: return SocialResult.Error(appContext.getString(R.string.social_error_group_code_not_found))
        return when (val joined = joinGroup(chat.id, displayName)) {
            is SocialResult.Success -> SocialResult.Success(chat.id)
            is SocialResult.Error -> joined
        }
    }

    override suspend fun updateGroupDescription(chatId: String, description: String): SocialResult<Unit> =
        runCatching {
            if (remote.isReady()) {
                remote.updateGroupDescription(chatId, description).getOrElse { err ->
                    return SocialResult.Error(
                        socialError(appContext, R.string.social_error_update_group, err),
                        err,
                    )
                }
            }
            chatDao.setDescription(chatId, description.trim())
            SocialResult.Success(Unit)
        }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_update_group, it), it) }

    override suspend fun deleteGroup(chatId: String): SocialResult<Unit> = runCatching {
        val chat = chatDao.getChat(chatId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_error_group_not_found))
        val me = actorId()
        val myRole = chatMemberDao.getMember(chatId, me)?.role
        val isCreator = chat.creatorId == me || myRole == "OWNER"
        if (!isCreator) {
            return SocialResult.Error(appContext.getString(R.string.social_error_delete_group))
        }
        if (remote.isReady()) {
            remote.deleteGroup(chatId).getOrElse { err ->
                return SocialResult.Error(
                    socialError(appContext, R.string.social_error_delete_group, err),
                    err,
                )
            }
        }
        messageDao.deleteAllInChat(chatId)
        chatMemberDao.deleteAllInChats(listOf(chatId))
        chatDao.deleteChat(chatId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_delete_group, it), it) }

    override suspend fun setModerator(chatId: String, userId: String): SocialResult<Unit> = runCatching {
        val chat = chatDao.getChat(chatId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_error_group_not_found))
        if (chat.creatorId.isNotBlank() && chat.creatorId != actorId()) {
            return SocialResult.Error(appContext.getString(R.string.social_error_update_group))
        }
        if (remote.isReady()) {
            remote.setGroupModerator(chatId, userId).getOrElse { err ->
                return SocialResult.Error(
                    socialError(appContext, R.string.social_error_update_group, err),
                    err,
                )
            }
        }
        chatMemberDao.clearModerators(chatId)
        chatMemberDao.setRole(chatId, userId, "MODERATOR")
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_update_group, it), it) }
}
