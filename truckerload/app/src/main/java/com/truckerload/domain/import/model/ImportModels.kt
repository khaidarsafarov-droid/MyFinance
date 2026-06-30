package com.truckerload.domain.import.model

import com.truckerload.domain.model.Load

data class ParsedLoad(
    val tripId: String,
    val totalRate: Double,
    val totalMiles: Double,
    val pointA: String,
    val pointB: String,
    val stopCount: Int,
    val rawText: String? = null,
) {
    companion object {
        fun from(load: Load): ParsedLoad = ParsedLoad(
            tripId = load.tripId,
            totalRate = load.totalRate,
            totalMiles = load.totalMiles,
            pointA = load.pointA,
            pointB = load.pointB,
            stopCount = load.stopCount.takeIf { it > 0 } ?: (load.puCount + load.delCount),
            rawText = load.rawMessage.take(500).ifBlank { null },
        )
    }
}

sealed class ImportResult {
    data class Added(val load: ParsedLoad) : ImportResult()
    data class Skipped(val tripId: String, val reason: SkipReason) : ImportResult()
    data class Failed(val tripId: String?, val rawBlock: String, val error: String) : ImportResult()
}

enum class SkipReason { DUPLICATE, INVALID_DATA, ALREADY_BOOKED }

data class ImportReport(
    val totalFound: Int,
    val added: Int,
    val skipped: Int,
    val failed: Int,
    val addedLoads: List<ParsedLoad>,
    val skippedLoads: List<Pair<String, SkipReason>>,
    val failedBlocks: List<Pair<String, String>>,
    val durationMs: Long,
    val filesProcessed: Int = 0,
    val fileName: String? = null,
)

enum class ImportSessionState {
    IDLE,
    WAITING_INPUT,
    PROCESSING,
    COMPLETED,
    CANCELLED,
}

sealed class ImportException(message: String) : Exception(message) {
    class TooManyLoads(val found: Int, val max: Int) :
        ImportException("Too many loads: $found (max $max)")

    class Timeout(val timeoutMs: Long) :
        ImportException("Import timeout after ${timeoutMs}ms")
}
