package com.truckerload.domain.crowd

import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.money.Money

/**
 * Who contributed a lane rate. Map UI currently uses ME only; FRIEND/NETWORK reserved for later.
 */
enum class CrowdScope {
    ME,
}

/**
 * Local map heatmap row (on-device only). Not the Crowd RPM share payload.
 *
 * Shareable fields live on [AnonymizedRpmSample]. [rate] is reconstructed as
 * `rpm * miles` for local coloring — never log this type as a network body.
 */
data class CrowdRateReport(
    val id: String,
    val fromState: String,
    val toState: String,
    val rpm: Money,
    val rate: Money,
    val miles: Double,
    val reportedAtMillis: Long,
    val source: CrowdRateSource,
    /** Optional short label for friends (never email). */
    val peerLabel: String? = null,
    val equipmentType: EquipmentType? = null,
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
    val avgRpm: Money,
    val totalRevenue: Money,
    val totalMiles: Double,
    val equipmentType: EquipmentType? = null,
)

data class CrowdStateSummary(
    val stateCode: String,
    val outboundTrips: Int,
    val avgOutboundRpm: Money,
    val totalRevenue: Money,
    val totalMiles: Double,
    val recent: List<CrowdRateReport>,
)
