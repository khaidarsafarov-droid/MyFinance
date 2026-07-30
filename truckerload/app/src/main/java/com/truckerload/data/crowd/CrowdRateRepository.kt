package com.truckerload.data.crowd

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.CrowdRateEntity
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.presentation.screens.map.CrowdMapAggregator
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Combines local load-derived "me" rates with cached friend/network crowd rates.
 * When the network cache is empty, seeds a small anonymized community sample so the
 * All / Friends map UX is usable offline (clearly tagged as NETWORK / FRIEND).
 */
class CrowdRateRepository(
    private val db: AppDatabase,
    private val loadRepository: LoadRepository,
) {
    private val dao get() = db.crowdRateDao()

    suspend fun loadWeekReports(
        nowMillis: Long = System.currentTimeMillis(),
        windowMs: Long = CrowdMapAggregator.WEEK_MS,
    ): List<CrowdRateReport> {
        val cutoff = nowMillis - windowMs
        dao.deleteOlderThan(cutoff - TimeUnit.DAYS.toMillis(1))
        ensureCommunitySample(nowMillis)

        val me = CrowdMapAggregator.reportsFromLoads(
            loads = loadRepository.getAllLoadsOnce(),
            nowMillis = nowMillis,
            windowMs = windowMs,
        )
        val cached = dao.listSince(cutoff).map { it.toDomain() }
        // Prefer live "me" over any stale ME rows in cache.
        val nonMeCached = cached.filter { it.source != CrowdRateSource.ME }
        return (me + nonMeCached).distinctBy { it.id }
    }

    suspend fun replaceNetworkRates(reports: List<CrowdRateReport>) {
        val entities = reports
            .filter { it.source == CrowdRateSource.NETWORK || it.source == CrowdRateSource.FRIEND }
            .map { it.toEntity() }
        if (entities.isEmpty()) return
        dao.upsertAll(entities)
    }

    private suspend fun ensureCommunitySample(nowMillis: Long) {
        if (dao.countBySource(CrowdRateSource.NETWORK.name) > 0) return
        if (dao.countBySource(CrowdRateSource.FRIEND.name) > 0) return
        dao.upsertAll(buildCommunitySample(nowMillis))
    }

    companion object {
        /** Deterministic offline sample corridors for last-7-day crowd UX. */
        fun buildCommunitySample(nowMillis: Long): List<CrowdRateEntity> {
            val corridors = listOf(
                Triple("WA", "OR", 2.85),
                Triple("WA", "CA", 2.55),
                Triple("CA", "AZ", 2.40),
                Triple("TX", "OK", 2.70),
                Triple("TX", "LA", 2.35),
                Triple("FL", "GA", 2.50),
                Triple("NY", "PA", 3.10),
                Triple("IL", "IN", 2.65),
                Triple("GA", "NC", 2.45),
                Triple("OH", "KY", 2.75),
                Triple("AZ", "NV", 2.20),
                Triple("CO", "UT", 2.90),
            )
            val friendNames = listOf("Ivan", "Alexey", "Sergey", "Maria")
            val rnd = Random(30_07_2026L)
            val out = mutableListOf<CrowdRateEntity>()
            corridors.forEachIndexed { idx, (from, to, baseRpm) ->
                // Network: a few ages across the week
                listOf(2L, 14L, 36L, 60L, 90L).forEachIndexed { j, hoursAgo ->
                    val rpm = baseRpm + (rnd.nextDouble() - 0.5) * 0.35
                    val miles = 400.0 + rnd.nextDouble() * 500.0
                    val rate = rpm * miles
                    val at = nowMillis - TimeUnit.HOURS.toMillis(hoursAgo + idx)
                    out += CrowdRateEntity(
                        id = "net:$from-$to-$j",
                        fromState = from,
                        toState = to,
                        rpm = rpm,
                        rate = rate,
                        miles = miles,
                        reportedAtMillis = at,
                        source = CrowdRateSource.NETWORK.name,
                        peerLabel = null,
                        syncedAtMillis = nowMillis,
                    )
                }
                // Friends: fewer, tagged
                if (idx < friendNames.size) {
                    val hoursAgo = 3L + idx * 11L
                    val rpm = baseRpm + 0.12
                    val miles = 520.0 + idx * 40.0
                    out += CrowdRateEntity(
                        id = "friend:$from-$to",
                        fromState = from,
                        toState = to,
                        rpm = rpm,
                        rate = rpm * miles,
                        miles = miles,
                        reportedAtMillis = nowMillis - TimeUnit.HOURS.toMillis(hoursAgo),
                        source = CrowdRateSource.FRIEND.name,
                        peerLabel = friendNames[idx],
                        syncedAtMillis = nowMillis,
                    )
                }
            }
            return out.sortedByDescending { it.reportedAtMillis }
        }
    }
}

private fun CrowdRateEntity.toDomain(): CrowdRateReport =
    CrowdRateReport(
        id = id,
        fromState = fromState,
        toState = toState,
        rpm = rpm,
        rate = rate,
        miles = miles,
        reportedAtMillis = reportedAtMillis,
        source = runCatching { CrowdRateSource.valueOf(source) }.getOrDefault(CrowdRateSource.NETWORK),
        peerLabel = peerLabel,
    )

private fun CrowdRateReport.toEntity(): CrowdRateEntity =
    CrowdRateEntity(
        id = id,
        fromState = fromState,
        toState = toState,
        rpm = rpm,
        rate = rate,
        miles = miles,
        reportedAtMillis = reportedAtMillis,
        source = source.name,
        peerLabel = peerLabel,
    )

/** Relative age helper for UI tests. */
fun crowdReportAgeBucket(reportedAtMillis: Long, nowMillis: Long): Long =
    abs(nowMillis - reportedAtMillis)
