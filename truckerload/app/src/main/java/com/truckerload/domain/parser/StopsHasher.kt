package com.truckerload.domain.parser

import com.truckerload.domain.model.Stop
import java.security.MessageDigest

object StopsHasher {

    fun calculateStopsHash(stops: List<Stop>): String {
        val normalized = stops.map { stop ->
            "${stop.type}|${stop.city}|${stop.state}|${stop.facilityCode.orEmpty()}|${stop.scheduledTime}"
        }.sorted().joinToString("|")
        return sha256(normalized)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
