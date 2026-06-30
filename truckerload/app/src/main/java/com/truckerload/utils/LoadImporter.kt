package com.truckerload.utils

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LoadImporter {

    private val linePattern = Regex(
        """^\s*(?:\d+\.\s*)?(\d{2}\.\d{2}\.\d{4})\s*\|\s*(.+?)\s*(?:→|->)\s*(.+?)\s*\|\s*([\d,]+)\s*mi\s*\|\s*\$?\s*([\d,]+(?:\.\d+)?)\s*$""",
        RegexOption.IGNORE_CASE
    )

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val parsed: Int
    )

    data class ParsedLoadRow(
        val date: String,
        val pointA: String,
        val pointB: String,
        val totalMiles: Double,
        val totalRate: Double
    )

    fun parseExportText(text: String): List<ParsedLoadRow> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseLine(line) }
            .toList()
    }

    fun toLoads(rows: List<ParsedLoadRow>, rawSource: String = ""): List<Load> {
        val now = System.currentTimeMillis()
        return rows.mapIndexed { index, row ->
            val isoDate = toIsoDate(row.date)
            val tripId = buildTripId(isoDate, row.pointA, row.pointB, index)
            val load = Load(
                id = tripId,
                tripId = tripId,
                date = isoDate,
                totalRate = row.totalRate,
                totalMiles = row.totalMiles,
                pointA = row.pointA.trim(),
                pointB = row.pointB.trim(),
                puCount = 1,
                delCount = 1,
                weekNumber = 0,
                year = 0,
                rawMessage = rawSource,
                parsedAt = now,
                updatedAt = now,
                stops = listOf(
                    Stop(
                        id = 0,
                        loadId = tripId,
                        stopNumber = 1,
                        type = StopType.PU,
                        puNumber = null,
                        note = null,
                        scheduledTime = "",
                        timezone = "",
                        facilityCode = null,
                        fullAddress = row.pointA.trim(),
                        city = row.pointA.substringBefore(",").trim(),
                        state = row.pointA.substringAfter(",", "").trim(),
                        zip = ""
                    ),
                    Stop(
                        id = 0,
                        loadId = tripId,
                        stopNumber = 2,
                        type = StopType.DEL,
                        puNumber = null,
                        note = null,
                        scheduledTime = "",
                        timezone = "",
                        facilityCode = null,
                        fullAddress = row.pointB.trim(),
                        city = row.pointB.substringBefore(",").trim(),
                        state = row.pointB.substringAfter(",", "").trim(),
                        zip = ""
                    )
                ),
                penalties = emptyList()
            )
            val (weekNumber, year) = getLoadReportingWeek(load)
            load.copy(weekNumber = weekNumber, year = year)
        }
    }

    fun duplicateKey(load: Load): String = load.tripId.uppercase(Locale.US)

    suspend fun importFromText(loadRepository: LoadRepository, text: String): ImportResult =
        withContext(Dispatchers.IO) {
            val rows = parseExportText(text)
            if (rows.isEmpty()) return@withContext ImportResult(0, 0, 0)
            val loads = toLoads(rows, text.take(500))
            loadRepository.importLoadsIfNotDuplicate(loads, rows.size)
        }

    private fun parseLine(line: String): ParsedLoadRow? {
        if (line.startsWith("=") || line.startsWith("🚛") || line.startsWith("📦") ||
            line.startsWith("📊") || line.startsWith("Дата экспорта") ||
            line.startsWith("Всего рейсов") || line.startsWith("Общий доход") ||
            line.startsWith("Общие мили") || line.startsWith("Средний") ||
            line.startsWith("Средняя")
        ) {
            return null
        }
        val match = linePattern.matchEntire(line.replace("->", "→")) ?: return null
        val date = match.groupValues[1]
        val pointA = match.groupValues[2].trim()
        val pointB = match.groupValues[3].trim()
        val miles = parseNumber(match.groupValues[4])
        val rate = parseNumber(match.groupValues[5])
        if (pointA.isBlank() || pointB.isBlank() || miles <= 0 || rate <= 0) return null
        return ParsedLoadRow(date, pointA, pointB, miles, rate)
    }

    private fun parseNumber(raw: String): Double =
        raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    private fun toIsoDate(displayDate: String): String {
        val parts = displayDate.split(".")
        if (parts.size != 3) return displayDate
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }

    private fun buildTripId(isoDate: String, pointA: String, pointB: String, index: Int): String {
        val hash = "${isoDate}|${pointA}|${pointB}|$index"
            .uppercase(Locale.US)
            .hashCode()
            .toUInt()
            .toString(16)
            .uppercase(Locale.US)
        return "EXP-$hash"
    }
}
