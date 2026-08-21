package com.truckerload.data.repository.crowd

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.toReport
import com.truckerload.domain.crowd.CrowdRpmMath
import com.truckerload.domain.crowd.CrowdRpmSnapshot
import com.truckerload.domain.crowd.CrowdWeekWindow
import com.truckerload.utils.extractStateFromLocation
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local-only Crowd RPM: own weekly load stats plus anonymous Room crowd_rates.
 * Never contacts the network; never returns friend names.
 */
class CrowdRpmRepository(
    private val db: AppDatabase,
) {
    suspend fun snapshot(): CrowdRpmSnapshot = withContext(Dispatchers.IO) {
        val window = CrowdWeekWindow.current()
        val stats = db.loadDao().getWeeklyLoadStatsOnce(window.week, window.year)
        val myRpm = if (stats.totalMiles > 0) stats.totalRevenue / stats.totalMiles else 0.0
        val loads = db.loadDao().getLoadsByWeekOnce(window.week, window.year)
        val myLanes = loads.mapNotNull(::laneOf).toSet()
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val crowd = db.crowdRateDao().listSince(since).map { it.toReport() }
        CrowdRpmMath.build(
            myRpm = myRpm,
            myLanes = myLanes,
            crowd = crowd,
            extraAnonymousRpms = emptyList(),
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
