package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ChallengeParticipationEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.di.UserScope
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChallengeType
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.domain.social.SocialResult
import com.truckerload.utils.getCurrentWeekNumberAndYear
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn

/**
 * Weekly challenge + leaderboard helpers for [ProfileRepositoryImpl].
 */
@UserScope
class ProfileChallengeHelper @Inject constructor(
    db: AppDatabase,
    private val loadRepository: LoadRepository,
    private val userProfileStore: UserProfileStore,
    private val local: ProfileLocalDataSource,
    context: Context,
) {
    private val profileDao = db.driverProfileDao()
    private val peerDao = db.socialPeerDao()
    private val blockedUserDao = db.blockedUserDao()
    private val challengeDao = db.challengeParticipationDao()
    private val appContext = context.applicationContext

    fun watchLeaderboard(category: LeaderboardCategory): Flow<List<LeaderboardEntry>> {
        val (week, year) = getCurrentWeekNumberAndYear()
        return combine(
            loadRepository.watchWeeklyLoadStats(week, year),
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { weekStats, peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            local.buildLeaderboard(weekStats, peers.filter { it.id !in blockedSet }, category)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun hasJoinedWeeklyChallenge(): Boolean =
        challengeDao.getParticipation(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID) != null

    suspend fun refreshMyChallengeScore() {
        val (week, year) = getCurrentWeekNumberAndYear()
        val miles = loadRepository.getWeeklyLoadStatsOnce(week, year).totalMiles
        val existing = challengeDao.getParticipation(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID)
        if (existing != null) {
            challengeDao.updateScore(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID, miles)
        }
    }

    suspend fun joinWeeklyChallenge(): SocialResult<Unit> = runCatching {
        val (week, year) = getCurrentWeekNumberAndYear()
        val miles = loadRepository.getWeeklyLoadStatsOnce(week, year).totalMiles
        challengeDao.join(
            ChallengeParticipationEntity(
                challengeId = WEEKLY_CHALLENGE_ID,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                score = miles,
                joinedAt = System.currentTimeMillis(),
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_join_chat, it), it) }

    suspend fun weeklyChallenge(): Challenge {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(week, year)
        val myMiles = weekStats.totalMiles
        val myName = profileDao.getProfile()?.displayName
            ?: userProfileStore.profile.value?.displayName
            ?: "You"
        refreshMyChallengeScore()
        val participation = challengeDao.getParticipation(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID)
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
            id = WEEKLY_CHALLENGE_ID,
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
        return local.buildLeaderboard(weekStats, peers.filter { it.id !in blockedIds }, category)
    }

    companion object {
        const val WEEKLY_CHALLENGE_ID = "miles_week"
    }
}
