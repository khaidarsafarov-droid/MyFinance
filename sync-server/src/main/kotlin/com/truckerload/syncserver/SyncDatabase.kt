package com.truckerload.syncserver

import java.sql.Connection
import java.sql.DriverManager

/**
 * CDC: синхронизация грузов. Проверка Trip ID в памяти, batch insert.
 * Уникальный индекс на trip_id предотвращает дубликаты.
 */
class SyncDatabase {
    private val connection: Connection by lazy {
        DriverManager.getConnection("jdbc:sqlite:sync_loads.db")
    }

    init {
        createTableIfNotExists()
    }

    private fun createTableIfNotExists() {
        connection.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS loads (
                    id TEXT PRIMARY KEY,
                    trip_id TEXT NOT NULL UNIQUE,
                    date TEXT NOT NULL,
                    total_rate REAL NOT NULL,
                    total_miles REAL NOT NULL,
                    point_a TEXT NOT NULL,
                    point_b TEXT NOT NULL,
                    pu_count INTEGER DEFAULT 0,
                    del_count INTEGER DEFAULT 0,
                    week_number INTEGER NOT NULL,
                    year INTEGER NOT NULL,
                    raw_message TEXT,
                    parsed_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    fun syncLoadsCdc(loads: List<LoadDto>, messageDateSeconds: Long?): SyncResult {
        val validLoads = loads.filter { dto ->
            dto.effectiveTripId().isNotBlank() && dto.effectiveTripId() != "T-UNKNOWN" &&
                (dto.effectivePointA().isNotBlank() || dto.effectivePointB().isNotBlank()) && dto.effectiveTotalRate() > 0
        }
        if (validLoads.isEmpty()) return SyncResult.Empty

        val tripIds = validLoads.map { it.effectiveTripId() }
        val existingIds = getExistingTripIds(tripIds).toSet()
        val toInsert = validLoads.filter { it.effectiveTripId() !in existingIds }
        if (toInsert.isEmpty()) return SyncResult.Duplicate

        val now = System.currentTimeMillis()
        var inserted = 0
        var lastText = ""

        connection.autoCommit = false
        try {
            val insertStmt = connection.prepareStatement("""
                INSERT OR IGNORE INTO loads (id, trip_id, date, total_rate, total_miles, point_a, point_b, pu_count, del_count, week_number, year, raw_message, parsed_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent())

            for (dto in toInsert) {
                val (weekNumber, year) = weekAndYearFromDate(dto.date, messageDateSeconds)
                val tid = dto.effectiveTripId()
                insertStmt.setString(1, tid)
                insertStmt.setString(2, tid)
                insertStmt.setString(3, dto.date)
                insertStmt.setDouble(4, dto.effectiveTotalRate())
                insertStmt.setDouble(5, dto.effectiveTotalMiles())
                insertStmt.setString(6, dto.effectivePointA())
                insertStmt.setString(7, dto.effectivePointB())
                insertStmt.setInt(8, 0)
                insertStmt.setInt(9, 0)
                insertStmt.setInt(10, weekNumber)
                insertStmt.setInt(11, year)
                insertStmt.setString(12, "")
                insertStmt.setLong(13, now)
                insertStmt.setLong(14, now)
                val rows = insertStmt.executeUpdate()
                if (rows > 0) {
                    inserted++
                    lastText = "${dto.effectiveTripId()} — ${dto.effectivePointA()} → ${dto.effectivePointB()}, $${String.format("%,.2f", dto.effectiveTotalRate())}"
                }
            }
            insertStmt.close()
            connection.commit()
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }

        return SyncResult.Success(inserted, lastText)
    }

    private fun getExistingTripIds(tripIds: List<String>): List<String> {
        if (tripIds.isEmpty()) return emptyList()
        val placeholders = tripIds.joinToString(",") { "?" }
        val sql = "SELECT trip_id FROM loads WHERE trip_id IN ($placeholders)"
        connection.prepareStatement(sql).use { stmt ->
            tripIds.forEachIndexed { i, id -> stmt.setString(i + 1, id) }
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<String>()
                while (rs.next()) result.add(rs.getString("trip_id"))
                return result
            }
        }
    }

    private fun weekAndYearFromDate(dateStr: String, messageDateSeconds: Long?): Pair<Int, Int> {
        if (messageDateSeconds != null) {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = messageDateSeconds * 1000
            return Pair(cal.get(java.util.Calendar.WEEK_OF_YEAR), cal.get(java.util.Calendar.YEAR))
        }
        if (dateStr.length < 10) {
            val cal = java.util.Calendar.getInstance()
            return Pair(cal.get(java.util.Calendar.WEEK_OF_YEAR), cal.get(java.util.Calendar.YEAR))
        }
        val parts = dateStr.split("-")
        if (parts.size != 3) {
            val cal = java.util.Calendar.getInstance()
            return Pair(cal.get(java.util.Calendar.WEEK_OF_YEAR), cal.get(java.util.Calendar.YEAR))
        }
        val y = parts[0].toIntOrNull() ?: return weekAndYearFromDate("", null)
        val m = parts[1].toIntOrNull()?.minus(1) ?: return weekAndYearFromDate("", null)
        val d = parts[2].toIntOrNull() ?: return weekAndYearFromDate("", null)
        val cal = java.util.Calendar.getInstance()
        cal.set(y, m, d)
        return Pair(cal.get(java.util.Calendar.WEEK_OF_YEAR), cal.get(java.util.Calendar.YEAR))
    }
}

sealed class SyncResult {
    data class Success(val addedCount: Int, val lastAddedText: String) : SyncResult()
    object Duplicate : SyncResult()
    object Empty : SyncResult()
}
