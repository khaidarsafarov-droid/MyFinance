package com.truckerload.domain.parser

import com.truckerload.domain.model.Stop
import java.security.MessageDigest

object StopsHasher {

    /**
     * Stable hash of a stop list for change detection.
     *
     * Includes [Stop.stopNumber] and sorts by that number so list order from Room
     * does not matter, but a real route reorder (different stop numbers / sequence)
     * is detected.
     */
    fun calculateStopsHash(stops: List<Stop>): String {
        // FIX: previous sort-by-signature ignored PU/DEL order changes
        val normalized = stops
            .sortedBy { it.stopNumber }
            .joinToString("|") { stop ->
                "${stop.stopNumber}|${stop.type}|${stop.city}|${stop.state}|" +
                    "${stop.facilityCode.orEmpty()}|${stop.scheduledTime}"
            }
        return sha256(normalized)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
