package com.truckerload.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.WeekRepository
import kotlinx.coroutines.flow.first
import java.io.OutputStreamWriter

class ExportService(
    private val context: Context,
    private val weekRepository: WeekRepository,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository
) {
    /** Экспорт CSV в общедоступную папку Downloads. Возвращает SaveResult или null. */
    suspend fun exportCsvToStorage(year: Int): StorageHelper.SaveResult? {
        val weeks = (1..12).flatMap { month -> getWeeksInYear(month, year) }
            .distinctBy { "${it.weekNumber}-${it.year}" }
            .sortedWith(compareBy<com.truckerload.domain.model.WeekSummary> { it.year }.thenBy { it.weekNumber })
        val fileName = "TruckerLoad_$year.csv"
        val storageHelper = StorageHelper(context)
        return storageHelper.saveToPublicDownloads(fileName, "TruckerLoad", "text/csv") { out ->
            OutputStreamWriter(out, Charsets.UTF_8).use { w ->
                w.append("Week,Dates,Paycheck,Diesel,Loads,Miles,Gross,NetProfit\n")
                weeks.forEach { s ->
                    w.append("${s.weekNumber},${s.weekLabel},${s.paycheckAmount},${s.dieselAmount},${s.loadsCount},${s.totalMiles},${s.totalLoadRate},${s.netProfit}\n")
                }
                w.append("\nYEAR $year TOTAL\n")
                w.append(",,${weeks.sumOf { it.paycheckAmount }},${weeks.sumOf { it.dieselAmount }},${weeks.sumOf { it.loadsCount }},${weeks.sumOf { it.totalMiles }},${weeks.sumOf { it.totalLoadRate }},${weeks.sumOf { it.netProfit }}\n")
            }
        } ?: run {
            val file = storageHelper.saveToAppStorage(fileName, "exports") { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { w ->
                    w.append("Week,Dates,Paycheck,Diesel,Loads,Miles,Gross,NetProfit\n")
                    weeks.forEach { s ->
                        w.append("${s.weekNumber},${s.weekLabel},${s.paycheckAmount},${s.dieselAmount},${s.loadsCount},${s.totalMiles},${s.totalLoadRate},${s.netProfit}\n")
                    }
                    w.append("\nYEAR $year TOTAL\n")
                    w.append(",,${weeks.sumOf { it.paycheckAmount }},${weeks.sumOf { it.dieselAmount }},${weeks.sumOf { it.loadsCount }},${weeks.sumOf { it.totalMiles }},${weeks.sumOf { it.totalLoadRate }},${weeks.sumOf { it.netProfit }}\n")
                }
            }
            StorageHelper.SaveResult(storageHelper.getShareableUri(file), "TruckerLoad/$fileName")
        }
    }

    suspend fun exportCsv(year: Int): Uri? {
        return try {
            val result = exportCsvToStorage(year)
            result?.uri
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "exportCsv failed", e)
            null
        }
    }

    private suspend fun getWeeksInYear(month: Int, year: Int): List<com.truckerload.domain.model.WeekSummary> {
        return try {
            weekRepository.getWeeksInMonthSummaries(month, year)
        } catch (_: Exception) { emptyList() }
    }
}
