package com.truckerload.domain.parser

import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck
import java.util.Locale

data class DuplicateAuditReport(
    val scannedLoads: Int,
    val deletedLoads: Int,
    val deletedPaychecks: Int,
    val deletedDiesel: Int,
    val deletedLoadTripIds: List<String>,
    val durationMs: Long,
)

class DuplicateAuditUseCase(
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository,
) {
    suspend fun auditAndRemove(): DuplicateAuditReport {
        val startTime = System.currentTimeMillis()
        val allLoads = loadRepository.getAllLoadsOnce()
        val loadIdsToDelete = findDuplicateLoadIds(allLoads)

        loadIdsToDelete.forEach { loadId ->
            loadRepository.deleteLoad(loadId)
        }

        val paycheckIdsToDelete = findDuplicatePaycheckIds(paycheckRepository.getAllPaychecksOnce())
        paycheckIdsToDelete.forEach { paycheckRepository.deletePaycheck(it) }

        val dieselIdsToDelete = findDuplicateDieselIds(dieselRepository.getAllDieselOnce())
        dieselIdsToDelete.forEach { dieselRepository.deleteDiesel(it) }

        val deletedTripIds = allLoads
            .filter { it.id in loadIdsToDelete }
            .map { it.tripId }
            .distinct()

        return DuplicateAuditReport(
            scannedLoads = allLoads.size,
            deletedLoads = loadIdsToDelete.size,
            deletedPaychecks = paycheckIdsToDelete.size,
            deletedDiesel = dieselIdsToDelete.size,
            deletedLoadTripIds = deletedTripIds,
            durationMs = System.currentTimeMillis() - startTime,
        )
    }

    internal fun findDuplicateLoadIds(loads: List<Load>): Set<String> {
        if (loads.size < 2) return emptySet()

        val toDelete = mutableSetOf<String>()
        val sorted = loads.sortedBy { it.parsedAt }

        sorted
            .groupBy { it.tripId.uppercase(Locale.US) }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val keeper = pickCanonicalLoad(group)
                group.filter { it.id != keeper.id }.forEach { toDelete += it.id }
            }

        val remaining = sorted.filter { it.id !in toDelete }
        remaining
            .groupBy { loadFingerprint(it) }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val keeper = pickCanonicalLoad(group)
                group
                    .filter { it.id != keeper.id }
                    .filter { candidate -> areDuplicateLoads(keeper, candidate) }
                    .forEach { toDelete += it.id }
            }

        val stillRemaining = sorted.filter { it.id !in toDelete }
        stillRemaining
            .groupBy { routeFingerprint(it) }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val keeper = pickCanonicalLoad(group)
                group
                    .filter { it.id != keeper.id }
                    .filter { candidate -> areDuplicateLoads(keeper, candidate) }
                    .forEach { toDelete += it.id }
            }

        return toDelete
    }

    internal fun findDuplicatePaycheckIds(paychecks: List<Paycheck>): Set<Int> {
        val toDelete = mutableSetOf<Int>()
        paychecks
            .groupBy { it.weekNumber to it.year }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val keeper = group.maxBy { it.addedAt }
                group.filter { it.id != keeper.id }.forEach { toDelete += it.id }
            }
        return toDelete
    }

    internal fun findDuplicateDieselIds(dieselEntries: List<Diesel>): Set<Int> {
        val toDelete = mutableSetOf<Int>()
        dieselEntries
            .groupBy { dieselFingerprint(it) }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val keeper = group.maxBy { it.addedAt }
                group.filter { it.id != keeper.id }.forEach { toDelete += it.id }
            }
        return toDelete
    }

    private fun pickCanonicalLoad(group: List<Load>): Load =
        group.minWith(
            compareBy<Load> { it.parsedAt }
                .thenByDescending { it.rawMessage.length }
                .thenBy { it.tripId.length },
        )

    private fun loadFingerprint(load: Load): String {
        if (load.stops.isEmpty() || load.date.isBlank()) return ""
        return "${StopsHasher.calculateStopsHash(load.stops)}|${load.date}"
    }

    private fun routeFingerprint(load: Load): String {
        val origin = load.firstPuCityState.ifBlank { load.pointA }.trim()
        val destination = load.lastDelCityState.ifBlank { load.pointB }.trim()
        if (origin.isBlank() || destination.isBlank() || load.date.isBlank()) return ""
        return "${origin.uppercase(Locale.US)}|${destination.uppercase(Locale.US)}|${load.date}"
    }

    private fun dieselFingerprint(entry: Diesel): String =
        listOf(
            entry.weekNumber.toString(),
            entry.year.toString(),
            entry.rawExtractedText.trim().lowercase(Locale.US),
            "%.2f".format(Locale.US, entry.totalAmount),
            entry.location.orEmpty().trim().lowercase(Locale.US),
        ).joinToString("|")

    private fun areDuplicateLoads(keeper: Load, candidate: Load): Boolean {
        if (keeper.id == candidate.id) return false
        if (keeper.tripId.equals(candidate.tripId, ignoreCase = true)) return true

        val comparison = compareLoads(keeper, candidate)
        if (comparison.isIdentical()) return true
        if (keeper.date == candidate.date && comparison.stopsHashMatch) return true

        val sameRoute = routeFingerprint(keeper) == routeFingerprint(candidate) && routeFingerprint(keeper).isNotBlank()
        return sameRoute && comparison.isIdentical()
    }
}
