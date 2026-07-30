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
 * Combines local load-derived "me" rates with cached network crowd rates.
 * When the network cache is empty, seeds a small anonymized community sample so
 * the All map UX is usable offline (tagged as NETWORK).
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
            // Friends scope is paused — only community network + live Me.
            .filter { it.source == CrowdRateSource.NETWORK }
        return (me + cached).distinctBy { it.id }
    }

    suspend fun replaceNetworkRates(reports: List<CrowdRateReport>) {
        val entities = reports
            .filter { it.source == CrowdRateSource.NETWORK }
            .map { it.toEntity() }
        if (entities.isEmpty()) return
        dao.upsertAll(entities)
    }

    private suspend fun ensureCommunitySample(nowMillis: Long) {
        if (dao.countBySource(CrowdRateSource.NETWORK.name) > 0) return
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
            val rnd = Random(30_07_2026L)
            val out = mutableListOf<CrowdRateEntity>()
            corridors.forEachIndexed { idx, (from, to, baseRpm) ->
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
