package com.truckerload.domain.crowd

/**
 * Hard deny for Crowd RPM network publish.
 *
 * The app is local-first: geographic efficiency uses only the driver's own
 * loads on-device. There is no HTTP Crowd RPM endpoint and opt-in is gone.
 */
object CrowdRpmShareGate {

    @Suppress("UNUSED_PARAMETER")
    fun payloadOrNull(
        optIn: Boolean,
        samples: List<AnonymizedRpmSample>,
    ): List<AnonymizedRpmSample>? = null
}
