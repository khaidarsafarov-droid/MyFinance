package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceArchiveEntry

/** One shop visit: several saved services that were entered (or scanned) together. */
data class MaintenanceVisit(
    val shopName: String,
    val serviceDate: String,
    val photoPath: String?,
    val createdAt: Long,
    val lines: List<MaintenanceArchiveEntry>,
) {
    val total: Double get() = lines.sumOf { it.amount }
    val ids: List<Long> get() = lines.map { it.id }
}

object MaintenanceVisits {
    fun group(entries: List<MaintenanceArchiveEntry>): List<MaintenanceVisit> =
        entries
            .groupBy { VisitKey(it.serviceDate, it.photoPath.orEmpty(), it.createdAt) }
            .map { (key, lines) ->
                val ordered = lines.sortedBy { it.id }
                MaintenanceVisit(
                    shopName = ordered.firstOrNull { it.serviceName.isNotBlank() }?.serviceName.orEmpty(),
                    serviceDate = key.date,
                    photoPath = ordered.firstOrNull { !it.photoPath.isNullOrBlank() }?.photoPath,
                    createdAt = key.createdAt,
                    lines = ordered,
                )
            }
            .sortedWith(compareByDescending<MaintenanceVisit> { it.serviceDate }.thenByDescending { it.createdAt })

    private data class VisitKey(
        val date: String,
        val photoPath: String,
        val createdAt: Long,
    )
}
