package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.money.Money
import kotlin.math.abs

object LoadChangeDetector {

    fun isRateChanged(old: Money, new: Money): Boolean = abs(old.cents - new.cents) > 1L

    fun isRateChangedSignificant(old: Money, new: Money, thresholdPercent: Double): Boolean {
        if (!isRateChanged(old, new)) return false
        if (old.cents <= 0L) return true
        val percentChange = abs(new.cents - old.cents).toDouble() / old.cents.toDouble() * 100.0
        return percentChange >= thresholdPercent
    }

    fun isMilesChanged(old: Double, new: Double): Boolean = abs(old - new) > 0.1

    fun isStopCountChanged(old: Int, new: Int): Boolean = old != new

    fun isFirstPuTimeChanged(old: Long, new: Long): Boolean = abs(old - new) > 60_000

    fun isLastDelTimeChanged(old: Long, new: Long): Boolean = abs(old - new) > 60_000

    fun isStopAddressChanged(oldStops: List<Stop>, newStops: List<Stop>): Boolean {
        if (oldStops.size != newStops.size) return true
        return oldStops.zip(newStops).any { (oldStop, newStop) ->
            oldStop.fullAddress != newStop.fullAddress ||
                oldStop.city != newStop.city ||
                oldStop.state != newStop.state ||
                oldStop.facilityCode != newStop.facilityCode
        }
    }

    fun isStopStatusChanged(oldStops: List<Stop>, newStops: List<Stop>): Boolean {
        if (oldStops.size != newStops.size) return true
        return oldStops.zip(newStops).any { (oldStop, newStop) ->
            stopStatus(oldStop) != stopStatus(newStop)
        }
    }

    fun detectChanges(old: Load, new: Load): List<String> {
        val changes = mutableListOf<String>()
        if (isRateChanged(old.totalRate, new.totalRate)) {
            changes.add("totalRate: ${old.totalRate} → ${new.totalRate}")
        }
        if (isMilesChanged(old.totalMiles, new.totalMiles)) {
            changes.add("totalMiles: ${old.totalMiles} → ${new.totalMiles}")
        }
        if (isStopCountChanged(old.stops.size, new.stops.size)) {
            changes.add("stopCount: ${old.stops.size} → ${new.stops.size}")
        }
        if (isStopAddressChanged(old.stops, new.stops)) {
            changes.add("stopAddress")
        }
        if (isStopStatusChanged(old.stops, new.stops)) {
            changes.add("stopStatus")
        }
        return changes
    }

    private fun stopStatus(stop: Stop): String {
        val note = stop.note.orEmpty()
        return if (note.contains("CANCEL", ignoreCase = true)) "CANCELLED" else "ACTIVE"
    }
}
