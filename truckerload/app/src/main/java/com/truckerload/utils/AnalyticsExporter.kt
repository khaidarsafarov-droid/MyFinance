package com.truckerload.utils

import android.content.Context
import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.utils.BrandConstants.DOWNLOADS_FOLDER
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object AnalyticsExporter {

    fun exportToCsv(context: Context, dashboard: AnalyticsDashboard, period: AnalyticsPeriod): Result<File> {
        return try {
            val dir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                DOWNLOADS_FOLDER
            )
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.failure(IllegalStateException("Cannot create export directory"))
            }
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            val file = File(dir, "analytics_${period.name.lowercase(Locale.US)}_$stamp.csv")
            file.bufferedWriter().use { out ->
                out.appendLine("Truck Log Analytics,${period.name}")
                out.appendLine()
                out.appendLine("Summary")
                val s = dashboard.summary
                out.appendLine("Total loads,${s.totalLoads}")
                out.appendLine("Total gross,${s.totalGross}")
                out.appendLine("Total miles,${s.totalMiles}")
                out.appendLine("Avg RPM,${"%.2f".format(Locale.US, s.avgRpm)}")
                out.appendLine("Avg per load,${"%.2f".format(Locale.US, s.avgGrossPerLoad)}")
                s.bestWeek?.let {
                    out.appendLine("Best week,W${it.weekNumber} ${it.year},${it.gross}")
                }
                out.appendLine()
                out.appendLine("Weekly revenue")
                out.appendLine("Week,Year,Gross,Miles,Loads")
                dashboard.weeks.forEach { w ->
                    out.appendLine("W${w.weekNumber},${w.year},${w.gross},${w.miles},${w.loadCount}")
                }
                out.appendLine()
                out.appendLine("Top routes")
                out.appendLine("Route,Gross,Miles,RPM,Loads")
                dashboard.routes.forEach { r ->
                    out.appendLine("\"${r.route}\",${r.gross},${r.miles},${"%.2f".format(Locale.US, r.rpm)},${r.loadCount}")
                }
                out.appendLine()
                out.appendLine("Daily distribution")
                out.appendLine("Day,Gross,Loads")
                dashboard.daily.forEach { d ->
                    out.appendLine("${d.dayLabel},${d.gross},${d.loadCount}")
                }
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
