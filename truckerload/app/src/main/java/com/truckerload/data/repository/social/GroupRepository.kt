package com.truckerload.data.repository.social

import com.truckerload.domain.social.ChatMember
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.Flow

/**
 * Public/private groups, membership, and invite codes.
 */
interface GroupRepository {
    fun watchPublicGroups(): Flow<List<SocialChat>>
    fun recommendGroups(): Flow<List<SocialChat>>
    fun watchGroupMembers(chatId: String): Flow<List<ChatMember>>
    suspend fun createGroupChat(name: String, category: String = ""): SocialResult<String>
    suspend fun leaveGroup(chatId: String): SocialResult<Unit>
    suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit>
    suspend fun joinGroupByInviteCode(code: String, displayName: String): SocialResult<String>
}
