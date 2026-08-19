package com.truckerload.data.repository.social

import com.truckerload.domain.social.ChatMember
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroupChat(name: String, category: String = "", description: String = ""): SocialResult<String>
    suspend fun leaveGroup(chatId: String): SocialResult<Unit>
    fun watchGroupMembers(chatId: String): Flow<List<ChatMember>>
    suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit>
    suspend fun joinGroupByInviteCode(code: String, displayName: String): SocialResult<String>
    suspend fun updateGroupDescription(chatId: String, description: String): SocialResult<Unit>
    suspend fun deleteGroup(chatId: String): SocialResult<Unit>
    suspend fun setModerator(chatId: String, userId: String): SocialResult<Unit>
}
