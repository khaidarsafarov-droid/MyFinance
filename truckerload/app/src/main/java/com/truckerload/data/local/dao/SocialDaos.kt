package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.ChallengeParticipationEntity
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.DriverFollowEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverProfileDao {
    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    fun watchProfile(id: String = DriverProfileEntity.LOCAL_USER_ID): Flow<DriverProfileEntity?>

    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String = DriverProfileEntity.LOCAL_USER_ID): DriverProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: DriverProfileEntity)
}

@Dao
interface SocialChatDao {
    @Query("SELECT * FROM social_chats WHERE archived = 0 ORDER BY lastMessageAt DESC")
    fun watchChats(): Flow<List<SocialChatEntity>>

    @Query(
        """
        SELECT * FROM social_chats
        WHERE archived = 0 AND (
            title LIKE '%' || :query || '%' OR lastMessage LIKE '%' || :query || '%'
        )
        ORDER BY lastMessageAt DESC
        """,
    )
    fun watchChatsSearch(query: String): Flow<List<SocialChatEntity>>

    @Query("SELECT * FROM social_chats WHERE id = :chatId LIMIT 1")
    suspend fun getChat(chatId: String): SocialChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chats: List<SocialChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: SocialChatEntity)

    @Query("UPDATE social_chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markRead(chatId: String)

    @Query("UPDATE social_chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun incrementUnread(chatId: String)

    @Query("UPDATE social_chats SET archived = 1 WHERE id = :chatId")
    suspend fun archiveChat(chatId: String)

    @Query("SELECT * FROM social_chats WHERE inviteCode = :code LIMIT 1")
    suspend fun getChatByInviteCode(code: String): SocialChatEntity?

    @Query("DELETE FROM social_chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("SELECT COUNT(*) FROM social_chats WHERE archived = 0")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM social_chats WHERE archived = 0")
    fun watchTotalUnread(): Flow<Int>
}

@Dao
interface SocialMessageDao {
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM social_messages WHERE chatId = :chatId
            ORDER BY sentAt DESC LIMIT :limit
        ) ORDER BY sentAt ASC
        """,
    )
    fun watchRecentMessages(chatId: String, limit: Int): Flow<List<SocialMessageEntity>>

    @Query("SELECT * FROM social_messages WHERE chatId = :chatId ORDER BY sentAt ASC")
    fun watchMessages(chatId: String): Flow<List<SocialMessageEntity>>

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM social_messages WHERE chatId = :chatId
            ORDER BY sentAt DESC LIMIT :limit
        ) ORDER BY sentAt ASC
        """,
    )
    suspend fun getRecentMessages(chatId: String, limit: Int): List<SocialMessageEntity>

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM social_messages WHERE chatId = :chatId AND sentAt < :beforeSentAt
            ORDER BY sentAt DESC LIMIT :limit
        ) ORDER BY sentAt ASC
        """,
    )
    suspend fun getMessagesBefore(chatId: String, beforeSentAt: Long, limit: Int): List<SocialMessageEntity>

    @Query("SELECT COUNT(*) FROM social_messages WHERE chatId = :chatId")
    suspend fun countInChat(chatId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SocialMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SocialMessageEntity>)

    @Query("DELETE FROM social_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM social_messages WHERE chatId = :chatId")
    suspend fun deleteAllInChat(chatId: String)

    @Query("SELECT * FROM social_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessage(messageId: String): SocialMessageEntity?
}

@Dao
interface MessageReactionDao {
    @Query(
        """
        SELECT * FROM message_reactions
        WHERE messageId IN (SELECT id FROM social_messages WHERE chatId = :chatId)
        """,
    )
    fun watchReactionsForChat(chatId: String): Flow<List<MessageReactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reaction: MessageReactionEntity)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND userId = :userId AND reaction = :reaction")
    suspend fun remove(messageId: String, userId: String, reaction: String)
}

@Dao
interface BlockedUserDao {
    @Query("SELECT blockedId FROM blocked_users WHERE blockerId = :userId")
    fun watchBlockedIds(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(entity: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun unblock(blockerId: String, blockedId: String)

    @Query("SELECT COUNT(*) > 0 FROM blocked_users WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun isBlocked(blockerId: String, blockedId: String): Boolean
}

@Dao
interface DriverStatusDao {
    @Query("SELECT * FROM driver_statuses WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun watchActiveStatuses(now: Long): Flow<List<DriverStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: DriverStatusEntity)

    @Query("UPDATE driver_statuses SET viewed = 1 WHERE id = :statusId")
    suspend fun markViewed(statusId: String)

    @Query("DELETE FROM driver_statuses WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long)

    @Query("DELETE FROM driver_statuses WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface ChallengeParticipationDao {
    @Query("SELECT * FROM challenge_participation WHERE challengeId = :challengeId ORDER BY score DESC")
    suspend fun getLeaderboard(challengeId: String): List<ChallengeParticipationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun join(entity: ChallengeParticipationEntity)

    @Query("SELECT * FROM challenge_participation WHERE challengeId = :challengeId AND userId = :userId LIMIT 1")
    suspend fun getParticipation(challengeId: String, userId: String): ChallengeParticipationEntity?

    @Query("UPDATE challenge_participation SET score = :score WHERE challengeId = :challengeId AND userId = :userId")
    suspend fun updateScore(challengeId: String, userId: String, score: Double)
}

@Dao
interface DriverFollowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun follow(entity: DriverFollowEntity)

    @Query("DELETE FROM driver_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun unfollow(followerId: String, followingId: String)

    @Query("SELECT COUNT(*) > 0 FROM driver_follows WHERE followerId = :followerId AND followingId = :followingId")
    fun watchIsFollowing(followerId: String, followingId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM driver_follows WHERE followingId = :userId")
    suspend fun countFollowers(userId: String): Int

    @Query("SELECT COUNT(*) FROM driver_follows WHERE followerId = :userId")
    suspend fun countFollowing(userId: String): Int

    @Query("SELECT followingId FROM driver_follows WHERE followerId = :followerId")
    fun watchFollowingIds(followerId: String): Flow<List<String>>
}

@Dao
interface ChatMemberDao {
    @Query("SELECT * FROM chat_members WHERE chatId = :chatId ORDER BY joinedAt ASC")
    fun watchMembers(chatId: String): Flow<List<ChatMemberEntity>>

    @Query("SELECT COUNT(*) > 0 FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    suspend fun isMember(chatId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: ChatMemberEntity)

    @Query("DELETE FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    suspend fun remove(chatId: String, userId: String)

    @Query("DELETE FROM chat_members WHERE chatId IN (:chatIds)")
    suspend fun deleteAllInChats(chatIds: List<String>)

    @Query("SELECT COUNT(*) FROM chat_members WHERE chatId = :chatId")
    suspend fun countMembers(chatId: String): Int

    @Query("SELECT chatId FROM chat_members WHERE userId = :userId")
    fun watchMemberChatIds(userId: String): Flow<List<String>>

    @Query("SELECT * FROM chat_members")
    fun watchAll(): Flow<List<ChatMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<ChatMemberEntity>)
}

@Dao
interface SocialPeerDao {
    @Query("SELECT * FROM social_peers ORDER BY weeklyMiles DESC")
    suspend fun getAll(): List<SocialPeerEntity>

    @Query("SELECT * FROM social_peers ORDER BY weeklyMiles DESC")
    fun watchAll(): Flow<List<SocialPeerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(peers: List<SocialPeerEntity>)

    @Query("SELECT COUNT(*) FROM social_peers")
    suspend fun count(): Int

    @Query("DELETE FROM social_peers")
    suspend fun deleteAll()

    @Query("DELETE FROM social_peers WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM social_peers WHERE id = :peerId LIMIT 1")
    suspend fun getById(peerId: String): SocialPeerEntity?

    @Query("SELECT * FROM social_peers WHERE id = :peerId LIMIT 1")
    fun watchById(peerId: String): Flow<SocialPeerEntity?>
}
