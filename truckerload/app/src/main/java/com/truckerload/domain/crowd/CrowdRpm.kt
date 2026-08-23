package com.truckerload.domain.crowd

/**
 * Anonymous weekly RPM vs the community. Does not include names, CDL, or friend identity.
 */
data class CrowdRpmSnapshot(
    val myRpm: Double,
    val medianRpm: Double?,
    val percentile: Int?,
    val sampleCount: Int,
    val similarLaneCount: Int,
    val usedSimilarLanes: Boolean,
) {
    val hasCommunity: Boolean get() = sampleCount > 0 && medianRpm != null

    companion object {
        val Empty = CrowdRpmSnapshot(
            myRpm = 0.0,
            medianRpm = null,
            percentile = null,
            sampleCount = 0,
            similarLaneCount = 0,
            usedSimilarLanes = false,
        )
    }
}

object CrowdRpmMath {
    const val MIN_SIMILAR_SAMPLES = 3

    fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }

    /** Share of [samples] that are at or below [value], 0–100. */
    fun percentileRank(value: Double, samples: List<Double>): Int? {
        if (samples.isEmpty()) return null
        val atOrBelow = samples.count { it <= value }
        return ((atOrBelow.toDouble() / samples.size) * 100.0).toInt().coerceIn(0, 100)
    }

    fun crowdRpmsExcludingMe(reports: List<CrowdRateReport>): List<CrowdRateReport> =
        reports.filter { it.source != CrowdRateSource.ME && it.rpm > 0.0 }

    fun matchingLaneReports(
        myLanes: Set<Pair<String, String>>,
        crowd: List<CrowdRateReport>,
    ): List<CrowdRateReport> {
        if (myLanes.isEmpty()) return emptyList()
        return crowd.filter { (it.fromState to it.toState) in myLanes }
    }

    fun build(
        myRpm: Double,
        myLanes: Set<Pair<String, String>>,
        crowd: List<CrowdRateReport>,
        extraAnonymousRpms: List<Double> = emptyList(),
    ): CrowdRpmSnapshot {
        val network = crowdRpmsExcludingMe(crowd)
        val similar = matchingLaneReports(myLanes, network)
        val usedSimilar = similar.size >= MIN_SIMILAR_SAMPLES
        val sampleRpms = if (usedSimilar) {
            similar.map { it.rpm }
        } else {
            (network.map { it.rpm } + extraAnonymousRpms.filter { it > 0.0 })
        }
        return CrowdRpmSnapshot(
            myRpm = myRpm,
            medianRpm = median(sampleRpms),
            percentile = percentileRank(myRpm, sampleRpms),
            sampleCount = sampleRpms.size,
            similarLaneCount = similar.size,
            usedSimilarLanes = usedSimilar,
        )
    }
}
