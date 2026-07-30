package com.truckerload.domain.crowd

/**
 * Who contributed an anonymized lane rate to the crowd map.
 */
enum class CrowdScope {
    ME,
    ALL,
}

/**
 * One anonymized report: RPM observed on a from→to state lane at a point in time.
 */
data class CrowdRateReport(
    val id: String,
    val fromState: String,
    val toState: String,
    val rpm: Double,
    val rate: Double,
    val miles: Double,
    val reportedAtMillis: Long,
    val source: CrowdRateSource,
    /** Optional short label for friends (never email). */
    val peerLabel: String? = null,
)

enum class CrowdRateSource {
    ME,
    FRIEND,
    NETWORK,
}

data class CrowdLaneAggregate(
    val fromState: String,
    val toState: String,
    val tripCount: Int,
    val avgRpm: Double,
    val totalRevenue: Double,
    val totalMiles: Double,
)

data class CrowdStateSummary(
    val stateCode: String,
    val outboundTrips: Int,
    val avgOutboundRpm: Double,
    val totalRevenue: Double,
    val totalMiles: Double,
    val recent: List<CrowdRateReport>,
)
