package com.truckerload.syncserver

import com.google.gson.annotations.SerializedName

data class WebhookRequest(
    val loads: List<LoadDto>? = null,
    @SerializedName("message_date_seconds") val messageDateSeconds: Long? = null
)

/** Поддерживает snake_case (trip_id) и camelCase (tripId). */
data class LoadDto(
    @SerializedName("trip_id") val tripIdSnake: String? = null,
    @SerializedName("tripId") val tripId: String? = null,
    val date: String = "",
    @SerializedName("total_rate") val totalRateSnake: Double? = null,
    @SerializedName("totalRate") val totalRate: Double? = null,
    @SerializedName("total_miles") val totalMilesSnake: Double? = null,
    @SerializedName("totalMiles") val totalMiles: Double? = null,
    @SerializedName("point_a") val pointASnake: String? = null,
    @SerializedName("pointA") val pointA: String? = null,
    @SerializedName("point_b") val pointBSnake: String? = null,
    @SerializedName("pointB") val pointB: String? = null
) {
    fun effectiveTripId() = tripId ?: tripIdSnake ?: ""
    fun effectiveTotalRate() = totalRate ?: totalRateSnake ?: 0.0
    fun effectiveTotalMiles() = totalMiles ?: totalMilesSnake ?: 0.0
    fun effectivePointA() = pointA ?: pointASnake ?: ""
    fun effectivePointB() = pointB ?: pointBSnake ?: ""
}

data class WebhookResponse(
    val status: String,
    val message: String,
    @SerializedName("added_count") val addedCount: Int = 0,
    @SerializedName("last_added_text") val lastAddedText: String = ""
) {
    companion object {
        fun from(result: SyncResult): WebhookResponse = when (result) {
            is SyncResult.Success -> WebhookResponse(
                status = "success",
                message = "Добавлено ${result.addedCount} новых грузов. Последний: ${result.lastAddedText}",
                addedCount = result.addedCount,
                lastAddedText = result.lastAddedText
            )
            is SyncResult.Duplicate -> WebhookResponse(
                status = "duplicate",
                message = "Все данные уже заполнены. Отправьте новые данные."
            )
            is SyncResult.Empty -> WebhookResponse(
                status = "empty",
                message = "Новых данных не обнаружено"
            )
        }
    }
}
