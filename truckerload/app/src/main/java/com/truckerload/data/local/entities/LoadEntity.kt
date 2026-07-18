package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loads",
    indices = [
        Index(value = ["tripId"], unique = true),
        Index(value = ["date"]),
        Index(value = ["weekNumber", "year"]),
    ]
)
data class LoadEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val date: String,
    val totalRate: Double,
    val totalMiles: Double,
    val pointA: String,
    val pointB: String,
    val puCount: Int,
    val delCount: Int,
    val weekNumber: Int,
    val year: Int,
    val rawMessage: String,
    val parsedAt: Long,
    val updatedAt: Long,
    /** Earliest PU (epoch ms), denormalized for SQL yield. */
    val firstPuMillis: Long? = null,
    /** Latest DEL (epoch ms), denormalized for SQL yield. */
    val lastDelMillis: Long? = null,
    val route: String = "",
    val firstPuCityState: String = "",
    val lastDelCityState: String = "",
    val durationDays: Double = 0.0,
    val pace: Double = 0.0,
    val stopCount: Int = 0,
    /** Груз помечен как диспют (или был диспютом). */
    val isDispute: Boolean = false,
    /** Дата ответа по диспюту (YYYY-MM-DD). */
    val disputeResponseDate: String? = null,
    /** Диспют закрыт пользователем. */
    val disputeCompleted: Boolean = false,
)
