package com.truckerload.domain.crowd

/**
 * Gate for any future Crowd RPM network publish.
 *
 * There is **no** HTTP Crowd RPM endpoint today. When one exists, callers must
 * pass only [AnonymizedRpmSample] lists and must check opt-in first.
 * Do not log [samples].
 */
object CrowdRpmShareGate {

    fun payloadOrNull(
        optIn: Boolean,
        samples: List<AnonymizedRpmSample>,
    ): List<AnonymizedRpmSample>? {
        if (!optIn) return null
        return samples
    }
}
