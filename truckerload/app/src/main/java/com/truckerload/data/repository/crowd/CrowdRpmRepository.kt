package com.truckerload.data.repository.crowd

import com.truckerload.data.community.CommunityRemoteClient
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.toReport
import com.truckerload.domain.crowd.CrowdRpmMath
import com.truckerload.domain.crowd.CrowdRpmSnapshot
import com.truckerload.domain.social.CommunityWeekWindow
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.leaderboardScore
import com.truckerload.utils.extractStateFromLocation
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local-first Crowd RPM: Room cache first, remote peers as anonymous extras (best-effort).
 * Never blocks the UI on network; never returns friend names.
 */
class CrowdRpmRepository(
    private val db: AppDatabase,
    private val remote: CommunityRemoteClient,
) {
    suspend fun snapshot(): CrowdRpmSnapshot = withContext(Dispatchers.IO) {
        val window = CommunityWeekWindow.current()
        val stats = db.loadDao().getWeeklyLoadStatsOnce(window.week, window.year)
        val myRpm = stats.leaderboardScore(LeaderboardCategory.RPM)
        val loads = db.loadDao().getLoadsByWeekOnce(window.week, window.year)
        val myLanes = loads.mapNotNull(::laneOf).toSet()
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val crowd = db.crowdRateDao().listSince(since).map { it.toReport() }
        val peerRpms = db.socialPeerDao().getAll().map { it.weeklyRpm }.filter { it > 0.0 }
        val remoteRpms = if (remote.isReady()) {
            val me = remote.actorId()
            runCatching {
                remote.listPeers(window).getOrNull()
                    ?.filter { it.userId.isNotBlank() && it.userId != me }
                    ?.map { it.weeklyRpm }
                    ?.filter { it > 0.0 }
                    .orEmpty()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        CrowdRpmMath.build(
            myRpm = myRpm,
            myLanes = myLanes,
            crowd = crowd,
            extraAnonymousRpms = peerRpms + remoteRpms,
        )
    }
}

internal fun laneOf(load: LoadEntity): Pair<String, String>? {
    val from = extractStateFromLocation(load.pointA)
        ?: extractStateFromLocation(load.firstPuCityState)
        ?: return null
    val to = extractStateFromLocation(load.pointB)
        ?: extractStateFromLocation(load.lastDelCityState)
        ?: return null
    if (load.totalMiles <= 0.0 || load.totalRate <= 0.0) return null
    return from to to
}
