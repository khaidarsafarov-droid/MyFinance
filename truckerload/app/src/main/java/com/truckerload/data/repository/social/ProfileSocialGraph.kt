package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.DriverFollowDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.DriverFollowEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.repository.toPeerProfile
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Block / follow / peer lookups for [ProfileRepositoryImpl]. */
internal class ProfileSocialGraph(
    private val profileDao: DriverProfileDao,
    private val blockedUserDao: BlockedUserDao,
    private val followDao: DriverFollowDao,
    private val peerDao: SocialPeerDao,
    private val chatDao: SocialChatDao,
    context: Context,
) {
    private val appContext = context.applicationContext

    suspend fun blockUser(blockedId: String): SocialResult<Unit> = runCatching {
        blockedUserDao.block(
            BlockedUserEntity(
                blockerId = DriverProfileEntity.LOCAL_USER_ID,
                blockedId = blockedId,
                blockedAt = System.currentTimeMillis(),
            ),
        )
        followDao.unfollow(DriverProfileEntity.LOCAL_USER_ID, blockedId)
        chatDao.findPrivateChatForPeer(peerDao, blockedId)?.let { chatDao.archiveChat(it.id) }
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_block_user, it), it) }

    suspend fun unblockUser(blockedId: String): SocialResult<Unit> = runCatching {
        blockedUserDao.unblock(DriverProfileEntity.LOCAL_USER_ID, blockedId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_unblock_user, it), it) }

    suspend fun isBlocked(targetId: String): Boolean =
        blockedUserDao.isBlocked(DriverProfileEntity.LOCAL_USER_ID, targetId)

    fun watchIsBlocked(targetId: String): Flow<Boolean> =
        blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID)
            .map { blockedIds -> targetId in blockedIds }.flowOn(Dispatchers.IO)

    suspend fun followDriver(targetId: String): SocialResult<Unit> = runCatching {
        if (targetId == DriverProfileEntity.LOCAL_USER_ID) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_follow_self))
        }
        followDao.follow(
            DriverFollowEntity(
                followerId = DriverProfileEntity.LOCAL_USER_ID,
                followingId = targetId,
                followedAt = System.currentTimeMillis(),
            ),
        )
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_follow, it), it) }

    suspend fun unfollowDriver(targetId: String): SocialResult<Unit> = runCatching {
        followDao.unfollow(DriverProfileEntity.LOCAL_USER_ID, targetId)
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_unfollow, it), it) }

    fun watchIsFollowing(targetId: String): Flow<Boolean> =
        followDao.watchIsFollowing(DriverProfileEntity.LOCAL_USER_ID, targetId).flowOn(Dispatchers.IO)

    fun watchPeer(peerId: String): Flow<SocialPeerProfile?> =
        peerDao.watchById(peerId).map { entity -> entity?.toPeerProfile() }.flowOn(Dispatchers.IO)

    suspend fun getPeer(peerId: String): SocialPeerProfile? =
        peerDao.getById(peerId)?.toPeerProfile()

    private suspend fun updateFollowCounts() {
        val existing = profileDao.getProfile() ?: return
        profileDao.upsert(
            existing.copy(
                followers = followDao.countFollowers(DriverProfileEntity.LOCAL_USER_ID),
                following = followDao.countFollowing(DriverProfileEntity.LOCAL_USER_ID),
            ),
        )
    }
}
