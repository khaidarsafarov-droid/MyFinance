package com.truckerload.domain.import

import com.truckerload.domain.model.Load
import java.util.Locale

/**
 * Deduplicates parsed loads by trip id, keeping the **latest** occurrence.
 *
 * Telegram/HTML/JSON exports are chronological (oldest → newest). Rate/route
 * updates reuse the same Trip ID, so first-wins [Iterable.distinctBy] would
 * silently import stale data.
 */
object ImportTripDedup {

    /**
     * @return loads in original relative order, one entry per trip id (newest wins).
     */
    fun keepLatestByTripId(loads: List<Load>): List<Load> {
        if (loads.size <= 1) return loads
        return loads
            .asReversed()
            .distinctBy { it.tripId.uppercase(Locale.US) }
            .asReversed()
    }
}
