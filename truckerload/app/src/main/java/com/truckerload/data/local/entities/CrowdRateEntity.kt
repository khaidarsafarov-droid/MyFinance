package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached anonymized crowd rate (friend or network). Local "me" rates are derived from loads
 * via [com.truckerload.domain.crowd.CrowdRpmMapper] — never persist tripId, rawMessage,
 * or exact addresses here. Do not log row contents.
 */
@Entity(
    tableName = "crowd_rates",
    indices = [
        Index(value = ["fromState", "reportedAtMillis"]),
        Index(value = ["source", "reportedAtMillis"]),
        Index(value = ["fromState", "equipmentType", "reportedAtMillis"]),
    ],
)
data class CrowdRateEntity(
    @PrimaryKey val id: String,
    val fromState: String,
    val toState: String,
    val rpm: Double,
    val rate: Double,
    val miles: Double,
    val reportedAtMillis: Long,
    /** ME, FRIEND, or NETWORK */
    val source: String,
    val peerLabel: String? = null,
    val syncedAtMillis: Long = System.currentTimeMillis(),
    /** Trailer type name; null = unknown / mixed. */
    val equipmentType: String? = null,
)
