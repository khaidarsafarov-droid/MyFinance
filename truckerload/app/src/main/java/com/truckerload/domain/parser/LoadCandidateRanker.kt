package com.truckerload.domain.parser

import com.truckerload.domain.model.Load

/** Which parser produced a load. Higher [bonus] wins ties between equally consistent loads. */
internal enum class LoadParseSource(val bonus: Int) {
    JSON(30),
    RELAY(24),
    RATE_CONFIRMATION(22),
    DELIMITED(18),
    FLEXIBLE(8),
}

internal data class LoadParseGroup(
    val source: LoadParseSource,
    val loads: List<Load>,
)

/**
 * Picks the parser whose loads agree with each other: a positive rate, both ends of the
 * route, and a plausible rate-per-mile beat the first regex that happened to match.
 */
internal object LoadCandidateRanker {

    fun choose(groups: List<LoadParseGroup>): List<Load> {
        val viable = groups.filter { it.loads.isNotEmpty() }
        if (viable.isEmpty()) return emptyList()
        return viable.maxWith(compareBy<LoadParseGroup> { groupScore(it) }.thenBy { it.source.bonus }).loads
    }

    fun chooseOne(groups: List<LoadParseGroup>): Load? = choose(groups).firstOrNull()

    fun loadScore(load: Load): Int {
        var score = 0
        if (load.totalRate.isPositive()) score += 40
        if (load.pointA.isNotBlank()) score += 20
        if (load.pointB.isNotBlank()) score += 20
        if (load.totalMiles > 0.0) score += 10
        if (load.tripId.isNotBlank()) score += 8
        val miles = load.totalMiles
        if (miles > 0.0 && load.totalRate.isPositive()) {
            val rpm = load.totalRate.cents / 100.0 / miles
            if (rpm in MIN_PLAUSIBLE_RPM..MAX_PLAUSIBLE_RPM) score += 30
            if (rpm < ABSURD_LOW_RPM || rpm > ABSURD_HIGH_RPM) score -= 25
        }
        return score
    }

    /**
     * A diesel receipt or paycheck can contain a dollar amount and a city. That is not a load
     * unless the winning candidate has both ends of a route and a believable rate per mile.
     */
    fun keepDespiteReceipt(kind: MessageType, loads: List<Load>): Boolean {
        if (kind != MessageType.PAYCHECK && kind != MessageType.DIESEL) return true
        val best = loads.maxOfOrNull { loadScore(it) } ?: return false
        return best >= STRONG_LOAD_SCORE
    }

    private fun groupScore(group: LoadParseGroup): Int =
        group.loads.maxOf { loadScore(it) } + group.source.bonus

    private const val MIN_PLAUSIBLE_RPM = 0.8
    private const val MAX_PLAUSIBLE_RPM = 8.0
    private const val ABSURD_LOW_RPM = 0.3
    private const val ABSURD_HIGH_RPM = 20.0
    private const val STRONG_LOAD_SCORE = 110
}
