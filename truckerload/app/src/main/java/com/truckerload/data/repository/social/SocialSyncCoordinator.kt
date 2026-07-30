package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.ChallengeParticipationDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.ChallengeParticipationEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.SocialSeedHelper
import com.truckerload.data.social.SocialPeerSeedData
import com.truckerload.data.social.SocialSeedData
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChallengeType
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.leaderboardScore
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cross-repo social bootstrap: seed data, identity sync, challenges, leaderboard.
 *
 * Account-scoped (holds Room DAOs) — constructed inside [com.truckerload.di.UserComponent],
 * not a process-wide `@Singleton`. Bridged unscoped via [com.truckerload.di.SocialRepositoryModule].
 */
class SocialSyncCoordinator(
    private val profileDao: DriverProfileDao,
    private val chatDao: SocialChatDao,
    private val messageDao: SocialMessageDao,
    private val peerDao: SocialPeerDao,
    private val blockedUserDao: BlockedUserDao,
    private val challengeDao: ChallengeParticipationDao,
    private val loadRepository: LoadRepository,
    private val userProfileStore: UserProfileStore,
    private val profileRepository: ProfileRepository,
    private val statusRepository: StatusRepository,
    private val seedHelper: SocialSeedHelper,
    context: Context,
) {
    private val appContext = context.applicationContext
    private val initMutex = Mutex()

    suspend fun ensureInitialized() {
        initMutex.withLock {
            val userProfile = userProfileStore.profile.value
            val displayName = userProfile?.displayName.orEmpty()
            SocialSeedData.seedIfEmpty(
                chatDao,
                messageDao,
                profileDao,
                displayName,
                userProfile?.photoUrl,
                userProfile?.phoneNumber,
            )
            profileRepository.syncIdentityFromUserProfile()
            profileRepository.maybeMarkSetupCompleteFromExistingProfile()
            SocialPeerSeedData.seedIfEmpty(peerDao)
            seedHelper.seedDemoStatuses(displayName, SocialConstants.STATUS_TTL_MS)
            seedHelper.seedGroupMemberships(displayName)
            seedHelper.backfillGroupInviteCodes()
            statusRepository.purgeExpired()
            refreshMyChallengeScore()
        }
    }

    fun watchLeaderboard(
        category: LeaderboardCategory = LeaderboardCategory.OVERALL,
    ): Flow<List<LeaderboardEntry>> {
        val (week, year) = getCurrentWeekNumberAndYear()
        return combine(
            loadRepository.watchWeeklyLoadStats(week, year),
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { weekStats, peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            buildLeaderboard(weekStats, peers.filter { it.id !in blockedSet }, category)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun hasJoinedWeeklyChallenge(): Boolean =
        challengeDao.getParticipation(
            SocialConstants.WEEKLY_CHALLENGE_ID,
            DriverProfileEntity.LOCAL_USER_ID,
        ) != null

    suspend fun refreshMyChallengeScore() {
        val (week, year) = getCurrentWeekNumberAndYear()
        val miles = loadRepository.getWeeklyLoadStatsOnce(week, year).totalMiles
        val existing = challengeDao.getParticipation(
            SocialConstants.WEEKLY_CHALLENGE_ID,
            DriverProfileEntity.LOCAL_USER_ID,
        )
        if (existing != null) {
            challengeDao.updateScore(
                SocialConstants.WEEKLY_CHALLENGE_ID,
                DriverProfileEntity.LOCAL_USER_ID,
                miles,
            )
        }
    }

    suspend fun joinWeeklyChallenge(): SocialResult<Unit> = runCatching {
        val (week, year) = getCurrentWeekNumberAndYear()
        val miles = loadRepository.getWeeklyLoadStatsOnce(week, year).totalMiles
        challengeDao.join(
            ChallengeParticipationEntity(
                challengeId = SocialConstants.WEEKLY_CHALLENGE_ID,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                score = miles,
                joinedAt = System.currentTimeMillis(),
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_join_chat, it), it) }

    suspend fun weeklyChallenge(): Challenge {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(week, year)
        val myMiles = weekStats.totalMiles
        val myName = profileDao.getProfile()?.displayName
            ?: userProfileStore.profile.value?.displayName
            ?: "You"
        refreshMyChallengeScore()
        val participation = challengeDao.getParticipation(
            SocialConstants.WEEKLY_CHALLENGE_ID,
            DriverProfileEntity.LOCAL_USER_ID,
        )
        val score = participation?.score ?: myMiles
        val now = System.currentTimeMillis()
        val peers = peerDao.getAll()
        val peerBoard = peers.map { peer ->
            LeaderboardEntry(
                rank = 0,
                displayName = peer.displayName,
                score = peer.weeklyMiles,
                rating = peer.rating,
                trend = "—",
                userId = peer.id,
            )
        }
        val myEntry = LeaderboardEntry(
            rank = 0,
            displayName = myName,
            score = score,
            rating = 4.8,
            trend = if (score > 0) "⬆" else "—",
            isMe = true,
        )
        val merged = (peerBoard + myEntry).sortedByDescending { it.score }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
        val myPosition = merged.indexOfFirst { it.isMe }.let { if (it >= 0) it + 1 else merged.size }
        return Challenge(
            id = SocialConstants.WEEKLY_CHALLENGE_ID,
            title = appContext.getString(R.string.social_challenge_king_miles_title),
            description = appContext.getString(R.string.social_challenge_king_miles_desc, week, year),
            type = ChallengeType.MILES,
            goal = 3000.0,
            startDate = now - 4 * 24 * 60 * 60_000L,
            endDate = now + 3 * 24 * 60 * 60_000L,
            leaderboard = merged.take(10),
            myPosition = myPosition,
            myScore = score,
        )
    }

    suspend fun getLeaderboard(category: LeaderboardCategory): List<LeaderboardEntry> {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(week, year)
        val peers = peerDao.getAll()
        val blockedIds = blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID).first().toSet()
        return buildLeaderboard(weekStats, peers.filter { it.id !in blockedIds }, category)
    }

    private fun buildLeaderboard(
        weekStats: WeeklyLoadStatsAgg,
        peers: List<SocialPeerEntity>,
        category: LeaderboardCategory,
    ): List<LeaderboardEntry> {
        val localName = userProfileStore.profile.value?.displayName ?: "You"
        val myScore = weekStats.leaderboardScore(category)
        val peerEntries = peers.map { peer ->
            val score = when (category) {
                LeaderboardCategory.LOADS -> peer.weeklyLoads.toDouble()
                LeaderboardCategory.REVENUE -> peer.weeklyRevenue
                LeaderboardCategory.RPM -> peer.weeklyRpm
                LeaderboardCategory.OVERALL -> peer.weeklyMiles
            }
            LeaderboardEntry(
                rank = 0,
                displayName = peer.displayName,
                score = score,
                rating = peer.rating,
                trend = "—",
                userId = peer.id,
            )
        }
        val myEntry = LeaderboardEntry(
            rank = 0,
            displayName = localName,
            score = myScore,
            rating = 4.8,
            trend = if (myScore > 0) "⬆" else "—",
            isMe = true,
        )
        return (peerEntries + myEntry)
            .sortedByDescending { it.score }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }
}
