package com.truckerload.domain.maintenance

/**
 * Selects which journal loads count toward a ТО miles reminder.
 *
 * Previously we summed every load with `date >= startDate`, which pulled in the whole
 * journal (including old trips mis-dated into that range) — e.g. 14k mi while "this week"
 * only showed ~3k. The odometer the driver enters already includes past driving, so only
 * loads recorded **after** the reminder was created should add miles.
 */
object MaintenanceMilesFromLoads {

    data class LoadMiles(
        val tripId: String,
        val id: String,
        val date: String,
        val totalMiles: Double,
        val parsedAt: Long,
    )

    data class Result(
        val miles: Double,
        val loadCount: Int,
    )

    fun sumForTask(
        taskCreatedAt: Long,
        startDate: String,
        loads: List<LoadMiles>,
    ): Result {
        val seen = LinkedHashSet<String>()
        var miles = 0.0
        var count = 0
        for (load in loads) {
            if (load.parsedAt < taskCreatedAt) continue
            // Ignore journal rows with no usable date, and anything before the snapshot day.
            if (load.date.isBlank() || load.date < startDate) continue
            val key = load.tripId.ifBlank { load.id }
            if (!seen.add(key)) continue
            if (load.totalMiles <= 0.0) continue
            miles += load.totalMiles
            count++
        }
        return Result(miles = miles, loadCount = count)
    }
}
